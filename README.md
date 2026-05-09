# 墨麒麟记账 (InkQilin Ledger)

一款基于 Jetpack Compose 构建的 Android 个人记账应用，采用 Material 3 设计语言，支持深浅色主题、账单管理、分类统计、分类管理、Excel 导入导出等功能。

## 功能特性

- **记账管理** — 快速添加收入/支出账单，支持自定义金额、备注、分类和日期
- **分类系统** — 内置多种收支分类，支持用户自定义创建新分类，可自定义图标和颜色
- **分类管理** — 专门的分类管理界面，支持添加、修改、删除分类（设置 → 分类管理）
- **账单编辑** — 左滑账单条目展示编辑/删除菜单，弹窗修改金额、备注、类型、分类、时间
- **数据统计** — 按日/周/月/年/自定义时间范围查看收支统计，支持按分类钻取查看明细
- **统计编辑** — 统计页左滑分类条目可编辑分类名称、图标和展示颜色
- **账单筛选** — 主页按日期分组展示，支持按时间范围快速筛选
- **Excel 导出** — 一键导出账单为 Excel 文件，支持自选存储位置
- **模板下载** — 下载账单模板文件（含填写说明），方便批量导入数据
- **智能导入** — 从 Excel 导入账单，自动识别并创建不存在的分类
- **搜索功能** — 按关键词搜索账单
- **主题定制** — 浅色模式 (#715CFF) / 深色模式 (#51B4FF)，可在设置中自由切换
- **颜色管理** — 自定义收入/支出展示颜色，应用于所有账单条目和统计图表
- **沉浸式显示** — 全屏沉浸式布局，透明状态栏与导航栏
- **丝滑动画** — 页面切换和交互元素采用弹簧物理动画

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
│   ├── AppDatabase.kt             # Room 数据库配置
│   ├── Transaction.kt             # 账单实体
│   ├── TransactionDao.kt          # 账单数据访问
│   ├── Category.kt                # 分类实体（含自定义颜色）
│   └── CategoryDao.kt             # 分类数据访问
├── ui/                            # 界面层
│   ├── screens/
│   │   ├── MainScreen.kt          # 主界面（导航框架、路由）
│   │   ├── HomeScreen.kt          # 首页（账单列表、编辑弹窗）
│   │   ├── AddTransactionScreen.kt# 添加账单
│   │   ├── StatisticsScreen.kt    # 收支统计（分类排行、钻取、编辑）
│   │   ├── CategoryTransactionsScreen.kt  # 分类账单明细（二级页面）
│   │   ├── CategoryManagementScreen.kt    # 分类管理（增删改）
│   │   ├── SearchScreen.kt        # 搜索账单
│   │   ├── SettingsScreen.kt      # 设置页面
│   │   └── TransactionComponents.kt       # 公共组件（滑动菜单、账单卡片、分类编辑弹窗）
│   ├── theme/
│   │   ├── Color.kt               # 主题颜色定义
│   │   ├── Theme.kt               # Material 3 主题配置
│   │   └── Type.kt                # 字体排版
│   └── TransactionViewModel.kt    # ViewModel 业务逻辑
├── util/                          # 工具类
│   ├── ExcelExporter.kt           # Excel 导出（账单导出、模板下载）
│   ├── ExcelImporter.kt           # Excel 导入（含自动创建分类）
│   └── ThemeManager.kt            # 主题管理（模式、收支颜色持久化）
└── MainActivity.kt                # 入口 Activity（沉浸式配置）
```

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

### Category（分类）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| name | String | 分类名称 |
| icon | String | 分类图标（Emoji） |
| type | TransactionType | 收入/支出 |
| color | String | 展示颜色（十六进制，默认 #715CFF） |

## 主题配色

| 模式 | 主题色 | 说明 |
|------|--------|------|
| 浅色模式 | `#715CFF` | 紫色系 |
| 深色模式 | `#51B4FF` | 蓝色系 |

收入/支出的展示颜色可在设置中自定义，应用于账单条目、统计图表等全局位置。

## 许可证

本项目采用 [GNU General Public License v3.0](LICENSE) 许可证。
