package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.util.CloudBackupManager
import com.inkqilin.ledger.util.WebDavConfig
import com.inkqilin.ledger.util.WebDavObjectMeta

@Composable
internal fun ColumnScope.WebDavBackupSection(
    config: WebDavConfig,
    backups: List<WebDavObjectMeta>,
    loading: Boolean,
    working: Boolean,
    onBackup: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (WebDavObjectMeta) -> Unit,
    onDelete: (WebDavObjectMeta) -> Unit
) {
    if (!config.isConfigured) {
        Text("请先配置 WebDAV 服务器、用户名及密码。", style = MaterialTheme.typography.bodyMedium)
        return
    }
    ListItem(
        headlineContent = { Text("WebDAV 备份 "+backups.size+" 项") },
        supportingContent = { Text(config.displayEndpoint+" / "+config.prefix) }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = !working && !loading, onClick = onBackup) { Text("立即备份") }
        OutlinedButton(enabled = !working && !loading, onClick = onRefresh) { Text("刷新") }
    }
    if (loading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (backups.isEmpty() && !loading) {
        Text("暂无 WebDAV 备份", modifier = Modifier.padding(vertical = 16.dp))
    }
    LazyColumn(modifier = Modifier.weight(1f)) {
        items(backups, key = { it.path }) { backup ->
            Column {
                ListItem(
                    headlineContent = { Text(backup.path, softWrap = true) },
                    supportingContent = {
                        Text(CloudBackupManager.formatSize(backup.size)+" · "+backup.lastModified)
                    },
                    trailingContent = {
                        Row {
                            TextButton(enabled = !working && !loading, onClick = { onRestore(backup) }) {
                                Text("恢复")
                            }
                            IconButton(enabled = !working && !loading, onClick = { onDelete(backup) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除备份")
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
