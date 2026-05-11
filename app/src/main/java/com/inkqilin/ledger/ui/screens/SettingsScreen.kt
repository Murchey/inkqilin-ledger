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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
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
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

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
    onNavigateToContactManagement: () -> Unit = {},
    onNavigateToCurrencyManagement: () -> Unit = {}
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

        val multiCurrencyEnabled by viewModel.multiCurrencyEnabled.collectAsState()
        Text(text = "多币种管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("多币种资金管理") },
                    supportingContent = { Text(if (multiCurrencyEnabled) "已启用，首页显示多币种卡片" else "未启用") },
                    trailingContent = {
                        Switch(checked = multiCurrencyEnabled, onCheckedChange = { viewModel.setMultiCurrencyEnabled(it) })
                    }
                )
                if (multiCurrencyEnabled) {
                    Divider()
                    ListItem(
                        headlineContent = { Text("币种卡片管理") },
                        supportingContent = { Text("添加、编辑或删除币种金额卡片") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToCurrencyManagement() }
                    )
                }
            }
        }

        val checkUpdateEnabled by viewModel.checkUpdateEnabled.collectAsState()
        Text(text = "更新检测", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("启动时检测新版本") },
                    supportingContent = { Text(if (checkUpdateEnabled) "已启用，启动时自动检测 GitHub 新版本" else "已关闭") },
                    trailingContent = {
                        Switch(checked = checkUpdateEnabled, onCheckedChange = { viewModel.setCheckUpdateEnabled(it) })
                    }
                )
            }
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
                    supportingContent = { Text("版本 ${viewModel.getCurrentVersionName(context)} · GitHub 仓库") },
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
    var colorInput by remember { mutableStateOf(selectedColor) }
    val colors = listOf("#715CFF", "#51B4FF", "#4CAF50", "#F44336", "#FF9800", "#9C27B0", "#E91E63", "#00BCD4", "#000000", "#795548")

    Box {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(selectedColor)))
                .clickable { colorInput = selectedColor; expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val previewColor = try {
                        Color(android.graphics.Color.parseColor(colorInput))
                    } catch (e: Exception) {
                        Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                    )
                    OutlinedTextField(
                        value = colorInput,
                        onValueChange = { newVal ->
                            colorInput = newVal
                            if (newVal.matches(Regex("^#[0-9A-Fa-f]{6,8}$"))) {
                                onColorSelected(newVal)
                            }
                        },
                        label = { Text("颜色代码", fontSize = 11.sp) },
                        placeholder = { Text("#RRGGBB", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyManagementScreen(
    viewModel: TransactionViewModel
) {
    val allAssets by viewModel.allAssets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<CurrencyAsset?>(null) }

    if (showAddDialog) {
        CurrencyEditDialog(
            asset = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { asset ->
                viewModel.addCurrencyAsset(asset)
                showAddDialog = false
            }
        )
    }

    if (editingAsset != null) {
        CurrencyEditDialog(
            asset = editingAsset,
            onDismiss = { editingAsset = null },
            onConfirm = { asset ->
                viewModel.updateCurrencyAsset(asset)
                editingAsset = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "币种卡片管理",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加币种")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "管理您在首页展示的币种金额卡片，每张卡片代表一种货币的资产。点击心形图标可切换默认币种。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(allAssets, key = { it.id }) { asset ->
            val cardColor = try {
                Color(android.graphics.Color.parseColor(asset.cardColor))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${asset.symbol} ${asset.name}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = asset.code + if (asset.isDefault) " · 默认" else "",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!asset.isDefault) {
                            TextButton(onClick = {
                                val currentDefault = allAssets.firstOrNull { it.isDefault }
                                if (currentDefault != null) {
                                    viewModel.updateCurrencyAsset(currentDefault.copy(isDefault = false))
                                }
                                viewModel.updateCurrencyAsset(asset.copy(isDefault = true))
                            }) {
                                Text("设为默认", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { editingAsset = asset }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White)
                        }
                        if (!asset.isDefault) {
                            IconButton(onClick = { viewModel.deleteCurrencyAsset(asset) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (allAssets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无币种卡片", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("点击上方按钮添加", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyEditDialog(
    asset: CurrencyAsset?,
    onDismiss: () -> Unit,
    onConfirm: (CurrencyAsset) -> Unit
) {
    var code by remember { mutableStateOf(asset?.code ?: "") }
    var symbol by remember { mutableStateOf(asset?.symbol ?: "") }
    var name by remember { mutableStateOf(asset?.name ?: "") }
    var cardColor by remember { mutableStateOf(asset?.cardColor ?: "#D32F2F") }
    var colorInput by remember { mutableStateOf(asset?.cardColor ?: "#D32F2F") }
    val isEdit = asset != null
    val presetColors = listOf(
        "#D32F2F", "#1565C0", "#2E7D32", "#E65100",
        "#6A1B9A", "#00838F", "#4E342E", "#37474F"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑币种" else "添加币种") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("币种代码（如 CNY）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEdit
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("符号（如 ¥）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（如 人民币）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("卡片颜色", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetColors) { color ->
                        val c = try {
                            Color(android.graphics.Color.parseColor(color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { cardColor = color; colorInput = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (cardColor == color) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val previewColor = try {
                        Color(android.graphics.Color.parseColor(colorInput))
                    } catch (e: Exception) {
                        Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                    )
                    OutlinedTextField(
                        value = colorInput,
                        onValueChange = { newVal ->
                            colorInput = newVal
                            if (newVal.matches(Regex("^#[0-9A-Fa-f]{6,8}$"))) {
                                cardColor = newVal
                            }
                        },
                        label = { Text("自定义颜色代码") },
                        placeholder = { Text("#RRGGBB") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (code.isNotBlank() && symbol.isNotBlank() && name.isNotBlank()) {
                    onConfirm(
                        (asset ?: CurrencyAsset(code = code, symbol = symbol, name = name, cardColor = cardColor)).copy(
                            code = code,
                            symbol = symbol,
                            name = name,
                            cardColor = cardColor
                        )
                    )
                }
            }) { Text(if (isEdit) "保存" else "添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SettingsScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("显示设置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column {
                        ListItem(headlineContent = { Text("深浅色模式") }, supportingContent = { Text("跟随系统") }, trailingContent = { TextButton(onClick = {}) { Text("切换") } })
                        Divider()
                        ListItem(headlineContent = { Text("收入展示颜色") }, trailingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF4CAF50))) })
                        Divider()
                        ListItem(headlineContent = { Text("支出展示颜色") }, trailingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF44336))) })
                    }
                }
                Text("分类管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    ListItem(headlineContent = { Text("账单标签（类别）管理") }, supportingContent = { Text("添加、修改或删除收支分类及人情标签") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable {})
                }
                Text("更新检测", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("启动时检测新版本") },
                        supportingContent = { Text("已启用，启动时自动检测 GitHub 新版本") },
                        trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                    )
                }
                Text("关于", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(headlineContent = { Text("关于 墨麒麟记账") }, supportingContent = { Text("版本 1.3.0 · GitHub 仓库") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable {})
                }
            }
        }
    }
}
