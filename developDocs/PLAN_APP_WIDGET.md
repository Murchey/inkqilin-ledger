# InkQilin Ledger - 桌面小部件（App Widget）功能规划

> **版本**: v1.6.0（规划目标版本，当前线上 1.5.1 / versionCode 3）
> **创建日期**: 2026-08-25
> **状态**: ✅ 已实现（v1.6.0，按新尺寸矩阵 2×2 / 4×2 / 4×4）

---

## 一、功能概述

为「墨麒麟记账」提供 Android 桌面小部件（App Widget），让用户**不打开 App**即可完成三个高频动作：**看一眼账、记一笔账、确认周期账单**。

### 1.1 小部件矩阵

| 小部件 | 默认尺寸（可缩放） | 分级 | 定位 |
|--------|--------------------|------|------|
| 📊 记账概览 | 2×2 / 4×4 | **Core（首版）** | 本月收支结余 + 预算进度 + 周期账单待办角标 + 一键记账入口 |
| ⚡ 快捷记账 | 2×2 / 4×2 | **Core（首版）** | 高频分类记账按钮（自动按最近使用排序），点击直达"记一笔"并预填分类 |
| 🧮 多功能计算器 | 2×2 / 4×4 | **已实现** | 一键直达定投、复利、个税、储蓄、分期等计算工具 |

### 1.2 设计原则

1. **纯展示 + 精确跳转**：小部件负责聚合展示（金额、进度、待办数），所有写操作一律跳转 App 内确认，不在桌面上直接落库（防误触）。
2. **零新依赖**：复用已有 Room / WorkManager / DataStore 栈，不引入新增运行时依赖（详见 4.2 技术选型）。
3. **隐私可控**：金额默认显示，支持一键全局隐藏（DataStore 开关）。
4. 圆角显示。

---

## 二、用户操作流程

### 2.1 添加小部件（系统侧，无需 App 内入口）

1. 桌面长按空白处 → 选择**【小组件 / Widgets】**
2. 下滑找到「墨麒麟记账」→ 选择「记账概览」或「快捷记账」
3. 拖入桌面后按当前网格自动适配尺寸（Android 12+ 依据 `targetCellWidth/Height` 对齐格子）

**首版不实现 configure 弹窗**（降低交付复杂度）：
- 首次添加即读取 DataStore 中的「全局小部件设置」渲染（默认显示金额、默认快捷键列表）
- 用户在 `设置 → 桌面小组件` 中的修改在**下一次刷新时全局生效**

> ⚠️ 权衡：Google 规定"设置类操作必须能直达 App"，i.e. 无 configure 的小部件需保证用户能在 App 内完成配置 → 设置页入口即为合规手段，见 2.3。

### 2.2 点击交互与路由映射

所有点击统一走 `WidgetClickReceiver` → 携带 `EXTRA_NAV_TARGET` 的 `PendingIntent` → `MainActivity` 解析并路由（机制见 4.6）。

| 小部件 / 区域 | 行为 | NAV_TARGET |
|---------------|------|------------|
| 概览 · 卡片整体 | 打开 App 首页 | `home` |
| 概览 · 「记一笔」 | 直达记一笔页（无预填） | `add_transaction` |
| 概览 · 周期账单待办角标 | 跳转周期账单列表页 | `cycle_bill_list` |
| 快捷记账 · 分类按钮 | 打开记一笔并**预填分类/类型** | `add_transaction?category=餐饮&type=EXPENSE` |
| 快捷记账 · 「随意记」 | 打开记一笔（无预填） | `add_transaction` |
| 计算器 · 功能格子 | 直达对应计算工具（定投/复利/个税/储蓄/分期/原理） | `calculator/{type}` |
| 计算器 · 整卡 | 打开计算器主页 | `calculator_hub` |

### 2.3 设置入口（SettingsScreen 新增区块）

