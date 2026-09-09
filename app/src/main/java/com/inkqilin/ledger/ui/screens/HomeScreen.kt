@file:Suppress("AssignedValueIsNeverRead")

package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.AppMode
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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
        color = if (isDark) FrostedDark.copy(alpha = 0.8f) else FrostedLight.copy(alpha = 0.75f),
        shape = shape
    )
    .border(
        width = 0.5.dp, // Thinner iOS-style border
        color = if (isDark) FrostedBorderDark.copy(alpha = 0.4f) else FrostedBorderLight.copy(alpha = 0.25f),
        shape = shape
    )


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TransactionViewModel,
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onNavigateToSearch: () -> Unit = {},
    onNavigateToOcrRecognition: () -> Unit = {},
    onNavigateToAssetManagement: () -> Unit = {}
) {
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val multiCurrencyEnabled by viewModel.multiCurrencyEnabled.collectAsState()
    val allAssets by viewModel.allAssets.collectAsState()
    val ocrEnabled by viewModel.ocrEnabled.collectAsState()
    val appMode by viewModel.appMode.collectAsState()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsState()
    val aiAnalysisLoading by viewModel.aiAnalysisLoading.collectAsState()
    val aiAnalysisFailed by viewModel.aiAnalysisFailed.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val expenseColor = Color(expenseColorHex.toColorInt())
    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val incomeColor = Color(incomeColorHex.toColorInt())
    val homeCardColorHex by viewModel.homeCardColor.collectAsState()

    var selectedYearMonth by remember {
        mutableStateOf(Calendar.getInstance().let { it.get(Calendar.YEAR) to it.get(Calendar.MONTH) })
    }
    var showMonthPicker by remember { mutableStateOf(false) }
    var enableCardAnimations by remember { mutableStateOf(false) }

    val defaultAsset = remember(allAssets) { allAssets.firstOrNull { it.isDefault } }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        enableCardAnimations = true
        viewModel.checkAndRunDailyAnalysis()
    }

    if (showMonthPicker) {
        var pickerYear by remember { mutableIntStateOf(selectedYearMonth.first) }
        AppleAlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = "${pickerYear}年",
            content = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { pickerYear-- }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一年")
                        }
                        IconButton(onClick = { pickerYear++ }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一年")
                        }
                    }
                    val months = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
                    val currentYM = Calendar.getInstance().let { it.get(Calendar.YEAR) to it.get(Calendar.MONTH) }
                    for (row in 0..3) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..2) {
                                val monthIndex = row * 3 + col
                                val isSelected = pickerYear == selectedYearMonth.first && monthIndex == selectedYearMonth.second
                                val isCurrent = pickerYear == currentYM.first && monthIndex == currentYM.second
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clickable {
                                            selectedYearMonth = pickerYear to monthIndex
                                            showMonthPicker = false
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                ) {
                                    Text(
                                        text = months[monthIndex],
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White
                                        else if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            buttons = listOf(
                AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { showMonthPicker = false }
            )
        )
    }

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    if (transactionToDelete != null) {
        AppleAlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = "确认删除",
            message = "确定要删除这条账单吗？此操作不可撤销。",
            buttons = listOf(
                AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { transactionToDelete = null },
                AppleDialogButton("删除", AppleDialogButtonStyle.DESTRUCTIVE) {
                    transactionToDelete?.let { viewModel.deleteTransaction(it) }
                    transactionToDelete = null
                }
            )
        )
    }

    if (transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            viewModel = viewModel,
            onDismiss = { transactionToEdit = null },
            onConfirm = { updatedTransaction ->
                viewModel.updateTransaction(updatedTransaction)
                transactionToEdit = null
            }
        )
    }

    val displayCalendar = remember(selectedYearMonth) {
        Calendar.getInstance().apply {
            set(selectedYearMonth.first, selectedYearMonth.second, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val homeDataState = produceState(
        initialValue = HomeData(),
        allTransactions,
        selectedYearMonth
    ) {
        value = withContext(Dispatchers.Default) {
            val periodSummary = buildPeriodSummary(allTransactions, 2, selectedYearMonth)
            val recentDays = buildRecentExpenseTrend(allTransactions)
            val groupedTransactions = buildDayTransactionGroups(allTransactions, selectedYearMonth)
            val currencySummaries = buildCurrencySummaries(periodSummary.transactions)
            
            HomeData(
                periodSummary = periodSummary,
                recentDays = recentDays,
                groupedTransactions = groupedTransactions,
                currencySummaries = currencySummaries,
                isLoaded = true
            )
        }
    }

    val homeData = homeDataState.value
    val isDataLoading = !homeData.isLoaded && allTransactions.isNotEmpty()
    val maxTrendValue = remember(homeData.recentDays) { homeData.recentDays.maxOfOrNull { it.second } ?: 1.0 }

    Scaffold(
        containerColor = Color.Transparent,
    ) { scaffoldPadding ->
        val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(bottom = navBarBottomPadding + 76.dp)
        ) {
            item {
                if (isDataLoading) {
                    OverviewCardSkeleton()
                } else if (multiCurrencyEnabled && allAssets.isNotEmpty()) {
                    MultiCurrencyOverviewCards(
                        allAssets = allAssets,
                        currencySummaries = homeData.currencySummaries,
                        displayCalendar = displayCalendar,
                        onMonthClick = { showMonthPicker = true },
                        enableAnimations = enableCardAnimations
                    )
                } else {
                    SingleCurrencyOverviewCard(
                        periodIncome = homeData.periodSummary.income,
                        periodExpense = homeData.periodSummary.expense,
                        monthlyBudget = monthlyBudget,
                        displayCalendar = displayCalendar,
                        defaultAsset = allAssets.firstOrNull { it.isDefault },
                        onMonthClick = { showMonthPicker = true },
                        enableAnimations = enableCardAnimations,
                        customColorHex = homeCardColorHex
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                if (isDataLoading) {
                    TrendChartSkeleton()
                } else {
                    val trendShape = RoundedCornerShape(24.dp)
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val totalWeekExpense = homeData.recentDays.sumOf { it.second }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .frostedGlass(trendShape, isDark)
                            .clickable { onNavigateToStatistics() },
                        shape = trendShape,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "近7日",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "¥${String.format("%.0f", totalWeekExpense)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                homeData.recentDays.forEach { (label, value) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val barHeight = if (maxTrendValue > 0) (value / maxTrendValue * 48).toFloat().dp else 0.dp
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .height(barHeight.coerceAtLeast(2.dp))
                                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (appMode == AppMode.SMART && !isDataLoading) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    if (aiAnalysisResult != null) {
                        AnomalyAlertCard(
                            aiAlerts = aiAnalysisResult!!.alerts,
                            isFailed = false,
                            isLoading = aiAnalysisLoading,
                            onRefresh = { viewModel.runAiAnalysis() }
                        )
                    } else if (aiAnalysisFailed) {
                        AnomalyAlertCard(
                            aiAlerts = emptyList(),
                            isFailed = true,
                            isLoading = aiAnalysisLoading,
                            onRefresh = { viewModel.runAiAnalysis() }
                        )
                    } else {
                        AnomalyAlertCard(
                            transactions = homeData.periodSummary.transactions,
                            allTransactions = allTransactions,
                            isLoading = aiAnalysisLoading,
                            onRefresh = { viewModel.runAiAnalysis() }
                        )
                    }
                }
            }

            if (isDataLoading) {
                items(5) {
                    TransactionItemSkeleton()
                }
            } else if (allTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "\uD83D\uDCDD", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "还没有账单记录",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val todayStart = todayCal.timeInMillis
                val yesterdayStart = todayStart - 86400000L
                val shortSdf = SimpleDateFormat("MM月dd日", Locale.getDefault())
                homeData.groupedTransactions.forEach { group ->
                    group.transactions.forEachIndexed { index, transaction ->
                        item {
                        // iOS-style staggered entry
                        var itemVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            itemVisible = true
                        }
                        Column {
                            if (index == 0) {
                                val symbol = defaultAsset?.symbol ?: "¥"
                                val dateLabel = when {
                                    group.dateKey >= todayStart -> "今天"
                                    group.dateKey >= yesterdayStart -> "昨天"
                                    else -> shortSdf.format(Date(group.dateKey))
                                }
                                val balanceColor = when {
                                    group.balance > 0 -> incomeColor
                                    group.balance < 0 -> expenseColor
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                val balanceText = when {
                                    group.balance > 0 -> "+$symbol${String.format("%.2f", group.balance)}"
                                    group.balance < 0 -> "-$symbol${String.format("%.2f", -group.balance)}"
                                    else -> "${symbol}0.00"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dateLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                                    Text(balanceText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = balanceColor.copy(alpha = 0.75f))
                                }
                            }
                            Box(modifier = Modifier.staggeredAppearance(index, itemVisible)) {
                                SwipeableTransactionItem(
                                    transaction = transaction,
                                    viewModel = viewModel,
                                    onDelete = { transactionToDelete = transaction },
                                    onEdit = { transactionToEdit = transaction },
                                    onClick = { transactionToEdit = transaction }
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleCurrencyOverviewCard(
    periodIncome: Double,
    periodExpense: Double,
    monthlyBudget: Double,
    displayCalendar: Calendar,
    defaultAsset: CurrencyAsset?,
    onMonthClick: () -> Unit,
    enableAnimations: Boolean,
    customColorHex: String? = null
) {
    val symbol = defaultAsset?.symbol ?: "¥"
    val balance = periodIncome - periodExpense
    val assetAccent = if (customColorHex != null) {
        try { Color(android.graphics.Color.parseColor(customColorHex)) } catch (_: Exception) { Color(0xFF6C63FF) }
    } else if (defaultAsset != null) resolveCardColor(defaultAsset, true) else Color(0xFF6C63FF)
    val cardColor by animateColorAsState(
        targetValue = assetAccent,
        animationSpec = if (enableAnimations) MotionSprings.interactive() else snap(),
        label = "singleCardColor"
    )

    // Apple Card style: dark gradient background, data is the hero
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        cardColor.copy(alpha = 0.95f),
                        cardColor.copy(alpha = 0.75f).copy(red = (cardColor.red * 0.6f).coerceIn(0f, 1f))
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
            // Lightweight header: asset name + month picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = defaultAsset?.name ?: "个人",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    onClick = onMonthClick
                ) {
                    Text(
                        text = "${displayCalendar.get(Calendar.YEAR)}.${String.format("%02d", displayCalendar.get(Calendar.MONTH) + 1)} ▾",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Balance hero number - the visual protagonist
            Text(
                text = "${symbol}${String.format("%.2f", balance)}",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )

            // Budget progress bar (if budget set)
            if (monthlyBudget > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val progress = (periodExpense / monthlyBudget).coerceIn(0.0, 1.0).toFloat()
                val remaining = monthlyBudget - periodExpense
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "预算剩余 ${symbol}${String.format("%.0f", remaining.coerceAtLeast(0.0))}",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${String.format("%.0f", progress * 100)}%",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (progress > 0.9f) Color(0xFFFF453A) else Color.White.copy(alpha = 0.7f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compact income/expense row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column {
                    Text(text = "收入", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${symbol}${String.format("%.2f", periodIncome)}",
                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(text = "支出", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${symbol}${String.format("%.2f", periodExpense)}",
                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiCurrencyOverviewCards(
    allAssets: List<CurrencyAsset>,
    currencySummaries: Map<String, CurrencyPeriodSummary>,
    displayCalendar: Calendar,
    onMonthClick: () -> Unit,
    enableAnimations: Boolean
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(allAssets, key = { it.id }) { asset ->
            val resolvedColor = resolveCardColor(asset, isDark)
            val cardColor by animateColorAsState(
                targetValue = resolvedColor,
                animationSpec = if (enableAnimations) MotionSprings.interactive() else snap(),
                label = "multiCardColor_${asset.id}"
            )
            val summary = currencySummaries[asset.code] ?: CurrencyPeriodSummary()
            val income = summary.income
            val expense = summary.expense
            val isDefault = asset.isDefault

            val interactionSource = remember { MutableInteractionSource() }
            // Apple Card style: dark gradient
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .pressScale(interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {}
                    )
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                cardColor.copy(alpha = 0.95f),
                                cardColor.copy(alpha = 0.75f).copy(red = (cardColor.red * 0.6f).coerceIn(0f, 1f))
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = asset.name,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "默认",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            onClick = onMonthClick
                        ) {
                            Text(
                                text = "${displayCalendar.get(Calendar.YEAR)}.${String.format("%02d", displayCalendar.get(Calendar.MONTH) + 1)} ▾",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${asset.symbol}${String.format("%.2f", income - expense)}",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Column {
                            Text(
                                text = "收入",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${asset.symbol}${String.format("%.2f", income)}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column {
                            Text(
                                text = "支出",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${asset.symbol}${String.format("%.2f", expense)}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Box(modifier = Modifier.size(80.dp, 20.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(120.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                }
                Box(modifier = Modifier.size(100.dp, 32.dp).clip(RoundedCornerShape(12.dp)).shimmer())
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(180.dp, 40.dp).clip(RoundedCornerShape(8.dp)).shimmer())
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Box(modifier = Modifier.size(40.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(100.dp, 20.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(modifier = Modifier.size(40.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(100.dp, 20.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                }
            }
        }
    }
}

@Composable
private fun TrendChartSkeleton() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().frostedGlass(RoundedCornerShape(24.dp), isDark)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(120.dp, 18.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Box(modifier = Modifier.size(60.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(7) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .shimmer()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItemSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmer()
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }
        }
    }
}

@Composable
internal fun FinancialScoreCard(
    income: Double,
    expense: Double,
    transactions: List<Transaction>,
    monthlyBudget: Double
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val savingsRate = if (income > 0) ((income - expense) / income * 100).coerceIn(0.0, 100.0) else 0.0
    val budgetUsage = if (monthlyBudget > 0) (expense / monthlyBudget * 100).coerceIn(0.0, 200.0) else -1.0
    val categoryCount = transactions.filter { it.type == TransactionType.EXPENSE }.map { it.category }.distinct().size
    val avgDailyExpense = if (transactions.isNotEmpty()) {
        val days = transactions.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.DAY_OF_YEAR)
        }.distinct().size.coerceAtLeast(1)
        expense / days
    } else 0.0

    val score = calculateFinancialScore(savingsRate, budgetUsage, avgDailyExpense, transactions.size)
    val scoreColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val scoreLabel = when {
        score >= 80 -> "优秀"
        score >= 70 -> "良好"
        score >= 60 -> "一般"
        else -> "需关注"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedGlass(shape, isDark)
            .clickable { expanded = !expanded },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = scoreColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "财务评分",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scoreLabel,
                        fontSize = 12.sp,
                        color = scoreColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${score.toInt()}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "/100",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "基于储蓄率、预算使用、日均支出、消费类别综合评估",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    ScoreDetailRow("储蓄率", "${String.format("%.1f", savingsRate)}%", savingsRate >= 20)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (budgetUsage >= 0) {
                        ScoreDetailRow("预算使用", "${String.format("%.0f", budgetUsage)}%", budgetUsage <= 80)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ScoreDetailRow("消费类别", "${categoryCount}类", categoryCount <= 8)
                }
            }
        }
    }
}

@Composable
private fun ScoreDetailRow(label: String, value: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGood) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun calculateFinancialScore(
    savingsRate: Double,
    budgetUsage: Double,
    avgDailyExpense: Double,
    transactionCount: Int
): Double {
    var score = 60.0

    score += when {
        savingsRate >= 30 -> 15.0
        savingsRate >= 20 -> 10.0
        savingsRate >= 10 -> 5.0
        savingsRate >= 0 -> 0.0
        else -> -10.0
    }

    if (budgetUsage >= 0) {
        score += when {
            budgetUsage <= 60 -> 10.0
            budgetUsage <= 80 -> 5.0
            budgetUsage <= 100 -> 0.0
            budgetUsage <= 120 -> -5.0
            else -> -10.0
        }
    }

    score += when {
        avgDailyExpense <= 100 -> 10.0
        avgDailyExpense <= 200 -> 5.0
        avgDailyExpense <= 300 -> 0.0
        else -> -5.0
    }

    if (transactionCount in 10..100) score += 5.0
    else if (transactionCount > 100) score += 2.0

    return score.coerceIn(0.0, 100.0)
}

@Composable
private fun AnomalyAlertCard(
    transactions: List<Transaction>,
    allTransactions: List<Transaction>,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val anomalies = remember(transactions, allTransactions) {
        detectAnomalies(transactions, allTransactions)
    }

    if (anomalies.isEmpty()) return

    val shape = RoundedCornerShape(24.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedGlass(shape, isDark),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "消费提醒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${anomalies.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading) {
                        AppleLoadingIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2f,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            anomalies.forEach { anomaly ->
                AnomalyAlertItem(anomaly)
                if (anomaly != anomalies.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AnomalyAlertCard(
    aiAlerts: List<com.inkqilin.ledger.service.AiAlert>,
    isFailed: Boolean,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val shape = RoundedCornerShape(24.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedGlass(shape, isDark),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "消费提醒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isFailed && aiAlerts.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${aiAlerts.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isFailed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "分析失败",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading) {
                        AppleLoadingIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2f,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isFailed) {
                Text(
                    text = "AI 分析暂不可用，请检查 API 配置或点击刷新按钮重试",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (aiAlerts.isEmpty()) {
                Text(
                    text = "暂无消费提醒，继续保持良好习惯！",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                aiAlerts.forEachIndexed { index, alert ->
                    AiAnomalyAlertItem(alert)
                    if (index < aiAlerts.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAnomalyAlertItem(alert: com.inkqilin.ledger.service.AiAlert) {
    var expanded by remember { mutableStateOf(false) }
    val alertColor = when (alert.severity) {
        "warning" -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }
    val alertIcon = when (alert.severity) {
        "warning" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = alertColor.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        alertIcon,
                        contentDescription = null,
                        tint = alertColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = alert.percent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = alertColor
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = alert.detail,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AnomalyAlertItem(anomaly: AnomalyInfo) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = anomaly.color.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        anomaly.icon,
                        contentDescription = null,
                        tint = anomaly.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = anomaly.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = anomaly.changePercent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = anomaly.color
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = anomaly.detail,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class AnomalyInfo(
    val title: String,
    val changePercent: String,
    val detail: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun detectAnomalies(
    currentTransactions: List<Transaction>,
    allTransactions: List<Transaction>
): List<AnomalyInfo> {
    val anomalies = mutableListOf<AnomalyInfo>()

    val currentExpenses = currentTransactions.filter { it.type == TransactionType.EXPENSE }
    if (currentExpenses.isEmpty()) return emptyList()

    val categoryGroups = currentExpenses.groupBy { it.category }
    val totalCurrentExpense = currentExpenses.sumOf { it.amount }

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val historicalTransactions = allTransactions.filter { tx ->
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
        txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) < currentMonth
    }
    val historicalExpenses = historicalTransactions.filter { it.type == TransactionType.EXPENSE }

    val historicalMonths = historicalTransactions.map {
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        cal.get(Calendar.MONTH)
    }.distinct().size.coerceAtLeast(1)

    categoryGroups.forEach { (category, txs) ->
        val currentCategoryTotal = txs.sumOf { it.amount }
        val currentCategoryPercent = if (totalCurrentExpense > 0) currentCategoryTotal / totalCurrentExpense * 100 else 0.0

        if (currentCategoryPercent > 30 && currentCategoryTotal > 500) {
            val historicalCategoryTotal = historicalExpenses.filter { it.category == category }.sumOf { it.amount }
            val historicalAvg = historicalCategoryTotal / historicalMonths

            if (historicalAvg > 0) {
                val changePercent = ((currentCategoryTotal - historicalAvg) / historicalAvg * 100)
                if (changePercent > 20) {
                    anomalies.add(
                        AnomalyInfo(
                            title = "本月${category}支出偏高",
                            changePercent = "+${String.format("%.0f", changePercent)}%",
                            detail = "本月${category}支出¥${String.format("%.0f", currentCategoryTotal)}，历史月均¥${String.format("%.0f", historicalAvg)}，占比${String.format("%.0f", currentCategoryPercent)}%",
                            color = Color(0xFFF44336),
                            icon = Icons.Default.Warning
                        )
                    )
                }
            }
        }
    }

    val dailyExpenses = currentExpenses.groupBy { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        cal.get(Calendar.DAY_OF_YEAR)
    }.map { it.value.sumOf { tx -> tx.amount } }

    if (dailyExpenses.isNotEmpty()) {
        val avgDaily = dailyExpenses.average()
        val maxDaily = dailyExpenses.maxOrNull() ?: 0.0
        if (maxDaily > avgDaily * 2 && maxDaily > 300) {
            anomalies.add(
                AnomalyInfo(
                    title = "存在单日高额消费",
                    changePercent = "¥${String.format("%.0f", maxDaily)}",
                    detail = "日均支出¥${String.format("%.0f", avgDaily)}，最高单日消费¥${String.format("%.0f", maxDaily)}，是均值的${String.format("%.1f", maxDaily / avgDaily)}倍",
                    color = Color(0xFFFF9800),
                    icon = Icons.Default.Info
                )
            )
        }
    }

    return anomalies.take(3)
}

private data class HomeData(
    val periodSummary: PeriodSummary = PeriodSummary(),
    val recentDays: List<Pair<String, Double>> = emptyList(),
    val groupedTransactions: List<DayTransactionGroup> = emptyList(),
    val currencySummaries: Map<String, CurrencyPeriodSummary> = emptyMap(),
    val isLoaded: Boolean = false
)

private data class PeriodSummary(
    val transactions: List<Transaction> = emptyList(),
    val income: Double = 0.0,
    val expense: Double = 0.0
)

private data class CurrencyPeriodSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0
)

private data class DayTransactionGroup(
    val dateKey: Long,
    val transactions: List<Transaction>,
    val income: Double,
    val expense: Double
) {
    val balance: Double
        get() = income - expense
}

private fun buildPeriodSummary(
    allTransactions: List<Transaction>,
    selectedPeriod: Int,
    selectedYearMonth: Pair<Int, Int>
): PeriodSummary {
    val calendar = Calendar.getInstance()
    val transactions = when (selectedPeriod) {
        0 -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            allTransactions.filter { it.date >= calendar.timeInMillis }
        }
        1 -> {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            allTransactions.filter { it.date >= calendar.timeInMillis }
        }
        2 -> {
            val start = Calendar.getInstance().apply {
                set(selectedYearMonth.first, selectedYearMonth.second, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                set(selectedYearMonth.first, selectedYearMonth.second + 1, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            allTransactions.filter { it.date in start until end }
        }
        3 -> {
            val start = Calendar.getInstance().apply {
                set(selectedYearMonth.first, Calendar.JANUARY, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                set(selectedYearMonth.first + 1, Calendar.JANUARY, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            allTransactions.filter { it.date in start until end }
        }
        else -> allTransactions
    }

    var income = 0.0
    var expense = 0.0
    transactions.forEach { transaction ->
        when (transaction.type) {
            TransactionType.INCOME -> income += transaction.amount
            TransactionType.EXPENSE -> expense += transaction.amount
        }
    }
    return PeriodSummary(transactions = transactions, income = income, expense = expense)
}

private fun buildRecentExpenseTrend(allTransactions: List<Transaction>): List<Pair<String, Double>> {
    val groups = mutableListOf<Pair<String, Double>>()
    for (i in 6 downTo 0) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        val end = start + 86400000L
        var sum = 0.0
        allTransactions.forEach { transaction ->
            if (transaction.type == TransactionType.EXPENSE && transaction.date in start until end) {
                sum += transaction.amount
            }
        }
        groups.add("${calendar.get(Calendar.DAY_OF_MONTH)}" to sum)
    }
    return groups
}

private fun buildDayTransactionGroups(allTransactions: List<Transaction>, selectedYearMonth: Pair<Int, Int>): List<DayTransactionGroup> {
    val monthStart = Calendar.getInstance().apply {
        set(selectedYearMonth.first, selectedYearMonth.second, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val monthEnd = Calendar.getInstance().apply {
        set(selectedYearMonth.first, selectedYearMonth.second + 1, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return allTransactions
        .filter { it.date in monthStart until monthEnd }
        .groupBy { transaction ->
            Calendar.getInstance().apply {
                timeInMillis = transaction.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        .toSortedMap(compareByDescending { it })
        .map { (dateKey, transactions) ->
            var income = 0.0
            var expense = 0.0
            transactions.forEach { transaction ->
                when (transaction.type) {
                    TransactionType.INCOME -> income += transaction.amount
                    TransactionType.EXPENSE -> expense += transaction.amount
                }
            }
            DayTransactionGroup(
                dateKey = dateKey,
                transactions = transactions,
                income = income,
                expense = expense
            )
        }
}

private fun buildCurrencySummaries(
    transactions: List<Transaction>
): Map<String, CurrencyPeriodSummary> {
    val summaries = linkedMapOf<String, CurrencyPeriodSummary>()
    transactions.forEach { transaction ->
        val current = summaries[transaction.currency] ?: CurrencyPeriodSummary()
        val updated = when (transaction.type) {
            TransactionType.INCOME -> current.copy(income = current.income + transaction.amount)
            TransactionType.EXPENSE -> current.copy(expense = current.expense + transaction.amount)
        }
        summaries[transaction.currency] = updated
    }
    return summaries
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    viewModel: TransactionViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var note by remember { mutableStateOf(transaction.note) }
    var type by remember { mutableStateOf(transaction.type) }
    var category by remember { mutableStateOf(transaction.category) }
    var date by remember { mutableLongStateOf(transaction.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val allAssets by viewModel.allAssets.collectAsState()
    val currencySymbol = allAssets.firstOrNull { it.code == transaction.currency }?.symbol ?: "¥"
    val categories = allCategories.filter { it.type == type }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        AppleDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        )
    }

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = "编辑账单",
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { 
                            type = TransactionType.EXPENSE
                            if (category !in allCategories.filter { it.type == TransactionType.EXPENSE }.map { it.name }) {
                                category = allCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.name ?: ""
                            }
                        },
                        label = { Text("支出") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { 
                            type = TransactionType.INCOME
                            if (category !in allCategories.filter { it.type == TransactionType.INCOME }.map { it.name }) {
                                category = allCategories.firstOrNull { it.type == TransactionType.INCOME }?.name ?: ""
                            }
                        },
                        label = { Text("收入") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("账单分类", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val selected = category == cat.name
                        InputChip(
                            selected = selected,
                            onClick = { category = cat.name },
                            label = { Text(cat.name) },
                            leadingIcon = { Text(cat.icon) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("账单金额") },
                    prefix = { Text("$currencySymbol ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("账单备注") },
                    modifier = Modifier.fillMaxWidth()
                )

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("账单时间", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sdf.format(Date(date)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        buttons = listOf(
            AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL, onDismiss),
            AppleDialogButton("确认修改", AppleDialogButtonStyle.DEFAULT) {
                val amountDouble = amount.toDoubleOrNull() ?: 0.0
                if (amountDouble > 0 && category.isNotEmpty()) {
                    onConfirm(transaction.copy(
                        amount = amountDouble,
                        note = note,
                        type = type,
                        category = category,
                        date = date
                    ))
                }
            }
        )
    )
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val transactions = listOf(
                Transaction(1, 35.50, "餐饮", "午餐", now, TransactionType.EXPENSE, "CNY"),
                Transaction(2, 12.00, "交通", "地铁", now, TransactionType.EXPENSE, "CNY"),
                Transaction(3, 5000.00, "工资", "", now - dayMs, TransactionType.INCOME, "CNY"),
                Transaction(4, 89.00, "购物", "", now - dayMs, TransactionType.EXPENSE, "CNY"),
                Transaction(5, 200.00, "餐饮", "聚餐", now - dayMs * 2, TransactionType.EXPENSE, "CNY")
            )
            val groupedTransactions = transactions.groupBy { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }.toSortedMap(compareByDescending { it })
            val daySdf = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("总览", color = Color.White, fontSize = 13.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text("收入", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp); Text("¥5,000.00", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                                Column(horizontalAlignment = Alignment.End) { Text("支出", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp); Text("¥336.50", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("结余 ¥4,663.50", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                groupedTransactions.forEach { (dateKey, txs) ->
                    val dayIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val dayExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val dayBalance = dayIncome - dayExpense
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(daySdf.format(Date(dateKey)), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val balanceText = when {
                                dayBalance > 0 -> "+¥${String.format("%.2f", dayBalance)}"
                                dayBalance < 0 -> "-¥${String.format("%.2f", -dayBalance)}"
                                else -> "¥0.00"
                            }
                            val balanceColor = when {
                                dayBalance > 0 -> Color(0xFF4CAF50)
                                dayBalance < 0 -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(balanceText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = balanceColor)
                        }
                    }
                    items(txs) { tx ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val isIncome = tx.type == TransactionType.INCOME
                                val accent = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                                val emoji = mapOf("餐饮" to "🍜", "交通" to "🚌", "购物" to "🛒", "工资" to "💰")
                                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text(emoji[tx.category] ?: "📋", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.category, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${if (isIncome) "+" else "-"}¥${String.format("%.2f", tx.amount)}", color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
