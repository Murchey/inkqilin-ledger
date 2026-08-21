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

// ═══════════════════════════════ Screen state enums & helpers ═══════════════════════════════

enum class CycleFilter { ALL, DUE_SOON, OVERDUE }

fun formatAmount(amount: Double): String = String.format(Locale.CHINA, "%.2f", amount)

fun calculateProgress(bill: CycleBill, now: Long): Float {
    if (bill.currentCycleEnd <= bill.currentCycleStart) return 0f
    val clamped = now.coerceIn(bill.currentCycleStart, bill.currentCycleEnd)
    return ((clamped - bill.currentCycleStart).toDouble() / (bill.currentCycleEnd - bill.currentCycleStart)).toFloat().coerceIn(0f, 1f)
}

fun formatDate(dateMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

fun formatTime(hour: Int, minute: Int): String {
    return String.format(Locale.CHINA, "%02d:%02d", hour, minute)
}

/** Returns the boundary after [periods] calendar-based billing cycles. */
fun cycleBoundary(startDate: Long, cycleType: CycleType, periods: Int = 1): Long {
    return Calendar.getInstance(TimeZone.getDefault()).run {
        timeInMillis = startDate
        when (cycleType) {
            CycleType.DAILY -> add(Calendar.DAY_OF_YEAR, periods)
            CycleType.WEEKLY -> add(Calendar.WEEK_OF_YEAR, periods)
            CycleType.MONTHLY -> add(Calendar.MONTH, periods)
            CycleType.YEARLY -> add(Calendar.YEAR, periods)
        }
        timeInMillis
    }
}

/** Returns updated start/end/nextTrigger for a bill based on current time */
fun computeCycleRange(bill: CycleBill, now: Long): Triple<Long, Long, Long> {
    var periods = 0
    var start = bill.startDate
    var end = cycleBoundary(bill.startDate, bill.cycleType, periods + 1)
    while (end <= now) {
        periods++
        start = end
        end = cycleBoundary(bill.startDate, bill.cycleType, periods + 1)
    }
    return Triple(start, end, end)
}

// ═══════════════════════════════ Dao provider via singleton ═══════════════════════════════

object CycleBillDaoProvider {
    @Volatile private var dao: CycleBillDao? = null
    @Volatile private var nLogDao: NotificationLogDao? = null
    fun init(database: AppDatabase) {
        if (dao == null) dao = database.cycleBillDao()
        if (nLogDao == null) nLogDao = database.notificationLogDao()
    }
    fun getCycleBillDao(): CycleBillDao = dao ?: throw IllegalStateException("Call init first")
    fun getNotificationLogDao(): NotificationLogDao = nLogDao ?: throw IllegalStateException("Call init first")
}

// ═══════════════════════════════ Main Screen ═══════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleBillScreen(
    onBack: () -> Unit, 
    onNavigateAddBill: () -> Unit, 
    onNavigateEditBill: (Long) -> Unit,
    onNavigateRecycleBin: () -> Unit,
    onCreateTransaction: (CycleBill) -> Unit,
    onUpdateTopBar: ((String?, (() -> Unit)?) -> Unit)? = null,
    context: Context = LocalContext.current
) {
    // 直接从数据库获取 DAO
    val db = AppDatabase.getDatabase(context)
    val cycleBillDao = remember { db.cycleBillDao() }
    val transactionDao = remember { db.transactionDao() }
    val allBills by cycleBillDao.getAllCycleBills().collectAsState(initial = emptyList())
    val recycledBills by cycleBillDao.getAllRecycledCycleBills().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf(CycleFilter.ALL) }
    var showSettings by remember { mutableStateOf(false) }
    var defaultAdvanceMinutes by remember { mutableIntStateOf(60) }
    var globalEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Set top bar title and actions
    DisposableEffect(Unit) {
        onUpdateTopBar?.invoke("周期账单", onBack)
        onDispose {
            onUpdateTopBar?.invoke(null, null)
        }
    }

    // Remove infinite loop - lifecycle updates handled by Worker/CycleBillWorker

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateAddBill,
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab bar
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CycleFilter.values().forEach { filter ->
                    BouncyTabItem(
                        text = when (filter) {
                            CycleFilter.ALL -> "全部"
                            CycleFilter.DUE_SOON -> "即将到期"
                            CycleFilter.OVERDUE -> "已过期"
                        },
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }
            
            AnimatedContent(
                targetState = selectedFilter,
                modifier = Modifier.weight(1f),
                label = "filterChange"
            ) { filter ->
                val filtered = when (filter) {
                    CycleFilter.ALL -> allBills
                    CycleFilter.DUE_SOON -> allBills.filter { it.enabled && it.nextTriggerDate in System.currentTimeMillis()..(System.currentTimeMillis() + 7 * 86_400_000L) }
                    CycleFilter.OVERDUE -> allBills.filter { it.overdue }
                }
                BillListView(filtered.sortedBy { it.nextTriggerDate }, onGenerate = onCreateTransaction, onToggle = { bill ->
                    scope.launch {
                        val updated = bill.copy(enabled = !bill.enabled)
                        cycleBillDao.updateCycleBill(updated)
                        if (updated.enabled && updated.reminderEnabled) {
                            NotificationHelper.scheduleCycleBillReminder(
                                context, updated.id, updated.name, updated.amount, "支出",
                                updated.nextTriggerDate - updated.advanceMinutes * 60_000L,
                                updated.advanceMinutes
                            )
                        } else {
                            NotificationHelper.cancelCycleBillReminder(context, updated.id)
                        }
                    }
                }, onEdit = { billId ->
                    onNavigateEditBill(billId)
                }, onRecycle = { bill ->
                    scope.launch {
                        try {
                            val recycled = RecycledCycleBill(
                                originalId = bill.id, recycleTime = System.currentTimeMillis(),
                                name = bill.name, type = bill.type, amount = bill.amount,
                                category = bill.category, currency = bill.currency,
                                cycleType = bill.cycleType, startDate = bill.startDate,
                                enabled = bill.enabled, reminderEnabled = bill.reminderEnabled,
                                advanceMinutes = bill.advanceMinutes, generationMode = bill.generationMode,
                                note = bill.note, colorHex = bill.colorHex
                            )
                            cycleBillDao.insertRecycledCycleBill(recycled)
                            cycleBillDao.deleteCycleBill(bill)
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }
}

// ═══════════════════════════════ Edit Screen ═══════════════════════════════

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
    val db = remember { AppDatabase.getDatabase(context) }
    val cycleBillDao = remember { db.cycleBillDao() }
    val categories by db.categoryDao().getAllCategories().collectAsState(initial = emptyList())
    
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
            value = formatDate(startDateMillis),
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
            value = formatTime(startHour, startMinute),
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
                val firstCycleEnd = cycleBoundary(combinedDateTimeMillis, cycleType)
                
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

@Composable
private fun TimeWheel(values: List<Int?>, listState: LazyListState) {
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

// ═══════════════════════════════ BouncyTabItem ═══════════════════════════════

@Composable
fun BouncyTabItem(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "tabScale"
    )

    Surface(
        modifier = modifier.clickable(onClick = onClick).scale(scale),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════ Bill List View ═══════════════════════════════

@Composable
fun EmptyCycleBillState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DateRange, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text("还没有周期账单", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("点击右下角 + 号添加你的第一个周期账单", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text("自动记录周期性支出，轻松管理账单", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun BillListView(
    bills: List<CycleBill>, 
    onGenerate: (CycleBill) -> Unit, 
    onToggle: (CycleBill) -> Unit,
    onEdit: (Long) -> Unit,
    onRecycle: (CycleBill) -> Unit
) {
    if (bills.isEmpty()) {
        EmptyCycleBillState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(bills, key = { it.id }) { bill ->
            CycleBillCard(
                bill, 
                onGenerate = { onGenerate(bill) }, 
                onToggle = { onToggle(bill) },
                onEdit = { onEdit(bill.id) },
                onRecycle = { onRecycle(bill) }
            )
        }
    }
}

@Composable
fun CycleBillCard(
    bill: CycleBill, 
    onGenerate: () -> Unit, 
    onToggle: () -> Unit, 
    onEdit: () -> Unit, 
    onRecycle: () -> Unit
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val progress = calculateProgress(bill, now)
    val isOverdue = bill.overdue || now > bill.currentCycleEnd && (bill.lastGeneratedDate == null || bill.lastGeneratedDate!! < bill.currentCycleStart)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                                        else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Name + Amount + Cycle Type
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = if (bill.type == TransactionType.EXPENSE) Icons.Default.KeyboardArrowDown
                                   else Icons.Default.KeyboardArrowUp
                        Icon(icon, contentDescription = null, tint = if (bill.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error
                                                                     else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(bill.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text(formatAmount(bill.amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            
            // Row 2: Category + Cycle Type
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(bill.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(when (bill.cycleType) {
                    CycleType.DAILY -> "每日"
                    CycleType.WEEKLY -> "每周"
                    CycleType.MONTHLY -> "每月"
                    CycleType.YEARLY -> "每年"
                }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(Modifier.height(8.dp))

            // Progress bar
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val progressColor = when {
                isOverdue -> MaterialTheme.colorScheme.error
                progress > 0.9f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                drawRect(trackColor)
                drawRect(progressColor.copy(alpha = 0.7f), size = androidx.compose.ui.geometry.Size(size.width * progress, size.height))
            }
            Spacer(Modifier.height(4.dp))

            // Progress + Next trigger
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${String.format("%.0f%%", progress * 100)}", style = MaterialTheme.typography.labelSmall,
                     color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                if (isOverdue) {
                    Button(
                        onClick = onGenerate,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("逾期 - 点击生成", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Text("下次: ${formatDate(bill.nextTriggerDate)}", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Action buttons
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("编辑", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onGenerate,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("生成", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onToggle,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (bill.enabled) "暂停" else "恢复", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onRecycle, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "移入回收站", 
                         modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ═══════════════════════════════ Settings Panel ═══════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleBillSettingsPanel(
    globalEnabled: Boolean, onGlobalEnabledChanged: (Boolean) -> Unit,
    defaultAdvanceMinutes: Int, onDefaultAdvanceMinutesChanged: (Int) -> Unit,
    onViewRecycleBin: () -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp)) {
            Text("周期账单设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("总开关", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = globalEnabled, onCheckedChange = onGlobalEnabledChanged)
            }
            Spacer(Modifier.height(16.dp))

            Text("默认提前提醒时间 (新建时预填)", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            AdvanceTimePicker(selectedMinutes = defaultAdvanceMinutes, onSelect = onDefaultAdvanceMinutesChanged)
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        (context as? Activity)?.requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                        )
                    } else {
                        NotificationHelper.showTestNotification(context)
                        android.widget.Toast.makeText(context, "测试通知已发送", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("测试系统通知")
            }
            Spacer(Modifier.height(16.dp))

            Button(onClick = onViewRecycleBin, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("查看回收站")
            }
            Spacer(Modifier.height(16.dp))

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                   colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Text("完成")
            }
        }
    }
}

@Composable
private fun AdvanceTimePicker(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    val options = arrayOf(15, 30, 60, 120, 720, 1440)
    val optionTexts = arrayOf("15分钟", "30分钟", "1小时", "2小时", "半天", "一天")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { index, minutes ->
            val isSelected = selectedMinutes == minutes
            Surface(modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null) {
                Text(optionTexts[index],
                     modifier = Modifier.fillMaxWidth().clickable { onSelect(minutes) }.padding(vertical = 8.dp),
                     style = MaterialTheme.typography.labelSmall,
                     color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                     textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

// ═══════════════════════════════ Recycle Bin Screen ═══════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit, 
    onUpdateTopBar: ((String?, (() -> Unit)?) -> Unit)? = null,
    context: Context = LocalContext.current
) {
    val db = AppDatabase.getDatabase(context)
    val cycleBillDao by remember { derivedStateOf { db.cycleBillDao() } }
    val recycledBills by cycleBillDao.getAllRecycledCycleBills().collectAsState(initial = emptyList())
    var filterDays by remember { mutableIntStateOf(0) }
    var showConfirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Set top bar title
    DisposableEffect(Unit) {
        onUpdateTopBar?.invoke("回收站", onBack)
        onDispose {
            onUpdateTopBar?.invoke(null, null)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Pair(0, "全部"), Pair(7, "近7天")).forEach {(days, label) ->
                    val isActive = filterDays == days
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent) {
                        Text(label, modifier = Modifier.fillMaxWidth().clickable { filterDays = days }.padding(vertical = 6.dp),
                             style = MaterialTheme.typography.labelMedium,
                             color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                             textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }

            val filtered = if (filterDays == 0) recycledBills
                           else recycledBills.filter { System.currentTimeMillis() - it.recycleTime <= 7 * 86_400_000L }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("回收站为空", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.originalId }) { recycled ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(recycled.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("被回收于 ${formatDate(recycled.recycleTime)}", style = MaterialTheme.typography.labelSmall,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(onClick = {
                                    scope.launch {
                                        try {
                                            cycleBillDao.deleteRecycledCycleBill(recycled.originalId)
                                            val restored = CycleBill(
                                                id = recycled.originalId, name = recycled.name, type = recycled.type,
                                                amount = recycled.amount, category = recycled.category, cycleType = recycled.cycleType,
                                                startDate = recycled.startDate, enabled = recycled.enabled,
                                                reminderEnabled = recycled.reminderEnabled, advanceMinutes = recycled.advanceMinutes,
                                                generationMode = recycled.generationMode, note = recycled.note, colorHex = recycled.colorHex
                                            )
                                            cycleBillDao.insertCycleBill(restored)
                                        } catch (e: Exception) {}
                                    }
                                }, shape = RoundedCornerShape(8.dp),
                                       contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("恢复")
                                }
                            }
                        }
                    }
                }
            }
    }
    
    // Confirmation dialog for clearing recycle bin
    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("确认清空回收站？") },
            text = { Text("此操作将永久删除所有已回收的周期账单，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            recycledBills.forEach { cycleBillDao.deleteRecycledCycleBill(it.originalId) }
                        } catch (e: Exception) {}
                        showConfirmClear = false
                    }
                }) {
                    Text("确认清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmClear = false }) { Text("取消") } }
        )
    }
}