```
┌────────────────────────────────┐
│ 桌面小组件（概览/快捷记账/计算器）│
├────────────────────────────────┤
│ ☑️ 小部件显示金额               │
│   说明: 关闭后概览仅显示"有收支" │
│                                │
│ 快捷记账小组件                  │
│   (自动显示最近使用的分类)      │
│                                │
│ [🔍 预览记账概览小部件]          │
│ [🔍 预览快捷记账小部件]          │
└────────────────────────────────┘
```

- 「预览」复用所选主题色渲染一张预览位图（App 内 Compose 手绘 Canvas 生成，不要截图），作为 `previewImage` 与设置页缩略图，一图两用。
- 设置持久化到 DataStore（扩展现有 `ThemeManager`，见 4.5）。

---

## 三、小部件 UI 原型（RemoteViews 布局）

> 工程约定：小部件不使用 Compose，全部为原生 XML RemoteViews 布局（决策见 4.2）。

### 3.1 记账概览 `widget_overview.xml`（2×2 默认，可缩放至 4×4）

```
┌─────────────────────────────────────────────┐
│  $ 墨麒麟记账                2026年7月 ▾     │   ← 标题行，月份角标
│                                             │
│  ▲ 收入 ¥12,345       ▼ 支出 ¥8,789        │   ← 收入绿 #34C759 / 支出橙 #FF9500
│  ─────────────────────────────────          │
│  结余 ¥3,556          预算 ████████░░ 68%   │   ← 预算进度条(自定义 drawable)
│                                             │
│  [ 记一笔 ]     [ 🔄 周期账单  ·2  ]        │   ← 待办角标(2 项到期)
└─────────────────────────────────────────────┘
```

- 4×4 竖版大尺寸：在上方追加「近 7 天支出 Top3 分类」小列表（Icon+分类名+金额）。
- 收/支金额颜色跟随 App 内 `incomeColor / expenseColor` 设置（onUpdate 时取色）。

### 3.2 快捷记账 `widget_quick_record.xml`（2×2 默认，可缩放至 4×2）

```
┌─────────────────────────────────────────────┐
│ 🍜餐饮   🚗交通   🛒购物   🎮娱乐   ✚随意记   │   ← 2 行平铺，每行 4 个
│                                            │
│ （4×2 时第二行：🏠居住  📦其他  ⚙️设置）      │
└─────────────────────────────────────────────┘
```

- 按钮图标 = 分类 Emoji（Category.icon），背景圆角色跟随主题主色。
- 按钮列表自动按**最近使用**排序（近 90 天支出分类优先），不足时依次用设置页勾选 / 全部分类补位。

### 3.3 多功能计算器 `widget_calculator.xml`（2×2 默认，可缩放至 4×4）

```
┌──────────────────────────────────────┐
│ 🧮 多功能计算器                       │
│ ┌──────┐ ┌──────┐ ┌──────┐          │
│ │📈定投│ │🔄复利│ │🧾个税│          │   ← 每格直达对应工具
│ └──────┘ └──────┘ └──────┘          │
│ ┌──────┐ ┌──────┐ ┌──────┐          │
│ │🎯储蓄│ │💳分期│ │📖原理│          │   ← 4×4 显示 6 个工具
│ └──────┘ └──────┘ └──────┘          │
└──────────────────────────────────────┘
```

- 路由：`calculator/{type}`（dca / compound_interest / income_tax / savings_goal / installment / math_docs）；整卡点击回计算器主页 `calculator_hub`。
- 2×2 只展示前 4 个高频工具（定投/复利/个税/储蓄）。
---

## 四、技术架构

### 4.1 总体架构图

