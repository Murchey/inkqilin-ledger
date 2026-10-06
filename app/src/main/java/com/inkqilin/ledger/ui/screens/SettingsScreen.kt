@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    SettingsExportDatePickers(
        showExportStartPicker = showExportStartPicker,
        showExportEndPicker = showExportEndPicker,
        showRenQingExportStartPicker = showRenQingExportStartPicker,
        showRenQingExportEndPicker = showRenQingExportEndPicker,
        onDismissExportStart = { showExportStartPicker = false },
        onDismissExportEnd = { showExportEndPicker = false },
        onDismissRenQingExportStart = { showRenQingExportStartPicker = false },
        onDismissRenQingExportEnd = { showRenQingExportEndPicker = false },
        onSelectExportStart = { exportStartDate = it },
        onSelectExportEnd = { exportEndDate = it },
        onSelectRenQingExportStart = { renQingExportStartDate = it },
        onSelectRenQingExportEnd = { renQingExportEndDate = it },
    )
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

    // 抽屉展开态放 VM：功能开关会重建底部 Tab / 设置页，rememberSaveable 会丢
    val settingsExpand by viewModel.settingsExpand.collectAsState()
    fun expand(key: String, default: Boolean = false) = settingsExpand[key] ?: default
    val appSectionExpanded = expand("app", true)
    val categorySectionExpanded = expand("category")
    val dataSectionExpanded = expand("data")
    val widgetSectionExpanded = expand("widget")
    var showAboutSheet by rememberSaveable { mutableStateOf(false) }
    var showUsageGuide by rememberSaveable { mutableStateOf(false) }
    var showHomeBgSheet by rememberSaveable { mutableStateOf(false) }
    var showPrivacyPolicy by rememberSaveable { mutableStateOf(false) }
    var showStorageSheet by rememberSaveable { mutableStateOf(false) }
    // exportProgressState / importProgress 见函数开头（exportLauncher 需要先声明）

    Column(modifier = Modifier.fillMaxSize().verticalScroll(viewModel.settingsScrollState).padding(horizontal = 24.dp, vertical = 16.dp)) {
        // region 1. 应用版本
        SettingsSectionHeader("应用版本", if (appMode == AppMode.SMART) "智能版" else "基础版", appSectionExpanded) { viewModel.toggleSettingsExpand("app", true) }
        SettingsDrawer(visible = appSectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = com.inkqilin.ledger.ui.theme.Corners.Md, elevation = CardDefaults.cardElevation(0.dp)) {
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

        SettingsSectionHeader("桌面小组件", "余额显示与刷新", widgetSectionExpanded) { viewModel.toggleSettingsExpand("widget") }
        SettingsDrawer(visible = widgetSectionExpanded) {
            WidgetSettingsPanel(viewModel)
        }

        SettingsDisplaySection(
            viewModel = viewModel,
            themeMode = themeMode,
            incomeColorHex = incomeColorHex,
            expenseColorHex = expenseColorHex,
            customPrimaryColorHex = customPrimaryColorHex,
            homeCardColorHex = homeCardColorHex,
            onShowHomeBgSheet = { showHomeBgSheet = true },
        )
        SettingsSectionHeader("分类管理", "账单分类与自动分类规则", categorySectionExpanded) { viewModel.toggleSettingsExpand("category") }
        SettingsDrawer(visible = categorySectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = com.inkqilin.ledger.ui.theme.Corners.Md, elevation = CardDefaults.cardElevation(0.dp)) {
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
        SettingsFeatureSection(
            viewModel = viewModel,
            renQingViewModel = renQingViewModel,
            onNavigateToCategoryManagement = onNavigateToCategoryManagement,
            onNavigateToKeywordCategoryManagement = onNavigateToKeywordCategoryManagement,
            onNavigateToContactManagement = onNavigateToContactManagement,
            onNavigateToCurrencyManagement = onNavigateToCurrencyManagement,
            onNavigateToAIConfig = onNavigateToAIConfig,
            onNavigateToOCRConfig = onNavigateToOCRConfig,
            onSendTestNotification = ::sendTestNotification,
            ocrApiKey = ocrApiKey,
            harmonyCompatMode = harmonyCompatMode,
        )
        SettingsSectionHeader("数据管理", "导入、导出与数据备份", dataSectionExpanded) { viewModel.toggleSettingsExpand("data") }
        SettingsDrawer(visible = dataSectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth(), shape = com.inkqilin.ledger.ui.theme.Corners.Md, elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("数据备份") },
                    supportingContent = { Text("本地备份 / 腾讯云 COS 云备份，可恢复账本") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToCloudBackup() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = com.inkqilin.ledger.ui.theme.Space.PageHorizontal))
                ListItem(
                    headlineContent = { Text("储存空间管理") },
                    supportingContent = { Text("查看相册/备份/缓存占用并清理") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showStorageSheet = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = com.inkqilin.ledger.ui.theme.Space.PageHorizontal))
                ListItem(
                    headlineContent = { Text("导出账单") },
                    supportingContent = { Text("选择时间范围，导出 Excel 或 CSV") },
                    leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Column(modifier = Modifier.padding(horizontal = com.inkqilin.ledger.ui.theme.Space.PageHorizontal, vertical = 4.dp)) {
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
                    Column(modifier = Modifier.padding(horizontal = com.inkqilin.ledger.ui.theme.Space.PageHorizontal, vertical = 4.dp)) {
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
        val aboutSectionExpanded = expand("about")
        SettingsSectionHeader(
            "关于 墨麒麟记账",
            "版本 ${viewModel.getCurrentVersionName(context)} · 仓库与使用引导",
            aboutSectionExpanded
        ) { viewModel.toggleSettingsExpand("about") }
        SettingsDrawer(visible = aboutSectionExpanded) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = com.inkqilin.ledger.ui.theme.Corners.Md,
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

        val safeAreaBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Spacer(modifier = Modifier.height(safeAreaBottom + floatingContentBottomInset))
    }

    // 关于抽屉
        SettingsAboutSheet(
            show = showAboutSheet,
            onDismiss = { showAboutSheet = false },
            onShowUsageGuide = { showUsageGuide = true },
            onShowPrivacy = { showPrivacyPolicy = true },
        )

    // 隐私政策（可再次查看）
    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(
            requireAccept = false,
            onAccept = { showPrivacyPolicy = false },
            onDismiss = { showPrivacyPolicy = false }
        )
    }

    // 首页背景图抽屉
        SettingsHomeBgSheet(
            show = showHomeBgSheet,
            onDismiss = { showHomeBgSheet = false },
            viewModel = viewModel,
            homeBgImagePath = viewModel.homeBgImagePath.collectAsState().value,
            homeBgOpacity = viewModel.homeBgOpacity.collectAsState().value,
            homeTxCardOpacity = viewModel.homeTxCardOpacity.collectAsState().value,
            onSetHomeBgPath = { viewModel.setHomeBgImagePath(it) },
            onSetHomeBgOpacity = { viewModel.setHomeBgOpacity(it) },
            onSetHomeTxCardOpacity = { viewModel.setHomeTxCardOpacity(it) },
            onImportHomeBg = { },
            onClearHomeBg = { viewModel.clearHomeBackground() },
        )

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
        SettingsStorageSheet(show = showStorageSheet, onDismiss = { showStorageSheet = false }, viewModel = viewModel)

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
