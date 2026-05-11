# 墨麒麟记账 (InkQilin Ledger)

一款基于 Jetpack Compose 构建的 Android 个人记账应用，采用 Material 3 设计语言，支持深浅色主题、多币种管理、账单管理、分类统计、分类管理、Excel 导入导出、人情账本等功能。

## 功能特性

### 主账本

- **记账管理** — 快速添加收入/支出账单，支持自定义金额、备注、分类和日期
- **多币种管理** — 开启后支持多币种资产管理，每种货币可自定义颜色卡片、符号和名称，支持切换默认币种
- **首页概览** — 紫色主题概览卡片，显示总收入/总支出/净余额，支持日/周/月/年时段切换和柱状图
- **多币种概览** — 开启多币种后，首页概览卡片按币种左右滑动展示，每张卡片显示该币种的净余额、收入和支出
- **分类系统** — 内置多种收支分类，支持用户自定义创建新分类，可自定义图标和颜色
- **分类管理** — 专门的分类管理界面，支持添加、修改、删除分类（设置 → 分类管理）
- **账单编辑** — 左滑账单条目展示编辑/删除菜单，弹窗修改金额、备注、类型、分类、时间
- **数据统计** — 按日/周/月/年/自定义时间范围查看收支统计，支持按分类钻取查看明细，多币种模式下支持按币种筛选
- **统计编辑** — 统计页左滑分类条目可编辑分类名称、图标和展示颜色
- **账单筛选** — 主页按日期分组展示，支持按时间范围快速筛选
- **预算管理** — 设置月度预算后，首页显示预算卡片，包含总预算进度和分类支出占比
- **搜索功能** — 按关键词搜索账单

### 人情账本

- **事件记录** — 记录婚礼、丧礼、生日、乔迁、升学、满月等人情往来事件
- **方向标记** — 区分「收到」和「送出」，自动计算净额
- **联系人管理** — 维护人情联系人列表，支持亲属/朋友/同事等关系分类
- **标签系统** — 自定义人情标签（如家族、同学、单位），支持自定义图标和颜色
- **年度仪表盘** — 按年查看总收入/总支出/净额，按月查看明细
- **标签统计** — 按标签维度查看人情往来分布
- **关系分析** — 按联系人维度查看往来金额排行与收支分布
- **联系人详情** — 查看单个联系人的所有来往记录与净额汇总
- **Excel 导出** — 支持按时间范围（全部/本年/自定义）导出人情账单和联系人列表

### 数据管理

- **时间范围导出** — 导出账单时可选择「全部 / 本年 / 自定义起止日期」，主账本和人情账本均支持
- **Excel 导出** — 一键导出账单为 Excel 文件，支持自选存储位置
- **模板下载** — 下载账单模板文件（含填写说明），方便批量导入数据
- **智能导入** — 从 Excel 导入账单，自动识别并创建不存在的分类

### 通用