```
┌──────────────────────────── 桌面 Launcher 进程 ───────────────────────────┐
│  AppWidgetHost 渲染 widget_overview.xml / widget_quick_record.xml 等        │
└──────────────────────────────────┬─────────────────────────────────────────┘
                                   │ RemoteViews 一次性下发
┌──────────────────────────────────▼─────────────────────────────────────────┐
│                 App 主进程（本应用，进程内可直读 Room）                       │
│                                                                             │
│  触发源：onUpdate / 时间变更 / 记账写库 / 周期账单 / 设置变更                 │
│         │                                                                   │
│         ▼                                                                   │
│  WidgetUpdater.refreshAll(appContext)  ←── 统一刷新入口（幂等）              │
│         │                                                                   │
│         ├──► DataLoader（goAsync + Dispatchers.IO）                          │
│         │        ├── Room TransactionDao：月度收支/预算/近7日Top分类          │
│         │        └── Room CycleBillDao：nextTriggerDate 升序列表              │
│         ├──► ThemeManager(@DataStore)：显示金额开关 / 快捷键 / 收支色          │
│         └──► RemoteViews 渲染 → AppWidgetManager.updateAppWidget()           │
│                                                                             │
│  点击 → WidgetClickReceiver → PendingIntent → MainActivity → NavController   │
└─────────────────────────────────────────────────────────────────────────────┘
```

小部件 Provider 运行在 **App 主进程**（Manifest 不声明 `android:process`），因此
可以直接复用 `AppDatabase.getDatabase()` 与 `ThemeManager`，**无需跨进程
ContentProvider/广播**。

### 4.2 技术选型：原生 RemoteViews（推荐） vs Glance

| 维度 | ✅ 原生 RemoteViews | Glance 1.1.1（备选） |
|------|--------------------|----------------------|
| 新增依赖 | 无（纯 Android framework） | `androidx.glance:glance-appwidget:1.1.1` |
| Compose 兼容风险 | **零**。小部件与 Compose 完全解耦 | 传递依赖较新 Compose runtime（~1.7），与当前 BOM 2024.06（Compose 1.6.x）及已知 Material3 组件二进制兼容问题**存在叠加风险** |
| 进度条 | 原生 `ProgressBar` + 自绘 drawable，成熟可靠 | 不支持 ProgressBar 组件，需 `Image` + 位图模拟，冗长 |
| 深色主题 | `layout-night` 双布局 or onUpdate 取色 | 声明式 + Android 12 `AppWidgetTheme`，更省事 |
| 多尺寸适配 | 手动监听 `getAppWidgetOptions` 切换布局 | `SizeMode.Responsive` 自动适配 |
| 性能 | 直接构造/传递 RemoteViews，最轻 | Glance 需编译 Compose 描述 → RemoteViews，额外开销 |
| 代码量 | XML + Provider，量稍大 | Kotlin DSL，量少 |

**结论**：本项目已有「避免 Material3 进度指示器 / 规避二进制兼容问题」的工程约定，
且控件多为自绘风格 → **采用原生 RemoteViews 方案**，将 Glance 作为"若需声明式
DSL 再用"的备选。此决定无需升级任何现有依赖。
### 4.3 核心组件详解

#### 4.3.1 `LedgerApplication`（新增，全局触发点）

```kotlin
class LedgerApplication : Application() {
    companion object {
        lateinit var instance: LedgerApplication
            private set
        fun refreshWidgets() { WidgetUpdater.refreshAll(instance) }
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        CycleBillWorker.scheduleDailyScan(this)   // 全局任务统一切入点
    }
}
```

- Manifest `<application android:name=".LedgerApplication">`。
- 将 Worker 等全局初始化下沉 Application，Database 仍在 Activity 按需懒加载（不动现有结构）。

#### 4.3.2 `BaseLedgerWidgetProvider` 基类 + 三个 Provider

```kotlin
abstract class BaseLedgerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context, appWidgetManager, appWidgetIds) {
        val result = goAsync()                    // 主线程回调内安全转异步
        CoroutineScope(Dispatchers.IO).launch {
            try { appWidgetIds.forEach { renderViews(context, appWidgetManager, it) } }
            finally { result.finish() }
        }
    }
    abstract fun renderViews(context, manager, appWidgetId)
    override fun onEnabled(context) { WidgetUpdater.refreshAll(context) }
    // onDeleted / onDisabled 预留空实现
}
```

