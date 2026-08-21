# InkQilin Ledger - 周期账单功能规划

> **版本**: v1.4.8  
> **创建日期**: 2026-08-20

---

## 一、功能概述

在首页底部 FAB 展开菜单中加入「周期账单」入口，进入后是一个列表页，以进度条展示每个周期事件的度过比例。支持自动或半自动生成账单，通过系统通知栏提醒用户。

---

## 二、用户操作流程

### 2.1 入口

1. 首页点击右下角 [+] FAB
2. 底部弹出 ModalBottomSheet 菜单（原为 OCR/资产管理/手动记账）
3. 新增「🔄 周期账单」选项，点击进入周期账单列表页

### 2.2 添加周期账单

1. 列表页右上角 [+] 或 FAB 点击新建
2. 弹窗填写：
   - 名称（如"房租""Spotify会员"）
   - 类型（支出/收入 Toggle）
   - 金额
   - 分类（下拉选现有或 inline 创建）
   - 周期（每日/每周/每月/每年）
   - 起始日（日历选择）
   - 提醒开关 + 提前时间（15min~1天）
   - 生成时机三选一：
     - 周期到期前自动生成（推荐）
     - 周期开始时生成
     - 仅提醒不生成（手动操作）
   - 可选：备注、独立颜色
3. 首次保存时顺带弹 Android 通知权限请求

### 2.3 列表页交互

| 操作 | 行为 |
|------|------|
| 正常态左滑 | [编辑][立即生成][暂停][移到回收站] |
| 逾期态左滑 | 同上 + 卡片上显示红色进度条 + "点击生成"按钮 |
| TabBar | [全部][即将到期(7天内)][已过期] 切换 |
| FAB | 悬浮在右下角，一键新建 |
| 空状态 | 引导文案 + 插画 |

### 2.4 设置面板

点击右上角 ⚙️ 弹出 Dialog：
```
┌──────────────────────┐
│  ⚙️ 周期账单设置      │
├──────────────────────┤
│  总开关:             │
│  [● 开启    ○ 关闭]  │
│                      │
│  默认提前提醒时间:    │
│  [15min ▼]           │
│                      │
│  [完成]              │
└──────────────────────┘
```

### 2.5 回收站

从左侧边栏或设置中进入：
```
┌──────────────────────┐
│  ♻️ 回收站         [清空]│
├──────────────────────┤
│  [全部][今天][近7天]  │
├──────────────────────┤
│  🏠 房租 ...已被回收于 │
│                    [恢复] │
└──────────────────────┘
```

---

## 三、技术架构

### 3.1 数据模型

#### CycleBill 实体

```kotlin
@Entity(tableName = "cycle_bills")
data class CycleBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val currency: String = "CNY",          // 固定默认币种
    val cycleType: CycleType,               // DAILY / WEEKLY / MONTHLY / YEARLY
    val startDate: Long,                    // 首次生效时间戳
    val enabled: Boolean = true,
    val reminderEnabled: Boolean = true,
    val advanceMinutes: Int = 60,           // 单条提前提醒分钟数
    val generationMode: GenerationMode,     // AUTO_BEFORE / AUTO_START / NOTIFY_ONLY
    val note: String = "",
    val colorHex: Int? = null
)

enum class CycleType { DAILY, WEEKLY, MONTHLY, YEARLY }
enum class GenerationMode { AUTO_BEFORE, AUTO_START, NOTIFY_ONLY }
```

#### RecycledCycleBill 实体

与 CycleBill 完全相同结构，额外增加 `recycleTime: Long` 字段。

#### NotificationLog 实体

```kotlin
@Entity(
    tableName = "notification_log",
    primaryKeys = ["cycleBillId", "logDate"],
    indices = [Index(value = ["cycleBillId", "logDate"])]
)
data class NotificationLog(
    val cycleBillId: Long,
    val logDate: String            // "yyyy-MM-dd"
)
```

#### Transaction 表扩展

```kotlin
// 在现有 Transaction 数据类中追加：
val cycleBillId: Long? = null   // 非 null 表示由周期自动生成
```

### 3.2 通知机制

**底层实现**：AlarmManager.setExactAndAllowWhileIdle + BroadcastReceiver + NotificationManager.notify()

**单条周期独立配置**：每条周期可单独开启/关闭提醒，设定提前时间。

**系统通知样式**：
```
标题: 墨麒麟记账 - 房租到期提醒
内容: 您的房租 ¥3000 将在1小时后到期，请确认是否生成账单。
动作: [打开 App] [稍后提醒]
点击通知 → 跳转到对应 Transaction 编辑页
```

### 3.3 触发逻辑

```
[每天凌晨 0:00 WorkManager PeriodicWorker]
        │
        ▼
扫描所有 enabled 的 CycleBill
        │
        ├── 计算当前周期结束时间
        ├── 判断: 是否应该触发?
        │       │
        │       ├── YES → 检查 notification_log
        │       │         │
        │       │         ├── 今天已发过 → 跳过
        │       │         │
        │       │         └── 没发过 → 
        │       │               │
        │       │               ├── generationMode == AUTO_BEFORE/AUTO_START
        │       │               │       → 生成 Transaction (带上 cycleBillId)
        │       │               │       → 写入 notification_log
        │       │               │       → 发送系统通知
        │       │               │
        │       │               └── generationMode == NOTIFY_ONLY
        │       │                       → 只写 notification_log
        │       │                       → 发送系统通知（需手动操作）
        │       │
        │       └── NO → 不处理
```

