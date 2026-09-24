---
feature: album-viewer-swipe-nav
status: delivered
updated: 2026-09-09
branch: main
commits: cf5505e..cf5505e # 工作区未提交
---

# 记账相册：系统相册式看图 + 底栏消失修复

## Report

**What was built** — 看图改为系统相册式 `HorizontalPager` 左右滑切换上一张/下一张（顶栏显示 `3/12` 页码，每页独立缩放/平移，备注/时间/删除跟随当前页）。系统返回键在查看器内先关弹层再关查看器，不再被主界面 BackHandler 劫去切 Tab。底栏消失修复：`showBottomBar` 仅在「相册 Tab 且交互中」隐藏；`AlbumScreen` dispose 时、离开相册 Tab 时、主界面返回键路径均复位 `isAlbumInteracting`。

**Verification** — `./gradlew.bat :app:compileDebugKotlin` → PASS。

**Journey log** — 1) 底栏卡死是 `isAlbumInteracting` 在 ViewModel 中残留 true，且系统返回被 MainScreen BackHandler 抢走；2) 看图必须以列表为上下文做 Pager，而不是单图状态。

## [S1] Problem

1. **看图操作与系统相册不一致**：`PhotoViewerScreen` 只能点返回退出，**不能左右滑动切换上一张/下一张**。
2. **底部导航栏不可恢复地消失**：查看照片时 `isAlbumInteracting=true` 隐藏底栏；按系统返回键被 `MainScreen` 的 `BackHandler`（非 home Tab）截走并切回首页，`selectedPhoto` 未清、`AlbumScreen` 被 dispose 时也不复位，`isAlbumInteracting` 永久为 true → `showBottomBar=false`。

## [S2] Design

### A. 看图支持左右滑切换

- `PhotoViewer` 改为 `HorizontalPager`，在当前相册 `photos` 列表内左右滑切换。
- 打开时定位到被点照片（`initialPage`）；滑动时更新当前备注/时间/删除目标。
- 删除当前张后：若还有下一张则切到相邻张，否则关闭查看器。
- 仍保留双指缩放/拖拽；横向滑优先切图（pager 手势），缩放后可拖拽平移。

### B. 系统返回键行为

- 查看器内 `BackHandler`：**先关闭查看器**（`selectedPhoto = null`），不切 Tab。
- 多选/删除确认对话框打开时返回先关对话框。

### C. 底栏可恢复

- `AlbumScreen` `DisposableEffect` `onDispose { setAlbumInteracting(false) }`。
- 查看器关闭 / 下拉状态归零时同步 `setAlbumInteracting(false)`（现有 LaunchedEffect 保留）。
- `MainScreen`：底栏仅在「相册 Tab 且交互中」隐藏：  
  `showBottomBar = currentRoute == "main" && !(currentPageRoute == "album" && isAlbumInteracting)`
- 离开相册 Tab 时强制 `setAlbumInteracting(false)`。

## [S3] Out of Scope

- 不做视频/动图；不改拍照路径（仍系统相机）。
- 不做双指捏合切换图片。

## Tasks

- [x] T1: PhotoViewer 改 HorizontalPager 左右切图 — acceptance: 左右滑切换上一张/下一张 (covers: S2.A)
- [x] T2: 查看器 BackHandler 先关查看器 — acceptance: 系统返回不切 Tab、底栏恢复 (covers: S2.B, S2.C)
- [x] T3: isAlbumInteracting 离开相册必复位 + MainScreen 底栏条件 — acceptance: 任意路径退出后底栏可恢复 (covers: S2.C)
- [x] T4: 编译验证 — acceptance: compileDebugKotlin PASS (covers: S2.C)
