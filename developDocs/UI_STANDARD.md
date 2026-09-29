# 墨麒麟记账 UI 设计规范

> 适用范围：Kotlin + Jetpack Compose + Material3  
> 风格锚点：Apple HIG（圆角卡片、毛玻璃、克制阴影）+ 记账产品信息密度  
> 目标：页面可拼装、组件可复用、亮暗主题一致

---

## 1. 设计原则

1. **本地优先、信息清晰**：金额、日期、分类一眼可读，装饰不抢内容。
2. **一套字阶、一套间距、一套圆角**：禁止在页面里临时发明 `16.5.dp` / `13.5.sp`。
3. **组件先于页面**：列表项、卡片、弹窗、按钮走公共组件，避免每页复制粘贴。
4. **主题可切换**：只使用 `MaterialTheme.colorScheme` 与主题色 token，禁止写死亮色 hex（功能色除外，见 §3）。
5. **动效克制**：进出场 ≤ 300ms；交互反馈优先 `pressScale` / `MotionSprings`，避免大面积视差。

---

## 2. 字体 Typography

字体族：`FontFamily.Default`（系统 SF / 思源黑体等）。  
**禁止**在业务代码里直接写 `fontSize = 17.sp`，应使用 `MaterialTheme.typography.*`；仅金额大数字、空状态插画等特殊场景可按 §2.3 例外。

### 2.1 字阶（与 `ui/theme/Type.kt` 对齐）

| Token | 字号 | 行高 | 字重 | 用途 |
|-------|------|------|------|------|
| `displayLarge` | 34sp | 41sp | Bold | 首页总览大数字、关键 KPI |
| `headlineLarge` | 28sp | 34sp | Bold | 页面主标题（少用） |
| `headlineMedium` | 22sp | 28sp | SemiBold | 弹窗大标题、统计页章节 |
| `titleLarge` | 22sp | 28sp | SemiBold | 页标题（顶栏） |
| `titleMedium` | 17sp | 22sp | SemiBold | 卡片标题、分组标题 |
| `titleSmall` | 15sp | 20sp | Medium | 列表主文案、按钮文字 |
| `bodyLarge` | 17sp | 22sp | Normal | 正文段落 |
| `bodyMedium` | 15sp | 20sp | Normal | 列表副文案 |
| `bodySmall` | 13sp | 18sp | Normal | 辅助说明、时间戳 |
| `labelLarge` | 15sp | 20sp | Medium | 主操作按钮 |
| `labelMedium` | 13sp | 18sp | Medium | Chip、次按钮、Tab |
| `labelSmall` | 11sp | 13sp | Medium | 角标、徽章、极次要标签 |

### 2.2 使用约定

| 场景 | 样式 |
|------|------|
| 顶栏标题 | `titleLarge` |
| 卡片标题 | `titleMedium` |
| 列表标题 / 副标题 | `titleSmall` + `bodySmall`（副） |
| 设置项 | 标题 `bodyLarge` / `titleSmall`，说明 `bodySmall`，色用 `onSurfaceVariant` |
| 金额（列表） | `titleMedium` 或 `titleSmall`，色：收入/支出/中性 |
| 金额（总览卡） | `displayLarge` 或 `headlineLarge` |
| 按钮 | `labelLarge` |
| Tab / Chip | `labelMedium` |
| 空状态说明 | `bodyMedium`，居中 |

### 2.3 金额与数字

- 列表金额：右对齐，等宽感靠 `FontWeight.SemiBold` + 固定小数位（`%.2f`）。
- 总览金额：可加大到 `displayLarge`，货币符号可用 `bodyLarge`/`titleMedium` 降一级。
- 禁止同一页面混用两种小数格式；统一 `¥1,234.56` 或 `¥1234.56` 一种。

---

## 3. 颜色 Color

### 3.1 品牌 / 主色

| Token | 值 | 说明 |
|-------|-----|------|
| `LightDefaultPrimary` | `#2E9E6A` | 默认「青松」主色（亮色） |
| `DarkDefaultPrimary` | `#4FAF82` | 暗色下提亮，保证对比 |

用户可在设置中换主题色；组件一律 `MaterialTheme.colorScheme.primary`。

### 3.2 中性色（亮 / 暗）

