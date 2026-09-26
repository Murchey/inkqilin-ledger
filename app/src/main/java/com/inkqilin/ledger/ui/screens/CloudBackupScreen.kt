package com.inkqilin.ledger.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.util.CloudBackupManager
import com.inkqilin.ledger.util.CosConfig
import com.inkqilin.ledger.util.CosObjectMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class BackupUiState {
    data object Idle : BackupUiState()
    data object Working : BackupUiState()
    data class Error(val message: String) : BackupUiState()
    data class Success(val message: String) : BackupUiState()
}

private sealed class RestoreConfirm {
    data class Cloud(val item: CosObjectMeta) : RestoreConfirm()
    data class Local(val file: File) : RestoreConfirm()
}

private sealed class PendingBackup {
    data object Local : PendingBackup()
    data object Cloud : PendingBackup()
}

/**
 * 数据备份二级页：本地备份 + 腾讯云 COS 云备份。
 * 标题与返回由 MainScreen 顶栏提供；COS 设置由顶栏齿轮驱动。
 */
@Composable
fun CloudBackupScreen(
    viewModel: TransactionViewModel,
    openSettings: Boolean,
    onOpenSettingsConsumed: () -> Unit,
    onNavigateAutoBackup: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cosConfig by viewModel.cosConfig.collectAsState()

    // 0 = 本地备份， 1 = 云端备份
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var uiState by remember { mutableStateOf<BackupUiState>(BackupUiState.Idle) }
    var showRestoreDoneDialog by remember { mutableStateOf(false) }
    var restoreConfirm by remember { mutableStateOf<RestoreConfirm?>(null) }
    var pendingBackup by remember { mutableStateOf<PendingBackup?>(null) }
    var showSafetyRestoreConfirm by remember { mutableStateOf(false) }
    var showFileInfo by remember { mutableStateOf(false) }
    var fileInfoText by remember { mutableStateOf("") }

    // 云端
    var cloudBackups by remember { mutableStateOf<List<CosObjectMeta>>(emptyList()) }
    var isLoadingCloudList by remember { mutableStateOf(false) }
    var deleteCloudTarget by remember { mutableStateOf<CosObjectMeta?>(null) }
    var lastCloudBackupInfo by remember { mutableStateOf<String?>(null) }

    // 本地
    var localBackups by remember { mutableStateOf<List<File>>(emptyList()) }
    var deleteLocalTarget by remember { mutableStateOf<File?>(null) }
    var lastLocalBackupInfo by remember { mutableStateOf<String?>(null) }
    var exportTarget by remember { mutableStateOf<File?>(null) }

    fun refreshLocalList() {
        localBackups = CloudBackupManager.listLocalBackups(context)
    }

    fun refreshCloudList() {
        if (!cosConfig.isConfigured) {
            cloudBackups = emptyList()
            return
        }
        isLoadingCloudList = true
        scope.launch {
            try {
                cloudBackups = withContext(Dispatchers.IO) { CloudBackupManager.listBackups(cosConfig) }
                uiState = BackupUiState.Idle
            } catch (e: Exception) {
                uiState = BackupUiState.Error(e.message ?: "加载云端列表失败")
            } finally {
                isLoadingCloudList = false
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val file = exportTarget
        exportTarget = null
        if (uri != null && file != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    CloudBackupManager.copyLocalBackupToUri(context, file, uri)
                }
                uiState = if (ok) {
                    BackupUiState.Success("已导出到所选位置")
                } else {
                    BackupUiState.Error("导出失败")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            uiState = BackupUiState.Working
            scope.launch {
                try {
                    val name = runCatching {
                        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                        }
                    }.getOrNull()
                    val file = withContext(Dispatchers.IO) {
                        CloudBackupManager.importBackupFromUri(context, uri, name)
                    }
                    uiState = BackupUiState.Success("已导入：${file.name}，可在列表中恢复")
                    refreshLocalList()
                } catch (e: Exception) {
                    uiState = BackupUiState.Error(e.message ?: "导入失败")
                }
            }
        }
    }

    LaunchedEffect(Unit) { refreshLocalList() }
    LaunchedEffect(cosConfig.isConfigured) {
        if (cosConfig.isConfigured) refreshCloudList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        // Tab
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("本地备份") }
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("云端备份") }
            )
        }

        Spacer(Modifier.height(12.dp))

        // 公共状态提示
        if (uiState is BackupUiState.Working) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text("处理中…", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
        }
        when (val s = uiState) {
            is BackupUiState.Error -> {
                Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            is BackupUiState.Success -> {
                Text(s.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            else -> Unit
        }

        if (selectedTab == 0) {
            val hasSafety = remember(localBackups) { CloudBackupManager.hasSafetyCopy(context) }
            val localSchedule by viewModel.localBackupSchedule.collectAsState()
            ListItem(
                headlineContent = { Text("自动备份设置") },
                supportingContent = {
                    Text(
                        if (localSchedule.frequency.name == "OFF") "未开启"
                        else localSchedule.frequency.label,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.clickable { onNavigateAutoBackup("local") }
            )
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            LocalBackupSection(
                lastInfo = lastLocalBackupInfo,
                backups = localBackups,
                working = uiState is BackupUiState.Working,
                hasSafetyCopy = hasSafety,
                onBackupNow = { pendingBackup = PendingBackup.Local },
                onRefresh = { refreshLocalList() },
                onRestore = { restoreConfirm = RestoreConfirm.Local(it) },
                onDelete = { deleteLocalTarget = it },
                onExport = { file ->
                    exportTarget = file
                    exportLauncher.launch(file.name)
                },
                onRestoreSafety = { showSafetyRestoreConfirm = true },
                onShowFileInfo = {
                    fileInfoText = CloudBackupManager.describeLocalBackupFiles(context)
                    showFileInfo = true
                },
                onImport = {
                    importLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/octet-stream",
                            "application/x-zip-compressed",
                            "*/*"
                        )
                    )
                }
            )
        } else {
            val cloudSchedule by viewModel.cloudBackupSchedule.collectAsState()
            ListItem(
                headlineContent = { Text("自动备份设置") },
                supportingContent = {
                    Text(
                        if (cloudSchedule.frequency.name == "OFF") "未开启"
                        else cloudSchedule.frequency.label,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.clickable { onNavigateAutoBackup("cloud") }
            )
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            CloudBackupSection(
                cosConfig = cosConfig,
                lastInfo = lastCloudBackupInfo,
                backups = cloudBackups,
                isLoadingList = isLoadingCloudList,
                working = uiState is BackupUiState.Working,
                onBackupNow = {
                    if (!cosConfig.isConfigured) {
                        uiState = BackupUiState.Error("请先点右上角齿轮配置 COS")
                        return@CloudBackupSection
                    }
                    pendingBackup = PendingBackup.Cloud
                },
                onRefresh = { refreshCloudList() },
                onRestore = { restoreConfirm = RestoreConfirm.Cloud(it) },
                onDelete = { deleteCloudTarget = it }
            )
        }
    }

    if (openSettings) {
        CosSettingsDialog(
            initial = cosConfig,
            onDismiss = { onOpenSettingsConsumed() },
            onSave = { config ->
                viewModel.setCosConfig(config)
                onOpenSettingsConsumed()
                uiState = BackupUiState.Success("COS 配置已保存")
            }
        )
    }

    // 备份：可选加密
    pendingBackup?.let { target ->
        BackupPasswordDialog(
            title = if (target is PendingBackup.Local) "本地备份" else "云端备份",
            onDismiss = { pendingBackup = null },
            onConfirm = { password ->
                pendingBackup = null
                uiState = BackupUiState.Working
                scope.launch {
                    try {
                        when (target) {
                            PendingBackup.Local -> {
                                val file = withContext(Dispatchers.IO) {
                                    CloudBackupManager.createLocalBackup(context, password)
                                }
                                lastLocalBackupInfo = "上次本地备份：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())} · ${CloudBackupManager.formatSize(file.length())}${if (password != null) " · 已加密" else ""}"
                                uiState = BackupUiState.Success("本地备份成功：${file.name}")
                                refreshLocalList()
                            }
                            PendingBackup.Cloud -> {
                                val meta = withContext(Dispatchers.IO) {
                                    CloudBackupManager.uploadBackup(context, cosConfig, password)
                                }
                                uiState = BackupUiState.Success("云备份成功：${meta.key.substringAfterLast('/')}")
                                lastCloudBackupInfo = "上次云备份：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())} · ${CloudBackupManager.formatSize(meta.size)}${if (password != null) " · 已加密" else ""}"
                                refreshCloudList()
                            }
                        }
                    } catch (e: Exception) {
                        uiState = BackupUiState.Error(e.message ?: "备份失败")
                    }
                }
            }
        )
    }

    restoreConfirm?.let { confirm ->
        val label = when (confirm) {
            is RestoreConfirm.Cloud -> confirm.item.key.substringAfterLast('/')
            is RestoreConfirm.Local -> confirm.file.name
        }
        var restorePassword by remember(confirm) { mutableStateOf("") }
        val localLooksEncrypted = when (confirm) {
            is RestoreConfirm.Local -> CloudBackupManager.isLocalBackupEncrypted(confirm.file)
            is RestoreConfirm.Cloud -> confirm.item.key.contains("_enc")
        }
        AlertDialog(
            onDismissRequest = { restoreConfirm = null },
            title = { Text("恢复将覆盖当前账本") },
            text = {
                Column {
                    Text("将使用备份「$label」替换本地数据库。恢复完成后需要关闭应用再打开。")
                    Spacer(Modifier.height(12.dp))
                    if (localLooksEncrypted) {
                        Text(
                            "此备份可能已加密，请输入备份密码。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                    } else {
                        Text(
                            "若为加密备份，请填写密码；未加密可留空。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("备份密码（未加密可留空）") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val confirmRef = confirm
                    restoreConfirm = null
                    val pwd = restorePassword.trim().toCharArray()
                    val pwdOrNull = if (pwd.isEmpty()) null else pwd
                    uiState = BackupUiState.Working
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                when (confirmRef) {
                                    is RestoreConfirm.Cloud ->
                                        CloudBackupManager.downloadAndRestore(
                                            context, cosConfig, confirmRef.item.key, pwdOrNull
                                        )
                                    is RestoreConfirm.Local ->
                                        CloudBackupManager.restoreLocalBackup(context, confirmRef.file, pwdOrNull)
                                }
                            }
                            // 恢复成功后 Room 单例已关闭，绝不能再走 Compose 继续跑旧 DAO
                            // 直接结束进程，用户重新打开即冷启动加载新库
                            (context as? android.app.Activity)?.finishAffinity()
                            android.os.Process.killProcess(android.os.Process.myPid())
                        } catch (e: Exception) {
                            // 失败时 Room 可能仍可用（校验阶段未 close）或已回滚
                            uiState = BackupUiState.Error(e.message ?: "恢复失败")
                        }
                    }
                }) { Text("我明白，恢复") }
            },
            dismissButton = {
                TextButton(onClick = { restoreConfirm = null }) { Text("取消") }
            }
        )
    }

    deleteCloudTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCloudTarget = null },
            title = { Text("删除云端备份") },
            text = { Text("确定删除 ${target.key.substringAfterLast('/')}？此操作不可撤销。") },
            confirmButton = {
                Button(onClick = {
                    val key = target.key
                    deleteCloudTarget = null
                    uiState = BackupUiState.Working
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { CloudBackupManager.deleteBackup(cosConfig, key) }
                            uiState = BackupUiState.Success("已删除")
                            refreshCloudList()
                        } catch (e: Exception) {
                            uiState = BackupUiState.Error(e.message ?: "删除失败")
                        }
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCloudTarget = null }) { Text("取消") }
            }
        )
    }

    deleteLocalTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteLocalTarget = null },
            title = { Text("删除本地备份") },
            text = { Text("确定删除 ${target.name}？此操作不可撤销。") },
            confirmButton = {
                Button(onClick = {
                    val ok = CloudBackupManager.deleteLocalBackup(target)
                    deleteLocalTarget = null
                    uiState = if (ok && !target.exists()) {
                        BackupUiState.Success("已彻底删除本地备份")
                    } else {
                        BackupUiState.Error("本地备份删除失败，请重试")
                    }
                    refreshLocalList()
                }) { Text("彻底删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteLocalTarget = null }) { Text("取消") }
            }
        )
    }

    if (showSafetyRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showSafetyRestoreConfirm = false },
            title = { Text("从安全副本恢复") },
            text = {
                Text("将用最近一次「恢复操作前」自动保存的库文件覆盖当前账本。仅当你确认当前数据异常时使用。完成后会关闭应用。")
            },
            confirmButton = {
                Button(onClick = {
                    showSafetyRestoreConfirm = false
                    uiState = BackupUiState.Working
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                CloudBackupManager.restoreFromSafetyCopy(context)
                            }
                            (context as? android.app.Activity)?.finishAffinity()
                            android.os.Process.killProcess(android.os.Process.myPid())
                        } catch (e: Exception) {
                            uiState = BackupUiState.Error(e.message ?: "安全副本恢复失败")
                        }
                    }
                }) { Text("覆盖并重启") }
            },
            dismissButton = {
                TextButton(onClick = { showSafetyRestoreConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showFileInfo) {
        AlertDialog(
            onDismissRequest = { showFileInfo = false },
            title = { Text("本机备份相关文件") },
            text = {
                Text(fileInfoText, fontSize = 12.sp)
            },
            confirmButton = {
                TextButton(onClick = { showFileInfo = false }) { Text("关闭") }
            }
        )
    }

    if (showRestoreDoneDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("恢复完成") },
            text = {
                Text(
                    "账本文件已替换。请点击「关闭应用」完全退出，" +
                        "再重新打开「墨麒麟记账」以加载新数据。\n\n" +
                        "若不退出，可能仍显示旧数据。"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRestoreDoneDialog = false
                    (context as? android.app.Activity)?.finishAffinity()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }) { Text("关闭应用") }
            }
        )
    }
}

