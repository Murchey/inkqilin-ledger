---
feature: privacy-policy
status: delivered
updated: 2026-09-09
branch: main
commits: cf5505e..cf5505e # 工作区未提交
---

# 隐私政策声明窗口

## Report

**What was built** — 新增 `PrivacyPolicy` 中文政策全文（十节，对齐 README：本地优先、无埋点、权限/可选联网清单、AI·云备份由用户自配、硬删除与开源可审计）。首次启动弹「同意并继续」（`privacy_accepted` 记忆，点外部不算同意）；设置 → 关于 增加「隐私政策」可随时重看。鸿蒙环境询问改为隐私确认通过后再弹。

**Verification** — `assembleDebug` BUILD SUCCESSFUL。

## [S1] Problem
应用缺少隐私政策声明窗口。结合 README / 开源（GPL-3.0）与「基础版本地、智能版可选」理念，需要首次启动展示并可在关于中再次查看。

## [S2] Design
- **文案口径**（对齐 README）：本地优先、无埋点/无账号；权限与可选联网（更新检测 / 用户自配 AI·OCR API / 用户自配 COS 云备份 / 自动记账通知监听）；数据导出与硬删除；开源可审计。
- **首启**：`privacy_accepted` 未确认时弹窗，「同意并继续」写入后不再弹；不同意可退出（不写入或写入后仍可退出 App）。
- **入口**：设置 → 关于 → 「隐私政策」可随时重看。
- 文案置于 `PrivacyPolicy.kt`，复用 `AppleAlertDialog` / 关于抽屉。

## [S3] Out of Scope
- 不做多语言英文版政策全文（中文优先）。
- 不做应用商店上架材料。

## Tasks
- [x] T1: ThemeManager privacy_accepted + VM 暴露 — acceptance: 可读写 (covers: S2)
- [x] T2: PrivacyPolicy 文案与对话框组件 — acceptance: 可展示全文 (covers: S2)
- [x] T3: MainActivity 首启弹窗（确认后不再弹） — acceptance: 仅首次 (covers: S2)
- [x] T4: 设置关于增加隐私政策入口 — acceptance: 可再次打开 (covers: S2)
- [x] T5: assembleDebug 验证 — acceptance: BUILD SUCCESSFUL (covers: S2)
