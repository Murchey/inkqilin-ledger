@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@file:Suppress("AssignedValueIsNeverRead")

package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.toColorInt
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.AssetFlowType
import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.data.UserAssetType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.AppMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


@Composable
fun StatisticsScreen(viewModel: TransactionViewModel, navController: NavController) {
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val incomeColor = Color(incomeColorHex.toColorInt())
    val expenseColor = Color(expenseColorHex.toColorInt())
    val multiCurrencyEnabled by viewModel.multiCurrencyEnabled.collectAsState()
    val allAssets by viewModel.allAssets.collectAsState()
    val defaultAsset = allAssets.firstOrNull { it.isDefault }
    val appMode by viewModel.appMode.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsState()
    val aiAnalysisFailed by viewModel.aiAnalysisFailed.collectAsState()
    val aiAnalysisLoading by viewModel.aiAnalysisLoading.collectAsState()
    val bodyShowPieChart = remember { mutableStateOf(false) }
    val bodyCategoryToEdit = remember { mutableStateOf<com.inkqilin.ledger.data.Category?>(null) }
    val allUserAssets by viewModel.allUserAssets.collectAsState()
    val userAssetTotalValue by viewModel.userAssetTotalValue.collectAsState()
    val allAssetFlows by viewModel.allAssetFlows.collectAsState()

    var selectedType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var selectedPeriod by rememberSaveable { mutableStateOf(TimePeriod.MONTH) }
    // 子筛选：年→月(1-12)、月→周(1-5)、周→上周/本周
    var selectedSubFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedWeekOffset by rememberSaveable { mutableStateOf<Int?>(null) } // 0=本周, -1=上周
    var showSubFilterBar by rememberSaveable { mutableStateOf(false) }
    var selectedCurrencyCode by rememberSaveable { mutableStateOf<String?>(null) }
    
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    
    var startDate by rememberSaveable { mutableStateOf(
        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
    ) }
    var endDate by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showPieChart by rememberSaveable { mutableStateOf(false) }
    val statisticsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    BackHandler(enabled = showSubFilterBar) {
        showSubFilterBar = false
        selectedSubFilter = null
        selectedWeekOffset = null
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        AppleDatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showStartDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
            }
        )
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        AppleDatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = it }
                    showEndDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
            }
        )
    }

    val filteredByPeriod = remember(transactions, selectedPeriod, startDate, endDate) {
        if (selectedPeriod == TimePeriod.CUSTOM) {
            val effectiveEnd = endDate + 86400000L - 1
            transactions.filter { it.date in startDate..effectiveEnd }
        } else {
            filterByPeriod(transactions, selectedPeriod, Calendar.getInstance())
        }
    }

    // ── 计算当前时间段的起止毫秒 ──
    val (periodStartMs, periodEndMs) = remember(selectedPeriod, startDate, endDate) {
        when (selectedPeriod) {
            TimePeriod.WEEK -> {
                val c = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                c.timeInMillis to (c.timeInMillis + 7 * 86400000L - 1)
            }
            TimePeriod.MONTH -> {
                val c = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    add(Calendar.MONTH, 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis - 1
                c.timeInMillis to end
            }
            TimePeriod.YEAR -> {
                val c = Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    add(Calendar.YEAR, 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis - 1
                c.timeInMillis to end
            }
            TimePeriod.CUSTOM -> startDate to (endDate + 86400000L - 1)
        }
    }

    // ── 时间段内资产价值变动 ──
    val (assetValueChange, assetChangePercent, assetPeriodEndValue) = remember(
        allAssetFlows, allUserAssets, periodStartMs, periodEndMs, userAssetTotalValue
    ) {
        val periodFlows = allAssetFlows.filter { it.date in periodStartMs..periodEndMs }
        val netChange = periodFlows.sumOf { flow ->
            when (flow.flowType) {
                AssetFlowType.INCREASE -> flow.amount
                AssetFlowType.DECREASE -> -flow.amount
                AssetFlowType.REVALUATION -> {
                    val prevFlow = allAssetFlows
                        .filter { it.assetId == flow.assetId && it.date < flow.date }
                        .maxByOrNull { it.date }
                    val prevValue = prevFlow?.newValue
                        ?: allUserAssets.find { it.id == flow.assetId }?.currentValue
                        ?: 0.0
                    flow.newValue - prevValue
                }
            }
        }
        val endValue = userAssetTotalValue
        val startValue = endValue - netChange
        val percent = if (startValue != 0.0) (netChange / startValue * 100) else 0.0
        Triple(netChange, percent, endValue)
    }

    // 切换时间段时重置子筛选；仅在用户真正切换时段时触发，
    // 避免从子页返回 / 页面重组时把已恢复的筛选清掉
    var lastAppliedPeriod by rememberSaveable { mutableStateOf(selectedPeriod) }
    LaunchedEffect(selectedPeriod) {
        if (selectedPeriod != lastAppliedPeriod) {
            selectedSubFilter = null
            selectedWeekOffset = null
            lastAppliedPeriod = selectedPeriod
        }
    }

    // ── 子筛选数据 ──
    val subFiltered = remember(filteredByPeriod, selectedPeriod, selectedSubFilter, selectedWeekOffset) {
        val now = Calendar.getInstance()
        when {
            // 本年 → 选择某月
            selectedPeriod == TimePeriod.YEAR && selectedSubFilter != null -> {
                val month = selectedSubFilter!!
                val start = Calendar.getInstance().apply {
                    set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                filteredByPeriod.filter { it.date in start until end }
            }
            // 本月 → 选择某周
            selectedPeriod == TimePeriod.MONTH && selectedSubFilter != null -> {
                val weekNum = selectedSubFilter!! // 1-based
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val weekStart = Calendar.getInstance().apply {
                    time = monthStart.time
                    add(Calendar.DAY_OF_MONTH, (weekNum - 1) * 7)
                }
                val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                val weekEnd = Calendar.getInstance().apply {
                    time = monthStart.time
                    add(Calendar.DAY_OF_MONTH, minOf(weekNum * 7, daysInMonth))
                }
                filteredByPeriod.filter { it.date in weekStart.timeInMillis until weekEnd.timeInMillis }
            }
            // 本周 → 上周/本周
            selectedPeriod == TimePeriod.WEEK && selectedWeekOffset != null -> {
                val offset = selectedWeekOffset!! // -1=上周, 0=本周
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    add(Calendar.DAY_OF_YEAR, offset * 7)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val weekEnd = weekStart + 7 * 86400000L
                filteredByPeriod.filter { it.date in weekStart until weekEnd }
            }
            else -> filteredByPeriod
        }
    }

    val effectiveCurrencyCode = if (multiCurrencyEnabled) {
        selectedCurrencyCode ?: defaultAsset?.code
    } else {
        defaultAsset?.code ?: "CNY"
    }

    val currencySymbol = if (multiCurrencyEnabled) {
        allAssets.firstOrNull { it.code == effectiveCurrencyCode }?.symbol ?: defaultAsset?.symbol ?: "¥"
    } else {
        defaultAsset?.symbol ?: "¥"
    }

    val filteredByCurrency = if (effectiveCurrencyCode != null) {
        subFiltered.filter { it.currency == effectiveCurrencyCode }
    } else {
        subFiltered
    }

    val filteredTransactions = filteredByCurrency.filter { it.type == selectedType }
    val totalAmount = filteredTransactions.sumOf { it.amount }
    val categoryTotals = filteredTransactions.groupBy { it.category }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .toList()
        .sortedByDescending { it.second }

    val previousPeriodTransactions = remember(transactions, selectedPeriod, selectedCurrencyCode, startDate, endDate, multiCurrencyEnabled) {
        val prevRange = when (selectedPeriod) {
            TimePeriod.WEEK -> {
                val c = Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, -1)
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                c.timeInMillis to (c.timeInMillis + 7 * 86400000L)
            }
            TimePeriod.MONTH -> {
                val c = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                c.timeInMillis to end
            }
            TimePeriod.YEAR -> {
                val c = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                    set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                c.timeInMillis to end
            }
            TimePeriod.CUSTOM -> startDate to endDate
        }
        val prevFiltered = transactions.filter { it.date in prevRange.first..prevRange.second }
        val prevCurrency = if (effectiveCurrencyCode != null) {
            prevFiltered.filter { it.currency == effectiveCurrencyCode }
        } else {
            prevFiltered
        }
        prevCurrency.filter { it.type == selectedType }
    }

    val previousTotal = previousPeriodTransactions.sumOf { it.amount }
    val changePercent = if (previousTotal > 0) ((totalAmount - previousTotal) / previousTotal * 100) else if (totalAmount > 0) 100.0 else 0.0

    val barChartData = remember(subFiltered, selectedPeriod, selectedType, selectedCurrencyCode, multiCurrencyEnabled, selectedSubFilter, selectedWeekOffset) {
        val currencyFilter: (Transaction) -> Boolean = { t ->
            effectiveCurrencyCode == null || t.currency == effectiveCurrencyCode
        }
        val groups = mutableListOf<Pair<String, Double>>()
        val isSubFiltered = selectedSubFilter != null || selectedWeekOffset != null

        when {
            // 本年 + 子筛选某月 → 该月每日
            selectedSubFilter != null && selectedPeriod == TimePeriod.YEAR -> {
                val month = selectedSubFilter!!
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, month - 1)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (day in 1..daysInMonth) {
                    val c = Calendar.getInstance().apply {
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val start = c.timeInMillis
                    val end = start + 86400000L
                    val sum = subFiltered.filter { it.type == selectedType && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add("$day" to sum)
                }
            }
            // 本月 + 子筛选某周 / 本周 + 子筛选 → 该周每日
            isSubFiltered && (selectedPeriod == TimePeriod.MONTH || selectedPeriod == TimePeriod.WEEK) -> {
                val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")
                // 取 subFiltered 中最早的交易日期所在周的周一
                val firstDate = subFiltered.map { it.date }.minOrNull() ?: System.currentTimeMillis()
                val weekStart = Calendar.getInstance().apply {
                    timeInMillis = firstDate
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val startMs = weekStart.timeInMillis
                for (i in 0..6) {
                    val start = startMs + i * 86400000L
                    val end = start + 86400000L
                    val sum = subFiltered.filter { it.type == selectedType && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    val dayOfWeek = Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.DAY_OF_WEEK)
                    val dayIdx = if (dayOfWeek == Calendar.SUNDAY) 0 else dayOfWeek - Calendar.SUNDAY
                    groups.add(dayNames[dayIdx] to sum)
                }
            }
            // 无子筛选时使用原有的完整数据
            !isSubFiltered -> when (selectedPeriod) {
                TimePeriod.WEEK -> {
                val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")
                val c = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                for (i in 0..6) {
                    val start = c.timeInMillis
                    val end = start + 86400000L
                    val sum = filteredByPeriod.filter { it.type == selectedType && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add(dayNames[i] to sum)
                    c.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            TimePeriod.MONTH -> {
                val cal = Calendar.getInstance()
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (day in 1..daysInMonth) {
                    val c = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val start = c.timeInMillis
                    val end = start + 86400000L
                    val sum = filteredByPeriod.filter { it.type == selectedType && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add("$day" to sum)
                }
            }
            TimePeriod.YEAR -> {
                for (month in 1..12) {
                    val c = Calendar.getInstance().apply {
                        set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val start = c.timeInMillis
                    c.set(Calendar.MONTH, month)
                    val end = c.timeInMillis
                    val sum = filteredByPeriod.filter { it.type == selectedType && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add("${month}月" to sum)
                }
            }
            TimePeriod.CUSTOM -> {
                val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
                val cal = Calendar.getInstance()
                cal.timeInMillis = startDate
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                while (cal.timeInMillis <= endDate) {
                    val start = cal.timeInMillis
                    val end = start + 86400000L
                    val sum = filteredByPeriod.filter { it.type == selectedType && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add(sdf.format(Date(start)) to sum)
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        }
        groups
    }

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)
    LazyColumn(
        state = statisticsListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = navBarBottomPadding + 76.dp)
    ) {
        item(key = "period_tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                TimePeriod.entries.forEach { period ->
                    val selected = selectedPeriod == period
                    BouncyTabItem(
                        label = period.label,
                        isSelected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedPeriod = period }
                    )
                }
            }
                IconButton(
                    onClick = {
                        showSubFilterBar = !showSubFilterBar
                        if (!showSubFilterBar) {
                            selectedSubFilter = null
                            selectedWeekOffset = null
                        }
                    }
                ) {
                    Icon(
                        FilterListIcon,
                        contentDescription = "筛选",
                        tint = if (showSubFilterBar) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── 子筛选 Bar ──
        item(key = "sub_filter_bar") {
            AnimatedVisibility(
                visible = selectedPeriod != TimePeriod.CUSTOM && showSubFilterBar,
                enter = expandVertically(
                    animationSpec = tween(MotionDurations.FAST)
                ) + fadeIn(animationSpec = tween(MotionDurations.FAST)),
                exit = shrinkVertically(
                    animationSpec = tween(MotionDurations.FAST)
                ) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    val subOptions = when (selectedPeriod) {
                        TimePeriod.YEAR -> (1..12).map { "${it}月" }
                        TimePeriod.MONTH -> {
                            val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
                            (1..((daysInMonth + 6) / 7)).map { "第${it}周" }
                        }
                        TimePeriod.WEEK -> listOf("上周", "本周")
                        TimePeriod.CUSTOM -> emptyList()
                    }
                    if (subOptions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .horizontalScroll(rememberScrollState())
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            subOptions.forEachIndexed { index, label ->
                                val isSelected = when (selectedPeriod) {
                                    TimePeriod.YEAR -> selectedSubFilter == index + 1
                                    TimePeriod.MONTH -> selectedSubFilter == index + 1
                                    TimePeriod.WEEK -> selectedWeekOffset == (index - 1)
                                    else -> false
                                }
                                BouncyTabItem(
                                    label = label,
                                    isSelected = isSelected,
                                    onClick = {
                                        when (selectedPeriod) {
                                            TimePeriod.YEAR -> {
                                                selectedSubFilter = if (isSelected) null else index + 1
                                            }
                                            TimePeriod.MONTH -> {
                                                selectedSubFilter = if (isSelected) null else index + 1
                                            }
                                            TimePeriod.WEEK -> {
                                                selectedWeekOffset = if (isSelected) null else (index - 1)
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedPeriod == TimePeriod.CUSTOM) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    AssistChip(
                        onClick = { showStartDatePicker = true },
                        label = { Text(sdf.format(Date(startDate))) },
                        modifier = Modifier.weight(1f)
                    )
                    Text("至", style = MaterialTheme.typography.bodySmall)
                    AssistChip(
                        onClick = { showEndDatePicker = true },
                        label = { Text(sdf.format(Date(endDate))) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item(key = "type_toggle") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(TransactionType.EXPENSE to "支出", TransactionType.INCOME to "收入").forEach { (type, label) ->
                    val selected = selectedType == type
                    val accentColor = if (type == TransactionType.EXPENSE) expenseColor else incomeColor
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) accentColor
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (allUserAssets.isNotEmpty()) {
        StatisticsAssetSummaryItem(
            allUserAssets = allUserAssets,
            userAssetTotalValue = userAssetTotalValue,
            assetValueChange = assetValueChange,
            assetChangePercent = assetChangePercent,
            assetPeriodEndValue = assetPeriodEndValue,
            onNavigateAssets = { navController.navigateSingle("asset_management") },
            allAssetFlows = allAssetFlows,
            periodStartMs = periodStartMs,
            periodEndMs = periodEndMs,
        )
        }

        if (multiCurrencyEnabled && allAssets.size > 1) {
            item(key = "currency_filter") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "币种：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    var currencyMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { currencyMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedAsset = allAssets.firstOrNull { it.code == selectedCurrencyCode }
                            Text(
                                text = selectedAsset?.name ?: (defaultAsset?.name ?: "全部"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部") },
                                onClick = {
                                    selectedCurrencyCode = null
                                    currencyMenuExpanded = false
                                }
                            )
                            allAssets.forEach { asset ->
                                DropdownMenuItem(
                                    text = { Text("${asset.name} (${asset.code})") },
                                    onClick = {
                                        selectedCurrencyCode = asset.code
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        StatisticsBodyItems(
            totalAmount = totalAmount,
            selectedType = selectedType,
            expenseColor = expenseColor,
            incomeColor = incomeColor,
            currencySymbol = currencySymbol,
            categoryTotals = categoryTotals,
            barChartData = barChartData,
            previousTotal = previousTotal,
            changePercent = changePercent,
            selectedPeriod = selectedPeriod,
            startDate = startDate,
            endDate = endDate,
            categories = categories,
            categoryToEdit = bodyCategoryToEdit,
            aiAnalysisResult = aiAnalysisResult,
            aiAnalysisFailed = aiAnalysisFailed,
            aiAnalysisLoading = aiAnalysisLoading,
            onRefreshAi = { },
            filteredTransactions = filteredTransactions,
            navController = navController,
            viewModel = viewModel,
            showPieChart = bodyShowPieChart,
            appMode = appMode,
            subFiltered = filteredTransactions,
            monthlyBudget = monthlyBudget,
        )
    }

    categoryToEdit?.let { category ->
        CategoryEditDialog(
            category = category,
            type = category.type,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, icon, color ->
                viewModel.updateCategory(category.copy(name = name, icon = icon, color = color))
                categoryToEdit = null
            }
        )
    }
}