| 角色 | Light | Dark |
|------|-------|------|
| Background | `#F5F5F7` | `#0B0B0F` |
| Surface | `#FFFFFF` | `#111318` |
| Secondary Surface | `#EFEFF4` | `#161A22` |
| On Surface | `#1D1D1F` | `#FFFFFF` |
| On Surface Variant | `#6E6E73` | `#FFFFFF` @ 65% |
| Outline | `#D1D1D6` | `#2E2F32` |
| Frosted | `#F2F2F7` | `#1C1C1E` |
| Frosted Border | `#D1D1D6` | `#38383A` |

### 3.3 功能色（收支语义，可跨主题固定）

| 语义 | 色值 | 用途 |
|------|------|------|
| 收入 / 成功 | `#34C759` | 收入金额、正向变化 |
| 支出 / 危险 | `#FF3B30` | 支出金额、删除 |
| 警告 | `#FF9F0A` | 预算超支、逾期 |
| 信息 / 链接 | `#007AFF` | 可点链接、次强调 |
| 强调紫 | `#AF52DE` | 图表序列 |
| 强调粉 | `#FF2D55` | 图表序列 |
| 强调青 | `#5AC8FA` | 图表序列 |
| 强调靛 | `#5856D6` | 图表序列 |

### 3.4 用色规则

- 正文：`onSurface`；辅助：`onSurfaceVariant`；分割线：`outline` / `HorizontalDivider`。
- 卡片底：`surface` 或透明 + `frostedGlass`，二者不叠多层阴影。
- 图表：优先功能色序列，同一图表不超过 6 色。
- **禁止** `Color.Red` / `Color.Blue` 等 Compose 预置色直接用于业务 UI。

---

## 4. 间距 Spacing

采用 **4dp 网格**。常用值：

| Token | 值 | 用途 |
|-------|-----|------|
| `Space.Xxs` | 2.dp | 图标与角标、极紧贴 |
| `Space.Xs` | 4.dp | 行内微间距、Chip 内 |
| `Space.Sm` | 8.dp | 图标与文字、列表项内行距 |
| `Space.Md` | 12.dp | 表单控件间距、卡片内组间距 |
| `Space.Lg` | 16.dp | **页面水平边距**、卡片外距 |
| `Space.Xl` | 20.dp | 卡片内边距（紧凑卡） |
| `Space.Xxl` | 24.dp | **区块间距**、卡片内边距（标准） |
| `Space.Xxxl` | 32.dp | 大分区间、弹窗上下 |

### 4.1 页面骨架

| 区域 | 规范 |
|------|------|
| 水平边距 | `16.dp`（内容区） |
| 列表项 | 左右 `16.dp`，上下 `8–12.dp` |
| 分组间距 | `24.dp` |
| 顶栏下沿到内容 | `8–12.dp` |
| 底栏 / FAB 避让 | `navigationBars` + 额外 `16–24.dp` |
| 弹窗内容 | `24.dp` 水平，`16.dp` 垂直，底部 `32.dp` |

### 4.2 组件内间距

| 组件 | 内边距 |
|------|--------|
| 卡片（标准） | `20–24.dp` |
| 卡片（列表紧凑） | `16–20.dp` |
| ListItem | 跟随 Material3 默认，自定义时 `16.dp` 水平 |
| 按钮 | 水平 `16–24.dp`，高度 `40–48.dp` |
| Chip | 水平 `12.dp`，高 `32.dp` |
| 图标 + 文字 | `8.dp` |

---

## 5. 圆角与形状 Shapes

与 `ui/theme/Theme.kt` 的 `AppShapes` 一致：

| Token | 圆角 | 用途 |
|-------|------|------|
| `extraSmall` | 8.dp | 输入框内块、小标签、进度条端 |
| `small` | 12.dp | 输入框、小卡片、缩略图 |
| `medium` | 18.dp | **默认卡片**、设置分组卡 |
| `large` | 24.dp | 总览大卡、毛玻璃主卡、BottomSheet 顶 |
| `extraLarge` | 32.dp | 特殊全宽英雄卡 |

- 底部弹窗：`topStart = 20.dp, topEnd = 20.dp`（与现有 `ModalBottomSheet` 一致）。
- 弹窗按钮条：可用 `RoundedCornerShape(14.dp)` 可点击区。
- **禁止**同屏出现 3 种以上卡片圆角。

---

