package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.MotionSprings
import com.inkqilin.ledger.ui.motion.pressScale
import com.inkqilin.ledger.ui.motion.MotionDurations
import com.inkqilin.ledger.ui.motion.MotionCurves
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.NavController
import com.inkqilin.ledger.data.Category
import kotlin.math.roundToInt

enum class TimePeriod(val label: String) {
    WEEK("本周"), MONTH("本月"), YEAR("本年"), CUSTOM("自定义")
}

private fun Modifier.frostedGlass(
    shape: RoundedCornerShape,
    isDark: Boolean
): Modifier = this
    /*.then(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Modifier.blur(20.dp) // iOS-style deep blur on Android 12+
        } else Modifier
    )*/
    .background(
        color = if (isDark) FrostedDark.copy(alpha = 0.85f) else FrostedLight.copy(alpha = 0.7f),
        shape = shape
    )
    .border(
        width = 0.5.dp, // Thinner iOS-style border
        color = if (isDark) FrostedBorderDark.copy(alpha = 0.5f) else FrostedBorderLight.copy(alpha = 0.3f),
        shape = shape
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: TransactionViewModel, navController: NavController) {
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val incomeColor = Color(android.graphics.Color.parseColor(incomeColorHex))
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))
    val multiCurrencyEnabled by viewModel.multiCurrencyEnabled.collectAsState()
    val allAssets by viewModel.allAssets.collectAsState()
    val defaultAsset = allAssets.firstOrNull { it.isDefault }

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTH) }
    var selectedCurrencyCode by remember { mutableStateOf<String?>(null) }
    
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    
    var startDate by remember { mutableLongStateOf(
        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
    ) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showStartDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = it }
                    showEndDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    val filteredByPeriod = remember(transactions, selectedPeriod, startDate, endDate) {
        if (selectedPeriod == TimePeriod.CUSTOM) {
            transactions.filter { it.date in startDate..endDate }
        } else {
            filterByPeriod(transactions, selectedPeriod, Calendar.getInstance())
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
        filteredByPeriod.filter { it.currency == effectiveCurrencyCode }
    } else {
        filteredByPeriod
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

    val barChartData = remember(filteredByPeriod, selectedPeriod, selectedType, selectedCurrencyCode, multiCurrencyEnabled) {
        val currencyFilter: (Transaction) -> Boolean = { t ->
            effectiveCurrencyCode == null || t.currency == effectiveCurrencyCode
        }
        val groups = mutableListOf<Pair<String, Double>>()
        when (selectedPeriod) {
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
        groups
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimePeriod.entries.forEach { period ->
                    val selected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.label,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
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

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                            .clip(RoundedCornerShape(10.dp))
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

        if (multiCurrencyEnabled && allAssets.size > 1) {
            item {
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

        item {
            Spacer(modifier = Modifier.height(24.dp))
            val totalShape = RoundedCornerShape(20.dp)
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val totalInteractionSource = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .pressScale(totalInteractionSource)
                    .frostedGlass(totalShape, isDark),
                shape = totalShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = selectedPeriod.label + "总" + if (selectedType == TransactionType.EXPENSE) "支出" else "收入",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currencySymbol}${String.format("%.2f", totalAmount)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "共 ${filteredTransactions.size} 笔记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            val compShape = RoundedCornerShape(16.dp)
            val isDarkComp = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val compInteractionSource = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .pressScale(compInteractionSource)
                    .frostedGlass(compShape, isDarkComp),
                shape = compShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "环比上期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isUp = changePercent >= 0
                        Icon(
                            imageVector = if (isUp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isUp) expenseColor else incomeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isUp) "+" else ""}${String.format("%.1f", changePercent)}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isUp) expenseColor else incomeColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "上期 ${currencySymbol}${String.format("%.2f", previousTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (barChartData.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "趋势图",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                val accentColor = if (selectedType == TransactionType.EXPENSE) expenseColor else incomeColor
                var tooltipIndex by remember { mutableStateOf<Int?>(null) }
                val hasAnyData = barChartData.any { it.second > 0 }

                val chartShape = RoundedCornerShape(20.dp)
                val isDarkChart = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val chartInteractionSource = remember { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .pressScale(chartInteractionSource) // iOS-style interactive feedback
                        .frostedGlass(chartShape, isDarkChart),
                    shape = chartShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    if (hasAnyData) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AnimatedBarChart(
                                data = barChartData,
                                accentColor = accentColor,
                                onBarLongPress = { index -> tooltipIndex = index },
                                onBarRelease = { tooltipIndex = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .horizontalScroll(rememberScrollState())
                            )

                            if (tooltipIndex != null && tooltipIndex!! < barChartData.size) {
                                val (label, value) = barChartData[tooltipIndex!!]
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        text = "$label · ${currencySymbol}${String.format("%.2f", value)}",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.inverseOnSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无数据",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "分类排行",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (categoryTotals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无数据",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(categoryTotals) { (categoryName, total) ->
                val percentage = if (totalAmount > 0) (total / totalAmount).toFloat() else 0f
                val accentColor = if (selectedType == TransactionType.EXPENSE) expenseColor else incomeColor
                val category = categories.find { it.name == categoryName && it.type == selectedType }
                val displayColor = category?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: accentColor

                val density = LocalDensity.current
                val menuWidth = 80.dp
                val menuWidthPx = with(density) { menuWidth.toPx() }
                var offsetX by remember(categoryName, selectedType) { mutableFloatStateOf(0f) }
                val draggableState = rememberDraggableState { delta ->
                    val newOffset = (offsetX + delta).coerceIn(-menuWidthPx, 0f)
                    offsetX = newOffset
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // 滑动展示的编辑按钮
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(menuWidth)
                            .fillMaxHeight()
                            .clickable {
                                offsetX = 0f
                                categoryToEdit = category
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            Text("编辑", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // 前景内容
                    val cardInteractionSource = remember { MutableInteractionSource() }
                    Card(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                            .fillMaxWidth()
                            .pressScale(cardInteractionSource) // iOS-style interactive feedback
                            .draggable(
                                state = draggableState,
                                orientation = Orientation.Horizontal,
                                onDragStopped = {
                                    val target = if (offsetX < -menuWidthPx / 2) -menuWidthPx else 0f
                                    animate(
                                        initialValue = offsetX,
                                        targetValue = target,
                                        animationSpec = MotionSprings.interactive() // iOS-like bouncy menu snap
                                    ) { value, _ -> offsetX = value }
                                }
                            )
                            .clickable(
                                interactionSource = cardInteractionSource,
                                indication = null
                            ) {
                                val dateRange = getDateRangeForPeriod(selectedPeriod, startDate, endDate)
                                navController.navigate("category_transactions/$categoryName/${selectedType.name}?startDate=${dateRange.first}&endDate=${dateRange.second}")
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(category?.icon ?: "📋", modifier = Modifier.padding(end = 8.dp))
                                    Text(
                                        text = categoryName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = "${currencySymbol}${String.format("%.2f", total)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = displayColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val animatedPercentage by animateFloatAsState(
                                targetValue = percentage,
                                animationSpec = MotionSprings.interactive(), // iOS-like bouncy progress
                                label = "categoryPercentage"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = animatedPercentage)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(displayColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.1f", percentage * 100)}% · ${filteredTransactions.count { it.category == categoryName }} 笔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
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

private fun filterByPeriod(
    transactions: List<Transaction>,
    period: TimePeriod,
    now: Calendar
): List<Transaction> {
    val range = getDateRangeForPeriod(period, now.timeInMillis, now.timeInMillis)
    return transactions.filter { it.date >= range.first && it.date <= range.second }
}

private fun getDateRangeForPeriod(
    period: TimePeriod,
    startDate: Long,
    endDate: Long
): Pair<Long, Long> {
    return when (period) {
        TimePeriod.WEEK -> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to System.currentTimeMillis()
        }
        TimePeriod.MONTH -> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to System.currentTimeMillis()
        }
        TimePeriod.YEAR -> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to System.currentTimeMillis()
        }
        TimePeriod.CUSTOM -> startDate to (endDate + 86400000L - 1)
    }
}

@Composable
private fun AnimatedBarChart(
    data: List<Pair<String, Double>>,
    accentColor: Color,
    onBarLongPress: (Int) -> Unit,
    onBarRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = data.maxOfOrNull { it.second } ?: 1.0
    val barCount = data.size
    val minBarWidth = 32.dp
    val barSpacing = 6.dp
    val totalBarArea = minBarWidth * barCount + barSpacing * (barCount - 1) + 32.dp
    val chartHeight = 170.dp

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = MotionSprings.appearance() // iOS-like smooth spring entry
        )
    }

    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .width(totalBarArea)
            .height(chartHeight)
            .pointerInput(data) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val barTotalWidth = size.width / barCount
                        val index = (offset.x / barTotalWidth).toInt().coerceIn(0, barCount - 1)
                        onBarLongPress(index)
                    },
                    onPress = {
                        awaitRelease()
                        onBarRelease()
                    }
                )
            }
    ) {
        val canvasW = size.width
        val canvasH = size.height
        val barAreaWidth = canvasW / barCount
        val barWidthPx = barAreaWidth * 0.55f
        val topPadding = 36f
        val bottomPadding = 32f
        val chartAreaHeight = canvasH - topPadding - bottomPadding

        val labelPaint = android.graphics.Paint().apply {
            color = onSurfaceVariant.toArgb()
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val valuePaint = android.graphics.Paint().apply {
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        data.forEachIndexed { index, (label, value) ->
            val barHeight = if (maxVal > 0) (value / maxVal).toFloat() * chartAreaHeight * animProgress.value else 0f
            val x = barAreaWidth * index + (barAreaWidth - barWidthPx) / 2
            val y = canvasH - bottomPadding - barHeight

            drawRoundRect(
                color = accentColor.copy(alpha = 0.85f),
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )

            val textX = x + barWidthPx / 2

            drawContext.canvas.nativeCanvas.apply {
                drawText(label, textX, canvasH - 6f, labelPaint)

                if (value > 0 && animProgress.value > 0.8f) {
                    val valueText = if (value >= 10000) {
                        "${String.format("%.1f", value / 10000)}w"
                    } else if (value >= 1000) {
                        String.format("%.0f", value)
                    } else {
                        String.format("%.2f", value)
                    }
                    valuePaint.color = accentColor.toArgb()
                    drawText(valueText, textX, y - 8f, valuePaint)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun StatisticsScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("本周", "本月", "本年", "自定义").forEach { label ->
                        FilterChip(selected = label == "本月", onClick = {}, label = { Text(label) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("支出") })
                    FilterChip(selected = false, onClick = {}, label = { Text("收入") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336)), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("总支出", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        Text("¥1,234.56", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("分类排行", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val categories = listOf(
                    Triple("🍜", "餐饮", 0.40 to 456.78),
                    Triple("🛒", "购物", 0.25 to 312.00),
                    Triple("🚌", "交通", 0.20 to 245.50),
                    Triple("☕", "饮品", 0.10 to 120.28),
                    Triple("📱", "通讯", 0.05 to 100.00)
                )
                categories.forEach { (icon, name, data) ->
                    val cardColor = try { Color(android.graphics.Color.parseColor("#715CFF")) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(14.dp)).background(cardColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 18.sp) }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(progress = data.first.toFloat(), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = Color(0xFFF44336), trackColor = Color(0xFFF44336).copy(alpha = 0.1f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("¥${String.format("%.2f", data.second)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${String.format("%.0f", data.first * 100)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