| 类 | 布局 | 数据来源 |
|----|------|----------|
| `OverviewWidgetProvider` | `widget_overview.xml` | 月度收支/结余/预算/周期账单待办数 |
| `QuickRecordWidgetProvider` | `widget_quick_record.xml` | 快捷键分类列表，动态填充按钮 |
| `CycleBillWidgetProvider`（二期） | `widget_cycle_bill.xml` | `CycleBillDao` 列表 |

#### 4.3.3 `WidgetUpdater`（统一刷新入口，幂等）

```kotlin
object WidgetUpdater {
    fun refreshAll(context: Context) {
        val am = AppWidgetManager.getInstance(context)
        val providers = listOf(
            OverviewWidgetProvider::class.java, QuickRecordWidgetProvider::class.java
        )
        val ids = providers.flatMap { am.getAppWidgetIds(ComponentName(context, it)) }
        if (ids.isEmpty()) return                       // 未挂小部件时零开销
        context.sendBroadcast(
            Intent(context, WidgetClickReceiver::class.java).setAction(ACTION_WIDGET_REFRESH)
        )
    }
}
```

- 由 `WidgetClickReceiver` 处理 `ACTION_WIDGET_REFRESH` → 分发到各 Provider 的 onUpdate。
- **关键约定：写库事务成功后再调 `LedgerApplication.refreshWidgets()`**，见 4.4。

#### 4.3.4 `WidgetClickReceiver`（点击/刷新统一转发）

```kotlin
const val ACTION_WIDGET_NAV     = "com.inkqilin.ledger.widget.NAV"
const val ACTION_WIDGET_REFRESH = "com.inkqilin.ledger.widget.REFRESH"
const val EXTRA_NAV_TARGET      = "nav_target"       // 见 2.2 路由映射表
```

- 点击 → `PendingIntent.getActivity(context, code, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)`
  - intent: `MainActivity` + `action=ACTION_WIDGET_NAV` + `EXTRA_NAV_TARGET`
- RequestCode 用 `target.hashCode()` 避免溢出（与 `NotificationHelper` 同款做法）。
- RemoteViews 上：按钮逐个 `setOnClickPendingIntent`；列表容器 `setPendingIntentTemplate` + `setOnClickFillInIntent`。

#### 4.3.5 `WidgetSettings`（扩展现有 ThemeManager 的 DataStore）

```kotlin
private val WIDGET_SHOW_AMOUNT_KEY = booleanPreferencesKey("widget_show_amount")
private val WIDGET_QUICK_CATEGORIES_KEY = stringPreferencesKey("widget_quick_categories")  // "|||"拼接

val widgetShowAmount: Flow<Boolean> = context.dataStore.data.map { it[WIDGET_SHOW_AMOUNT_KEY] ?: true }
suspend fun setWidgetShowAmount(enabled: Boolean) { ... }
suspend fun setWidgetQuickCategories(list: List<String>) { ... }
```

- 默认 `true`（显示金额）；快捷分类默认「餐饮/交通/购物/娱乐」（用 `Category.icon` 作按钮）。
- 设置变更成功后调用 `LedgerApplication.refreshWidgets()` 即时生效。

#### 4.3.6 TransactionDao 新增同步聚合查询（供 onUpdate goAsync 使用）

```kotlin
@Query("""SELECT COALESCE(SUM(amount),0) FROM transactions
           WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end""")
suspend fun getExpenseSumByRangeSync(start: Long, end: Long): Double

@Query("""SELECT category, COALESCE(SUM(amount),0) AS total FROM transactions
           WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end
           GROUP BY category ORDER BY total DESC LIMIT :limit""")
suspend fun getTopExpenseCategoriesSync(start: Long, end: Long, limit: Int): List<CategoryTotal>
```

（income 同理；沿用既有 `...Sync` 命名风格。）
### 4.4 刷新触发机制

