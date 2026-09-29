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


internal fun CalculateFinancialScore(
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

internal data class AnomalyInfo(
    val title: String,
    val changePercent: String,
    val detail: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

internal fun DetectAnomalies(
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

internal data class HomeData(
    val periodSummary: PeriodSummary = PeriodSummary(),
    val recentDays: List<Pair<String, Double>> = emptyList(),
    val groupedTransactions: List<DayTransactionGroup> = emptyList(),
    val currencySummaries: Map<String, CurrencyPeriodSummary> = emptyMap(),
    val isLoaded: Boolean = false
)

internal data class PeriodSummary(
    val transactions: List<Transaction> = emptyList(),
    val income: Double = 0.0,
    val expense: Double = 0.0
)

internal data class CurrencyPeriodSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0
)

internal data class DayTransactionGroup(
    val dateKey: Long,
    val transactions: List<Transaction>,
    val income: Double,
    val expense: Double
) {
    val balance: Double
        get() = income - expense
}

internal fun BuildPeriodSummary(
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

internal fun BuildRecentExpenseTrend(allTransactions: List<Transaction>): List<Pair<String, Double>> {
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

internal fun BuildDayTransactionGroups(allTransactions: List<Transaction>, selectedYearMonth: Pair<Int, Int>): List<DayTransactionGroup> {
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

internal fun BuildCurrencySummaries(
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
