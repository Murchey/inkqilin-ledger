package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TransactionViewModel,
    onNavigateToAddTransaction: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onNavigateToSearch: () -> Unit = {}
) {
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val multiCurrencyEnabled by viewModel.multiCurrencyEnabled.collectAsState()
    val allAssets by viewModel.allAssets.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))

    var selectedPeriod by remember { mutableIntStateOf(2) }
    var selectedChartCurrency by remember { mutableStateOf<String?>(null) }

    val defaultAsset = remember(allAssets) { allAssets.firstOrNull { it.isDefault } }
    LaunchedEffect(allAssets, multiCurrencyEnabled) {
        if (multiCurrencyEnabled && selectedChartCurrency == null) {
            selectedChartCurrency = defaultAsset?.code
        }
    }
    val periodOptions = listOf("日", "周", "月", "年")
    val calendar = remember { Calendar.getInstance() }

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条账单吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                ) {
                    Text("删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("取消")
                }
            }
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

    val periodTransactions = remember(allTransactions, selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            0 -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                allTransactions.filter { it.date >= cal.timeInMillis }
            }
            1 -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                allTransactions.filter { it.date >= cal.timeInMillis }
            }
            2 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                allTransactions.filter { it.date >= cal.timeInMillis }
            }
            3 -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                allTransactions.filter { it.date >= cal.timeInMillis }
            }
            else -> allTransactions
        }
    }

    val periodIncome = periodTransactions
        .filter { it.type == TransactionType.INCOME && (!multiCurrencyEnabled || selectedChartCurrency == null || it.currency == selectedChartCurrency) }
        .sumOf { it.amount }
    val periodExpense = periodTransactions
        .filter { it.type == TransactionType.EXPENSE && (!multiCurrencyEnabled || selectedChartCurrency == null || it.currency == selectedChartCurrency) }
        .sumOf { it.amount }

    val chartData = remember(periodTransactions, selectedPeriod, selectedChartCurrency, multiCurrencyEnabled) {
        val currencyFilter: (Transaction) -> Boolean = { t ->
            !multiCurrencyEnabled || selectedChartCurrency == null || t.currency == selectedChartCurrency
        }
        val groups = mutableListOf<Pair<String, Double>>()
        when (selectedPeriod) {
            0 -> {
                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                    val start = c.timeInMillis
                    val end = start + 86400000L
                    val sum = periodTransactions.filter { it.type == TransactionType.EXPENSE && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add("${c.get(Calendar.DAY_OF_MONTH)}" to sum)
                }
            }
            1 -> {
                val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")
                val c = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                for (i in 0..6) {
                    val start = c.timeInMillis
                    val end = start + 86400000L
                    val sum = periodTransactions.filter { it.type == TransactionType.EXPENSE && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add(dayNames[i] to sum)
                    c.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            2 -> {
                for (i in 1..12) {
                    val c2 = Calendar.getInstance().apply {
                        set(Calendar.MONTH, i - 1); set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val start = c2.timeInMillis
                    c2.set(Calendar.MONTH, i)
                    val end = c2.timeInMillis
                    val sum = allTransactions.filter { it.type == TransactionType.EXPENSE && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add("${i}月" to sum)
                }
            }
            3 -> {
                val y = Calendar.getInstance().get(Calendar.YEAR)
                for (i in y - 4..y) {
                    val start = Calendar.getInstance().apply { set(i, Calendar.JANUARY, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    val end = Calendar.getInstance().apply { set(i + 1, Calendar.JANUARY, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    val sum = allTransactions.filter { it.type == TransactionType.EXPENSE && currencyFilter(it) && it.date in start until end }.sumOf { it.amount }
                    groups.add("${i}" to sum)
                }
            }
        }
        groups
    }

    val categoryBudgets = remember(periodTransactions) {
        periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, txns) -> cat to txns.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(4)
    }
    val totalPeriodExpense = categoryBudgets.sumOf { it.second }
    val categoryColors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFF45B7D1),
        Color(0xFFFFA07A),
        Color(0xFF98D8C8),
        Color(0xFFF7DC6F)
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = Color(0xFF6C63FF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "记一笔", tint = Color.White)
            }
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (multiCurrencyEnabled && allAssets.isNotEmpty()) {
                    MultiCurrencyOverviewCards(
                        allAssets = allAssets,
                        periodTransactions = periodTransactions,
                        calendar = calendar,
                        periodOptions = periodOptions,
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { selectedPeriod = it }
                    )
                } else {
                    SingleCurrencyOverviewCard(
                        periodIncome = periodIncome,
                        periodExpense = periodExpense,
                        monthlyBudget = monthlyBudget,
                        calendar = calendar,
                        defaultAsset = allAssets.firstOrNull { it.isDefault },
                        periodOptions = periodOptions,
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { selectedPeriod = it }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                val selectedAsset = allAssets.firstOrNull { it.code == selectedChartCurrency }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "支出统计",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (multiCurrencyEnabled && allAssets.isNotEmpty()) {
                                var currencyMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { currencyMenuExpanded = true }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedAsset?.code ?: "全部",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = currencyMenuExpanded,
                                        onDismissRequest = { currencyMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("全部") },
                                            onClick = {
                                                selectedChartCurrency = null
                                                currencyMenuExpanded = false
                                            }
                                        )
                                        allAssets.forEach { asset ->
                                            DropdownMenuItem(
                                                text = { Text("${asset.name} (${asset.code})") },
                                                onClick = {
                                                    selectedChartCurrency = asset.code
                                                    currencyMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val maxVal = chartData.maxOfOrNull { it.second } ?: 1.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            chartData.forEach { (label, value) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val barHeight = if (maxVal > 0) (value / maxVal * 80).toFloat().dp else 0.dp
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(barHeight.coerceAtLeast(2.dp))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(expenseColor)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (monthlyBudget > 0) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "预算",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val budgetProgress = if (monthlyBudget > 0) (periodExpense / monthlyBudget).toFloat().coerceIn(0f, 1.5f) else 0f
                            val budgetColor = when {
                                budgetProgress <= 0.5f -> Color(0xFF4CAF50)
                                budgetProgress <= 0.8f -> Color(0xFFFF9800)
                                budgetProgress <= 1.0f -> Color(0xFFFF5722)
                                else -> Color(0xFFF44336)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("本月总预算", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("¥${String.format("%.2f", periodExpense)} / ¥${String.format("%.2f", monthlyBudget)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = budgetProgress.coerceAtMost(1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = budgetColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            if (categoryBudgets.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                categoryBudgets.forEachIndexed { index, (cat, amount) ->
                                    val color = categoryColors[index % categoryColors.size]
                                    val progress = if (totalPeriodExpense > 0) (amount / totalPeriodExpense).toFloat() else 0f
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(cat, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text(
                                            "¥${String.format("%.2f", amount)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier
                                                .width(60.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = color,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "账单",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (allTransactions.isEmpty()) {
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
                val daySdf = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())
                val groupedTransactions = allTransactions.groupBy { transaction ->
                    val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }.toSortedMap(compareByDescending { it })

                groupedTransactions.entries.forEachIndexed { groupIndex, (dateKey, transactions) ->
                    if (groupIndex > 0) {
                        item {
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = daySdf.format(Date(dateKey)),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(
                        items = transactions,
                        key = { it.id }
                    ) { transaction ->
                        SwipeableTransactionItem(
                            transaction = transaction,
                            viewModel = viewModel,
                            onDelete = { transactionToDelete = transaction },
                            onEdit = { transactionToEdit = transaction }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleCurrencyOverviewCard(
    periodIncome: Double,
    periodExpense: Double,
    monthlyBudget: Double,
    calendar: Calendar,
    defaultAsset: CurrencyAsset?,
    periodOptions: List<String>,
    selectedPeriod: Int,
    onPeriodSelected: (Int) -> Unit
) {
    val symbol = defaultAsset?.symbol ?: "¥"
    val cardColor = try {
        defaultAsset?.cardColor?.let { Color(android.graphics.Color.parseColor(it)) }
    } catch (e: Exception) { null } ?: Color(0xFF6C63FF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = defaultAsset?.name ?: "个人",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (monthlyBudget > 0) "预算剩余 ${symbol}${String.format("%.2f", monthlyBudget - periodExpense)}" else "本${periodOptions[selectedPeriod]}收支",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "${calendar.get(Calendar.YEAR)}.${String.format("%02d", calendar.get(Calendar.MONTH) + 1)} ▾",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "${symbol}${String.format("%.2f", periodIncome - periodExpense)}",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "收入",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${symbol}${String.format("%.2f", periodIncome)}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "支出",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${symbol}${String.format("%.2f", periodExpense)}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                periodOptions.forEachIndexed { index, label ->
                    val isSelected = selectedPeriod == index
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clickable { onPeriodSelected(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiCurrencyOverviewCards(
    allAssets: List<CurrencyAsset>,
    periodTransactions: List<Transaction>,
    calendar: Calendar,
    periodOptions: List<String>,
    selectedPeriod: Int,
    onPeriodSelected: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(allAssets, key = { it.id }) { asset ->
            val cardColor = try {
                Color(android.graphics.Color.parseColor(asset.cardColor))
            } catch (e: Exception) {
                Color(0xFF6C63FF)
            }
            val income = periodTransactions
                .filter { it.type == TransactionType.INCOME && it.currency == asset.code }
                .sumOf { it.amount }
            val expense = periodTransactions
                .filter { it.type == TransactionType.EXPENSE && it.currency == asset.code }
                .sumOf { it.amount }
            val isDefault = asset.isDefault

            Card(
                modifier = Modifier
                    .width(300.dp)
                    .let {
                        if (isDefault) it else it
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = asset.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isDefault) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "默认",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "本${periodOptions[selectedPeriod]}收支",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "${calendar.get(Calendar.YEAR)}.${String.format("%02d", calendar.get(Calendar.MONTH) + 1)} ▾",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${asset.symbol}${String.format("%.2f", income - expense)}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "收入",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${asset.symbol}${String.format("%.2f", income)}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "支出",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${asset.symbol}${String.format("%.2f", expense)}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        periodOptions.forEachIndexed { index, label ->
                            val isSelected = selectedPeriod == index
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .clickable { onPeriodSelected(index) }
                            )
                        }
                    }
                }
            }
        }
    }
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
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑账单") },
        text = {
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
        confirmButton = {
            Button(
                onClick = {
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
            ) { Text("确认修改") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}