## 6. 阴影与毛玻璃

| 策略 | 规范 |
|------|------|
| 默认卡片 | `CardDefaults.cardElevation(0.dp)`，无阴影 |
| 选中/浮起 | 可用 `1–4.dp` elevation，极少用 |
| 毛玻璃 | `frostedGlass(shape, isDark)`：底 `FrostedLight/Dark` + 0.5.dp 边框 |
| 按钮 | `appButtonElevation()`（全 0，靠色面与 press 反馈） |

---

## 7. 组件规范

### 7.1 按钮

| 类型 | 规格 | 用法 |
|------|------|------|
| Primary | 高 48dp，`primary` 底，`onPrimary` 字，`labelLarge`，圆角 12–18 | 主操作（保存、确定） |
| Secondary / Tonal | 高 40–48dp，`primaryContainer` 或描边 | 次操作 |
| Text | 无底，`primary` 字 | 顶栏文字钮、展开 |
| Icon | 48dp 触控区，图标 24dp | 顶栏、列表尾 |
| FAB | 56dp，`primary`，图标 24dp | 记一笔 |
| 危险 | 文/图标用 `#FF3B30` 或 `error` | 删除、清空 |

- 按钮文字：`labelLarge`；禁用态降透明度 0.38。
- 同一行主次按钮：主右次左（或 iOS 风格对话框双列）。

### 7.2 卡片 Card

- 默认 `medium`（18dp）圆角，`surface` 底，无阴影。
- 大型总览：`large`（24dp）+ `frostedGlass`。
- 可点卡片：`pressScale` + `interactionSource`，整卡可点或仅明确热区。
- 列表卡之间竖距 `8–12.dp`。

### 7.3 列表与列表项

| 元素 | 规范 |
|------|------|
| 列表 | `LazyColumn`，`contentPadding` 处理底栏避让 |
| 交易行 | 左图标块 40–48dp，中标题 `titleSmall` + 副文案 `bodySmall`，右金额 `titleMedium` |
| 分组头 | 日期/标题 `labelMedium` 或 `titleSmall`，`onSurfaceVariant` |
| 滑动菜单 | 展开宽 80–120dp，阻尼与回弹用 `MotionSprings.interactive()` |
| 分割 | `HorizontalDivider`，水平 `16.dp`，或靠间距分隔 |

### 7.4 输入

| 组件 | 规范 |
|------|------|
| 文本框 | `OutlinedTextField`，圆角 12dp，高 ≥ 48dp |
| 标签 | 组件自带或上置 `labelSmall` |
| 金额 | 右对齐、`KeyboardType.Decimal` 或自研 `AmountKeypad` |
| 开关 | `Switch`，说明用 `bodySmall` |
| Chip | `FilterChip` / `InputChip`，高 32dp，`labelMedium` |

### 7.5 弹窗 Dialog / BottomSheet

| 类型 | 规范 |
|------|------|
| 确认框 | `AppleAlertDialog` 或 M3 `AlertDialog`，标题 `titleMedium`，按钮 `labelLarge` |
| 表单弹窗 | 宽 ≈ 屏宽 − 32dp，内容 `verticalScroll`，按钮固定底部 |
| BottomSheet | 顶角 20dp，拖拽条 36×4dp，底部安全区 ≥ 32dp |
| 日期选择 | `AppleDatePickerDialog` / `DatePickerDialog` |

### 7.6 顶栏与导航

| 元素 | 规范 |
|------|------|
| 顶栏高 | 默认 M3 TopAppBar |
| 标题 | `titleLarge`，单行省略 |
| 返回 | 系统自动镜像箭头 `AutoMirrored` |
| 底栏 | 浮动玻璃 Tab，图标 24dp + `labelSmall` |
| 二级页 | 顶栏标题 + 返回，避免重复「首页」类标题 |

### 7.7 空状态 / 加载 / 错误

| 类型 | 规范 |
|------|------|
| 空状态 | 图标或插画 + `titleSmall` 标题 + `bodySmall` 说明 + 可选主按钮 |
| 加载 | `AppleLoadingIndicator` 或 `CircularProgressIndicator`，居中 |
| 骨架屏 | 与真布局同构的 Skeleton（卡片/列表项） |
| 错误 | `error` 色图标 + 说明 + 「重试」 |

### 7.8 数字与图表

