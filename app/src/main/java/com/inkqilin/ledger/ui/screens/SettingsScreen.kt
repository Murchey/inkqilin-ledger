@file:Suppress("AssignedValueIsNeverRead")

package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
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
import com.inkqilin.ledger.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private enum class ExportTimeRange(val label: String) {
    ALL("全部"),
    THIS_YEAR("本年"),
    CUSTOM("自定义")
}

private enum class ExportFormat(val label: String) {
    EXCEL("Excel"),
    CSV("CSV")
}

/** 导出过程中的进度展示 */
private sealed class ExportProgressState {
    data class Running(val fraction: Float, val message: String) : ExportProgressState()
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packageNames.contains(context.packageName)
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "收起" else "展开"
        )
    }
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
    onNavigateToAIConfig: () -> Unit = {},
    onNavigateToOCRConfig: () -> Unit = {},
    onNavigateToBillImport: () -> Unit = {},
    onNavigateToCloudBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationHelper.showTestNotification(context)
            Toast.makeText(context, "测试通知已发送", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "未授予通知权限", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationHelper.showTestNotification(context)
            Toast.makeText(context, "测试通知已发送", Toast.LENGTH_SHORT).show()
        }
    }
    val themeMode by viewModel.themeMode.collectAsState()
    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val customPrimaryColorHex by viewModel.customPrimaryColorHex.collectAsState()
    val homeCardColorHex by viewModel.homeCardColor.collectAsState()
    // 导出进度（必须在 exportLauncher 之前声明）
    var exportProgressState by remember { mutableStateOf<ExportProgressState.Running?>(null) }
    val importProgress by viewModel.excelProgress.collectAsState()
    var exportFormat by remember { mutableStateOf(ExportFormat.EXCEL) }
    val autoRecordEnabled by viewModel.autoRecordEnabled.collectAsState()
    val harmonyCompatMode by viewModel.harmonyCompatMode.collectAsState()
    val ocrEnabled by viewModel.ocrEnabled.collectAsState()
    val albumEnabled by viewModel.albumEnabled.collectAsState()
    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val ocrApiKey by viewModel.ocrApiKey.collectAsState()
    val appMode by viewModel.appMode.collectAsState()
    val aiDataRange by viewModel.aiDataRange.collectAsState()

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

    suspend fun runExport(uri: Uri, format: ExportFormat) {
        exportProgressState = ExportProgressState.Running(0f, "准备导出…")
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
        val assets = viewModel.allUserAssets.value
        val flows = viewModel.allAssetFlows.value
        val categories = viewModel.allCategories.first()
        val success = withContext(Dispatchers.IO) {
            when (format) {
                ExportFormat.EXCEL ->
                    ExcelExporter.exportToUri(context, uri, transactions, assets, flows, categories) { p ->
                        exportProgressState = ExportProgressState.Running(p.fraction, p.message)
                    }
                ExportFormat.CSV ->
                    CsvExporter.exportTransactionsCsv(context, uri, transactions, assets, flows) { p ->
                        exportProgressState = ExportProgressState.Running(p.fraction, p.message)
                    }
            }
        }
        exportProgressState = null
        Toast.makeText(
            context,
            if (success) "导出成功！账单${transactions.size}条（${format.label}）" else "导出失败",
            Toast.LENGTH_SHORT
        ).show()
    }

    if (showExportStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = exportStartDate)
        AppleDatePickerDialog(
            onDismissRequest = { showExportStartPicker = false },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { exportStartDate = it }
                    showExportStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showExportStartPicker = false }) { Text("取消") } }
        )
    }

    if (showExportEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = exportEndDate)
        AppleDatePickerDialog(
            onDismissRequest = { showExportEndPicker = false },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { exportEndDate = it }
                    showExportEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showExportEndPicker = false }) { Text("取消") } }
        )
    }

    if (showRenQingExportStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = renQingExportStartDate)
        AppleDatePickerDialog(
            onDismissRequest = { showRenQingExportStartPicker = false },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { renQingExportStartDate = it }
                    showRenQingExportStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showRenQingExportStartPicker = false }) { Text("取消") } }
        )
    }

    if (showRenQingExportEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = renQingExportEndDate)
        AppleDatePickerDialog(
            onDismissRequest = { showRenQingExportEndPicker = false },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { renQingExportEndDate = it }
                    showRenQingExportEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showRenQingExportEndPicker = false }) { Text("取消") } }
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let { scope.launch { runExport(it, ExportFormat.EXCEL) } }
        }
    )

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            uri?.let { scope.launch { runExport(it, ExportFormat.CSV) } }
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

    // 使用 rememberSaveable：进入二级页（云备份等）返回后仍保持展开状态
    var appSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var displaySectionExpanded by rememberSaveable { mutableStateOf(true) }
    var categorySectionExpanded by rememberSaveable { mutableStateOf(false) }
    var featureSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var currencySectionExpanded by rememberSaveable { mutableStateOf(false) }
    var updateSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var dataSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var widgetSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var showAboutSheet by rememberSaveable { mutableStateOf(false) }
    var showUsageGuide by rememberSaveable { mutableStateOf(false) }
    var showHomeBgSheet by rememberSaveable { mutableStateOf(false) }
    var showPrivacyPolicy by rememberSaveable { mutableStateOf(false) }
    var showStorageSheet by rememberSaveable { mutableStateOf(false) }
    // exportProgressState / importProgress 见函数开头（exportLauncher 需要先声明）

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        // region 1. 应用版本
        SettingsSectionHeader("应用版本", if (appMode == AppMode.SMART) "智能版" else "基础版", appSectionExpanded) { appSectionExpanded = !appSectionExpanded }
        AnimatedVisibility(visible = appSectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("基础版", fontWeight = if (appMode == AppMode.BASIC) FontWeight.Bold else FontWeight.Normal) },
                    supportingContent = { Text("注重隐私保护，软件不会对本地数据进行任何计算采集") },
                    leadingContent = {
                        RadioButton(
                            selected = appMode == AppMode.BASIC,
                            onClick = { viewModel.setAppMode(AppMode.BASIC) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setAppMode(AppMode.BASIC) }
                )
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("智能版", fontWeight = if (appMode == AppMode.SMART) FontWeight.Bold else FontWeight.Normal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "AI",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    },
                    supportingContent = { Text("软件会主动采集分析收入、支出习惯") },
                    leadingContent = {
                        RadioButton(
                            selected = appMode == AppMode.SMART,
                            onClick = { viewModel.setAppMode(AppMode.SMART) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setAppMode(AppMode.SMART) }
                )
                if (appMode == AppMode.SMART) {
                    Spacer(modifier = Modifier.height(0.5.dp))
                    ListItem(
                        headlineContent = { Text("AI 分析配置") },
                        supportingContent = {
                            Text(
                                if (aiApiKey.isBlank()) "未配置 API Key，点击配置"
                                else "数据范围：${aiDataRange.label}"
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToAIConfig() }
                    )
                }
            }
        }
        }
        // endregion

        SettingsSectionHeader("桌面小组件", "余额显示与刷新", widgetSectionExpanded) { widgetSectionExpanded = !widgetSectionExpanded }
        AnimatedVisibility(visible = widgetSectionExpanded) {
            WidgetSettingsPanel(viewModel)
        }

        // region 2. 显示设置
        var displaySettingsExpanded by rememberSaveable { mutableStateOf(false) }
        SettingsSectionHeader("显示设置", when (themeMode) {
            ThemeMode.AUTO -> "跟随系统"
            ThemeMode.LIGHT -> "浅色模式"
            ThemeMode.DARK -> "深色模式"
        }, displaySectionExpanded) { displaySectionExpanded = !displaySectionExpanded }
        AnimatedVisibility(visible = displaySectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("收入展示颜色") },
                    trailingContent = {
                        ColorPickerButton(
                            selectedColor = incomeColorHex,
                            onColorSelected = { viewModel.setIncomeColor(it) }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("支出展示颜色") },
                    trailingContent = {
                        ColorPickerButton(
                            selectedColor = expenseColorHex,
                            onColorSelected = { viewModel.setExpenseColor(it) }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(0.5.dp))
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
                        Spacer(modifier = Modifier.height(0.5.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        val currentPrimary = MaterialTheme.colorScheme.primary
                        val currentHex = (customPrimaryColorHex ?: DEFAULT_PRIMARY_COLOR_HEX).lowercase()
                        var showThemeColorPicker by remember { mutableStateOf(false) }

                        @Composable
                        fun PresetSwatchRow(
                            label: String,
                            presets: List<com.inkqilin.ledger.util.ThemeColorPreset>,
                            trailing: (@Composable () -> Unit)? = null
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(presets) { preset ->
                                    val parsed = try {
                                        Color(android.graphics.Color.parseColor(preset.hex))
                                    } catch (_: Exception) {
                                        currentPrimary
                                    }
                                    val isSelected = currentHex == preset.hex.lowercase()
                                    val checkTint = if (parsed.luminance() > 0.55f) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.White
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(parsed)
                                            .clickable { viewModel.setCustomPrimaryColor(preset.hex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = preset.name,
                                                tint = checkTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                if (trailing != null) {
                                    item { trailing() }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        PresetSwatchRow("亮色", com.inkqilin.ledger.util.BrightThemePresets) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                        )
                                    )
                                    .clickable { showThemeColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "自定义颜色",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        PresetSwatchRow("暗色（低饱和）", com.inkqilin.ledger.util.DarkThemePresets)

                        if (showThemeColorPicker) {
                            ColorPickerDialog(
                                initialColor = customPrimaryColorHex ?: DEFAULT_PRIMARY_COLOR_HEX,
                                onColorSelected = {
                                    viewModel.setCustomPrimaryColor(it)
                                    showThemeColorPicker = false
                                },
                                onDismiss = { showThemeColorPicker = false }
                            )
                        }

                        if (customPrimaryColorHex != null && !currentHex.equals(DEFAULT_PRIMARY_COLOR_HEX, ignoreCase = true)) {
                            TextButton(onClick = { viewModel.setCustomPrimaryColor(null) }) {
                                Text("恢复默认主题色")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 主页主币种卡片颜色
                        Text(
                            "主页主币种卡片颜色",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        var showHomeCardColorPicker by remember { mutableStateOf(false) }

                        @Composable
                        fun HomeCardSwatchRow(
                            label: String,
                            hexes: List<String>,
                            trailing: (@Composable () -> Unit)? = null
                        ) {
                            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(hexes) { hex ->
                                    val c = try {
                                        Color(android.graphics.Color.parseColor(hex))
                                    } catch (_: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    val selectedHex = homeCardColorHex ?: DEFAULT_HOME_CARD_COLOR_HEX
                                    val isSelected = selectedHex.equals(hex, true)
                                    val checkTint = if (c.luminance() > 0.55f) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.White
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .clickable { viewModel.setHomeCardColor(hex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = checkTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                if (trailing != null) {
                                    item { trailing() }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        HomeCardSwatchRow("亮色", com.inkqilin.ledger.ui.theme.BrightHomeCardPresets) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                        )
                                    )
                                    .clickable { showHomeCardColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "自定义颜色",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HomeCardSwatchRow("暗色（低饱和）", com.inkqilin.ledger.ui.theme.DarkHomeCardPresets)

                        if (homeCardColorHex != null) {
                            TextButton(onClick = { viewModel.setHomeCardColor(null) }) {
                                Text("恢复默认")
                            }
                        }

                        if (showHomeCardColorPicker) {
                            ColorPickerDialog(
                                initialColor = homeCardColorHex ?: DEFAULT_HOME_CARD_COLOR_HEX,
                                onColorSelected = {
                                    viewModel.setHomeCardColor(it)
                                    showHomeCardColorPicker = false
                                },
                                onDismiss = { showHomeCardColorPicker = false }
                            )
                        }
                    }
                }

                // 显示设置一级项：首页背景图（不放在主题色展开区内）
                Spacer(modifier = Modifier.height(0.5.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                val homeBgPathForRow by viewModel.homeBgImagePath.collectAsState()
                val homeBgOpacityForRow by viewModel.homeBgOpacity.collectAsState()
                ListItem(
                    headlineContent = { Text("首页背景图") },
                    supportingContent = {
                        Text(
                            if (homeBgPathForRow.isNullOrBlank()) "未设置 · 点击选择图片"
                            else "已设置 · 不透明度 ${(homeBgOpacityForRow * 100).toInt()}%"
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showHomeBgSheet = true }
                )
            }
        }

        }
        SettingsSectionHeader("分类管理", "账单分类与自动分类规则", categorySectionExpanded) { categorySectionExpanded = !categorySectionExpanded }
        AnimatedVisibility(visible = categorySectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("账单标签（类别）管理") },
                    supportingContent = { Text("添加、修改或删除收支分类及人情标签") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToCategoryManagement() }
                )
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("备注自动识别关键词管理") },
                    supportingContent = { Text("配置关键词自动选择账单分类") },
                    leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToKeywordCategoryManagement() }
                )
            }
        }

        }
        val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
        SettingsSectionHeader("功能开关", if (renQingEnabled) "人情账本已启用" else "按需开启页面功能", featureSectionExpanded) { featureSectionExpanded = !featureSectionExpanded }
        AnimatedVisibility(visible = featureSectionExpanded) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text("人情账本") },
                        supportingContent = {
                            Text(if (renQingEnabled) "已启用，底部导航栏显示人情页面" else "未启用；首次使用可按需开启")
                        },
                        trailingContent = {
                            Switch(checked = renQingEnabled, onCheckedChange = { renQingViewModel.setRenQingEnabled(it) })
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("记账相册") },
                        supportingContent = {
                            Text(if (albumEnabled) "已启用，底部导航栏显示相册页面" else "未启用；需要时再开启")
                        },
                        trailingContent = {
                            Switch(checked = albumEnabled, onCheckedChange = { viewModel.setAlbumEnabled(it) })
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("自动记账") },
                        supportingContent = {
                            Text(
                                when {
                                    harmonyCompatMode -> "鸿蒙/兼容环境不支持自动记账"
                                    autoRecordEnabled -> "已启用；需要通知监听权限"
                                    else -> "未启用；需要时再开启"
                                }
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = autoRecordEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !isNotificationServiceEnabled(context)) {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        Toast.makeText(context, "请先开启通知监听权限", Toast.LENGTH_LONG).show()
                                    }
                                    viewModel.setAutoRecordEnabled(enabled)
                                }
                            )
                        }
                    )
                }
            }
        }
        val multiCurrencyEnabled by viewModel.multiCurrencyEnabled.collectAsState()
        SettingsSectionHeader("多币种管理", if (multiCurrencyEnabled) "已启用" else "未启用", currencySectionExpanded) { currencySectionExpanded = !currencySectionExpanded }
        AnimatedVisibility(visible = currencySectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("多币种资金管理") },
                    supportingContent = { Text(if (multiCurrencyEnabled) "已启用，首页显示多币种卡片" else "未启用") },
                    trailingContent = {
                        Switch(checked = multiCurrencyEnabled, onCheckedChange = { viewModel.setMultiCurrencyEnabled(it) })
                    }
                )
                if (multiCurrencyEnabled) {
                    Spacer(modifier = Modifier.height(0.5.dp))
                    ListItem(
                        headlineContent = { Text("币种卡片管理") },
                        supportingContent = { Text("添加、编辑或删除币种金额卡片") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToCurrencyManagement() }
                    )
                }
            }
        }

        }
        val checkUpdateEnabled by viewModel.checkUpdateEnabled.collectAsState()
        val updateProxyUrl by viewModel.updateProxyUrl.collectAsState()
        val updateRepo by viewModel.updateRepo.collectAsState()
        val githubRepo by viewModel.githubRepo.collectAsState()
        val proxyOptions = com.inkqilin.ledger.util.PROXY_SOURCES + "自定义"
        var showProxyDropdown by remember { mutableStateOf(false) }
        var showCustomProxyInput by remember { mutableStateOf(false) }
        var customProxyUrl by remember { mutableStateOf("") }
        var showUpdateRepoDialog by remember { mutableStateOf(false) }
        var updateRepoInput by remember { mutableStateOf(updateRepo) }
        var showGithubRepoDialog by remember { mutableStateOf(false) }
        var githubRepoInput by remember { mutableStateOf(githubRepo) }

        SettingsSectionHeader("更新检测", if (checkUpdateEnabled) "启动时自动检查" else "已关闭", updateSectionExpanded) { updateSectionExpanded = !updateSectionExpanded }
        AnimatedVisibility(visible = updateSectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("启动时检测新版本") },
                    supportingContent = { Text(if (checkUpdateEnabled) "已启用，启动时自动检测 Gitee 新版本" else "已关闭") },
                    trailingContent = {
                        Switch(checked = checkUpdateEnabled, onCheckedChange = { viewModel.setCheckUpdateEnabled(it) })
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("更新检测仓库") },
                    supportingContent = { Text("Gitee: $updateRepo", maxLines = 2, fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        updateRepoInput = updateRepo
                        showUpdateRepoDialog = true
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("GitHub 下载仓库") },
                    supportingContent = { Text("GitHub: $githubRepo", maxLines = 2, fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        githubRepoInput = githubRepo
                        showGithubRepoDialog = true
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("检查更新") },
                    supportingContent = { Text("立即检测是否有新版本，有则弹出更新") },
                    leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    modifier = Modifier.clickable { viewModel.triggerManualUpdateCheck() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // 代理源选择
                Box {
                    ListItem(
                        headlineContent = { Text("使用代理源") },
                        supportingContent = {
                            val displayText = if (updateProxyUrl in com.inkqilin.ledger.util.PROXY_SOURCES) {
                                val idx = com.inkqilin.ledger.util.PROXY_SOURCES.indexOf(updateProxyUrl)
                                "代理 ${idx + 1}: ${com.inkqilin.ledger.util.PROXY_SOURCES[idx]}"
                            } else {
                                "自定义: $updateProxyUrl"
                            }
                            Text(displayText, maxLines = 1, fontSize = 12.sp)
                        },
                        modifier = Modifier.clickable { showProxyDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showProxyDropdown,
                        onDismissRequest = { showProxyDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        proxyOptions.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label, maxLines = 1, fontSize = 13.sp) },
                                onClick = {
                                    if (label == "自定义") {
                                        showCustomProxyInput = true
                                    } else {
                                        viewModel.setUpdateProxyUrl(label)
                                    }
                                    showProxyDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        }
        // 自定义代理源输入对话框
        if (showCustomProxyInput) {
            AlertDialog(
                onDismissRequest = { showCustomProxyInput = false },
                title = { Text("自定义代理源") },
                text = {
                    Column {
                        Text("请输入代理源 URL 前缀", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customProxyUrl,
                            onValueChange = { customProxyUrl = it },
                            placeholder = { Text("https://example.com/") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val url = customProxyUrl.trim()
                        if (url.isNotBlank()) {
                            viewModel.setUpdateProxyUrl(url)
                        }
                        showCustomProxyInput = false
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomProxyInput = false }) { Text("取消") }
                }
            )
        }

        // 更新检测仓库对话框
        if (showUpdateRepoDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateRepoDialog = false },
                title = { Text("更新检测仓库") },
                text = {
                    Column {
                        Text(
                            "填写 Gitee 仓库路径，用于检测新版本。支持 owner/repo 或完整地址。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "默认: ${com.inkqilin.ledger.util.DEFAULT_UPDATE_REPO}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = updateRepoInput,
                            onValueChange = { updateRepoInput = it },
                            placeholder = { Text("Murchey/inkqinlin-ledger") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val raw = updateRepoInput.trim()
                        val normalized = com.inkqilin.ledger.util.normalizeGiteeRepo(raw)
                        if (normalized.isBlank()) {
                            Toast.makeText(context, "路径不能为空", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.setUpdateRepo(normalized)
                            Toast.makeText(context, "已保存: $normalized", Toast.LENGTH_SHORT).show()
                            showUpdateRepoDialog = false
                        }
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateRepoDialog = false }) { Text("取消") }
                }
            )
        }

        // GitHub 下载仓库对话框
        if (showGithubRepoDialog) {
            AlertDialog(
                onDismissRequest = { showGithubRepoDialog = false },
                title = { Text("GitHub 下载仓库") },
                text = {
                    Column {
                        Text(
                            "填写 GitHub 仓库路径，用于「GitHub 仓库」和「代理」下载源。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "默认: ${com.inkqilin.ledger.util.DEFAULT_GITHUB_REPO}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = githubRepoInput,
                            onValueChange = { githubRepoInput = it },
                            placeholder = { Text("Murchey/inkqilin-ledger") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val raw = githubRepoInput.trim()
                        val normalized = com.inkqilin.ledger.util.normalizeGithubRepo(raw)
                        if (normalized.isBlank()) {
                            Toast.makeText(context, "路径不能为空", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.setGithubRepo(normalized)
                            Toast.makeText(context, "已保存: $normalized", Toast.LENGTH_SHORT).show()
                            showGithubRepoDialog = false
                        }
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showGithubRepoDialog = false }) { Text("取消") }
                }
            )
        }

        var labExpanded by rememberSaveable { mutableStateOf(false) }
        SettingsSectionHeader(
            title = "实验室功能",
            summary = if (autoRecordEnabled || ocrEnabled || albumEnabled) "部分功能已启用" else "未启用实验室功能",
            expanded = labExpanded,
            onClick = { labExpanded = !labExpanded }
        )
        AnimatedVisibility(visible = labExpanded) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column {
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("测试系统通知") },
                    supportingContent = { Text("发送一条测试通知，确认系统通知权限和声音正常") },
                    leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = { sendTestNotification() }) { Text("测试") }
                    }
                )
                Spacer(modifier = Modifier.height(0.5.dp))
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
                    Spacer(modifier = Modifier.height(0.5.dp))
                    ListItem(
                        headlineContent = { Text("OCR 识别 API 配置") },
                        supportingContent = { Text(if (ocrApiKey.isEmpty()) "点击配置 API Key" else "已配置 API Key") },
                        trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToOCRConfig() }
                    )
                }
            }
        }
    }


        SettingsSectionHeader("数据管理", "导入、导出与数据备份", dataSectionExpanded) { dataSectionExpanded = !dataSectionExpanded }
        AnimatedVisibility(visible = dataSectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("数据备份") },
                    supportingContent = { Text("本地备份 / 腾讯云 COS 云备份，可恢复账本") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToCloudBackup() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("储存空间管理") },
                    supportingContent = { Text("查看相册/备份/缓存占用并清理") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showStorageSheet = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("导出账单") },
                    supportingContent = { Text("选择时间范围，导出 Excel 或 CSV") },
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = exportFormat == fmt,
                                onClick = { exportFormat = fmt },
                                label = { Text(fmt.label) }
                            )
                        }
                    }
                    if (exportFormat == ExportFormat.CSV) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "CSV 更快，适合大数据量；可用 Excel/WPS 打开",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                if (transactions.isEmpty()) {
                                    Toast.makeText(context, "暂无数据可导出", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val ts = System.currentTimeMillis()
                                when (exportFormat) {
                                    ExportFormat.EXCEL -> exportLauncher.launch("墨麒麟记账_$ts.xlsx")
                                    ExportFormat.CSV -> csvExportLauncher.launch("墨麒麟记账_$ts.csv")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("导出 ${exportFormat.label}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(0.5.dp))
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
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("导入账单") },
                    supportingContent = { Text("支持本APP、微信、支付宝账单格式导入") },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToBillImport() }
                )
                if (renQingEnabled) {
                    Spacer(modifier = Modifier.height(0.5.dp))
                    ListItem(
                        headlineContent = { Text("联系人管理") },
                        supportingContent = { Text("添加、编辑或删除人情联系人") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToContactManagement() }
                    )
                    Spacer(modifier = Modifier.height(0.5.dp))
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
                    Spacer(modifier = Modifier.height(0.5.dp))
                    ListItem(
                        headlineContent = { Text("导出联系人") },
                        supportingContent = { Text("导出所有人情联系人列表") },
                        leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val contacts = renQingViewModel.allContacts.first()
                                        if (contacts.isNotEmpty()) {
                                            renQingContactsExportLauncher.launch("联系人_${System.currentTimeMillis()}.xlsx")
                                        } else {
                                            Toast.makeText(context, "暂无联系人可导出", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                elevation = appButtonElevation()
                            ) {
                                Text("导出")
                            }
                        }
                    )
                }
            }
        }
        }

        // 一级节点：关于（与数据管理等分区同级）
        var aboutSectionExpanded by rememberSaveable { mutableStateOf(false) }
        SettingsSectionHeader(
            "关于 墨麒麟记账",
            "版本 ${viewModel.getCurrentVersionName(context)} · 仓库与使用引导",
            aboutSectionExpanded
        ) { aboutSectionExpanded = !aboutSectionExpanded }
        AnimatedVisibility(visible = aboutSectionExpanded) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                ListItem(
                    headlineContent = { Text("关于 墨麒麟记账") },
                    supportingContent = { Text("版本、开源仓库与使用引导") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showAboutSheet = true }
                )
            }
        }

        val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)
        Spacer(modifier = Modifier.height(navBarBottomPadding + 76.dp))
    }

    // 关于抽屉
    if (showAboutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutSheet = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "关于 墨麒麟记账",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "版本 ${viewModel.getCurrentVersionName(context)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "一款基于 Jetpack Compose 的 Android 个人记账应用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))
                Text("开源仓库", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Gitee 仓库") },
                    supportingContent = { Text("gitee.com/Murchey/inkqinlin-ledger", fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://gitee.com/Murchey/inkqinlin-ledger"))
                            )
                        }
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("GitHub 仓库") },
                    supportingContent = { Text("github.com/Murchey/inkqilin-ledger", fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Murchey/inkqilin-ledger"))
                            )
                        }
                    }
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        showAboutSheet = false
                        showUsageGuide = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("使用引导")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        showAboutSheet = false
                        showPrivacyPolicy = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("隐私政策")
                }
            }
        }
    }

    // 隐私政策（可再次查看）
    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(
            requireAccept = false,
            onAccept = { showPrivacyPolicy = false },
            onDismiss = { showPrivacyPolicy = false }
        )
    }

    // 首页背景图抽屉
    if (showHomeBgSheet) {
        val homeBgPath by viewModel.homeBgImagePath.collectAsState()
        val savedOpacity by viewModel.homeBgOpacity.collectAsState()
        // 预览与滑条用草稿值，点「确定」才写入
        var draftOpacity by remember(showHomeBgSheet) { mutableFloatStateOf(savedOpacity) }
        val savedTxOpacity by viewModel.homeTxCardOpacity.collectAsState()
        var draftTxOpacity by remember(showHomeBgSheet) { mutableFloatStateOf(savedTxOpacity) }
        val bgPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { viewModel.importHomeBackground(context, it) }
        }

        ModalBottomSheet(
            onDismissRequest = { showHomeBgSheet = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    "首页背景图",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "从相册选择图片作为首页背景；账单列表会半透明显示，便于透出背景。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                // 可滚动主体：内容足够显示时随内容收缩；超高才内部滚动，按钮固定可见
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val file = homeBgPath?.let { java.io.File(it) }
                        if (file != null && file.exists()) {
                            coil.compose.AsyncImage(
                                model = file,
                                contentDescription = "背景预览",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alpha = draftOpacity.coerceIn(0.05f, 1f),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                "尚未选择背景图",
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { bgPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (homeBgPath.isNullOrBlank()) "选择图片" else "更换图片")
                        }
                        if (!homeBgPath.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { viewModel.clearHomeBackground() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("清除背景")
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "不透明度 ${(draftOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = draftOpacity,
                        onValueChange = { draftOpacity = it },
                        valueRange = 0.05f..1f
                    )
                    Text(
                        "值越大背景越清晰；建议 20%–50% 以保证账单可读性。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "账单条目不透明度 ${(draftTxOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = draftTxOpacity,
                        onValueChange = { draftTxOpacity = it },
                        valueRange = 0.08f..1f
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showHomeBgSheet = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            viewModel.setHomeBgOpacity(draftOpacity)
                            viewModel.setHomeTxCardOpacity(draftTxOpacity)
                            showHomeBgSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("确定") }
                }
            }
        }
    }

    // 导出/导入 进度
    exportProgressState?.let { state ->
        AlertDialog(
            onDismissRequest = { /* 导出中不允许关闭 */ },
            title = { Text("正在导出") },
            text = {
                Column {
                    Text(state.message, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { state.fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${(state.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    importProgress?.let { (fraction, message) ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("正在导入") },
            text = {
                Column {
                    Text(message, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    // 储存空间管理抽屉
    if (showStorageSheet) {
        var usageList by remember { mutableStateOf(emptyList<com.inkqilin.ledger.util.StorageUsageManager.UsageItem>()) }
        var storageMsg by remember { mutableStateOf<String?>(null) }
        var clearTarget by remember { mutableStateOf<String?>(null) } // album / backup / cache / update

        LaunchedEffect(showStorageSheet) {
            if (showStorageSheet) {
                usageList = withContext(Dispatchers.IO) {
                    com.inkqilin.ledger.util.StorageUsageManager.collectUsage(context)
                }
            }
        }

        val total = com.inkqilin.ledger.util.StorageUsageManager.totalBytes(usageList)

        ModalBottomSheet(
            onDismissRequest = { showStorageSheet = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("储存空间管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "本机占用合计 ${com.inkqilin.ledger.util.StorageUsageManager.formatSize(total)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                storageMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))

                usageList.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.label) },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(com.inkqilin.ledger.util.StorageUsageManager.formatSize(item.sizeBytes))
                                    if (item.fileCount > 0) append(" · ${item.fileCount} 个文件")
                                }
                            )
                        },
                        trailingContent = {
                            val clearable = item.key in setOf("album", "backup", "cache", "update")
                            if (clearable && item.sizeBytes > 0) {
                                TextButton(onClick = { clearTarget = item.key }) { Text("清理") }
                            }
                        }
                    )
                    HorizontalDivider()
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "清理后不可恢复；重要数据请先到「数据备份」导出。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        clearTarget?.let { key ->
            val label = usageList.find { it.key == key }?.label ?: "数据"
            AlertDialog(
                onDismissRequest = { clearTarget = null },
                title = { Text("清理$label") },
                text = { Text("将删除 $label 占用的文件，此操作不可撤销。") },
                confirmButton = {
                    Button(onClick = {
                        clearTarget = null
                        scope.launch {
                            when (key) {
                                "album" -> {
                                    val ok = viewModel.clearAllAlbumPhotos(context)
                                    storageMsg = if (ok) "已清空记账相册" else "清理失败"
                                }
                                "backup" -> {
                                    val ok = viewModel.clearLocalBackups(context)
                                    storageMsg = if (ok) "已清空本地备份" else "清理失败"
                                }
                                "cache" -> {
                                    val ok = withContext(Dispatchers.IO) {
                                        com.inkqilin.ledger.util.StorageUsageManager.clearCache(context)
                                    }
                                    storageMsg = if (ok) "已清空缓存" else "清理失败"
                                }
                                "update" -> {
                                    val ok = withContext(Dispatchers.IO) {
                                        com.inkqilin.ledger.util.StorageUsageManager.clearUpdatePackages(context)
                                    }
                                    storageMsg = if (ok) "已删除更新包" else "清理失败"
                                }
                            }
                            usageList = withContext(Dispatchers.IO) {
                                com.inkqilin.ledger.util.StorageUsageManager.collectUsage(context)
                            }
                        }
                    }) { Text("清理") }
                },
                dismissButton = {
                    TextButton(onClick = { clearTarget = null }) { Text("取消") }
                }
            )
        }
    }

    // 使用引导
    if (showUsageGuide) {
        AlertDialog(
            onDismissRequest = { showUsageGuide = false },
            title = { Text("使用引导") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("1. 首页底部「+」快速记一笔，可填金额、分类与备注。")
                    Text("2. 金额键盘支持四则运算与括号，例如 (20+5)×2。")
                    Text("3. 统计页按周/月/年查看收支，并可按分类钻取。")
                    Text("4. 设置里可开关基础版/智能版、主题色、人情账本与桌面小组件。")
                    Text("5. 数据管理支持 Excel 导入导出，以及本地/云端备份与恢复。")
                    Text("6. 更新检测默认走 Gitee Release，可在设置中改检测仓库。")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "更多说明见 Gitee / GitHub 仓库 README。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showUsageGuide = false }) { Text("知道了") }
            }
        )
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
        modifier = modifier.pressScale(interactionSource),
        interactionSource = interactionSource,
        elevation = appButtonElevation(),
        content = content
    )
}

@Composable
fun ColorPickerButton(selectedColor: String, onColorSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var colorInput by remember { mutableStateOf(selectedColor) }
    val presetColors = listOf("#5856D6", "#51B4FF", "#34C759", "#FF3B30", "#FF9500", "#9C27B0", "#FF2D55", "#00BCD4", "#000000")
    var showCustomPicker by remember { mutableStateOf(false) }

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
                    presetColors.take(5).forEach { colorHex ->
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
                    presetColors.drop(5).take(4).forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .clickable { onColorSelected(colorHex); expanded = false }
                        )
                    }
                    // Custom color button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                )
                            )
                            .clickable { showCustomPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "自定义颜色",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
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

    if (showCustomPicker) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = {
                onColorSelected(it)
                showCustomPicker = false
                expanded = false
            },
            onDismiss = { showCustomPicker = false }
        )
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
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
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
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    elevation = appButtonElevation()
                ) {
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
                shape = RoundedCornerShape(18.dp),
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

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = if (isEdit) "编辑币种" else "添加币种",
        content = {
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
                var showCurrencyColorPicker by remember { mutableStateOf(false) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CardColorPresets.take(CardColorPresets.size - 1)) { preset ->
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
                    item {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                    )
                                )
                                .clickable { showCurrencyColorPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "自定义颜色",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (showCurrencyColorPicker) {
                    ColorPickerDialog(
                        initialColor = cardColor,
                        onColorSelected = {
                            cardColor = it
                            cardColorLight = it
                            colorInput = it
                            showCurrencyColorPicker = false
                        },
                        onDismiss = { showCurrencyColorPicker = false }
                    )
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
        buttons = listOf(
            AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL, onDismiss),
            AppleDialogButton(if (isEdit) "保存" else "添加", AppleDialogButtonStyle.DEFAULT) {
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
            }
        )
    )
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SettingsScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("显示设置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column {
                        ListItem(headlineContent = { Text("深浅色模式") }, supportingContent = { Text("跟随系统") }, trailingContent = { TextButton(onClick = {}) { Text("切换") } })
                        Spacer(modifier = Modifier.height(0.5.dp))
                        ListItem(headlineContent = { Text("收入展示颜色") }, trailingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF34C759))) })
                        Spacer(modifier = Modifier.height(0.5.dp))
                        ListItem(headlineContent = { Text("支出展示颜色") }, trailingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFF3B30))) })
                    }
                }
                Text("分类管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(headlineContent = { Text("账单标签（类别）管理") }, supportingContent = { Text("添加、修改或删除收支分类及人情标签") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable {})
                }
                Text("更新检测", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(
                        headlineContent = { Text("启动时检测新版本") },
                        supportingContent = { Text("已启用，启动时自动检测 Gitee 新版本") },
                        trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                    )
                }
                Text("关于", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(headlineContent = { Text("关于 墨麒麟记账") }, supportingContent = { Text("版本 1.3.0 · GitHub 仓库") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable {})
                }
            }
        }
    }
}
