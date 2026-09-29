@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import androidx.compose.foundation.layout.heightIn
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
