---
feature: harmony-album-camera
status: delivered
updated: 2026-09-09
branch: main
commits: cf5505e..cf5505e # 工作区未提交；base=cf5505e
---

# 鸿蒙（卓易通）记账相册相机闪退修复（纯系统相机）

## Report

**What was built** — 彻底移除 CameraX（源码 + Gradle 依赖），下拉拍照改为纯系统相机：松手经 `TakePicture` + FileProvider 拉起系统相机，胶囊内不再创建 `PreviewView`/绑定 HAL，从根上消除卓易通 native abort。`tempSystemCameraUri` 改 `rememberSaveable` 并在进程重建后用 `latestAlbumPhotoFile` 兜底，取消时删除半截文件。去掉相机权限请求（系统相机自带权限）。自动记账在鸿蒙兼容模式下安全降级（`onNotificationPosted`/`onListenerConnected` 全包 Throwable，兼容模式直接 return），设置页提示「鸿蒙/兼容环境不支持自动记账」。首启仍询问一次环境（`harmonyAskedLoaded` 门控，已答不覆写）。

**Verification** — `./gradlew.bat :app:compileDebugKotlin` → PASS；`rg androidx.camera` 全仓仅剩 spec 文档描述，源码/Gradle 为 0。独立审查 + 定向复审：Gradle 依赖与丢照 2 个 critical 均 FIXED，无新 critical。

**Journey log** — 1) 卓易通上 CameraX 是 native abort，Java try/catch 无效，「隔离调用」不够，必须删类与依赖。2) `TakePicture` 外部相机会杀进程，`remember` 的 URI 会丢，必须 `rememberSaveable` + 目录兜底。3) 纯系统相机路径不需要 `CAMERA` 权限，权限弹窗是 CameraX 遗留。4) `rg androidx.camera` 验收要扫全仓（含 build.gradle），只扫 `.kt` 会漏。

## [S1] Problem

鸿蒙（卓易通）下拉打开相机**仍然闪退**。再次排查确认：

1. **native abort 不可捕获**：下拉展开即 `PreviewView` + `ProcessCameraProvider.bindToLifecycle`，卓易通 CameraX/HAL 层直接 abort 进程，Java `try/catch` 无效。
2. **类加载即风险**：`AlbumScreen` 顶层 `import androidx.camera.*`（`ImageCapture`/`PreviewView` 等），组合时就会解析 CameraX 类型，即使走「系统相机分支」也已加载。
3. **默认路径错误**：`harmonyCompatMode` 默认 false，未选/误选时仍进 CameraX。
4. **自动记账**：`NotificationCaptureService` 在鸿蒙不可用，`onNotificationPosted` 等未全包 `Throwable`。

## [S2] Design

### 决策（用户已选）

**下拉拍照改为纯系统相机**：删除 CameraX 预览与绑定，下拉松手 → `TakePicture` + FileProvider。彻底不再引用 `androidx.camera.*`（含 Gradle 依赖）。

### 合约

#### A. 相册相机（唯一路径）

- 删除 `androidx.camera.*` 全部 import / 绑定逻辑 / Gradle 依赖。
- 下拉 `expansion > 0.6` 松手 → 系统相机（`ActivityResultContracts.TakePicture` + `${applicationId}.fileprovider`）。
- 胶囊展开显示静态文案「松手打开相机」，无 `AndroidView`/`PreviewView`。
- `tempSystemCameraUri`：`rememberSaveable`；success 且 URI 丢失时用 `latestAlbumPhotoFile` 兜底；取消时删半截文件。
- 不再请求 `CAMERA` 权限（系统相机自带）。
- 解码统一 `DeviceCompat.decodeBitmapSampled`；失败路径全 `Throwable` 吞掉并 Toast。

#### B. 自动记账鸿蒙降级

- `NotificationCaptureService.onNotificationPosted` / `onListenerConnected` 整体 `try/catch Throwable`。
- `harmonyCompatMode == true` 时解析/入库直接 return。
- 设置页：兼容模式下显示「鸿蒙/兼容环境不支持自动记账」。

#### C. 首启询问（保留）

- `harmonyAskedLoaded` 门控；已答不覆写；用于自动记账降级开关。
- 按钮预填 `DeviceCompat.guessHarmonyOs()`。

## [S3] Out of Scope

- 不恢复 CameraX 实时预览。
- 不做 HarmonyOS NEXT 原生适配。
- 不改相册浏览/删除。

## Tasks

- [x] T1: 移除 AlbumScreen 全部 CameraX 依赖（含 Gradle），下拉改纯系统相机 — acceptance: 源码/Gradle 无 `androidx.camera`；松手拉起系统相机 (covers: S2.A)
- [x] T2: 拍照/解码/入库/胶囊动画 Throwable 级加固 + URI 可恢复 — acceptance: 进程重建不丢照；各失败点不崩 (covers: S2.A)
- [x] T3: 自动记账鸿蒙安全降级 — acceptance: 兼容模式下通知不入库；异常不崩 (covers: S2.B)
- [x] T4: 首启询问保留并服务自动记账文案 — acceptance: 仅首次；已答不覆写 (covers: S2.C)
- [x] T5: 编译验证 — acceptance: `compileDebugKotlin` PASS 且全仓 rg 无 `androidx.camera` (covers: S2.A)