- **主题定制** — 浅色模式 (#715CFF) / 深色模式 (#51B4FF)，支持跟随系统自动切换
- **颜色管理** — 自定义收入/支出展示颜色，应用于所有账单条目和统计图表
- **沉浸式显示** — 全屏沉浸式布局，透明状态栏与导航栏

### 动画与交互

- **统一动画系统** — 集中管理动画时长、曲线与弹簧参数，保证全局一致的动态质感
- **页面转场** — fade + 轻微 slide 进入，快速 fade 退出，返回手势自然流畅
- **底部导航** — 选中图标 subtle scale + opacity 渐变，无 bounce
- **卡片交互** — 点击缩放 0.98，松开 spring 恢复，反馈轻量克制
- **滑动删除** — 左滑菜单带阻尼感 spring 弹回，手感自然
- **FAB 按钮** — 按下缩放 0.92，松开 snappy spring 恢复
- **图表动画** — 分类百分比进度条 progressive reveal（300ms easeOutCubic）
- **统计页滑动** — 分类条目左滑编辑/删除，统一阻尼弹回

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 1.9.22 | 开发语言 |
| Jetpack Compose | BOM 2023.08.00 | 声明式 UI 框架 |
| Material 3 | - | UI 设计规范 |
| Room | 2.6.1 | 本地数据库 |
| Navigation Compose | 2.7.7 | 页面导航 |
| DataStore | 1.0.0 | 偏好数据存储 |
| Apache POI | 5.2.3 | Excel 文件读写 |
| KoalaPlot | 0.4.0 | 图表绘制 |
| Gradle | 8.5 | 构建工具 |
| Android Gradle Plugin | 8.2.2 | 构建插件 |

## 项目结构

```
app/src/main/java/com/inkqilin/ledger/
├── data/                          # 数据层
│   ├── AppDatabase.kt             # Room 数据库配置（主账本 + 人情账本）
│   ├── Transaction.kt             # 账单实体（含 currency 字段）
│   ├── TransactionDao.kt          # 账单数据访问（含按日期范围、币种查询）
│   ├── Category.kt                # 分类实体（含自定义颜色）
│   ├── CategoryDao.kt             # 分类数据访问
│   ├── CurrencyAsset.kt           # 币种资产实体（代码、符号、名称、卡片颜色、默认标记）
│   ├── CurrencyAssetDao.kt        # 币种资产数据访问
│   ├── RenQingEvent.kt            # 人情事件实体
│   ├── RenQingContact.kt          # 人情联系人实体
│   ├── RenQingTag.kt              # 人情标签实体
│   ├── RenQingEventDao.kt         # 人情事件数据访问
│   ├── RenQingContactDao.kt       # 人情联系人数据访问
│   └── RenQingTagDao.kt           # 人情标签数据访问
├── ui/                            # 界面层
│   ├── screens/
│   │   ├── MainScreen.kt          # 主界面（导航框架、路由、转场动画）
│   │   ├── HomeScreen.kt          # 首页（概览卡片、柱状图、预算卡片、账单列表、编辑弹窗）
│   │   ├── AddTransactionScreen.kt# 添加账单
│   │   ├── StatisticsScreen.kt    # 收支统计（分类排行、钻取、编辑）
│   │   ├── CategoryTransactionsScreen.kt  # 分类账单明细（二级页面）
│   │   ├── CategoryManagementScreen.kt    # 分类管理（增删改）
│   │   ├── SearchScreen.kt        # 搜索账单
│   │   ├── SettingsScreen.kt      # 设置页面（多币种管理、预算设置、时间范围导出）
│   │   ├── TransactionComponents.kt       # 公共组件（滑动菜单、账单卡片、分类编辑弹窗）
│   │   ├── RenQingMainScreen.kt   # 人情账本主界面
│   │   ├── AddRenQingEventScreen.kt       # 添加人情事件
│   │   ├── RenQingContactDetailScreen.kt  # 联系人详情
│   │   ├── RenQingMonthDetailScreen.kt    # 月度详情
│   │   ├── RenQingTagStatsScreen.kt       # 标签统计
│   │   ├── RenQingContactAnalysisScreen.kt# 关系分析
│   │   └── ContactManagementScreen.kt     # 联系人管理
│   ├── motion/
│   │   └── MotionSystem.kt        # 统一动画配置（时长、曲线、弹簧、可复用 Modifier）
│   ├── theme/
│   │   ├── Color.kt               # 主题颜色定义
│   │   ├── Theme.kt               # Material 3 主题配置
│   │   └── Type.kt                # 字体排版
│   ├── TransactionViewModel.kt    # 主账本 ViewModel
│   └── RenQingViewModel.kt        # 人情账本 ViewModel
├── util/                          # 工具类
│   ├── ExcelExporter.kt           # Excel 导出（账单导出、模板下载）
│   ├── ExcelImporter.kt           # Excel 导入（含自动创建分类）
│   ├── RenQingExporter.kt         # 人情账本 Excel 导出
│   └── ThemeManager.kt            # 主题管理（模式、收支颜色、多币种开关、月度预算持久化）
└── MainActivity.kt                # 入口 Activity（沉浸式配置）
```

## 动画系统

项目内置统一动画配置中心 `MotionSystem.kt`，集中管理全局动画参数：

### 时长 (MotionDurations)

| 常量 | 时长 | 用途 |
|------|------|------|
| FAST | 150ms | 按压反馈、退出动画 |
| SHORT | 220ms | 标题切换、淡出 |
| MEDIUM | 300ms | 页面转场、进度条 |
| LONG | 400ms | 复杂过渡 |

### 曲线 (MotionCurves)

| 曲线 | 用途 |
|------|------|
| EaseOutCubic | 通用进入动画（减速感） |
| EaseInOutCubic | 对称过渡 |
| StandardDecelerate | Android 原生风格减速 |
| StandardAccelerate | 退出动画（加速离开） |

### 弹簧 (MotionSprings)

| 配置 | 阻尼比 | 刚度 | 用途 |
|------|--------|------|------|
| default() | MediumBouncy | Medium | 通用弹回 |
| gentle() | NoBouncy | MediumLow | 轻柔恢复（卡片点击） |
| snappy() | MediumBouncy | High | 快速弹回（FAB） |

## 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更高版本
- **JDK** 8 或以上
- **Android SDK** compileSdk 34
- **最低支持设备** Android 8.0 (API 26)

## 快速开始

### 1. 克隆项目

```bash
git clone <仓库地址>
cd inkqilin-ledger
```

### 2. 配置 Gradle 镜像（国内用户）

项目已内置阿里云 Maven 镜像配置（位于 [settings.gradle.kts](settings.gradle.kts)），国内用户可直接构建。

如需修改镜像地址，编辑 `settings.gradle.kts` 中的 `repositories` 部分即可。

### 3. 构建运行

```bash
# 调试版构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

或直接使用 Android Studio 打开项目，点击 **Run** 按钮运行。

## 数据模型

### Transaction（账单）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| amount | Double | 金额 |
| category | String | 分类名称 |
| note | String | 备注 |
| date | Long | 时间戳 |
| type | TransactionType | 收入/支出 |
| currency | String | 币种代码（默认 CNY） |

### Category（分类）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| name | String | 分类名称 |
| icon | String | 分类图标（Emoji） |
| type | TransactionType | 收入/支出 |
| color | String | 展示颜色（十六进制，默认 #715CFF） |

### CurrencyAsset（币种资产）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| code | String | 币种代码（如 CNY、USD） |
| symbol | String | 货币符号（如 ¥、$） |
| name | String | 币种名称（如人民币、美元） |
| cardColor | String | 卡片颜色（十六进制） |
| isDefault | Boolean | 是否为默认币种 |

### RenQingEvent（人情事件）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| contactId | Long | 关联联系人 ID |
| contactName | String | 联系人姓名 |
| eventType | RenQingEventType | 事件类型（婚礼/丧礼/生日/乔迁/升学/满月/其他） |
| tagId | Long | 关联标签 ID |
| tagName | String | 标签名称 |
| direction | RenQingDirection | 方向（收到/送出） |
| amount | Double | 金额 |
| giftDescription | String | 礼品描述 |
| date | Long | 时间戳 |
| location | String | 地点 |
| note | String | 备注 |
| photoUri | String? | 照片路径 |

### RenQingContact（人情联系人）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| name | String | 姓名 |
| relationship | RelationshipType | 关系（亲属/朋友/同事/其他） |
| phone | String | 电话 |
| birthday | Long? | 生日 |
| note | String | 备注 |

### RenQingTag（人情标签）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| name | String | 标签名称 |
| icon | String | 标签图标（Emoji） |
| color | String | 展示颜色（十六进制） |

## 主题配色

| 模式 | 主题色 | 说明 |
|------|--------|------|
| 浅色模式 | `#715CFF` | 紫色系 |
| 深色模式 | `#51B4FF` | 蓝色系 |

收入/支出的展示颜色可在设置中自定义，应用于账单条目、统计图表等全局位置。

## 许可证

本项目采用 [GNU General Public License v3.0](LICENSE) 许可证。
