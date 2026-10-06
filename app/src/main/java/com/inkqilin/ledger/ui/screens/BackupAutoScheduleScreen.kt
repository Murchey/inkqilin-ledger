package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.theme.Corners
import com.inkqilin.ledger.util.BackupFrequency
import com.inkqilin.ledger.util.BackupSchedule

/**
 * 自动备份计划二级页：本地 / 云端各一份。
 * [target] = "local" | "cloud"
 */
@Composable
fun BackupAutoScheduleScreen(
    viewModel: TransactionViewModel,
    target: String
) {
    val isLocal = target != "cloud"
    val schedule by if (isLocal) {
        viewModel.localBackupSchedule.collectAsState()
    } else {
        viewModel.cloudBackupSchedule.collectAsState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            if (isLocal) "自动备份（本地）" else "自动备份（云端）",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "自动备份文件名以 auto_backup 开头，与手动备份区分。" +
                if (!isLocal) "\n云端备份需先配置 COS 密钥。" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        AutoBackupScheduleForm(
            schedule = schedule,
            onChange = { transform ->
                if (isLocal) viewModel.updateLocalBackupSchedule(transform)
                else viewModel.updateCloudBackupSchedule(transform)
            }
        )
    }
}

@Composable
fun AutoBackupScheduleForm(
    schedule: BackupSchedule,
    onChange: ((BackupSchedule) -> BackupSchedule) -> Unit
) {
    // 密钥用本地草稿：避免 DataStore 异步回写导致光标前移/丢字
    var passwordDraft by remember { mutableStateOf(schedule.autoPassword) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Corners.Md,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("备份时机", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            FrequencyOptions(
                selected = schedule.frequency,
                onSelected = { frequency ->
                    onChange { it.copy(frequency = frequency) }
                }
            )

            when (schedule.frequency) {
                BackupFrequency.WEEKLY -> {
                    Spacer(Modifier.height(16.dp))
                    WeeklyDayOptions(
                        selectedWeekday = schedule.weekday,
                        onSelected = { weekday -> onChange { it.copy(weekday = weekday) } }
                    )
                }

                BackupFrequency.MONTHLY -> {
                    Spacer(Modifier.height(16.dp))
                    MonthlyDayOptions(
                        selectedDay = schedule.dayOfMonth,
                        onSelected = { day -> onChange { it.copy(dayOfMonth = day) } }
                    )
                }

                else -> Unit
            }

            Spacer(Modifier.height(16.dp))
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
                    onCheckedChange = { checked ->
                        onChange { it.copy(deletePreviousAuto = checked) }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = passwordDraft,
                onValueChange = { new ->
                    passwordDraft = new
                    onChange { it.copy(autoPassword = new) }
                },
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
}

/** 所有频率同时可见，最后一项横跨整行避免留下半格空白。 */
@Composable
private fun FrequencyOptions(
    selected: BackupFrequency,
    onSelected: (BackupFrequency) -> Unit
) {
    val frequencies = BackupFrequency.entries
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        frequencies.take(4).chunked(2).forEach { rowFrequencies ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowFrequencies.forEach { frequency ->
                    FrequencyOptionCard(
                        frequency = frequency,
                        selected = frequency == selected,
                        onClick = { onSelected(frequency) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        frequencies.getOrNull(4)?.let { frequency ->
            FrequencyOptionCard(
                frequency = frequency,
                selected = frequency == selected,
                onClick = { onSelected(frequency) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FrequencyOptionCard(
    frequency: BackupFrequency,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = Corners.Sm
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 58.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = frequency.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun WeeklyDayOptions(
    selectedWeekday: Int,
    onSelected: (Int) -> Unit
) {
    val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
    Column {
        Text("每周备份日", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekLabels.forEachIndexed { index, label ->
                DateOption(
                    label = label,
                    selected = selectedWeekday == index + 1,
                    onClick = { onSelected(index + 1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MonthlyDayOptions(
    selectedDay: Int,
    onSelected: (Int) -> Unit
) {
    val days = (1..31).toList()
    val listState = rememberLazyListState()
    val selectedIndex = (selectedDay - 1).coerceIn(0, days.lastIndex)

    // 页面重新进入或外部配置加载完成时，让当前日期落在可见区域。
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    Column {
        Text("每月备份日", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(days) { day ->
                DateOption(
                    label = "${day}日",
                    selected = day == selectedDay,
                    onClick = { onSelected(day) },
                    modifier = Modifier.width(48.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "超过当月天数则按月末执行",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                },
                shape = shape
            ),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
