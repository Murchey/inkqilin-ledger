package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.ui.TransactionViewModel
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
import androidx.navigation.NavController
import com.inkqilin.ledger.data.Category
import kotlin.math.roundToInt

enum class TimePeriod(val label: String) {
    WEEK("本周"), MONTH("本月"), YEAR("本年"), CUSTOM("自定义")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: TransactionViewModel, navController: NavController) {
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val incomeColor = Color(android.graphics.Color.parseColor(incomeColorHex))
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTH) }
    
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

    val filteredTransactions = filteredByPeriod.filter { it.type == selectedType }
    val totalAmount = filteredTransactions.sumOf { it.amount }
    val categoryTotals = filteredTransactions.groupBy { it.category }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .toList()
        .sortedByDescending { it.second }

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
                    val accentColor = if (type == TransactionType.EXPENSE) InkRed else InkGreen
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

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = selectedPeriod.label + "总" + if (selectedType == TransactionType.EXPENSE) "支出" else "收入",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥${String.format("%.2f", totalAmount)}",
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
                        .clip(RoundedCornerShape(12.dp))
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
                    Card(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                            .fillMaxWidth()
                            .draggable(
                                state = draggableState,
                                orientation = Orientation.Horizontal,
                                onDragStopped = {
                                    offsetX = if (offsetX < -menuWidthPx / 2) -menuWidthPx else 0f
                                }
                            )
                            .clickable {
                                navController.navigate("category_transactions/$categoryName/${selectedType.name}")
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                    text = "¥${String.format("%.2f", total)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = displayColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
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
                                        .fillMaxWidth(fraction = percentage)
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
    val start = Calendar.getInstance()
    start.timeInMillis = now.timeInMillis
    when (period) {
        TimePeriod.WEEK -> {
            start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)
        }
        TimePeriod.MONTH -> {
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)
        }
        TimePeriod.YEAR -> {
            start.set(Calendar.MONTH, Calendar.JANUARY)
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)
        }
        TimePeriod.CUSTOM -> return transactions // Handled outside
    }
    return transactions.filter { it.date >= start.timeInMillis }
}
