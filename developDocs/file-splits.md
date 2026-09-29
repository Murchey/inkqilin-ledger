# 文件拆分记录

命名约定：文件 `PascalCase.kt`，变量 `camelCase`，函数 `PascalCase`（Composable 与公开函数）。

---

## NotificationParser.kt

- **原文件路径**: `app/src/main/java/com/inkqilin/ledger/service/NotificationParser.kt`
- **拆分日期**: 2026-09-29
- **原大小**: 20.6KB / 414 行

### 新文件列表

| 新文件 | 职责 |
|--------|------|
| `service/parser/ParsedNotification.kt` | 解析结果数据模型 |
| `service/parser/NotificationKeywords.kt` | 收支/广告/强信号关键词与金额正则 |
| `service/parser/NotificationParseSupport.kt` | 共用过滤、分类、商户提取、收入/支出/兜底层 |
| `service/parser/AlipayParser.kt` | 支付宝渠道解析 |
| `service/parser/WeChatParser.kt` | 微信支付渠道解析 |
| `service/parser/UnionPayParser.kt` | 云闪付渠道解析 |
| `service/parser/GenericPaymentParser.kt` | 通用/系统通知解析 |
| `service/parser/NotificationParser.kt` | 门面 `Parse()`，按包名分发 |

### 包结构调整

- 新增包 `com.inkqilin.ledger.service.parser`
- 删除 `com.inkqilin.ledger.service.NotificationParser`
- 调用点 `NotificationCaptureService` 改为 `service.parser.NotificationParser.Parse(...)`

### 验证

- `compileDebugKotlin` BUILD SUCCESSFUL
- 解析规则与关键词集合未改语义

---

## HomeScreen.kt

- **原文件路径**: `app/src/main/java/com/inkqilin/ledger/ui/screens/HomeScreen.kt`
- **拆分日期**: 2026-09-29
- **原大小**: 82.5KB / 1771 行

### 新文件列表

| 新文件 | 职责 |
|--------|------|
| `HomeScreen.kt` | 首页主界面编排（月切换/列表/滚动） |
| `HomeStyle.kt` | `frostedGlass` 修饰符（首页/统计共用） |
| `HomeAnalytics.kt` | `CalculateFinancialScore` / `DetectAnomalies` / `BuildPeriodSummary` 等纯函数与 `HomeData` 模型 |
| `HomeOverviewCards.kt` | 单币种/多币种总览卡 + Skeleton |
| `HomeScoreCard.kt` | `FinancialScoreCard` / `ScoreDetailRow` |
| `HomeAnomalyAlerts.kt` | 消费提醒卡（规则 + AI） |
| `EditTransactionDialog.kt` | 编辑账单弹窗 |
| `HomeScreenPreview.kt` | Compose Preview |

### 包结构调整

- 仍为 `ui.screens` 同包拆文件，对外 `HomeScreen` / `EditTransactionDialog` 签名不变
- 跨文件函数改为 `internal`，并按约定使用 PascalCase 函数名
- `StatisticsScreen` 内重复的 `private frostedGlass` 删除，改用 `HomeStyle` 共用实现

### 验证

- `compileDebugKotlin` BUILD SUCCESSFUL

---

## CycleBillScreen.kt

- **原文件路径**: `app/src/main/java/com/inkqilin/ledger/ui/screens/CycleBillScreen.kt`
- **拆分日期**: 2026-09-29
- **原大小**: 48.2KB / 967 行

### 新文件列表

| 新文件 | 职责 |
|--------|------|
| `CycleBillMath.kt` | `CycleFilter` / `FormatAmount` / `CalculateProgress` / `CycleBoundary` / `ComputeCycleRange` 等 |
| `CycleBillScreen.kt` | 周期账单列表主界面 |
| `CycleBillEditScreen.kt` | 新增/编辑账单 + `TimeWheel` |
| `CycleBillComponents.kt` | `CycleBillCard` / `BillListView` / `EmptyCycleBillState` / `BouncyTabItem` |
| `CycleBillSettingsPanel.kt` | 设置面板 + `AdvanceTimePicker` |
| `RecycleBinScreen.kt` | 回收站 |

### 包结构调整

- 同包 `ui.screens` 拆文件；`MainScreen` 中 `cycleBoundary` 调用改为 `CycleBoundary`
- 跨文件组件 `TimeWheel` / `AdvanceTimePicker` 为 `internal`

### 验证

- `compileDebugKotlin` BUILD SUCCESSFUL

---

## AssetManagementScreen.kt

- **原文件路径**: `app/src/main/java/com/inkqilin/ledger/ui/screens/AssetManagementScreen.kt`
- **拆分日期**: 2026-09-29
- **原大小**: 49.5KB / 1155 行

### 新文件列表

| 新文件 | 职责 |
|--------|------|
| `AssetManagementScreen.kt` | 资产列表主界面 |
| `AssetFormatUtils.kt` | 金额/日期格式化、`currencySymbolOf`、`AssetSortMode` |
| `AssetCard.kt` | 资产卡片 + `iconForAssetType` |
| `AssetEditDialog.kt` | 资产编辑弹窗 |
| `AssetFlowScreen.kt` | 资产流转明细 |
| `AssetSummary.kt` | 汇总卡 / 趋势图 |
| `AssetFlowItems.kt` | 流转条目 `FlowItem` |
| `AssetFlowEditDialog.kt` | 流转编辑弹窗 |

### 验证

- `compileDebugKotlin` BUILD SUCCESSFUL

---

## CloudBackupScreen.kt

- **原文件路径**: `app/src/main/java/com/inkqilin/ledger/ui/screens/CloudBackupScreen.kt`
- **拆分日期**: 2026-09-29
- **原大小**: 47.7KB / 1108 行

### 新文件列表

| 新文件 | 职责 |
|--------|------|
| `CloudBackupScreen.kt` | 备份页主界面编排 |
| `CloudBackupModels.kt` | `BackupUiState` / `RestoreConfirm` / `PendingBackup` |
| `LocalBackupSection.kt` | 本地备份区 |
| `CloudBackupSection.kt` | 云端备份区 |
| `CosSettingsDialog.kt` | COS 配置弹窗 |
| `BackupPasswordDialog.kt` | 备份密码弹窗 |
| `AutoBackupScheduleCard.kt` | 自动备份计划卡片 |

### 验证

- `compileDebugKotlin` BUILD SUCCESSFUL

---

## MainScreen.kt

- **原文件路径**: `app/src/main/java/com/inkqilin/ledger/ui/screens/MainScreen.kt`
- **拆分日期**: 2026-09-29
- **原大小**: 54.6KB / 1090 行

### 新文件列表

| 新文件 | 职责 |
|--------|------|
| `MainScreen.kt` | 导航壳 + Scaffold + 底栏 + NavHost 路由（暂留，交叉引用密集） |
| `NavExtensions.kt` | `NavController.navigateSingle` |

### 说明

- 周期账单生成处已改为 `CycleBoundary`（来自 `CycleBillMath.kt`）
- NavHost 路由表与顶栏回调耦合较紧，本阶段只抽出导航扩展；后续可再拆 `MainNavGraph`

### 验证

- `compileDebugKotlin` BUILD SUCCESSFUL
