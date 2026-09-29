package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.util.NotificationHelper
import com.inkqilin.ledger.ui.screens.AppleDatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleBillEditScreen(
    onBack: () -> Unit, 
    onSave: (CycleBill) -> Unit, 
    editBillId: Long? = null, 
    onUpdateTopBar: ((String?, (() -> Unit)?) -> Unit)? = null,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val repos = remember { com.inkqilin.ledger.data.repository.LedgerRepositories.get(context) }
    val cycleBillDao = remember { repos.cycleBills }
    val categories by repos.categories.getAllCategories().collectAsState(initial = emptyList())
    
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var cycleType by remember { mutableStateOf(CycleType.MONTHLY) }
    var showCycleTypeDropdown by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("其他") }
    var categoryType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var startDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var advanceMinutes by remember { mutableIntStateOf(60) }
    var reminderEnabled by remember { mutableStateOf(true) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    
    // Set top bar title
    DisposableEffect(Unit) {
        onUpdateTopBar?.invoke(if (editBillId != null) "编辑周期账单" else "新建周期账单", onBack)
        onDispose {
            onUpdateTopBar?.invoke(null, null)
        }
    }

    // Load existing bill if editing
    LaunchedEffect(editBillId) {
        if (editBillId != null && editBillId > 0) {
            val existing = cycleBillDao.getCycleBillById(editBillId)
            if (existing != null) {
                name = existing.name
                amountStr = existing.amount.toString()
                cycleType = existing.cycleType
                category = existing.category
                categoryType = existing.type
                startDateMillis = existing.startDate
                // Extract hour and minute from startDate
                val cal = Calendar.getInstance().apply { timeInMillis = existing.startDate }
                startHour = cal.get(Calendar.HOUR_OF_DAY)
                startMinute = cal.get(Calendar.MINUTE)
                advanceMinutes = existing.advanceMinutes
                reminderEnabled = existing.reminderEnabled
            }
        }
    }

    // Form validation
    val isFormValid = name.isNotBlank() && amountStr.isNotEmpty() && amountStr.toDoubleOrNull() != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name input
        OutlinedTextField(
            value = name, 
            onValueChange = { name = it }, 
            label = { Text("账单名称") }, 
            placeholder = { Text("例如：房租、水电费、会员费") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Amount input
        OutlinedTextField(
            value = amountStr, 
            onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("金额") },
            placeholder = { Text("0.00") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Cycle Type selector
        Text("重复周期", style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = showCycleTypeDropdown,
            onExpandedChange = { showCycleTypeDropdown = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = when (cycleType) { 
                    CycleType.DAILY -> "每天"
                    CycleType.WEEKLY -> "每周" 
                    CycleType.MONTHLY -> "每月" 
                    CycleType.YEARLY -> "每年" 
                },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCycleTypeDropdown) }
            )
            ExposedDropdownMenu(
                expanded = showCycleTypeDropdown,
                onDismissRequest = { showCycleTypeDropdown = false },
                modifier = Modifier.exposedDropdownSize()
            ) {
                listOf(
                    CycleType.DAILY to "每天", CycleType.WEEKLY to "每周",
                    CycleType.MONTHLY to "每月", CycleType.YEARLY to "每年"
                ).forEach { (type, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { cycleType = type; showCycleTypeDropdown = false }
                    )
                }
            }
        }

        // Category selector
        Text("账单分类", style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = showCategoryDropdown,
            onExpandedChange = { showCategoryDropdown = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) }
            )
            ExposedDropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false },
                modifier = Modifier.exposedDropdownSize()
            ) {
                val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
                val incomeCategories = categories.filter { it.type == TransactionType.INCOME }
                if (expenseCategories.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("支出分类", fontWeight = FontWeight.Medium) },
                        onClick = {},
                        enabled = false
                    )
                }
                expenseCategories.forEach { item ->
                    DropdownMenuItem(
                        text = { Text("${item.icon} ${item.name}") },
                        onClick = {
                            category = item.name
                            categoryType = item.type
                            showCategoryDropdown = false
                        }
                    )
                }
                if (incomeCategories.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("收入分类", fontWeight = FontWeight.Medium) },
                        onClick = {},
                        enabled = false
                    )
                }
                incomeCategories.forEach { item ->
                    DropdownMenuItem(
                        text = { Text("${item.icon} ${item.name}") },
                        onClick = {
                            category = item.name
                            categoryType = item.type
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }

        // Date picker
        Text("起始日期", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = FormatDate(startDateMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("选择周期开始日期") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showDatePickerDialog = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                }
            }
        )

        if (showDatePickerDialog) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
            AppleDatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { startDateMillis = it }
                        showDatePickerDialog = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("取消")
                    }
                },
                state = datePickerState
            )
        }

        // Time picker
        Text("起始时间", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = FormatTime(startHour, startMinute),
            onValueChange = {},
            readOnly = true,
            label = { Text("选择周期开始时间") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showTimePickerDialog = true }) {
                    Text("选择", color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        if (showTimePickerDialog) {
            var tempHour by remember { mutableIntStateOf(startHour) }
            var tempMinute by remember { mutableIntStateOf(startMinute) }
            val hourValues = remember { listOf<Int?>(null) + (0..23).toList() + listOf(null) }
            val minuteValues = remember { listOf<Int?>(null) + (0..59).toList() + listOf(null) }
            val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = startHour)
            val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = startMinute)

            LaunchedEffect(hourListState.isScrollInProgress) {
                if (!hourListState.isScrollInProgress) {
                    val selectedIndex = (hourListState.firstVisibleItemIndex + 1).coerceIn(1, 24)
                    tempHour = hourValues[selectedIndex] ?: tempHour
                    hourListState.animateScrollToItem(selectedIndex - 1)
                }
            }
            LaunchedEffect(minuteListState.isScrollInProgress) {
                if (!minuteListState.isScrollInProgress) {
                    val selectedIndex = (minuteListState.firstVisibleItemIndex + 1).coerceIn(1, 60)
                    tempMinute = minuteValues[selectedIndex] ?: tempMinute
                    minuteListState.animateScrollToItem(selectedIndex - 1)
                }
            }

            AlertDialog(
                onDismissRequest = { showTimePickerDialog = false },
                title = { Text("选择起始时间") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("时", style = MaterialTheme.typography.bodySmall)
                                TimeWheel(values = hourValues, listState = hourListState)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("分", style = MaterialTheme.typography.bodySmall)
                                TimeWheel(values = minuteValues, listState = minuteListState)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("滑动滚轮，中央高亮项即为选中时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        startHour = tempHour
                        startMinute = tempMinute
                        showTimePickerDialog = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePickerDialog = false }) { Text("取消") }
                }
            )
        }

        Text("提前提醒", style = MaterialTheme.typography.bodyMedium)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = reminderEnabled,
                onCheckedChange = { enabled ->
                    reminderEnabled = enabled
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
                Text(if (reminderEnabled) "已开启" else "不提醒", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (reminderEnabled) {
                Spacer(Modifier.height(8.dp))
                AdvanceTimePicker(selectedMinutes = advanceMinutes, onSelect = { advanceMinutes = it })
            }
        }

        // Save button
        Button(
            onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                // Combine date and time into a single timestamp
                val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                cal.set(Calendar.HOUR_OF_DAY, startHour)
                cal.set(Calendar.MINUTE, startMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val combinedDateTimeMillis = cal.timeInMillis
                val firstCycleEnd = CycleBoundary(combinedDateTimeMillis, cycleType)
                
                val newBill = CycleBill(
                    id = editBillId ?: 0L,
                    name = name, 
                    type = categoryType,
                    amount = amt, 
                    category = category,
                    cycleType = cycleType, 
                    startDate = combinedDateTimeMillis,
                    reminderEnabled = reminderEnabled,
                    advanceMinutes = if (reminderEnabled) advanceMinutes else 0,
                    generationMode = GenerationMode.AUTO_BEFORE, 
                    note = "",
                    currency = "CNY",
                    enabled = true,
                    currentCycleStart = combinedDateTimeMillis,
                    currentCycleEnd = firstCycleEnd,
                    lastGeneratedDate = null,
                    nextTriggerDate = firstCycleEnd,
                    overdue = false
                )
                scope.launch {
                    try {
                        val savedBill = if (editBillId != null) {
                            val updated = newBill.copy(id = editBillId)
                            cycleBillDao.updateCycleBill(updated)
                            updated
                        } else {
                            val id = cycleBillDao.insertCycleBill(newBill)
                            newBill.copy(id = id)
                        }
                        if (savedBill.reminderEnabled) {
                            NotificationHelper.scheduleCycleBillReminder(
                                context, savedBill.id, savedBill.name, savedBill.amount,
                                if (savedBill.type == TransactionType.INCOME) "收入" else "支出",
                                savedBill.nextTriggerDate - savedBill.advanceMinutes * 60_000L,
                                savedBill.advanceMinutes
                            )
                        } else {
                            NotificationHelper.cancelCycleBillReminder(context, savedBill.id)
                        }
                        onSave(savedBill)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, 
            enabled = isFormValid, 
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("保存", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeWheel(values: List<Int?>, listState: LazyListState) {
    val centerIndex = (listState.firstVisibleItemIndex + 1).coerceIn(1, values.lastIndex - 1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(values.size) { index ->
                val value = values[index]
                Text(
                    text = value?.let { String.format(Locale.CHINA, "%02d", it) }.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = if (index == centerIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == centerIndex) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
        )
    }
}