| # | 触发点 | 实现 | 说明 |
|---|--------|------|------|
| 1 | **记账增删改** | `TransactionViewModel.addTransaction/update/delete` 末尾调 `LedgerApplication.refreshWidgets()` | 主路径，秒级生效 |
| 2 | **周期账单生成/状态变更** | `CycleBillWorker`、`CycleBillBroadcastReceiver` 末尾追加 refresh | 账单进展实时反馈 |
| 3 | **周期任务兜底** | 复用既有周期 WorkManager（已每 6 小时调度），末尾追加 refresh | 防厂商杀进程丢刷新 |
| 4 | `updatePeriodMillis = 1_800_000`（30min） | Provider `onUpdate` 系统回调 | 系统级兜底（不保证精确） |
| 5 | **时间/时区变更 + 开机** | `ACTION_TIME_CHANGED / TIMEZONE_CHANGED / DATE_CHANGED / BOOT_COMPLETED` → Provider `onReceive` | 跨月后"本月"区间必须重算 |
| 6 | **设置页偏好变更** | `setWidgetShowAmount` 等成功后再 refresh | 隐私开关即时生效 |

**跨月边界**：7/31 → 8/1 时"本月"统计区间变化 → 由触发 5 保证。

### 4.5 布局与多尺寸适配

- **Android 12+（API 31+）**：`AppWidgetProviderInfo` 声明 `targetCellWidth / targetCellHeight`，直接映射网格格数。
- **Android 11 及以下**：`minWidth / minHeight` 兜底（经验换算 `dp ≈ 格数×70 − 30`）：

| 尺寸 | Android 12+ | Android 11- |
|------|-------------|-------------|
| 2×2（默认） | targetCellWidth=2, Height=2 | minWidth=110dp, minHeight=110dp |
| 4×2 | targetCellWidth=4, Height=2 | minWidth=250dp, minHeight=110dp |
| 4×4 | targetCellWidth=4, Height=4 | minWidth=250dp, minHeight=250dp |

- Provider 在 onUpdate 读 `getAppWidgetOptions(id).OPTION_APPWIDGET_MIN_WIDTH/HEIGHT` 判定实际尺寸 → 选择 small/big 两套 RemoteViews（如 4×2 概览 vs 4×4 概览）。
- `resizeMode="horizontal|vertical"`；两套布局 = `res/layout/`(默认) + `res/layout-large/`(大图)，或运行时 `setViewLayoutParams`，以最小成本达成。

### 4.6 深度跳转：MainActivity ↔ NavController 路由

MainScreen 目前内部 `rememberNavController()`，外部无法直接导航。方案：

1. `MainActivity` 持有 `MutableStateFlow<String?> widgetNavTarget`，`onCreate` 与 `onNewIntent` 解析：

```kotlin
private fun parseWidgetTarget(intent: Intent): String? {
    if (intent.action != WidgetClickReceiver.ACTION_WIDGET_NAV) return null
    return intent.getStringExtra(WidgetClickReceiver.EXTRA_NAV_TARGET)
}
```

2. 传入 `MainScreen(externalNavTarget = ...)`：

```kotlin
LaunchedEffect(target) {
    target?.let {
        navController.navigate(it) { launchSingleTop = true }
        onTargetHandled()          // 消费掉，避免重复导航
    }
}
```

3. Manifest `MainActivity` 设 `android:launchMode="singleTop"`，保证从桌面连续点按时只回前台不重建。

### 4.7 主题与深色模式

- **双布局**：`res/layout/` 与 `res/layout-night/` 各一份 widget 布局，颜色引用 `values/colors.xml` 与 `values-night/colors.xml`，由系统按当前 UI 模式解析，无需运行时判断。
- **运行时取色**：收/支金额颜色（`incomeColor / expenseColor`）、主按钮色（`customPrimaryColorHex`）→ onUpdate 里 `Color.parseColor` 后 `setTextColor / GradientDrawable.setColor`。
- **隐私模式**：`widgetShowAmount=false` → 金额统一替换为 `¥ •••`，图标/文案不变。

