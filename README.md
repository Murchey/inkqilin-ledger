# 墨麒麟记账 (InkQilin Ledger)

一款基于 Jetpack Compose 构建的 Android 个人记账应用，采用 Material 3 设计语言，支持深浅色主题、账单管理、分类统计、Excel 导入导出等功能。

## 功能特性

- **记账管理** — 快速添加收入/支出账单，支持自定义金额、备注、分类和日期
- **分类系统** — 内置多种收支分类，支持用户自定义创建新分类
- **账单编辑** — 左滑账单条目展示编辑/删除菜单，弹窗修改金额、备注、类型、分类、时间
- **数据统计** — 按日/周/月/年/自定义时间范围查看收支统计图表
- **账单筛选** — 主页支持按时间范围快速筛选账单
- **Excel 导出** — 一键导出账单为 Excel 文件，支持自选存储位置
- **模板下载** — 下载账单模板文件，方便批量导入数据
- **搜索功能** — 按关键词搜索账单
- **深浅色主题** — 浅色模式 (#715CFF) / 深色模式 (#51B4FF)，可在设置中自由切换
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
│   ├── Category.kt                # 分类实体
│   └── CategoryDao.kt             # 分类数据访问
├── ui/                            # 界面层
│   ├── screens/
│   │   ├── MainScreen.kt          # 主界面（导航框架）
│   │   ├── HomeScreen.kt          # 首页（账单列表、编辑弹窗、滑动菜单）
│   │   ├── AddTransactionScreen.kt # 添加账单
│   │   ├── StatisticsScreen.kt    # 收支统计
│   │   ├── SearchScreen.kt        # 搜索账单
│   │   └── SettingsScreen.kt      # 设置页面
│   ├── theme/
│   │   ├── Color.kt               # 主题颜色定义
│   │   ├── Theme.kt               # Material 3 主题配置
│   │   └── Type.kt                # 字体排版
│   └── TransactionViewModel.kt    # ViewModel 业务逻辑
├── util/                          # 工具类
│   ├── ExcelExporter.kt           # Excel 导出
│   ├── ExcelImporter.kt           # Excel 导入
│   └── ThemeManager.kt            # 主题管理
└── MainActivity.kt                # 入口 Activity
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
| icon | String | 分类图标 |
| type | TransactionType | 收入/支出 |

## 主题配色

| 模式 | 主题色 | 说明 |
|------|--------|------|
| 浅色模式 | `#715CFF` | 紫色系 |
| 深色模式 | `#51B4FF` | 蓝色系 |

## 许可证

本项目采用 [GNU General Public License v3.0](LICENSE) 许可证。
