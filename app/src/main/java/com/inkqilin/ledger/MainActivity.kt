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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 下载 UI 状态 */
private sealed class DownloadUiState {
    data object Idle : DownloadUiState()
    data class Downloading(val progress: Float = 0f) : DownloadUiState()
    data object Downloaded : DownloadUiState()
    data object Failed : DownloadUiState()
}

class MainActivity : ComponentActivity() {
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
                    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                    var enableStartupAnimations by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        withFrameNanos { }
                        enableStartupAnimations = true
                        // 启动时清理历史 APK
                        AppUpdateDownloader.cleanOldApks(context)
                    }

                    // ── 下载状态 ──
                    var downloadState by remember { mutableStateOf<DownloadUiState>(DownloadUiState.Idle) }

                    LaunchedEffect(checkUpdateEnabled) {
                        if (checkUpdateEnabled) {
                            delay(1200)
                            // 检查前先清理历史 APK
                            AppUpdateDownloader.cleanOldApks(context)
                            val result = AppUpdateChecker.checkForUpdate(context)
                            if (result != null) {
                                updateInfo = result
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
                                                AppUpdateDownloader.download(context, info.versionName, source, effectiveProxy).collect { progress ->
                                                    when (progress) {
                                                        is DownloadProgress.Progress ->
                                                            downloadState = DownloadUiState.Downloading(progress.fraction)
                                                        is DownloadProgress.Completed ->
                                                            downloadState = DownloadUiState.Downloaded
                                                        is DownloadProgress.Failed ->
                                                            downloadState = DownloadUiState.Failed
                                                    }
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

                    MainScreen(
                        viewModel = viewModel,
                        renQingViewModel = renQingViewModel,
                        enableAnimations = enableStartupAnimations
                    )
                }
            }
        }
    }
}
