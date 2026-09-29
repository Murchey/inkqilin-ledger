@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.ui.*
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.*
import com.inkqilin.ledger.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun SettingsAboutSheet(show: Boolean, onDismiss: () -> Unit, onShowUsageGuide: () -> Unit = {}, onShowPrivacy: () -> Unit = {}) {
    val context = LocalContext.current
    if (!show) return
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "关于 墨麒麟记账",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "版本 ${AppVersionUtils.Get(context)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "一款基于 Jetpack Compose 的 Android 个人记账应用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))
                Text("开源仓库", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Gitee 仓库") },
                    supportingContent = { Text("gitee.com/Murchey/inkqinlin-ledger", fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://gitee.com/Murchey/inkqinlin-ledger"))
                            )
                        }
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("GitHub 仓库") },
                    supportingContent = { Text("github.com/Murchey/inkqilin-ledger", fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Murchey/inkqilin-ledger"))
                            )
                        }
                    }
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onShowUsageGuide()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("使用引导")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        /* privacy via parent */
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("隐私政策")
                }
            }
        }
    }

@Composable
internal fun SettingsHomeBgSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: TransactionViewModel,
    homeBgImagePath: String?,
    homeBgOpacity: Float,
    homeTxCardOpacity: Float,
    onSetHomeBgPath: (String?) -> Unit,
    onSetHomeBgOpacity: (Float) -> Unit,
    onSetHomeTxCardOpacity: (Float) -> Unit,
    onImportHomeBg: () -> Unit,
    onClearHomeBg: () -> Unit,
) {
    if (!show) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (show) {
        val homeBgPath by viewModel.homeBgImagePath.collectAsState()
        val savedOpacity by viewModel.homeBgOpacity.collectAsState()
        // 预览与滑条用草稿值，点「确定」才写入
        var draftOpacity by remember(show) { mutableFloatStateOf(savedOpacity) }
        val savedTxOpacity by viewModel.homeTxCardOpacity.collectAsState()
        var draftTxOpacity by remember(show) { mutableFloatStateOf(savedTxOpacity) }
        val bgPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { viewModel.importHomeBackground(context, it) }
        }

        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    "首页背景图",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "从相册选择图片作为首页背景；账单列表会半透明显示，便于透出背景。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                // 可滚动主体：内容足够显示时随内容收缩；超高才内部滚动，按钮固定可见
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val file = homeBgPath?.let { java.io.File(it) }
                        if (file != null && file.exists()) {
                            coil.compose.AsyncImage(
                                model = file,
                                contentDescription = "背景预览",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alpha = draftOpacity.coerceIn(0.05f, 1f),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                "尚未选择背景图",
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { bgPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (homeBgPath.isNullOrBlank()) "选择图片" else "更换图片")
                        }
                        if (!homeBgPath.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { viewModel.clearHomeBackground() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("清除背景")
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "不透明度 ${(draftOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = draftOpacity,
                        onValueChange = { draftOpacity = it },
                        valueRange = 0.05f..1f
                    )
                    Text(
                        "值越大背景越清晰；建议 20%–50% 以保证账单可读性。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "账单条目不透明度 ${(draftTxOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = draftTxOpacity,
                        onValueChange = { draftTxOpacity = it },
                        valueRange = 0.08f..1f
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            viewModel.setHomeBgOpacity(draftOpacity)
                            viewModel.setHomeTxCardOpacity(draftTxOpacity)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("确定") }
                }
            }
        }
    }
}

@Composable
internal fun SettingsStorageSheet(show: Boolean, onDismiss: () -> Unit, viewModel: TransactionViewModel) {
    if (!show) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (show) {
        var usageList by remember { mutableStateOf(emptyList<com.inkqilin.ledger.util.StorageUsageManager.UsageItem>()) }
        var storageMsg by remember { mutableStateOf<String?>(null) }
        var clearTarget by remember { mutableStateOf<String?>(null) } // album / backup / cache / update

        LaunchedEffect(show) {
            if (show) {
                usageList = withContext(Dispatchers.IO) {
                    com.inkqilin.ledger.util.StorageUsageManager.collectUsage(context)
                }
            }
        }

        val total = com.inkqilin.ledger.util.StorageUsageManager.totalBytes(usageList)

        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("储存空间管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "本机占用合计 ${com.inkqilin.ledger.util.StorageUsageManager.formatSize(total)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                storageMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))

                usageList.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.label) },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(com.inkqilin.ledger.util.StorageUsageManager.formatSize(item.sizeBytes))
                                    if (item.fileCount > 0) append(" · ${item.fileCount} 个文件")
                                }
                            )
                        },
                        trailingContent = {
                            val clearable = item.key in setOf("album", "backup", "cache", "update")
                            if (clearable && item.sizeBytes > 0) {
                                TextButton(onClick = { clearTarget = item.key }) { Text("清理") }
                            }
                        }
                    )
                    HorizontalDivider()
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "清理后不可恢复；重要数据请先到「数据备份」导出。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        clearTarget?.let { key ->
            val label = usageList.find { it.key == key }?.label ?: "数据"
            AlertDialog(
                onDismissRequest = { clearTarget = null },
                title = { Text("清理$label") },
                text = { Text("将删除 $label 占用的文件，此操作不可撤销。") },
                confirmButton = {
                    Button(onClick = {
                        clearTarget = null
                        scope.launch {
                            when (key) {
                                "album" -> {
                                    val ok = viewModel.clearAllAlbumPhotos(context)
                                    storageMsg = if (ok) "已清空记账相册" else "清理失败"
                                }
                                "backup" -> {
                                    val ok = viewModel.clearLocalBackups(context)
                                    storageMsg = if (ok) "已清空本地备份" else "清理失败"
                                }
                                "cache" -> {
                                    val ok = withContext(Dispatchers.IO) {
                                        com.inkqilin.ledger.util.StorageUsageManager.clearCache(context)
                                    }
                                    storageMsg = if (ok) "已清空缓存" else "清理失败"
                                }
                                "update" -> {
                                    val ok = withContext(Dispatchers.IO) {
                                        com.inkqilin.ledger.util.StorageUsageManager.clearUpdatePackages(context)
                                    }
                                    storageMsg = if (ok) "已删除更新包" else "清理失败"
                                }
                            }
                            usageList = withContext(Dispatchers.IO) {
                                com.inkqilin.ledger.util.StorageUsageManager.collectUsage(context)
                            }
                        }
                    }) { Text("清理") }
                },
                dismissButton = {
                    TextButton(onClick = { clearTarget = null }) { Text("取消") }
                }
            )
        }
    }
}
