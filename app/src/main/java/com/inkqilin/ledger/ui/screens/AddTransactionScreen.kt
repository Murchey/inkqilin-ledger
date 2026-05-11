package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.RenQingContact
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel,
    onSaved: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var syncToRenQing by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<RenQingContact?>(null) }

    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val categories = allCategories.filter { it.type == type }
    val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
    val allContacts by renQingViewModel.allContacts.collectAsState()

    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val incomeColor = Color(android.graphics.Color.parseColor(incomeColorHex))
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

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
        ) { DatePicker(state = datePickerState) }
    }

    if (showAddCategoryDialog) {
        CategoryEditDialog(
            category = Category(name = "", icon = "", type = type),
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { newCategory ->
                viewModel.addCategory(newCategory.name, newCategory.icon, newCategory.type, newCategory.color)
                category = newCategory.name
                showAddCategoryDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("记一笔", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(sdf.format(Date(date))) },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(TransactionType.EXPENSE to "支出", TransactionType.INCOME to "收入").forEach { (t, label) ->
                    val selected = type == t
                    val accentColor = if (t == TransactionType.EXPENSE) expenseColor else incomeColor
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (selected) accentColor else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { type = t; category = "" }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it },
                        label = { Text("金额") },
                        prefix = { Text("¥ ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("选择分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showAddCategoryDialog = true }) { Text("+ 新增") }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        val chunkedCategories = categories.chunked(3)
        chunkedCategories.forEach { rowCategories ->
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowCategories.forEach { cat ->
                        CategoryChip(cat.name, cat.icon, category == cat.name) { category = cat.name }
                    }
                    repeat(3 - rowCategories.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth().padding(4.dp), shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (renQingEnabled) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = syncToRenQing, onCheckedChange = {
                                syncToRenQing = it
                                if (!it) selectedContact = null
                            })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("同步添加到人情账本", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (syncToRenQing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            var contactMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedContact?.name ?: "",
                                    onValueChange = {},
                                    modifier = Modifier.fillMaxWidth().clickable { contactMenuExpanded = true },
                                    label = { Text("选择联系人") },
                                    readOnly = true,
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { contactMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = contactMenuExpanded,
                                    onDismissRequest = { contactMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    allContacts.forEach { contact ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(contact.name, fontWeight = FontWeight.Medium)
                                                    Text(contact.relationship.label, style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            onClick = {
                                                selectedContact = contact
                                                contactMenuExpanded = false
                                            }
                                        )
                                    }
                                    if (allContacts.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("暂无联系人，请先添加", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            onClick = { contactMenuExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (amountDouble > 0 && category.isNotEmpty()) {
                        viewModel.addTransaction(
                            Transaction(
                                amount = amountDouble,
                                category = category,
                                note = note,
                                date = date,
                                type = type
                            )
                        )
                        if (syncToRenQing) {
                            renQingViewModel.addRenQingEventFromTransaction(
                                amountDouble, type, category, note, date,
                                contactId = selectedContact?.id ?: 0,
                                contactName = selectedContact?.name ?: ""
                            )
                        }
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.EXPENSE) expenseColor else incomeColor
                )
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RowScope.CategoryChip(cat: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = cat, fontSize = 12.sp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