### 4.8 隐私与权限

| 项目 | 处理 |
|------|------|
| 新增普通权限 | **无**（不读通讯录/相册/通知，仅展示应用自身数据） |
| 新增系统权限 | 仅 `RECEIVE_BOOT_COMPLETED`（开机后刷新/重注册） |
| INTERNET | 已存在，小部件本身不发网络请求 |
| 通知权限 | 不受影响（周期账单 App 内逻辑不变） |
| 金额可见 | 数据仅在本机 Launcher 进程渲染；锁屏不展示（小部件不感知锁屏，敏感用户建议关闭"显示金额"） |
| DataStore | 快捷分类仅存分类名，无余额明文 |
---

## 五、改动文件清单

### 5.1 新增文件

| 路径 | 说明 |
|------|------|
| `java/.../LedgerApplication.kt` | 全局 Application，refreshWidgets 静态入口，Worker 调度统一切入点 |
| `java/.../widget/BaseLedgerWidgetProvider.kt` | Provider 基类（goAsync + IO + 生命周期钩子） |
| `java/.../widget/OverviewWidgetProvider.kt` | 记账概览 Provider |
| `java/.../widget/QuickRecordWidgetProvider.kt` | 快捷记账 Provider |
| `java/.../widget/CalculatorWidgetProvider.kt` | 多功能计算器入口 Provider |
| `java/.../widget/WidgetUpdater.kt` | 统一幂等刷新调度 |
| `java/.../widget/WidgetClickReceiver.kt` | 点击/刷新广播统一处理 + 常量定义 |
| `res/layout/widget_overview.xml`（+`layout-night/`） | 概览布局（small/big 两版） |
| `res/layout/widget_quick_record.xml`（+`layout-night/`） | 快捷记账布局 |
| `res/layout/widget_calculator.xml`（+`layout-night/`） | 计算器入口布局（small/big 两版） |
| `res/xml/overview_widget_info.xml` / `quick_record_widget_info.xml` / `calculator_widget_info.xml` | `AppWidgetProviderInfo` 元数据 |
| `res/drawable/widget_progress_track.xml` / `widget_progress_fill.xml` / `widget_bg.xml` / `widget_btn_bg.xml` | 进度条轨道/填充、卡片背景、按钮背景 |
| `res/mipmap-*/ic_widget_overview_preview.png` 等 | `previewImage`（设置页渲染导出，一图两用） |

### 5.2 修改文件

| 路径 | 改动 |
|------|------|
| `AndroidManifest.xml` | `android:name=".LedgerApplication"`；注册 3 个 `<receiver>`（含 `APPWIDGET_UPDATE` intent-filter + `appwidget-provider` meta-data）；`RECEIVE_BOOT_COMPLETED` 权限；MainActivity `launchMode="singleTop"` |
| `util/ThemeManager.kt` | 追加 `widgetShowAmount` / `widgetQuickCategories` 读写 |
| `data/TransactionDao.kt` | 追加月度收支/预算/Top 分类同步聚合查询 |
| `data/CycleBill.kt` | DAO 追加 `getWidgetBillsSync()`（概览小部件待办角标用） |
| `ui/TransactionViewModel.kt` | 记账增删改方法末尾接 `refreshWidgets` |
| `ui/screens/MainScreen.kt` | 支持 `externalNavTarget` 参数与消费逻辑 |
| `MainActivity.kt` | `onNewIntent` 解析 widget 导航目标 |
| `ui/screens/SettingsScreen.kt` | 「桌面小组件」设置区块 + 预览 |
| `worker/CycleBillWorker.kt` | `doWork` 末尾追加 refresh |

> **AppDatabase 不需要升级**（version 保持 17）：小部件只读现有表，配置放 DataStore，
> 不新增实体、不写 Migration。

---

## 六、待定点（评审确认）

