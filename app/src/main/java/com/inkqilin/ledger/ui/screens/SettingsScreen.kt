@file:Suppress("AssignedValueIsNeverRead")

package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.ui.*
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class ExportTimeRange(val label: String) {
    ALL("全部"),
    THIS_YEAR("本年"),
    CUSTOM("自定义")
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packageNames.contains(context.packageName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToKeywordCategoryManagement: () -> Unit = {},
    onNavigateToContactManagement: () -> Unit = {},
    onNavigateToCurrencyManagement: () -> Unit = {},
    onNavigateToAIConfig: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by viewModel.themeMode.collectAsState()
    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val customPrimaryColorHex by viewModel.customPrimaryColorHex.collectAsState()
    val autoRecordEnabled by viewModel.autoRecordEnabled.collectAsState()
    val ocrEnabled by viewModel.ocrEnabled.collectAsState()
    val albumEnabled by viewModel.albumEnabled.collectAsState()
    val aiApiKey by viewModel.aiApiKey.collectAsState()

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
        var displaySettingsExpanded by remember { mutableStateOf(false) }
        Text(
            text = "显示设置",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { displaySettingsExpanded = !displaySettingsExpanded }
                .padding(bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
                Divider()
                ListItem(
                    headlineContent = { Text("主题色") },
                    supportingContent = { Text(if (customPrimaryColorHex != null) "自定义" else "默认靛蓝") },
                    trailingContent = {
                        Icon(
                            if (displaySettingsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.clickable { displaySettingsExpanded = !displaySettingsExpanded }
                        )
                    },
                    modifier = Modifier.clickable { displaySettingsExpanded = !displaySettingsExpanded }
                )

                AnimatedVisibility(
                    visible = displaySettingsExpanded,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(MotionDurations.MEDIUM)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(MotionDurations.SHORT)
                    ) + fadeOut(
                        animationSpec = tween(MotionDurations.FAST)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        val currentPrimary = MaterialTheme.colorScheme.primary
                        val presetThemeColors = listOf(
                            "#7C5CFF" to "紫罗兰",
                            DEFAULT_PRIMARY_COLOR_HEX to "青翠绿",
                            "#1565C0" to "深蓝",
                            "#00897B" to "青绿",
                            "#43A047" to "翠绿",
                            "#E65100" to "深橙",
                            "#D32F2F" to "中国红",
                            "#00838F" to "暗青",
                            "#5C6BC0" to "蓝紫",
                            "#EC407A" to "玫粉"
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(presetThemeColors) { (hex, _) ->
                                val parsed = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (_: Exception) {
                                    currentPrimary
                                }
                                val isSelected = (customPrimaryColorHex ?: DEFAULT_PRIMARY_COLOR_HEX).equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(parsed)
                                        .clickable { viewModel.setCustomPrimaryColor(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        var colorInput by remember(customPrimaryColorHex) {
                            mutableStateOf(customPrimaryColorHex ?: DEFAULT_PRIMARY_COLOR_HEX)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val previewColor = try {
                                Color(android.graphics.Color.parseColor(colorInput))
                            } catch (_: Exception) {
                                currentPrimary
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
                                    if (newVal.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                                        viewModel.setCustomPrimaryColor(newVal)
                                    }
                                },
                                label = { Text("自定义颜色", fontSize = 12.sp) },
                                placeholder = { Text("#RRGGBB", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (customPrimaryColorHex != null && customPrimaryColorHex != DEFAULT_PRIMARY_COLOR_HEX) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.setCustomPrimaryColor(null) }) {
                                Text("恢复默认主题色")
                            }
                        }
                    }
                }
            }
        }

        Text(text = "分类管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("账单标签（类别）管理") },
                    supportingContent = { Text("添加、修改或删除收支分类及人情标签") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToCategoryManagement() }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("备注自动识别关键词管理") },
                    supportingContent = { Text("配置关键词自动选择账单分类") },
                    leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToKeywordCategoryManagement() }
                )
            }
        }

        val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
        Text(text = "人情账本", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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

        Text(text = "实验室功能", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("自动记账") },
                    supportingContent = { Text("捕获支付宝/微信支付通知，自动记录账单。需要在手机的“自启动管理”和“电池优化”中把“墨麒麟记账”设为“不受限制”") },
                    trailingContent = {
                        Switch(
                            checked = autoRecordEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !isNotificationServiceEnabled(context)) {
                                    // 引导开启权限
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                    Toast.makeText(context, "请先开启通知监听权限", Toast.LENGTH_LONG).show()
                                }
                                viewModel.setAutoRecordEnabled(enabled)
                            }
                        )
                    }
                )
                if (autoRecordEnabled && !isNotificationServiceEnabled(context)) {
                    Divider()
                    ListItem(
                        headlineContent = { Text("未开启监听权限", color = MaterialTheme.colorScheme.error) },
                        supportingContent = { Text("点击去开启，否则自动记账无法生效") },
                        trailingContent = {
                            TextButton(onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            }) {
                                Text("去开启")
                            }
                        }
                    )
                }
                if (albumEnabled) {
                    Divider()
                    var cleanupResult by remember { mutableStateOf<TransactionViewModel.CleanupResult?>(null) }
                    var isCleaning by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text("清理相册缓存") },
                        supportingContent = {
                            Text(
                                cleanupResult?.let {
                                    if (it.deletedCount > 0) "已清理 ${it.deletedCount} 个文件，释放 ${formatFileSize(it.freedBytes)}"
                                    else "没有需要清理的缓存文件"
                                } ?: "删除已从相册移除但仍在缓存中的图片"
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    isCleaning = true
                                    scope.launch {
                                        cleanupResult = viewModel.cleanupOrphanedAlbumFiles(context)
                                        isCleaning = false
                                    }
                                },
                                enabled = !isCleaning
                            ) {
                                Text(if (isCleaning) "清理中..." else "清理")
                            }
                        }
                    )
                }
                Divider()
                ListItem(
                    headlineContent = { Text("OCR账单识别") },
                    supportingContent = { Text("通过 AI 识别图片账单并批量导入") },
                    trailingContent = {
                        Switch(
                            checked = ocrEnabled,
                            onCheckedChange = { viewModel.setOcrEnabled(it) }
                        )
                    }
                )
                if (ocrEnabled) {
                    Divider()
                    ListItem(
                        headlineContent = { Text("AI API 配置") },
                        supportingContent = { Text(if (aiApiKey.isEmpty()) "点击配置 API Key" else "已配置 API Key") },
                        trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToAIConfig() }
                    )
                }
                Divider()
                ListItem(
                    headlineContent = { Text("记账相册") },
                    supportingContent = { Text("用于保存重要账单的原件，可以直接连接OCR功能。拍摄的照片会同步到系统相册，删除照片仅在本APP生效，不会删除系统相册内容。") },
                    trailingContent = {
                        Switch(
                            checked = albumEnabled,
                            onCheckedChange = { viewModel.setAlbumEnabled(it) }
                        )
                    }
                )
            }
        }

        Text(text = "数据管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
    
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource), // iOS-style interactive feedback
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
                    } catch (_: Exception) {
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
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
            val isDark = MaterialTheme.colorScheme.background.let { it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f } < 0.5f
            val resolvedColor = resolveCardColor(asset, isDark)
            val animatedCardColor by animateColorAsState(
                targetValue = resolvedColor,
                animationSpec = MotionSprings.interactive(), // iOS-like bouncy card color
                label = "cardColor_${asset.id}"
            )
            val assetInteractionSource = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(assetInteractionSource), // iOS-style interactive feedback
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = animatedCardColor),
                interactionSource = assetInteractionSource,
                onClick = {}
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
    var cardColor by remember { mutableStateOf(asset?.cardColor ?: "#1E6FFF") }
    var cardColorLight by remember { mutableStateOf(asset?.cardColorLight ?: asset?.cardColor ?: "#5B87FF") }
    var colorInput by remember { mutableStateOf(asset?.cardColor ?: "#1E6FFF") }
    val isEdit = asset != null
    val isDark = MaterialTheme.colorScheme.background.let { it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f } < 0.5f

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
                    items(CardColorPresets) { preset ->
                        val displayHex = if (isDark) preset.dark else preset.light
                        val c = try {
                            Color(android.graphics.Color.parseColor(displayHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        val isSelected = cardColor == preset.dark
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable {
                                    cardColor = preset.dark
                                    cardColorLight = preset.light
                                    colorInput = preset.dark
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
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
                    } catch (_: Exception) {
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
                        (asset ?: CurrencyAsset(code = code, symbol = symbol, name = name, cardColor = cardColor, cardColorLight = cardColorLight)).copy(
                            code = code,
                            symbol = symbol,
                            name = name,
                            cardColor = cardColor,
                            cardColorLight = cardColorLight
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
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column {
                        ListItem(headlineContent = { Text("深浅色模式") }, supportingContent = { Text("跟随系统") }, trailingContent = { TextButton(onClick = {}) { Text("切换") } })
                        Divider()
                        ListItem(headlineContent = { Text("收入展示颜色") }, trailingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF4CAF50))) })
                        Divider()
                        ListItem(headlineContent = { Text("支出展示颜色") }, trailingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF44336))) })
                    }
                }
                Text("分类管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(headlineContent = { Text("账单标签（类别）管理") }, supportingContent = { Text("添加、修改或删除收支分类及人情标签") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable {})
                }
                Text("更新检测", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(
                        headlineContent = { Text("启动时检测新版本") },
                        supportingContent = { Text("已启用，启动时自动检测 GitHub 新版本") },
                        trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                    )
                }
                Text("关于", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(headlineContent = { Text("关于 墨麒麟记账") }, supportingContent = { Text("版本 1.3.0 · GitHub 仓库") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable {})
                }
            }
        }
    }
}
