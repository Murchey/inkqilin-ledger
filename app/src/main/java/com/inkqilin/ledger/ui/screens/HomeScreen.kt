package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: TransactionViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("全部") }
    val filterOptions = listOf("全部", "本周", "本月", "本年")
    
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
                    colors = ButtonDefaults.buttonColors(containerColor = InkRed)
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

    val filteredTransactions = remember(allTransactions, selectedFilter) {
        val calendar = Calendar.getInstance()
        when (selectedFilter) {
            "本周" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                allTransactions.filter { it.date >= calendar.timeInMillis }
            }
            "本月" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                allTransactions.filter { it.date >= calendar.timeInMillis }
            }
            "本年" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                allTransactions.filter { it.date >= calendar.timeInMillis }
            }
            else -> allTransactions
        }
    }

    val totalIncome = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedFilter == "全部") "总结余" else "$selectedFilter 结余",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = selectedFilter, color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                filterOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedFilter = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥${String.format("%.2f", totalIncome - totalExpense)}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "收入",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "¥${String.format("%.2f", totalIncome)}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "支出",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "¥${String.format("%.2f", totalExpense)}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (selectedFilter == "全部") "最近账单" else "$selectedFilter 账单",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📝",
                            fontSize = 36.sp
                        )
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
            val groupedTransactions = filteredTransactions.groupBy { transaction ->
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
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = daySdf.format(Date(dateKey)),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val dayTotal = transactions.sumOf {
                            if (it.type == TransactionType.EXPENSE) -it.amount else it.amount
                        }
                        Text(
                            text = "¥${String.format("%.2f", dayTotal)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (dayTotal >= 0) InkGreen else InkRed
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
                // 类型切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { 
                            type = TransactionType.EXPENSE
                            // 如果切换类型，尝试匹配一个新类型的分类
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

                // 分类选择
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

                // 金额修改
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("账单金额") },
                    prefix = { Text("¥ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // 备注修改
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("账单备注") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 时间修改
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