- 柱状/饼图：高度约 170dp，柱间距 6–8dp。
- 图例：`labelSmall`，色点 8–12dp。
- 变化率：正向收入绿、负向支出红，并带 `+` / `−`。

---

## 8. 图标

| 项 | 规范 |
|----|------|
| 默认 | Material Icons（`Filled` / `Outlined`），必要时 `AutoMirrored` |
| 尺寸 | 列表 20–24dp，按钮 24dp，空状态 40–48dp |
| 分类图标 | 可用 emoji（分类管理已支持），列表内 16–20dp 等效 |
| 触控 | 最小 48×48dp 热区 |

---

## 9. 动效 Motion

| 场景 | 规范 |
|------|------|
| 按压 | `pressScale`（约 0.97），`MotionSprings.interactive()` |
| 列表进场 | stagger ≤ 8 项，单项 ≤ 300ms |
| 路由 | `fadeIn` + 水平 `slideIn`，可关（设置「显示动画」） |
| 数值/颜色 | `animate*AsState`，热点用 `graphicsLayer` 读 State |
| BottomSheet | 系统 `ModalBottomSheet` 默认 |

---

## 10. 文案与可读性

1. 按钮/标题：动词或名词短句，不加句号。
2. 说明文字：一行 15–20 汉字，最多 2 行。
3. 金额单位统一 `¥` 或币种符号，小数 2 位。
4. 日期：`yyyy-MM-dd` 列表，相对时间可用「今天 / 昨天」。
5. 对比度：正文对背景 ≥ 4.5:1；大字 ≥ 3:1。

---

## 11. 无障碍与适配

- 所有 `Icon` 提供 `contentDescription`（纯装饰用 `null`）。
- 可点区域 ≥ 48dp。
- 支持系统字体缩放：优先用 `sp` + `typography`，避免固定高度裁切。
- 深色模式：只换 token，不写第二套布局。
- 鸿蒙/卓易通：避免依赖 CameraX 等原生崩溃点（项目已禁用）。

---

## 12. 代码组织约定

| 类型 | 位置 | 命名 |
|------|------|------|
| 主题 token | `ui/theme/` | `PascalCase` 文件，`val` 用 PascalCase 色名 |
| 页面 | `ui/screens/` 或模块子包 | 文件/Composable `PascalCase` |
| 公共组件 | `ui/screens/*Components*` 或模块包 | `PascalCase` |
| 状态 | ViewModel / 专用 Controller | 属性 `camelCase`，函数 `PascalCase` |
| 工具 | `util/` | 对象 + `PascalCase` 函数 |

- Composable 与公开函数：`PascalCase`。
- 局部变量/参数：`camelCase`。
- 不在 UI 层拼 SQL / 直连 Dao，走 Repository。

---

## 13. 组件速查表

| 组件 | 圆角 | 字号 | 间距 |
|------|------|------|------|
| 总览大卡 | 24 | displayLarge 金额 | 内 24，外 16 |
| 普通卡片 | 18 | titleMedium | 内 20，外 16 |
| 列表交易行 | — | titleSmall + bodySmall | 行内 8，行间 8 |
| 主按钮 | 12–18 | labelLarge | 高 48 |
| Chip | 16 | labelMedium | 高 32 |
| 输入框 | 12 | bodyMedium | 高 ≥ 48 |
| 弹窗 | 16–20 | titleMedium | 内 24 |
| BottomSheet | 顶 20 | titleMedium | 内 24 / 底 32 |
| 空状态 | — | titleSmall + bodySmall | 区块 24 |

---

## 14. 新页面检查清单

- [ ] 使用 `MaterialTheme.typography` / `colorScheme`，无硬编码业务色
- [ ] 水平边距 16dp，分组 24dp，符合 4dp 网格
- [ ] 卡片圆角 18（或大卡 24），elevation 0
- [ ] 列表可滚动且底栏/导航避让
- [ ] 空态、加载、错误齐全
- [ ] 亮/暗主题均目测通过
- [ ] 触控 ≥ 48dp，Icon 有 contentDescription
- [ ] 金额格式全项目一致

---

*文档位置：`developDocs/UI_STANDARD.md`。与实现冲突时以 `ui/theme/Type.kt`、`Color.kt`、`Theme.kt` 为准，并回来更新本文。*
