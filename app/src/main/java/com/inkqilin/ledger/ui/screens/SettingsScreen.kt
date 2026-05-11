package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.util.ExcelExporter
import com.inkqilin.ledger.util.ExcelImporter
import com.inkqilin.ledger.util.RenQingExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.compose.runtime.*
import com.inkqilin.ledger.util.ThemeMode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.inkqilin.ledger.ui.motion.MotionDurations
import com.inkqilin.ledger.ui.motion.MotionSprings
import java.text.SimpleDateFormat
import java.util.*

private enum class ExportTimeRange(val label: String) {
    ALL("全部"),
    THIS_YEAR("本年"),
    CUSTOM("自定义")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToContactManagement: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by viewModel.themeMode.collectAsState()
    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()

    var exportTimeRange by remember { mutableStateOf(ExportTimeRange.ALL) }
    var exportStartDate by remember { mutableLongStateOf(
        Calendar.getInstance().apply { set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
    ) }
    var exportEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showExportStartPicker by remember { mutableStateOf(false) }
    var showExportEndPicker by remember { mutableStateOf(false) }

    var renQingExportTimeRange by remember { mutableStateOf(ExportTimeRange.ALL) }
    var renQingExportStartDate by remember { mutableLongStateOf(
        Calendar.getInstance().apply { set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
    ) }
    var renQingExportEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showRenQingExportStartPicker by remember { mutableStateOf(false) }
    var showRenQingExportEndPicker by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    if (showExportStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = exportStartDate)
        DatePickerDialog(
            onDismissRequest = { showExportStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { exportStartDate = it }
                    showExportStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showExportStartPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showExportEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = exportEndDate)
        DatePickerDialog(
            onDismissRequest = { showExportEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { exportEndDate = it }
                    showExportEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showExportEndPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showRenQingExportStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = renQingExportStartDate)
        DatePickerDialog(
            onDismissRequest = { showRenQingExportStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { renQingExportStartDate = it }
                    showRenQingExportStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showRenQingExportStartPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showRenQingExportEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = renQingExportEndDate)
        DatePickerDialog(
            onDismissRequest = { showRenQingExportEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { renQingExportEndDate = it }
                    showRenQingExportEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showRenQingExportEndPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val transactions = when (exportTimeRange) {
                        ExportTimeRange.ALL -> viewModel.allTransactions.first()
                        ExportTimeRange.THIS_YEAR -> {
                            val range = viewModel.getYearRange(Calendar.getInstance().get(Calendar.YEAR))
                            viewModel.getTransactionsByDateRange(range.first, range.second).first()
                        }
                        ExportTimeRange.CUSTOM -> {
                            val end = exportEndDate + 86400000L - 1
                            viewModel.getTransactionsByDateRange(exportStartDate, end).first()
                        }
                    }
                    val success = ExcelExporter.exportTransactionsToUri(context, it, transactions)
                    if (success) {
                        Toast.makeText(context, "导出成功！共 ${transactions.size} 条记录", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val templateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val success = ExcelExporter.exportTemplateToUri(context, it)
                    if (success) {
                        Toast.makeText(context, "模板下载成功！", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "模板下载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                viewModel.importTransactions(context, it)
                Toast.makeText(context, "导入请求已提交，正在后台处理...", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val renQingEventsExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val events = when (renQingExportTimeRange) {
                        ExportTimeRange.ALL -> renQingViewModel.allEvents.first()
                        ExportTimeRange.THIS_YEAR -> {
                            val range = renQingViewModel.getYearRange(Calendar.getInstance().get(Calendar.YEAR))
                            renQingViewModel.getEventsByDateRange(range.first, range.second).first()
                        }
                        ExportTimeRange.CUSTOM -> {
                            val end = renQingExportEndDate + 86400000L - 1
                            renQingViewModel.getEventsByDateRange(renQingExportStartDate, end).first()
                        }
                    }
                    val success = RenQingExporter.exportEventsToUri(context, it, events)
                    if (success) {
                        Toast.makeText(context, "人情账单导出成功！共 ${events.size} 条记录", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val renQingContactsExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val contacts = renQingViewModel.allContacts.first()
                    val success = RenQingExporter.exportContactsToUri(context, it, contacts)
                    if (success) {
                        Toast.makeText(context, "联系人导出成功！", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(text = "显示设置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("深浅色模式") },
                    supportingContent = {
                        Text(when (themeMode) {
                            ThemeMode.AUTO -> "跟随系统"
                            ThemeMode.LIGHT -> "浅色模式"
                            ThemeMode.DARK -> "深色模式"
                        })
                    },
                    trailingContent = {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text("切换")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("跟随系统") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.AUTO); expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("浅色模式") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT); expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("深色模式") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.DARK); expanded = false }
                                )
                            }
                        }
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("收入展示颜色") },
                    trailingContent = {
                        ColorPickerButton(
                            selectedColor = incomeColorHex,
                            onColorSelected = { viewModel.setIncomeColor(it) }
                        )
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("支出展示颜色") },
                    trailingContent = {
                        ColorPickerButton(
                            selectedColor = expenseColorHex,
                            onColorSelected = { viewModel.setExpenseColor(it) }
                        )
                    }
                )
            }
        }

        Text(text = "分类管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text("账单标签（类别）管理") },
                supportingContent = { Text("添加、修改或删除收支分类及人情标签") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToCategoryManagement() }
            )
        }

        val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
        Text(text = "人情账本", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text("启用人情账本") },
                supportingContent = { Text(if (renQingEnabled) "已启用，底部导航栏显示" else "未启用") },
                trailingContent = {
                    Switch(checked = renQingEnabled, onCheckedChange = { renQingViewModel.setRenQingEnabled(it) })
                }
            )
        }

        Text(text = "数据管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text("导出账单为 Excel") },
                    supportingContent = { Text("选择时间范围并导出记账记录") },
                    leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).animateContentSize()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportTimeRange.entries.forEach { range ->
                            FilterChip(
                                selected = exportTimeRange == range,
                                onClick = { exportTimeRange = range },
                                label = { Text(range.label) }
                            )
                        }
                    }
                    if (exportTimeRange == ExportTimeRange.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { showExportStartPicker = true },
                                label = { Text(sdf.format(Date(exportStartDate))) },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            Text("至", style = MaterialTheme.typography.bodySmall)
                            AssistChip(
                                onClick = { showExportEndPicker = true },
                                label = { Text(sdf.format(Date(exportEndDate))) },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedPressButton(
                        onClick = {
                            scope.launch {
                                val transactions = when (exportTimeRange) {
                                    ExportTimeRange.ALL -> viewModel.allTransactions.first()
                                    ExportTimeRange.THIS_YEAR -> {
                                        val range = viewModel.getYearRange(Calendar.getInstance().get(Calendar.YEAR))
                                        viewModel.getTransactionsByDateRange(range.first, range.second).first()
                                    }
                                    ExportTimeRange.CUSTOM -> {
                                        val end = exportEndDate + 86400000L - 1
                                        viewModel.getTransactionsByDateRange(exportStartDate, end).first()
                                    }
                                }
                                if (transactions.isNotEmpty()) {
                                    exportLauncher.launch("墨麒麟记账_${System.currentTimeMillis()}.xlsx")
                                } else {
                                    Toast.makeText(context, "暂无数据可导出", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("导出")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Divider()
                ListItem(
                    headlineContent = { Text("下载账单模板") },
                    supportingContent = { Text("导出 Excel 模板，填写后可导入") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = {
                            templateLauncher.launch("墨麒麟账单模板.xlsx")
                        }) {
                            Text("下载")
                        }
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("导入账单") },
                    supportingContent = { Text("从填写好的 Excel 模板导入记录") },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = {
                            importLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        }) {
                            Text("选择文件")
                        }
                    }
                )
                if (renQingEnabled) {
                    Divider()
                    ListItem(
                        headlineContent = { Text("联系人管理") },
                        supportingContent = { Text("添加、编辑或删除人情联系人") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToContactManagement() }
                    )
                    Divider()
                    ListItem(
                        headlineContent = { Text("导出人情账单") },
                        supportingContent = { Text("选择时间范围并导出人情来往记录") },
                        leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).animateContentSize()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExportTimeRange.entries.forEach { range ->
                                FilterChip(
                                    selected = renQingExportTimeRange == range,
                                    onClick = { renQingExportTimeRange = range },
                                    label = { Text(range.label) }
                                )
                            }
                        }
                        if (renQingExportTimeRange == ExportTimeRange.CUSTOM) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = { showRenQingExportStartPicker = true },
                                    label = { Text(sdf.format(Date(renQingExportStartDate))) },
                                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("至", style = MaterialTheme.typography.bodySmall)
                                AssistChip(
                                    onClick = { showRenQingExportEndPicker = true },
                                    label = { Text(sdf.format(Date(renQingExportEndDate))) },
                                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedPressButton(
                            onClick = {
                                scope.launch {
                                    val events = when (renQingExportTimeRange) {
                                        ExportTimeRange.ALL -> renQingViewModel.allEvents.first()
                                        ExportTimeRange.THIS_YEAR -> {
                                            val range = renQingViewModel.getYearRange(Calendar.getInstance().get(Calendar.YEAR))
                                            renQingViewModel.getEventsByDateRange(range.first, range.second).first()
                                        }
                                        ExportTimeRange.CUSTOM -> {
                                            val end = renQingExportEndDate + 86400000L - 1
                                            renQingViewModel.getEventsByDateRange(renQingExportStartDate, end).first()
                                        }
                                    }
                                    if (events.isNotEmpty()) {
                                        renQingEventsExportLauncher.launch("人情账单_${System.currentTimeMillis()}.xlsx")
                                    } else {
                                        Toast.makeText(context, "暂无人情账单可导出", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("导出")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Divider()
                    ListItem(
                        headlineContent = { Text("导出联系人") },
                        supportingContent = { Text("导出所有人情联系人列表") },
                        leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Button(onClick = {
                                scope.launch {
                                    val contacts = renQingViewModel.allContacts.first()
                                    if (contacts.isNotEmpty()) {
                                        renQingContactsExportLauncher.launch("联系人_${System.currentTimeMillis()}.xlsx")
                                    } else {
                                        Toast.makeText(context, "暂无联系人可导出", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Text("导出")
                            }
                        }
                    )
                }
                Divider()
                ListItem(
                    headlineContent = { Text("关于 墨麒麟记账") },
                    supportingContent = { Text("版本 1.2.0 · GitHub 仓库") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Murchey/inkqilin-ledger"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun AnimatedPressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = if (isPressed) {
            androidx.compose.animation.core.tween(MotionDurations.FAST)
        } else {
            MotionSprings.gentle()
        },
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun ColorPickerButton(selectedColor: String, onColorSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val colors = listOf("#715CFF", "#51B4FF", "#4CAF50", "#F44336", "#FF9800", "#9C27B0", "#E91E63", "#00BCD4", "#000000", "#795548")

    Box {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(selectedColor)))
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colors.take(5).forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                            .clickable { onColorSelected(colorHex); expanded = false }
                    )
                }
            }
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colors.drop(5).forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                            .clickable { onColorSelected(colorHex); expanded = false }
                    )
                }
            }
        }
    }
}
