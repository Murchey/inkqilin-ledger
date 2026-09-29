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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.zIndex

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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TransactionViewModel,
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToEditTransaction: (Transaction) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onNavigateToSearch: () -> Unit = {},
    onNavigateToOcrRecognition: () -> Unit = {},
    onNavigateToAssetManagement: () -> Unit = {}
) {
    // StateFlow 已有缓存值，不要再用 emptyList 当 initial，否则返回首页时会闪一帧空列表
    val allTransactions by viewModel.allTransactions.collectAsState()
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
    val homeBgImagePath by viewModel.homeBgImagePath.collectAsState()
    val homeBgOpacity by viewModel.homeBgOpacity.collectAsState()
    val homeTxCardOpacity by viewModel.homeTxCardOpacity.collectAsState()
    val autoBackupError by viewModel.autoBackupError.collectAsState()

    // 自动备份失败：首页弹窗提醒（看过即清）
    if (!autoBackupError.isNullOrBlank()) {
        AppleAlertDialog(
            onDismissRequest = { viewModel.clearAutoBackupError() },
            title = "自动备份失败",
            message = autoBackupError,
            buttons = listOf(
                AppleDialogButton("知道了", AppleDialogButtonStyle.DEFAULT) {
                    viewModel.clearAutoBackupError()
                }
            )
        )
    }

    var selectedYearMonth by rememberSaveable(
        stateSaver = listSaver(
            save = { listOf(it.first, it.second) },
            restore = { it[0] to it[1] }
        )
    ) {
        mutableStateOf(Calendar.getInstance().let { it.get(Calendar.YEAR) to it.get(Calendar.MONTH) })
    }
    var showMonthPicker by remember { mutableStateOf(false) }
    var enableCardAnimations by remember { mutableStateOf(false) }
    // 列表入场动画闸门：只在首次进入时打开，返回首页不重放
    var hasPlayedListEntry by rememberSaveable { mutableStateOf(false) }

    val defaultAsset = remember(allAssets) { allAssets.firstOrNull { it.isDefault } }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        enableCardAnimations = true
        viewModel.checkAndRunDailyAnalysis()
        if (!hasPlayedListEntry) {
            kotlinx.coroutines.delay(STAGGER_MAX_DELAY_MS + STAGGER_DURATION_MS + 80L)
            hasPlayedListEntry = true
        }
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

    val displayCalendar = remember(selectedYearMonth) {
        Calendar.getInstance().apply {
            set(selectedYearMonth.first, selectedYearMonth.second, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // 同步计算：produceState 首帧会给出空 HomeData，导致返回时列表结构先塌缩再撑开，滚动位置被夹掉
    val homeData = remember(allTransactions, selectedYearMonth) {
        if (allTransactions.isEmpty()) {
            HomeData(isLoaded = true)
        } else {
            val periodSummary = BuildPeriodSummary(allTransactions, 2, selectedYearMonth)
            val recentDays = BuildRecentExpenseTrend(allTransactions)
            val groupedTransactions = BuildDayTransactionGroups(allTransactions, selectedYearMonth)
            val currencySummaries = BuildCurrencySummaries(periodSummary.transactions)
            HomeData(
                periodSummary = periodSummary,
                recentDays = recentDays,
                groupedTransactions = groupedTransactions,
                currencySummaries = currencySummaries,
                isLoaded = true
            )
        }
    }
    val isDataLoading = false
    val maxTrendValue = remember(homeData.recentDays) { homeData.recentDays.maxOfOrNull { it.second } ?: 1.0 }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    // 共享 shimmer 进度：骨架块不再各自开无限动画
    val shimmerProgress = rememberShimmerProgress()
    val staggerGate = !hasPlayedListEntry

    CompositionLocalProvider(LocalShimmerProgress provides shimmerProgress) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 首页自定义背景（设置里导入，可调不透明度）
        val bgFile = homeBgImagePath?.let { java.io.File(it) }
        if (bgFile != null && bgFile.exists()) {
            coil.compose.AsyncImage(
                model = bgFile,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = homeBgOpacity.coerceIn(0.05f, 1f),
                modifier = Modifier
                    .fillMaxSize()
                    .matchParentSize()
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
        ) { scaffoldPadding ->
            val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (bgFile != null && bgFile.exists()) Color.Transparent
                        else MaterialTheme.colorScheme.background
                    )
                    .padding(scaffoldPadding),
                contentPadding = PaddingValues(bottom = navBarBottomPadding + 76.dp)
            ) {
            item(key = "overview") {
                if (isDataLoading) {
                    OverviewCardSkeleton()
                } else if (multiCurrencyEnabled && allAssets.isNotEmpty()) {
                    MultiCurrencyOverviewCards(
                        allAssets = allAssets,
                        currencySummaries = homeData.currencySummaries,
                        displayCalendar = displayCalendar,
                        onMonthClick = { showMonthPicker = true },
                        enableAnimations = enableCardAnimations,
                        translucent = bgFile != null && bgFile.exists()
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
                        customColorHex = homeCardColorHex,
                        translucent = bgFile != null && bgFile.exists()
                    )
                }
            }

            item(key = "trend") {
                Spacer(modifier = Modifier.height(16.dp))

                if (isDataLoading) {
                    TrendChartSkeleton()
                } else {
                    val trendShape = com.inkqilin.ledger.ui.theme.Corners.Lg
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val totalWeekExpense = homeData.recentDays.sumOf { it.second }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = com.inkqilin.ledger.ui.theme.Space.PageHorizontal)
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
                item(key = "ai_alert") {
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
                items(5, key = { "skeleton_$it" }) {
                    TransactionItemSkeleton()
                }
            } else if (allTransactions.isEmpty()) {
                item(key = "empty") {
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
                // 跨日分组的全局条目序号
                var globalTxIndex = 0
                homeData.groupedTransactions.forEach { group ->
                    group.transactions.forEachIndexed { index, transaction ->
                        // 全局列表序号：stagger 只作用于整表前 N 条，而不是每天分组内前 N 条
                        val globalIndex = globalTxIndex
                        globalTxIndex++
                        item(key = "tx_${transaction.id}") {
                        // 首次进入对前 N 条 stagger；二级页返回/滚动复用不重放
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
                            Box(modifier = Modifier.staggeredAppearance(globalIndex, visible = staggerGate)) {
                                SwipeableTransactionItem(
                                    transaction = transaction,
                                    viewModel = viewModel,
                                    onDelete = { transactionToDelete = transaction },
                                    onEdit = { onNavigateToEditTransaction(transaction) },
                                    onClick = { onNavigateToEditTransaction(transaction) },
                                    cardOpacity = homeTxCardOpacity
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
    }
}
