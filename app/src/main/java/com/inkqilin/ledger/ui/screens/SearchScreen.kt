package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.SearchSummary
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 搜索日期筛选模式 */
private enum class SearchDateMode(val label: String) {
    ALL("全部"),
    SINGLE_DAY("单日"),
    WEEK("本周"),
    MONTH("本月"),
    YEAR("本年"),
    CUSTOM("自定义")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: TransactionViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    var dateMode by rememberSaveable { mutableStateOf(SearchDateMode.ALL.name) }
    var customStart by rememberSaveable { mutableStateOf(0L) }
    var customEnd by rememberSaveable { mutableStateOf(0L) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var submittedDateMode by rememberSaveable { mutableStateOf(SearchDateMode.ALL.name) }
    var submittedStart by rememberSaveable { mutableStateOf(0L) }
    var submittedEnd by rememberSaveable { mutableStateOf(0L) }

    val mode = remember(dateMode) { SearchDateMode.valueOf(dateMode) }
    val submittedMode = remember(submittedDateMode) { SearchDateMode.valueOf(submittedDateMode) }

    val submittedRange = remember(submittedMode, submittedStart, submittedEnd) {
        resolveDateRange(submittedMode, submittedStart, submittedEnd)
    }
    val hasSearched = submittedQuery.isNotBlank() || submittedMode != SearchDateMode.ALL
    val dateFilterActive = submittedMode != SearchDateMode.ALL
    val daySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val shortSdf = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }

    val resultsFlow: Flow<List<Transaction>> = remember(submittedQuery, submittedMode, submittedStart, submittedEnd) {
        val range = submittedRange
        if (!hasSearched) flowOf(emptyList())
        else viewModel.searchTransactionsFiltered(submittedQuery, range?.first, range?.second)
    }
    val summaryFlow: Flow<SearchSummary> = remember(submittedQuery, submittedMode, submittedStart, submittedEnd) {
        val range = submittedRange
        if (!hasSearched) flowOf(SearchSummary(0.0, 0.0))
        else viewModel.searchSummaryFiltered(submittedQuery, range?.first, range?.second)
    }

