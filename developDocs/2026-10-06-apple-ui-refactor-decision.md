# 墨麒麟记账 UI 重构决策摘要（2026-10-06）

- 保留 Material 3 与现有业务流程，统一公共色彩和形状为 Apple HIG 语义 token；收入/支出等功能色仍分别使用绿/红。
- 默认品牌主色从青松绿调整为 Apple Blue（亮色 `#007AFF`、暗色 `#0A84FF`），绿色只表达收入/成功，减少主题色与业务语义混淆。
- 背景、surface、分组面和 outline 使用现有 `Color.kt` token；卡片采用 18 dp，页面间距 16/24 dp，去除页面内临时颜色。
- 只改主题层和 token 层，数据库、备份、桌面小组件和数据导入逻辑保持原样。

## 本轮实现

- 默认品牌色与 FAB 回退色切换为 Apple Blue；收入、支出和警告色仍保持各自语义。
- Compose 的 Material 3 颜色、Typography、形状和既有浮动 Tab 栏继续共享同一套 token。
- `:app:assembleDebug` 已通过，未改动备份和小组件业务实现。
- 已用 Android 模拟器安装并检查主页：Apple Blue FAB、胶囊底部导航、低阴影圆角卡片和亮色系统背景正常显示；用户保存的自定义卡片主题仍按原设置保留。

## 验证

- `./gradlew.bat :app:assembleDebug --no-daemon` 通过。
- APK 已安装到 `emulator-5554` 并完成主页冒烟检查；数据库、备份和导入流程未改动。
