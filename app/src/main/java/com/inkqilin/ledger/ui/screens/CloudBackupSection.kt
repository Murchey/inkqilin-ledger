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
internal fun ColumnScope.CloudBackupSection(
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
                items(backups, key = { it.key }) { item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                item.key.substringAfterLast('/'),
                                fontSize = 13.sp,
                                softWrap = true
                            )
                        },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(CloudBackupManager.formatSize(item.size))
                                    append(" · ")
                                    append(item.lastModified.take(19).replace('T', ' '))
                                    if (item.key.contains("_enc")) {
                                        append(" · 已加密")
                                    }
                                },
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
