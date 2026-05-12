
以下是关于墨麒麟记账 App **更新检测机制**的完整技术文档：

---

## 更新检测机制文档

### 1. 整体架构概览

更新检测功能由 **4 个核心组件** 协同工作，采用 MVVM 分层设计：

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  (触发入口 + 弹窗 UI)                                            │
│    └─ LaunchedEffect(checkUpdateEnabled)                         │
│         └─ AppUpdateChecker.checkForUpdate(context)              │
│              └─ OkHttp 请求 GitHub API                           │
│                   └─ 版本比较 → UpdateInfo?                       │
│                        └─ AlertDialog 展示 → 跳转浏览器下载        │
├─────────────────────────────────────────────────────────────────┤
│                     ThemeManager (DataStore)                      │
│  checkUpdateEnabled: Flow<Boolean>  ←→  setCheckUpdateEnabled() │
├─────────────────────────────────────────────────────────────────┤
│                   TransactionViewModel                            │
│  暴露 checkUpdateEnabled StateFlow + getCurrentVersionName()     │
├─────────────────────────────────────────────────────────────────┤
│                     SettingsScreen                                │
│  Switch 开关 → viewModel.setCheckUpdateEnabled()                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### 2. 各组件详解

#### 2.1 `AppUpdateChecker` — 更新检测工具类

**文件位置**: `app/src/main/java/com/inkqilin/ledger/util/AppUpdateChecker.kt`

这是一个 Kotlin `object` 单例，负责所有与 GitHub Release 交互的逻辑：

**核心流程**：

```
App启动 → checkForUpdate(context)
            │
            ├─ 1. 获取当前版本号
            │     └─ context.packageManager.getPackageInfo().versionName
            │        (从 build.gradle.kts 的 versionName = "1.3.0" 读取)
            │
            ├─ 2. 请求 GitHub API
            │     ├─ URL: https://api.github.com/repos/Murchey/inkqilin-ledger/releases/latest
            │     ├─ 使用 OkHttp 同步请求 (在 IO 协程调度器执行)
            │     ├─ 超时设置: 连接 10s + 读取 10s
            │     └─ Header: Accept: application/vnd.github.v3+json
            │
            ├─ 3. 解析 JSON 响应
            │     ├─ tag_name → 去掉 "v"/"V" 前缀 → 最新版本号
            │     ├─ body → 更新日志 (Markdown 格式)
            │     └─ html_url → Release 页面的浏览器链接
            │
            ├─ 4. 版本比较 (isNewerVersion)
            │     ├─ 将 "1.4.0" 和 "1.3.0" 按 "." 分割为 [1,4,0] vs [1,3,0]
            │     ├─ 逐位比较 (major → minor → patch)
            │     └─ 任何一位大于当前版本 → 返回 true
            │
            └─ 5. 结果
                  ├─ 有更新 → 返回 UpdateInfo(versionName, releaseNotes, downloadUrl)
                  └─ 无更新/异常 → 返回 null (静默失败，不打扰用户)
```

**关键设计**：
- 使用 `Dispatchers.IO` 确保网络请求不阻塞主线程
- 异常全部 catch → 返回 `null`，保证无网络或 API 限流时不会崩溃
- 版本比较支持任意段数（如 `1.3.0`、`2.0`、`1.3.1.2`）

**数据模型**：
```kotlin
data class UpdateInfo(
    val versionName: String,    // "1.4.0"
    val releaseNotes: String,   // "修复了xxx问题\n新增xxx功能"
    val downloadUrl: String     // "https://github.com/Murchey/inkqilin-ledger/releases/tag/v1.4.0"
)
```

---

#### 2.2 `MainActivity` — 启动入口 + 弹窗 UI

**文件位置**: `app/src/main/java/com/inkqilin/ledger/MainActivity.kt`

在 App 启动时的 `setContent` Compose 入口中，集成了更新检测逻辑：

**触发时机**：
```kotlin
LaunchedEffect(checkUpdateEnabled) {
    if (checkUpdateEnabled) {
        val result = AppUpdateChecker.checkForUpdate(context)
        if (result != null) {
            updateInfo = result  // 触发弹窗
        }
    }
}
```

- `LaunchedEffect(checkUpdateEnabled)` — 当 `checkUpdateEnabled` 值变化时重新执行
- 默认首次启动时 `checkUpdateEnabled = true`，所以会自动检测一次
- 如果用户在设置中关闭开关，`LaunchedEffect` 被重新触发但 `if` 条件不满足，不会请求网络

**弹窗 UI**：