- [ ] **技术路线**：确认采用原生 RemoteViews（不引入 Glance），避免 Compose 版本叠加风险
- [x] **首版范围**：已交付「记账概览 + 快捷记账 + 多功能计算器」三款（周期账单小部件按要求替换为计算器入口）
- [ ] **金额展示**：默认显示；是否需要「锁屏隐藏」行为？（小部件不感知锁屏，只能全局关）
- [ ] **逾期条目交互**：点击后仅跳转 App 内确认生成（推荐），还是桌面点击即生成？
- [ ] **快捷记账按钮分类**：由设置页勾选固定分类，还是支持长按 App 内自选任意分类？
- [ ] **previewImage 生成**：用 App 内 Canvas 渲染导图（推荐），还是临时手绘静态图？
- [ ] **深色适配**：`layout-night` 双布局 vs 运行时强制取色，全部都要还是仅双布局？
- [ ] **4×4 大尺寸内容**：概览大版本的「近7日 Top 分类」是否需要更多维度（如预算剩余天数）？

---

## 七、注意事项与风险

1. **RemoteViews 能力边界**：仅支持 Android framework View 的有限方法（`setText`、
   `setTextViewTextSize`、`setProgressBar`、`setInt(backgroundColor…)`、GradientDrawable 动态色等）。
   不支持自定义 View 类、不支持 Compose。所有"好看"都应在布局 XML + drawable + 有限 setInt 内完成。
2. **goAsync 必须成对**：`onUpdate` 内开协程后必须在 `finally { result.finish() }`，否则 ANR/WMS 警告。
3. **系统更新周期不可靠**：`updatePeriodMillis` 从 Android 5.0 起即不被精确保证
   （低频合批、节电），因此必须依靠 4.4 的多路触发，绝不能单靠系统轮询。
4. **厂商 ROM 差异**：MIUI/EMUI 等可能收紧后台广播与 WorkManager 频率 → 兜底链路
   （写库即刷新 + 30min 系统回调）更重要，`RECEIVE_BOOT_COMPLETED` 必须显式声明。
5. **跨月/跨日统计**：所有"本月/今日"聚合必须每次刷新重新计算区间
   （以 `Calendar.getInstance()` 为准），不能缓存"上个月结果"。
6. **尺寸判定时机**：Android 12 的 `getAppWidgetOptions` 在 `onAppWidgetOptionsChanged`
   回调中更新 → 该回调也要触发 re-render（基类补一个空实现并在 onReceive 中转发）。
7. **预览图分辨率**：`previewImage` 建议按 dpi 目录提供（mdpi~xxxhdpi），否则厂商桌面上预览模糊。
8. **Material3 / Compose 兼容约定**：小部件全部走 XML + framework，与现有 Compose UI
   完全隔离，天然满足「规避 Material3 进度条二进制兼容问题」的工程约定。
9. **代码编码**：涉及新增的资源字符串全部 UTF-8（与现有 `strings.xml` 一致）。

---

## 八、里程碑

| 阶段 | 内容 | 验收标准 |
|------|------|----------|
| M1 | 架构落地：Application + 基类 + WidgetUpdater + 路由打通 | 桌面添加"记账概览"→ 显示真实月度数据；点击跳转首页/记账页 |
| M2 | 快捷记账 + 设置页 + 隐私开关 + 深色模式 | 分类按钮直达预填；关闭显示金额后全改 `¥ •••`；夜间模式布局正确 |
| M3 | 刷新机制全覆盖（写库/周期/时间/兜底） | 记账后<1s 桌面更新；跨月自动切换统计区间 |
| M4 | 多功能计算器入口小部件 + 深浅色适配（layout-night + 动态取色）| 计算器直达可用；深/浅色布局均正确 |
| M5 | 发布准备 | `versionCode+1`、release 构建、真机（API 26/31/34）回归 |

> 目标里程碑归属版本：M1~M3 → v1.6.0；M4 → v1.7.0。