package com.inkqilin.ledger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.inkqilin.ledger.data.AppDatabase
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.TransactionViewModelFactory
import com.inkqilin.ledger.ui.screens.MainScreen
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import com.inkqilin.ledger.util.AppUpdateChecker
import com.inkqilin.ledger.util.AppUpdateDownloader
import com.inkqilin.ledger.util.DownloadProgress
import com.inkqilin.ledger.util.DownloadSource
import com.inkqilin.ledger.util.ThemeManager
import com.inkqilin.ledger.util.ThemeMode
import com.inkqilin.ledger.util.UpdateInfo
import com.inkqilin.ledger.widget.WidgetIntents
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** 下载 UI 状态 */
private sealed class DownloadUiState {
    data object Idle : DownloadUiState()
    data class Downloading(val progress: Float = 0f) : DownloadUiState()
    data object Downloaded : DownloadUiState()
    data object Failed : DownloadUiState()
}

class MainActivity : ComponentActivity() {
    /** 桌面小部件导航目标（由 WidgetClickReceiver 携带，经此路由到 MainScreen） */
    private val widgetNavTarget = MutableStateFlow<String?>(null)
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val themeManager by lazy { ThemeManager(this) }
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(
            database.transactionDao(),
            database.categoryDao(),
            database.currencyAssetDao(),
            database.albumPhotoDao(),
            database.keywordCategoryDao(),
            database.userAssetDao(),
            database.assetFlowDao(),
            themeManager
        )
    }
    private val renQingViewModel: RenQingViewModel by viewModels {
        RenQingViewModel.Factory(
            database.renQingContactDao(),
            database.renQingEventDao(),
            database.renQingTagDao(),
            database.transactionDao(),
            database.categoryDao(),
            themeManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        widgetNavTarget.value = parseWidgetTarget(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val customPrimaryColorHex by viewModel.customPrimaryColorHex.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            InkQilinLedgerTheme(darkTheme = darkTheme, customPrimaryColorHex = customPrimaryColorHex) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val checkUpdateEnabled by viewModel.checkUpdateEnabled.collectAsState()
                    val updateRepo by viewModel.updateRepo.collectAsState()
                    val githubRepo by viewModel.githubRepo.collectAsState()
                    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                    var enableStartupAnimations by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        withFrameNanos { }
                        enableStartupAnimations = true
                        // 启动时清理历史 APK
                        AppUpdateDownloader.cleanOldApks(context)
                        // 为存量交易自动生成 UUID（去重依赖）
                        viewModel.backfillTransactionUuids()
                        // 为存量流转自动生成 UUID
                        viewModel.backfillAssetFlowUuids()
                    }

                    // 首次启动：先展示隐私政策，确认后再问运行环境
                    val privacyAccepted by viewModel.privacyAccepted.collectAsState()
                    val privacyAcceptedLoaded by viewModel.privacyAcceptedLoaded.collectAsState()
                    var showPrivacy by remember { mutableStateOf(false) }
                    LaunchedEffect(privacyAcceptedLoaded, privacyAccepted) {
                        if (privacyAcceptedLoaded && !privacyAccepted) {
                            showPrivacy = true
                        }
                    }

                    // 首次启动询问运行环境（鸿蒙/卓易通 vs 标准 Android）。
                    // 必须等 DataStore 真值再弹，避免 stateIn 初值 false 导致每次冷启动误弹并覆写用户选择。
                    val harmonyAsked by viewModel.harmonyCompatAsked.collectAsState()
                    val harmonyAskedLoaded by viewModel.harmonyAskedLoaded.collectAsState()
                    var showHarmonyAsk by remember { mutableStateOf(false) }
                    LaunchedEffect(harmonyAskedLoaded, harmonyAsked, privacyAccepted) {
                        if (privacyAccepted && harmonyAskedLoaded && !harmonyAsked && !showHarmonyAsk) {
                            showHarmonyAsk = true
                        }
                        if (harmonyAsked) {
                            showHarmonyAsk = false
                        }
                    }
                    if (showPrivacy) {
                        com.inkqilin.ledger.ui.screens.PrivacyPolicyDialog(
                            requireAccept = true,
                            onAccept = {
                                viewModel.acceptPrivacyPolicy()
                                showPrivacy = false
                            },
                            onDismiss = { /* 首启不允许无确认关闭 */ }
                        )
                    }
                    if (!showPrivacy && showHarmonyAsk) {
                        val defaultHarmony = remember { com.inkqilin.ledger.util.DeviceCompat.guessHarmonyOs() }
                        com.inkqilin.ledger.ui.screens.AppleAlertDialog(
                            onDismissRequest = {
                                // 仅在「确实需要询问」时落盘；已问过则绝不改写 mode
                                if (!harmonyAsked) {
                                    viewModel.setHarmonyCompatMode(false)
                                }
                                showHarmonyAsk = false
                            },
                            title = "运行环境确认",
                            message = "是否运行在鸿蒙系统上（例如通过卓易通等兼容层）？\n\n" +
                                "选择「鸿蒙」后将关闭自动记账（通知监听在该环境不可用）。\n" +
                                "相册拍照已统一使用系统相机，安卓/鸿蒙均可。\n（仅询问这一次）",
                            buttons = listOf(
                                com.inkqilin.ledger.ui.screens.AppleDialogButton(
                                    if (defaultHarmony) "鸿蒙 / 兼容模式" else "标准 Android",
                                    com.inkqilin.ledger.ui.screens.AppleDialogButtonStyle.DEFAULT
                                ) {
                                    viewModel.setHarmonyCompatMode(defaultHarmony)
                                    showHarmonyAsk = false
                                },
                                com.inkqilin.ledger.ui.screens.AppleDialogButton(
                                    if (defaultHarmony) "标准 Android" else "鸿蒙 / 兼容模式",
                                    com.inkqilin.ledger.ui.screens.AppleDialogButtonStyle.CANCEL
                                ) {
                                    viewModel.setHarmonyCompatMode(!defaultHarmony)
                                    showHarmonyAsk = false
                                }
                            )
                        )
                    }

                    // ── 下载状态 ──
                    var downloadState by remember { mutableStateOf<DownloadUiState>(DownloadUiState.Idle) }

                    LaunchedEffect(checkUpdateEnabled, updateRepo) {
                        if (checkUpdateEnabled) {
                            delay(1200)
                            // 检查前先清理历史 APK
                            AppUpdateDownloader.cleanOldApks(context)
                            val result = AppUpdateChecker.checkForUpdate(context, updateRepo)
                            if (result != null) {
                                updateInfo = result
                            }
                        }
                    }

                    // 设置页手动「检查更新」：复用同一套更新弹窗
                    LaunchedEffect(Unit) {
                        viewModel.manualUpdateCheckTrigger.collect {
                            AppUpdateDownloader.cleanOldApks(context)
                            val result = AppUpdateChecker.checkForUpdate(context, viewModel.updateRepo.value)
                            if (result != null) {
                                downloadState = DownloadUiState.Idle
                                updateInfo = result
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "已是最新版本",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    updateInfo?.let { info ->
                        var selectedSourceIndex by remember { mutableStateOf(0) }
                        val sources = listOf("Gitee 镜像 (国内推荐)", "GitHub 仓库", "GitHub 源 (代理)")
                        val sourceEnums = listOf(DownloadSource.GITEE, DownloadSource.GITHUB, DownloadSource.PROXY)
                        val proxyUrl by viewModel.updateProxyUrl.collectAsState()
                        var expanded by remember { mutableStateOf(false) }

                        val isDownloading = downloadState is DownloadUiState.Downloading
                        val isDownloaded = downloadState is DownloadUiState.Downloaded
                        val isFailed = downloadState is DownloadUiState.Failed

                        AlertDialog(
                            onDismissRequest = {
                                if (!isDownloading) {
                                    updateInfo = null
                                    downloadState = DownloadUiState.Idle
                                }
                            },
                            title = {
                                Text(
                                    when {
                                        isFailed -> "下载失败"
                                        isDownloading -> "正在下载…"
                                        isDownloaded -> "下载完成"
                                        else -> "发现新版本 v${info.versionName}"
                                    }
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    if (!isDownloading && !isDownloaded) {
                                        Text(
                                            text = "当前版本: v${viewModel.getCurrentVersionName(context)}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("更新内容:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = info.releaseNotes.ifBlank { "暂无更新说明" },
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text("选择下载源:", style = MaterialTheme.typography.labelMedium)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { expanded = true },
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp),
                                                tonalElevation = 1.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = sources[selectedSourceIndex],
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false },
                                                modifier = Modifier.fillMaxWidth(0.7f)
                                            ) {
                                                sources.forEachIndexed { index, name ->
                                                    DropdownMenuItem(
                                                        text = { Text(name) },
                                                        onClick = {
                                                            selectedSourceIndex = index
                                                            expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (isDownloading) {
                                        val progress = (downloadState as DownloadUiState.Downloading).progress
                                        val percent = (progress * 100).toInt()
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    "正在下载 v${info.versionName}…",
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            LinearProgressIndicator(
                                                progress = progress.coerceIn(0f, 1f),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "$percent%",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }

                                    if (downloadState is DownloadUiState.Failed) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("✗ ", color = MaterialTheme.colorScheme.error)
                                            Text("下载失败，请重试", color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    if (isDownloaded) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("✓ ", color = MaterialTheme.colorScheme.primary)
                                            Text("已下载完成，可立即安装")
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                when {
                                    isDownloading -> {
                                        // 下载中：不显示确认按钮
                                    }
                                    isFailed -> {
                                        Button(onClick = {
                                            downloadState = DownloadUiState.Idle
                                        }) { Text("返回重试") }
                                    }
                                    isDownloaded -> {
                                        Button(onClick = {
                                            AppUpdateDownloader.install(context, info.versionName)
                                            updateInfo = null
                                            downloadState = DownloadUiState.Idle
                                        }) { Text("立即安装") }
                                    }
                                    else -> {
                                        Button(onClick = {
                                            downloadState = DownloadUiState.Downloading()
                                            val source = sourceEnums[selectedSourceIndex]
                                            val effectiveProxy = if (source == DownloadSource.PROXY) proxyUrl else null
                                            scope.launch {
                                                try {
                                                    AppUpdateDownloader.download(
                                                        context = context,
                                                        versionName = info.versionName,
                                                        source = source,
                                                        proxyPrefix = effectiveProxy,
                                                        giteeRepo = updateRepo,
                                                        githubRepo = githubRepo
                                                    ).collect { progress ->
                                                        when (progress) {
                                                            is DownloadProgress.Progress ->
                                                                downloadState = DownloadUiState.Downloading(progress.fraction)
                                                            is DownloadProgress.Completed ->
                                                                downloadState = DownloadUiState.Downloaded
                                                            is DownloadProgress.Failed ->
                                                                downloadState = DownloadUiState.Failed
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("AppUpdate", "下载异常", e)
                                                    downloadState = DownloadUiState.Failed
                                                }
                                            }
                                        }) { Text("下载更新") }
                                    }
                                }
                            },
                            dismissButton = {
                                if (isDownloading) {
                                    TextButton(onClick = {
                                        updateInfo = null
                                        downloadState = DownloadUiState.Idle
                                    }) { Text("取消") }
                                } else if (!isDownloaded) {
                                    TextButton(onClick = {
                                        updateInfo = null
                                        downloadState = DownloadUiState.Idle
                                    }) { Text("稍后再说") }
                                }
                            }
                        )
                    }

                    val widgetTarget by widgetNavTarget.collectAsState()
                    MainScreen(
                        viewModel = viewModel,
                        renQingViewModel = renQingViewModel,
                        enableAnimations = enableStartupAnimations,
                        externalNavTarget = widgetTarget,
                        onExternalTargetHandled = { widgetNavTarget.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseWidgetTarget(intent)?.let { widgetNavTarget.value = it }
    }

    /** 解析桌面小部件导航目标 */
    private fun parseWidgetTarget(intent: Intent): String? {
        if (intent.action != WidgetIntents.ACTION_WIDGET_NAV) return null
        return intent.getStringExtra(WidgetIntents.EXTRA_NAV_TARGET)
    }
}