    val searchResults by resultsFlow.collectAsState(initial = emptyList())
    val summary by summaryFlow.collectAsState(initial = SearchSummary(0.0, 0.0))

    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val incomeColor = Color(android.graphics.Color.parseColor(incomeColorHex))
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))

    fun performSearch() {
        submittedQuery = query.trim()
        submittedDateMode = dateMode
        submittedStart = customStart
        submittedEnd = customEnd
        keyboardController?.hide()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasSearched) "结果 ${searchResults.size} 条" else "搜索交易",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasSearched) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "支 ¥${money(summary.expenseTotal)}",
                        color = expenseColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "收 ¥${money(summary.incomeTotal)}",
                        color = incomeColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索分类或备注...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (query.isNotBlank() || mode != SearchDateMode.ALL) {
                    IconButton(onClick = { performSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { performSearch() }),
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── 日期筛选：分段控件 ──
        SearchDateSegmented(
            selected = mode,
            onSelect = { m ->
                dateMode = m.name
                if (m == SearchDateMode.SINGLE_DAY && customStart <= 0) {
                    customStart = startOfDay(System.currentTimeMillis())
                    customEnd = customStart
                }
                if (m != SearchDateMode.CUSTOM && !(m == SearchDateMode.SINGLE_DAY && customStart <= 0)) {
                    performSearch()
                }
            }
        )

        // ── 单日：一次点选日期即可查询 ──
        if (mode == SearchDateMode.SINGLE_DAY) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("日期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (customStart > 0) daySdf.format(Date(customStart)) else "未选择",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (customStart > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showStartPicker = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { showStartPicker = true }) { Text("更换日期") }
                if (customStart > 0) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { performSearch() }) { Text("查询") }
                }
            }
        }

        // ── 自定义区间 ──
        if (mode == SearchDateMode.CUSTOM) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("起", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                DateTextCell(
                    value = if (customStart > 0) daySdf.format(Date(customStart)) else "—",
                    onClick = { showStartPicker = true }
                )
                Spacer(Modifier.width(10.dp))
                Text("止", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                DateTextCell(
                    value = if (customEnd > 0) daySdf.format(Date(customEnd)) else "—",
                    onClick = { showEndPicker = true }
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { performSearch() },
                    enabled = customStart > 0 && customEnd > 0 && customEnd >= customStart
                ) { Text("查询") }
            }
        }

        // ── 已提交的筛选条件 ──
        if (hasSearched && dateFilterActive) {
            submittedRange?.let { (s, e) ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (submittedMode) {
                        SearchDateMode.SINGLE_DAY -> "单日 ${daySdf.format(Date(s))}"
                        SearchDateMode.WEEK -> "本周 ${shortSdf.format(Date(s))}–${shortSdf.format(Date(e))}"
                        SearchDateMode.MONTH -> "本月 ${shortSdf.format(Date(s))}–${shortSdf.format(Date(e))}"
                        SearchDateMode.YEAR -> "本年 ${shortSdf.format(Date(s))}–${shortSdf.format(Date(e))}"
                        SearchDateMode.CUSTOM -> "${shortSdf.format(Date(s))}–${shortSdf.format(Date(e))}"
                        else -> ""
                    }.ifBlank { return@let },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            !hasSearched -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("输入关键词或选择日期范围后搜索", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            searchResults.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (dateFilterActive && submittedQuery.isBlank()) "该日期范围内没有账单"
                    else "没有找到相关账单",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults, key = { it.id }) { transaction ->
                    TransactionItem(transaction, viewModel)
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }

    if (showStartPicker) {
        val initial = if (customStart > 0) customStart else System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        AppleDatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            state = state,
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val day = startOfDay(ms)
                        customStart = day
                        if (mode == SearchDateMode.SINGLE_DAY) {
                            customEnd = day
                        } else if (customEnd in 1 until day) {
                            customEnd = endOfDay(day)
                        }
                    }
                    showStartPicker = false
                    if (mode == SearchDateMode.SINGLE_DAY || mode == SearchDateMode.CUSTOM) {
                        // 单日选完即查；自定义在起止都有效时也直接查
                        if (customStart > 0 && (mode == SearchDateMode.SINGLE_DAY || (customEnd > 0 && customEnd >= customStart))) {
                            performSearch()
                        }
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("取消") } }
        )
    }

    if (showEndPicker) {
        val initial = if (customEnd > 0) customEnd else if (customStart > 0) customStart else System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        AppleDatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            state = state,
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        customEnd = endOfDay(ms)
                        if (customStart in 1 until customEnd && customStart > customEnd) customStart = startOfDay(ms)
                    }
                    showEndPicker = false
                    if (customStart > 0 && customEnd >= customStart) performSearch()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } }
        )
    }
}

/** 日期筛选分段控件：单行、无 Chip 边框，选中态实心/文字加粗 */
@Composable
private fun SearchDateSegmented(
    selected: SearchDateMode,
    onSelect: (SearchDateMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SearchDateMode.entries.forEach { m ->
            val isSelected = m == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(m) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = m.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DateTextCell(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun endOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

private fun resolveDateRange(mode: SearchDateMode, start: Long, end: Long): Pair<Long, Long>? {
    return when (mode) {
        SearchDateMode.ALL -> null
        SearchDateMode.SINGLE_DAY -> {
            if (start > 0) startOfDay(start) to endOfDay(start)
            else null
        }
        SearchDateMode.WEEK -> {
            val c = Calendar.getInstance()
            c.firstDayOfWeek = Calendar.MONDAY
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val s = startOfDay(c.timeInMillis)
            c.add(Calendar.DAY_OF_YEAR, 6)
            s to endOfDay(c.timeInMillis)
        }
        SearchDateMode.MONTH -> {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, 1)
            val s = startOfDay(c.timeInMillis)
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
            s to endOfDay(c.timeInMillis)
        }
        SearchDateMode.YEAR -> {
            val c = Calendar.getInstance()
            c.set(Calendar.MONTH, Calendar.JANUARY)
            c.set(Calendar.DAY_OF_MONTH, 1)
            val s = startOfDay(c.timeInMillis)
            c.set(Calendar.MONTH, Calendar.DECEMBER)
            c.set(Calendar.DAY_OF_MONTH, 31)
            s to endOfDay(c.timeInMillis)
        }
        SearchDateMode.CUSTOM -> {
            if (start > 0 && end > 0 && end >= start) startOfDay(start) to endOfDay(end)
            else null
        }
    }
}

private fun money(value: Double): String = String.format(Locale.CHINA, "%,.2f", value)

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                OutlinedTextField(
                    value = "餐饮",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索分类或备注...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("输入关键词后点击搜索", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
