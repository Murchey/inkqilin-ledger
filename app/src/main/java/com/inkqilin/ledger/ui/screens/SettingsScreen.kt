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
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
    onNavigateToAIConfig: () -> Unit = {},
    onNavigateToOCRConfig: () -> Unit = {},
    onNavigateToBillImport: () -> Unit = {}
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
    val autoRecordEnabled by viewModel.autoRecordEnabled.collectAsState()
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
                    val assets = viewModel.allUserAssets.value
                    val flows = viewModel.allAssetFlows.value
                    val success = ExcelExporter.exportToUri(context, it, transactions, assets, flows)
                    if (success) {
                        Toast.makeText(context, "导出成功！账单${transactions.size}条，资产${assets.size}项，流转${flows.size}条", Toast.LENGTH_SHORT).show()
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

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        // region 1. 应用版本
        Text(text = "应用版本", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
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
        // endregion

        // 桌面小组件设置已移至左侧抽屉（MainScreen 内 WidgetSettingsPanel）

        // region 2. 显示设置
        var displaySettingsExpanded by remember { mutableStateOf(false) }
        Text(
            text = "显示设置",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { displaySettingsExpanded = !displaySettingsExpanded }
                .padding(top = 8.dp, bottom = 12.dp)
        )
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
                        val presetThemeColors = listOf(
                            "#5856D6" to "靛蓝",
                            DEFAULT_PRIMARY_COLOR_HEX to "青翠绿",
                            "#007AFF" to "深蓝",
                            "#00897B" to "青绿",
                            "#FF9500" to "深橙",
                            "#FF3B30" to "苹果红",
                            "#00838F" to "暗青",
                            "#5C6BC0" to "蓝紫"
                        )
                        var showThemeColorPicker by remember { mutableStateOf(false) }
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
                            item {
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
                        }

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

                        if (customPrimaryColorHex != null && customPrimaryColorHex != DEFAULT_PRIMARY_COLOR_HEX) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val homeColorPresets = listOf(
                                "#007AFF", "#FF9500", "#AF52DE", "#34C759", "#FF3B30",
                                "#5856D6", "#FF2D55", "#00C7BE"
                            )
                            items(homeColorPresets) { hex ->
                                val c = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                val isSelected = homeCardColorHex == hex
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
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            // Custom color button
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
                        }

                        if (homeCardColorHex != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.setHomeCardColor(null) }) {
                                Text("恢复默认")
                            }
                        }

                        if (showHomeCardColorPicker) {
                            ColorPickerDialog(
                                initialColor = homeCardColorHex ?: "#34C759",
                                onColorSelected = {
                                    viewModel.setHomeCardColor(it)
                                    showHomeCardColorPicker = false
                                },
                                onDismiss = { showHomeCardColorPicker = false }
                            )
                        }
                    }
                }
            }
        }

        Text(text = "分类管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
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

        val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
        Text(text = "人情账本", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            ListItem(
                headlineContent = { Text("启用人情账本") },
                supportingContent = { Text(if (renQingEnabled) "已启用，底部导航栏显示" else "未启用") },
                trailingContent = {
                    Switch(checked = renQingEnabled, onCheckedChange = { renQingViewModel.setRenQingEnabled(it) })
                }
            )
        }

        val multiCurrencyEnabled by viewModel.multiCurrencyEnabled.collectAsState()
        Text(text = "多币种管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
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

        val checkUpdateEnabled by viewModel.checkUpdateEnabled.collectAsState()
        val updateProxyUrl by viewModel.updateProxyUrl.collectAsState()
        val proxyOptions = com.inkqilin.ledger.util.PROXY_SOURCES + "自定义"
        var showProxyDropdown by remember { mutableStateOf(false) }
        var showCustomProxyInput by remember { mutableStateOf(false) }
        var customProxyUrl by remember { mutableStateOf("") }

        Text(text = "更新检测", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("启动时检测新版本") },
                    supportingContent = { Text(if (checkUpdateEnabled) "已启用，启动时自动检测 GitHub 新版本" else "已关闭") },
                    trailingContent = {
                        Switch(checked = checkUpdateEnabled, onCheckedChange = { viewModel.setCheckUpdateEnabled(it) })
                    }
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
                        proxyOptions.forEachIndexed { index, label ->
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

        var labExpanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "实验室功能", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { labExpanded = !labExpanded }) {
                Icon(
                    imageVector = if (labExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (labExpanded) "收起" else "展开"
                )
            }
        }
        if (labExpanded) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
                    Spacer(modifier = Modifier.height(0.5.dp))
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
                Spacer(modifier = Modifier.height(0.5.dp))
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
    }


        Text(text = "数据管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
                Spacer(modifier = Modifier.height(0.5.dp))
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
        val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)
        Spacer(modifier = Modifier.height(navBarBottomPadding + 76.dp))
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
                        supportingContent = { Text("已启用，启动时自动检测 GitHub 新版本") },
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
