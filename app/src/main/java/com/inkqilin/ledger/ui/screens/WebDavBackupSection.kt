package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.inkqilin.ledger.util.WebDavConfig
import com.inkqilin.ledger.util.WebDavObjectMeta

@Composable
internal fun ColumnScope.WebDavBackupSection(
    config: WebDavConfig,
    lastInfo: String?,
    backups: List<WebDavObjectMeta>,
    loading: Boolean,
    working: Boolean,
    onBackup: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (WebDavObjectMeta) -> Unit,
    onDelete: (WebDavObjectMeta) -> Unit,
    onConfigure: () -> Unit
) {
    CloudBackupDisplaySection(
        configured = config.isConfigured,
        subtitle = if (config.isConfigured) "WebDAV · ${config.displayEndpoint} / ${config.prefix}" else "未配置 WebDAV，请填写服务器、用户名及密码。",
        unconfiguredText = "尚未配置 WebDAV，请点「配置云备份」设置。",
        lastInfo = lastInfo,
        backups = backups,
        describe = { CloudBackupDisplayItem(it.path, it.size, it.lastModified) },
        isLoadingList = loading,
        working = working,
        onBackupNow = onBackup,
        onRefresh = onRefresh,
        onRestore = onRestore,
        onDelete = onDelete,
        onConfigure = onConfigure
    )
}