@Composable
private fun ColumnScope.LocalBackupSection(
    lastInfo: String?,
    backups: List<File>,
    working: Boolean,
    hasSafetyCopy: Boolean,
    onBackupNow: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (File) -> Unit,
    onDelete: (File) -> Unit,
    onExport: (File) -> Unit,
    onRestoreSafety: () -> Unit,
    onShowFileInfo: () -> Unit,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("本地备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "打包账本数据库保存在应用私有目录。卸载应用会丢失，重要备份请「导出」到文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (lastInfo != null) {
                Spacer(Modifier.height(6.dp))
                Text(lastInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            // 主操作：等宽两列，避免挤在一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBackupNow,
                    enabled = !working,
                    modifier = Modifier.weight(1f)
                ) { Text("立即备份", maxLines = 1) }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !working,
                    modifier = Modifier.weight(1f)
                ) { Text("刷新", maxLines = 1) }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onImport,
                    enabled = !working,
                    modifier = Modifier.weight(1f)
                ) { Text("导入文件", maxLines = 1) }
                OutlinedButton(
                    onClick = onShowFileInfo,
                    modifier = Modifier.weight(1f)
                ) { Text("文件信息", maxLines = 1) }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRestoreSafety,
                enabled = hasSafetyCopy && !working,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (hasSafetyCopy) "从安全副本恢复" else "无安全副本",
                    maxLines = 1
                )
            }
            if (hasSafetyCopy) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "检测到「恢复前安全副本」，若当前账本异常可用它回滚。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("本地备份历史", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))

    if (backups.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有本地备份，点「立即本地备份」创建。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(backups, key = { it.absolutePath }) { file ->
                    ListItem(
                        headlineContent = {
                            Text(file.name, maxLines = 1, fontSize = 14.sp)
                        },
                        supportingContent = {
                            Text(
                                "${CloudBackupManager.formatSize(file.length())} · ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}",
                                fontSize = 12.sp
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onExport(file) }) { Text("导出") }
                                TextButton(onClick = { onRestore(file) }) { Text("恢复") }
                                IconButton(onClick = { onDelete(file) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.CloudBackupSection(
    cosConfig: CosConfig,
    lastInfo: String?,
    backups: List<CosObjectMeta>,
    isLoadingList: Boolean,
    working: Boolean,
    onBackupNow: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (CosObjectMeta) -> Unit,
    onDelete: (CosObjectMeta) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("云端备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (cosConfig.isConfigured) {
                    "腾讯云 COS · ${cosConfig.host}"
                } else {
                    "未配置 COS（点右上角齿轮填写密钥与存储桶 URL）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (lastInfo != null) {
                Spacer(Modifier.height(6.dp))
                Text(lastInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBackupNow, enabled = !working) { Text("立即云备份") }
                OutlinedButton(onClick = onRefresh, enabled = !working && !isLoadingList) {
                    if (isLoadingList) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("刷新")
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("云端备份历史", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))

    if (!cosConfig.isConfigured) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "尚未配置对象存储，请点右上角齿轮设置。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (backups.isEmpty() && !isLoadingList) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有云备份，点「立即云备份」创建第一份。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(backups, key = { it.key }) { item ->
                    ListItem(
                        headlineContent = {
                            Text(item.key.substringAfterLast('/'), maxLines = 1, fontSize = 14.sp)
                        },
                        supportingContent = {
                            Text(
                                "${CloudBackupManager.formatSize(item.size)} · ${item.lastModified.take(19).replace('T', ' ')}",
                                fontSize = 12.sp
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onRestore(item) }) { Text("恢复") }
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CosSettingsDialog(
    initial: CosConfig,
    onDismiss: () -> Unit,
    onSave: (CosConfig) -> Unit
) {
    var secretId by remember { mutableStateOf(initial.secretId) }
    var secretKey by remember { mutableStateOf(initial.secretKey) }
    var bucketUrl by remember { mutableStateOf(initial.bucketUrl) }
    var prefix by remember { mutableStateOf(initial.prefix) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("COS 设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "私有读写需要腾讯云 API 密钥。密钥仅保存在本机，请使用子账号并仅授权该存储桶。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = secretId,
                    onValueChange = { secretId = it },
                    label = { Text("SecretId") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = { Text("SecretKey") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bucketUrl,
                    onValueChange = { bucketUrl = it },
                    label = { Text("存储桶 URL") },
                    placeholder = { Text("https://xxx-1250000000.cos.ap-guangzhou.myqcloud.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "从 COS 控制台复制默认访问域名即可，无需再单独填地域。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("对象前缀") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val rawUrl = bucketUrl.trim().let {
                    if (it.isBlank()) "" else if (it.startsWith("http")) it else "https://$it"
                }
                onSave(
                    CosConfig(
                        secretId = secretId.trim(),
                        secretKey = secretKey.trim(),
                        bucketUrl = rawUrl,
                        prefix = prefix.trim().ifBlank { "backups/v1" }
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 备份选项：可不加密，或设置密码后 AES 加密整个备份包。
 * @param onConfirm password 为 null 表示不加密
 */
@Composable
private fun BackupPasswordDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (password: CharArray?) -> Unit
) {
    var encryptEnabled by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    "可选择是否为备份包设置密码。加密使用 AES-256，忘记密码将无法恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = encryptEnabled,
                        onCheckedChange = {
                            encryptEnabled = it
                            errorText = null
                        }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("加密备份")
                }
                if (encryptEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorText = null
                        },
                        label = { Text("密码（建议 8 位以上）") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordConfirm,
                        onValueChange = {
                            passwordConfirm = it
                            errorText = null
                        },
                        label = { Text("确认密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                errorText?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!encryptEnabled) {
                    onConfirm(null)
                    return@Button
                }
                val p = password
                if (p.length < 4) {
                    errorText = "密码至少 4 位"
                    return@Button
                }
                if (p != passwordConfirm) {
                    errorText = "两次密码不一致"
                    return@Button
                }
                onConfirm(p.toCharArray())
            }) {
                Text(if (encryptEnabled) "加密备份" else "不加密备份")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 自动备份计划卡片：本地 / 云端各一份 */
@Composable
private fun ColumnScope.AutoBackupScheduleCard(
    title: String,
    schedule: com.inkqilin.ledger.util.BackupSchedule,
    onChange: (com.inkqilin.ledger.util.BackupSchedule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "自动备份文件名以 auto_backup 开头，与手动备份区分；不会覆盖手动备份。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("备份时机", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.inkqilin.ledger.util.BackupFrequency.entries.take(3).forEach { freq ->
                    FilterChip(
                        selected = schedule.frequency == freq,
                        onClick = { onChange(schedule.copy(frequency = freq)) },
                        label = { Text(freq.label, fontSize = 11.sp) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.inkqilin.ledger.util.BackupFrequency.entries.drop(3).forEach { freq ->
                    FilterChip(
                        selected = schedule.frequency == freq,
                        onClick = { onChange(schedule.copy(frequency = freq)) },
                        label = { Text(freq.label, fontSize = 11.sp) }
                    )
                }
            }

            if (schedule.frequency == com.inkqilin.ledger.util.BackupFrequency.WEEKLY) {
                Spacer(Modifier.height(10.dp))
                Text("每周", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    weekLabels.forEachIndexed { i, label ->
                        val dow = i + 1
                        FilterChip(
                            selected = schedule.weekday == dow,
                            onClick = { onChange(schedule.copy(weekday = dow)) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (schedule.frequency == com.inkqilin.ledger.util.BackupFrequency.MONTHLY) {
                Spacer(Modifier.height(10.dp))
                Text("每月日期（超出当月天数则月末）", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    (1..28).forEach { day ->
                        FilterChip(
                            selected = schedule.dayOfMonth == day,
                            onClick = { onChange(schedule.copy(dayOfMonth = day)) },
                            label = { Text("$day", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("删除上次自动备份", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "仅删除 auto_backup 文件，手动备份保留",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = schedule.deletePreviousAuto,
                    onCheckedChange = { onChange(schedule.copy(deletePreviousAuto = it)) }
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = schedule.autoPassword,
                onValueChange = { onChange(schedule.copy(autoPassword = it)) },
                label = { Text("自动备份密钥（可选）") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = {
                    Text("填入后自动备份将加密；留空则不加密", style = MaterialTheme.typography.labelSmall)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}