当 `updateInfo != null` 时，展示 `AlertDialog`：
```
┌─────────────────────────────────────────┐
│  发现新版本 v1.4.0                       │
│                                          │
│  当前版本: v1.3.0                        │
│                                          │
│  更新内容:                               │
│  修复了xxx问题                           │
│  新增xxx功能                             │
│                                          │
│           [稍后再说]    [前往更新]        │
└─────────────────────────────────────────┘
```

- **"前往更新"** → 通过 `Intent(ACTION_VIEW)` 打开系统浏览器，跳转到 GitHub Release 页面
- **"稍后再说"** / 点击外部 → 关闭弹窗，`updateInfo = null`

---

#### 2.3 `ThemeManager` — 偏好持久化

**文件位置**: `app/src/main/java/com/inkqilin/ledger/util/ThemeManager.kt`

使用 Android Jetpack **DataStore Preferences** 存储用户偏好：

```kotlin
private val CHECK_UPDATE_ENABLED_KEY = booleanPreferencesKey("check_update_enabled")

val checkUpdateEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[CHECK_UPDATE_ENABLED_KEY] ?: true  // 默认开启
}

suspend fun setCheckUpdateEnabled(enabled: Boolean) {
    context.dataStore.edit { preferences ->
        preferences[CHECK_UPDATE_ENABLED_KEY] = enabled
    }
}
```

- **Key**: `"check_update_enabled"`
- **默认值**: `true`（首次安装默认开启更新检测）
- 持久化到本地，App 重启后保留用户选择

---

#### 2.4 `TransactionViewModel` — 状态桥接层

**文件位置**: `app/src/main/java/com/inkqilin/ledger/ui/TransactionViewModel.kt`

将 ThemeManager 的 `Flow` 转换为 Compose 可观察的 `StateFlow`：

```kotlin
val checkUpdateEnabled: StateFlow<Boolean> = themeManager.checkUpdateEnabled.stateIn(
    viewModelScope, SharingStarted.WhileSubscribed(5000), true
)

fun setCheckUpdateEnabled(enabled: Boolean) {
    viewModelScope.launch { themeManager.setCheckUpdateEnabled(enabled) }
}

fun getCurrentVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    } catch (e: PackageManager.NameNotFoundException) { "0.0.0" }
}
```

---

#### 2.5 `SettingsScreen` — 用户控制开关

**文件位置**: `app/src/main/java/com/inkqilin/ledger/ui/screens/SettingsScreen.kt`（第 357-369 行）

```kotlin
val checkUpdateEnabled by viewModel.checkUpdateEnabled.collectAsState()
// ...
ListItem(
    headlineContent = { Text("启动时检测新版本") },
    supportingContent = { Text(if (checkUpdateEnabled) "已启用，启动时自动检测 GitHub 新版本" else "已关闭") },
    trailingContent = {
        Switch(checked = checkUpdateEnabled, onCheckedChange = { viewModel.setCheckUpdateEnabled(it) })
    }
)
```

---

### 3. 数据流全景图

```
用户操作                    状态存储              观察者               触发行为
─────────                ──────────           ─────────           ─────────
App 启动     ──→  checkUpdateEnabled=true  ──→  LaunchedEffect  ──→  请求 GitHub API
                       (DataStore)              (MainActivity)       弹窗/静默

设置中关闭开关 ──→  checkUpdateEnabled=false ──→  LaunchedEffect  ──→  不请求，无弹窗
                       (DataStore)              (MainActivity)

用户点"前往更新" ──────────────────────────────→  Intent(ACTION_VIEW)
                                                  打开浏览器 → GitHub Release 页
```

---

### 4. 依赖与权限

| 项目 | 配置 |
|------|------|
| **网络权限** | `AndroidManifest.xml` 中 `<uses-permission android:name="android.permission.INTERNET" />` |
| **HTTP 客户端** | `com.squareup.okhttp3:okhttp:4.12.0`（build.gradle.kts） |
| **版本来源** | `build.gradle.kts` 的 `versionName = "1.3.0"` + `versionCode = 3` |
| **GitHub API** | `https://api.github.com/repos/Murchey/inkqilin-ledger/releases/latest`（无需 Token，限流 60次/小时） |

---

### 5. 容错与边界处理

| 场景 | 处理方式 |
|------|---------|
| 无网络连接 | OkHttp 抛异常 → catch → 返回 null → 静默跳过 |
| GitHub API 限流 (403) | `response.isSuccessful` 为 false → 返回 null |
| JSON 解析失败 | `optString` 提供默认值，不会崩溃 |
| 版本格式不规范 | `toIntOrNull() ?: 0` 兜底 |
| 用户关闭检测开关 | `checkUpdateEnabled = false` → 不进入检测逻辑 |
| Release 无 body | 显示 `"暂无更新说明"` |