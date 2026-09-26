package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.util.BackupFrequency
import com.inkqilin.ledger.util.BackupSchedule
import kotlinx.coroutines.flow.distinctUntilChanged

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
            onChange = {
                if (isLocal) viewModel.setLocalBackupSchedule(it)
                else viewModel.setCloudBackupSchedule(it)
            }
        )
    }
}

/** 滚轮选择器：中项高亮，滚动停稳即生效 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5
) {
    val itemHeight = 36.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val centerIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val viewport = layout.viewportEndOffset - layout.viewportStartOffset
            val center = layout.viewportStartOffset + viewport / 2
            layout.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - center)
            }?.index ?: selectedIndex
        }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && selectedIndex != centerIndex) {
            listState.scrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx in items.indices && idx != selectedIndex) onSelected(idx)
            }
    }

    Box(modifier = modifier.height(itemHeight * visibleCount)) {
        // 中项高亮
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        )
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            horizontalAlignment = Alignment.CenterHorizontally,
            // 用 contentPadding 做首尾留白，保证 item 索引与 items 列表一致
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                vertical = itemHeight * (visibleCount / 2)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { i ->
                val selected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[i],
                        fontSize = if (selected) 16.sp else 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun AutoBackupScheduleForm(
    schedule: BackupSchedule,
    onChange: (BackupSchedule) -> Unit
) {
    val frequencyLabels = BackupFrequency.entries.map { it.label }
    val frequencyIndex = BackupFrequency.entries.indexOf(schedule.frequency).coerceAtLeast(0)
    val weekLabels = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    val dayLabels = (1..31).map { " $it 日" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("备份时机", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            WheelPicker(
                items = frequencyLabels,
                selectedIndex = frequencyIndex,
                onSelected = { idx ->
                    val freq = BackupFrequency.entries.getOrNull(idx) ?: BackupFrequency.OFF
                    onChange(schedule.copy(frequency = freq))
                },
                modifier = Modifier.fillMaxWidth()
            )

            when (schedule.frequency) {
                BackupFrequency.WEEKLY -> {
                    Spacer(Modifier.height(8.dp))
                    Text("每周", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    WheelPicker(
                        items = weekLabels,
                        selectedIndex = (schedule.weekday - 1).coerceIn(0, 6),
                        onSelected = { onChange(schedule.copy(weekday = it + 1)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BackupFrequency.MONTHLY -> {
                    Spacer(Modifier.height(8.dp))
                    Text("每月日期（超出当月天数则月末）", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    WheelPicker(
                        items = dayLabels,
                        selectedIndex = (schedule.dayOfMonth - 1).coerceIn(0, 30),
                        onSelected = { onChange(schedule.copy(dayOfMonth = it + 1)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> Unit
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
}
