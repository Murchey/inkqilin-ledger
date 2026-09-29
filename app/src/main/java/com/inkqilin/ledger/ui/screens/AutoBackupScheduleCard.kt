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
internal fun ColumnScope.AutoBackupScheduleCard(
    title: String,
    schedule: com.inkqilin.ledger.util.BackupSchedule,
    onChange: (com.inkqilin.ledger.util.BackupSchedule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = com.inkqilin.ledger.ui.theme.Corners.Md,
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