### 3.4 App 启动补偿

Application.onCreate() 或 MainActivity.onStart() 中：

```kotlin
fun checkTodayNotifications() {
    val today = SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
    
    val dueBills = cycleBillDao.getDueByToday()
    
    for (bill in dueBills) {
        val alreadySent = notificationLogDao.exists(bill.id, today)
        if (!alreadySent) {
            triggerNotification(bill)   // 发送 + 写日志
        }
    }
}
```

保证用户关掉 App 期间错过的提醒，打开就补齐。

### 3.5 周期计算逻辑

```
currentCycleEnd = startDate + n * cycleDuration
where n = ceil((today - startDate) / cycleDuration)

progress = min(1.0, (today - currentCycleStart) / cycleDuration)
```

举例：月周期，startDate=2026-07-15，today=2026-08-12
- currentCycleStart = 2026-08-01（按月对齐到最近起点）
- currentCycleEnd = 2026-09-01
- progress = 12/31 ≈ 39%

---

## 四、页面结构

### 4.1 周期账单列表页 (CycleBillScreen)

```
┌─────────────────────────────────┐
│  ⚙️ 周期账单              [+]   │  ← AppBar
├─────────────────────────────────┤
│  [全部] [即将到期] [已过期]      │  ← TabBar (BouncyTabItem)
├─────────────────────────────────┤
│  ┌───────────────────────────┐  │
│  │ 🏠 房租           💸¥3000 │  │
│  │ ████████████░░░░ 75%      │  │  ← LinearProgressIndicator
│  │ 下次生成: 2026-09-01      │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ 💧 水电            🔴逾期  │  │  ← 进度条变红
│  │ ███░░░░░░░░░░░░░░░ 30%    │  │     "点击生成"按钮
│  │ 上次过期: 2026-08-01      │  │
│  └───────────────────────────┘  │
├─────────────────────────────────┤
│                                   │
│              [+ FAB]              │
└─────────────────────────────────┘
```

### 4.2 新建/编辑弹窗 (CycleBillDialog)

```
┌──────────────────────────────────┐
│  ✏️ 新建周期账单        [🗑删除]   │
├──────────────────────────────────┤
│  名称    [___________]           │
│  类型    [💸 支出]  [💵 收入]    │
│  金额    [_______]               │
│  分类    [下拉 ▼ 或创建新分类]    │
│  周期    [每月 ▼]                │
│  起始日  [2026-08-15]            │
│                                      │
│  ── 提醒设置 ──                     │
│  ☑️ 开启提醒                        │
│  提前: [1小时 ▼]                   │
│                                      │
│  ── 生成时机 ──                     │
│  (●) 周期到期前自动生成(推荐)        │
│  (○) 周期开始时生成                  │
│  (○) 仅提醒不生成                    │
│                                      │
│  备注    [____________]             │
│  颜色    [自定义色块 ▼]             │
│                                      │
│  [取消]         [保存]              │
└──────────────────────────────────┘
```

### 4.3 回收站 (RecycleBinScreen)

同上 2.5 节。

---

## 五、改动文件清单

| 文件路径 | 操作 | 说明 |
|----------|------|------|
| `data/Transaction.kt` | 改 | 加 `cycleBillId: Long?` |
| `data/TransactionDao.kt` | 改 | 迁移 + 新查询方法 |
| **`data/CycleBill.kt`** | **新增** | 实体 + DAO |
| **`data/NotificationLog.kt`** | **新增** | 通知去重实体 + DAO |
| **`utils/NotificationHelper.kt`** | **新增** | 系统通知封装 |
| **`worker/CycleBillWorker.kt`** | **新增** | WorkManager 定时任务 |
| `ui/screens/MainScreen.kt` | 改 | FAB→展开菜单; 加周期账单入口和路由 |
| **`ui/screens/CycleBillScreen.kt`** | **新增** | 列表页 UI |
| **`ui/screens/CycleBillDialog.kt`** | **新增** | 新建/编辑弹窗 |
| `ui/screens/SettingsScreen.kt` | 改 | 加周期账单设置入口 |
| **`ui/CycleBillViewModel.kt`** | **新增** | ViewModel + Repository |
| `AppDatabase.kt` | 改 | 版本号 + Migration |

---

## 六、待定点（全部确认完毕）

- [x] FAB 行为：直接跳转 → 展开 ModalBottomSheet
- [x] 提醒方式：统一系统通知
- [x] 通知权限：在设置周期账单弹窗内顺带弹请求
- [x] 排序：按下次生成时间升序
- [x] 逾期处理：红色进度条 + "点击生成"按钮
- [x] 交互：左滑菜单，不做长按弹出
- [x] 回收站：保留，支持恢复

---

## 七、注意事项

1. **工程约定**: 避免使用 Material3 的 CircularProgressIndicator 或 LinearProgressIndicator（已知二进制兼容性问题），改用文本 ProgressBar 或 Canvas 绘制
2. **编码问题**: 参考支付宝 CSV 经验，若涉及外部数据导入需注意 GBK/UTF-8
3. **包体积**: 移除 material-icons-extended，使用 Icons.Default 中的标准图标
4. **Room 迁移**: 需处理枚举类序列化（Room 默认存名而非数字）
