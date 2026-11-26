@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.util.CloudBackupManager
import com.inkqilin.ledger.util.CosConfig
import com.inkqilin.ledger.util.CosObjectMeta

@Composable
internal fun ColumnScope.CloudBackupSection(
    cosConfig: CosConfig,
    lastInfo: String?,
    backups: List<CosObjectMeta>,
    isLoadingList: Boolean,
    working: Boolean,
    onBackupNow: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (CosObjectMeta) -> Unit,
    onDelete: (CosObjectMeta) -> Unit,
    onConfigure: () -> Unit
) {
    CloudBackupDisplaySection(
        configured = cosConfig.isConfigured,
        subtitle = if (cosConfig.isConfigured) "腾讯云 COS · ${cosConfig.host}" else "未配置 COS，请填写密钥与存储桶 URL。",
        unconfiguredText = "尚未配置对象存储，请点「配置云备份」设置。",
        lastInfo = lastInfo,
        backups = backups,
        describe = {
            CloudBackupDisplayItem(it.key, it.size, it.lastModified.take(19).replace('T', ' '))
        },
        isLoadingList = isLoadingList,
        working = working,
        onBackupNow = onBackupNow,
        onRefresh = onRefresh,
        onRestore = onRestore,
        onDelete = onDelete,
        onConfigure = onConfigure
    )
}

internal data class CloudBackupDisplayItem(
    val path: String,
    val size: Long,
    val lastModified: String
)

@Composable
internal fun <T> ColumnScope.CloudBackupDisplaySection(
    configured: Boolean,
    subtitle: String,
    unconfiguredText: String,
    lastInfo: String?,
    backups: List<T>,
    describe: (T) -> CloudBackupDisplayItem,
    isLoadingList: Boolean,
    working: Boolean,
    onBackupNow: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (T) -> Unit,
    onDelete: (T) -> Unit,
    onConfigure: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = com.inkqilin.ledger.ui.theme.Corners.Md,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("云端备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (lastInfo != null) {
                Spacer(Modifier.height(6.dp))
                Text(lastInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = if (configured) onBackupNow else onConfigure, enabled = !working) {
                    Text(if (configured) "立即云备份" else "配置云备份")
                }
                OutlinedButton(onClick = onRefresh, enabled = configured && !working && !isLoadingList) {
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

    if (!configured) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    unconfiguredText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (backups.isEmpty() && !isLoadingList) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp)
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
                .heightIn(min = 360.dp)
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(backups, key = { describe(it).path }) { item ->
                    val displayItem = describe(item)
                    ListItem(
                        headlineContent = {
                            Text(
                                displayItem.path.substringAfterLast('/'),
                                fontSize = 13.sp,
                                softWrap = true
                            )
                        },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(CloudBackupManager.formatSize(displayItem.size))
                                    append(" · ")
                                    append(displayItem.lastModified)
                                    if (displayItem.path.contains("_enc")) {
                                        append(" · 已加密")
                                    }
                                },
                                fontSize = 12.sp
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(enabled = !working && !isLoadingList, onClick = { onRestore(item) }) { Text("恢复") }
                                IconButton(enabled = !working && !isLoadingList, onClick = { onDelete(item) }) {
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
