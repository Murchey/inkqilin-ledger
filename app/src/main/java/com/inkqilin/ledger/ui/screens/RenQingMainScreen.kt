@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.appButtonElevation
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import com.inkqilin.ledger.ui.theme.floatingContentBottomInset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun RenQingMainScreen(
    viewModel: RenQingViewModel,
    onNavigateToContactDetail: (Long) -> Unit = {},
    onNavigateToMonthDetail: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToTagStats: (Int) -> Unit = {},
    onNavigateToContactAnalysis: (Int) -> Unit = {},
    onNavigateToRenQingStats: () -> Unit = {}
) {
    val allEvents by viewModel.allEvents.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var filterDirection by remember { mutableStateOf<RenQingDirection?>(null) }
    var filterTagId by remember { mutableStateOf<Long?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var selectedYear by rememberSaveable { mutableIntStateOf(currentYear) }

    val contactsListState = rememberLazyListState()

    val yearEvents = remember(allEvents, selectedYear) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(selectedYear, Calendar.DECEMBER, 31, 23, 59, 59)
        val end = cal.timeInMillis
        allEvents.filter { it.date in start..end }
    }
    val yearReceived = remember(yearEvents) {
        yearEvents.filter { it.direction == RenQingDirection.RECEIVED }.sumOf { it.amount }
    }
    val yearGiven = remember(yearEvents) {
        yearEvents.filter { it.direction == RenQingDirection.GIVEN }.sumOf { it.amount }
    }

    val filteredEvents = remember(allEvents, searchQuery, filterDirection, filterTagId) {
        allEvents.filter { event ->
            val matchQuery = searchQuery.isEmpty() ||
                event.contactName.contains(searchQuery, ignoreCase = true) ||
                event.note.contains(searchQuery, ignoreCase = true) ||
                event.location.contains(searchQuery, ignoreCase = true)
            val matchDirection = filterDirection == null || event.direction == filterDirection
            val matchTag = filterTagId == null || filterTagId == 0L || event.tagId == filterTagId
            matchQuery && matchDirection && matchTag
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            filterDirection = filterDirection,
            filterTagId = filterTagId,
            tags = allTags,
            onDismiss = { showFilterDialog = false },
            onApply = { direction, tagId ->
                filterDirection = direction
                filterTagId = tagId
                showFilterDialog = false
            }
        )
    }

    // 不用 Scaffold：外层 NavHost 已应用顶部内边距，再套 Scaffold 会重复加系统栏高度
    Column(modifier = Modifier.fillMaxSize()) {
            // 年度净额 Hero（事件 Tab 顶部），点击进入人情统计
            if (selectedTab == 0) {
                RenQingYearHero(
                    year = selectedYear,
                    received = yearReceived,
                    given = yearGiven,
                    onPrevYear = { selectedYear-- },
                    onNextYear = { selectedYear++ },
                    onOpenStats = onNavigateToRenQingStats
                )
            }

            // 搜索/筛选仅事件 Tab 需要
            if (selectedTab == 0) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("搜索联系人/地点/备注") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "筛选",
                            tint = if (filterDirection != null || filterTagId != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (filterDirection != null || filterTagId != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("筛选中: ", style = MaterialTheme.typography.bodySmall)
                        if (filterDirection != null) {
                            FilterChip(
                                selected = true,
                                onClick = { filterDirection = null },
                                label = { Text(filterDirection!!.label) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (filterTagId != null) {
                            val tagName = allTags.find { it.id == filterTagId }?.name ?: ""
                            FilterChip(
                                selected = true,
                                onClick = { filterTagId = null },
                                label = { Text(tagName) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
            }

            // 事件 / 联系人（统计并入 Hero 入口，不再单独 Tab）
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("事件") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("联系人") })
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (selectedTab) {
                    0 -> RenQingEventsList(filteredEvents, allTags, viewModel)
                    1 -> RenQingContactsList(allContacts, viewModel, onNavigateToContactDetail, contactsListState)
                }
            }
        }
}

@Composable
internal fun RenQingYearHero(
    year: Int,
    received: Double,
    given: Double,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onOpenStats: () -> Unit
) {
    val net = received - given
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 0.dp, bottom = 8.dp)
            .clickable { onOpenStats() },
        shape = com.inkqilin.ledger.ui.theme.Corners.Lg,
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevYear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一年")
                }
                Text(
                    "$year 年人情净额",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onNextYear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一年")
                }
            }
            Text(
                text = "${if (net >= 0) "+" else "-"}¥${String.format(Locale.US, "%.2f", kotlin.math.abs(net))}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "收到 ¥${String.format(Locale.US, "%.2f", received)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "送出 ¥${String.format(Locale.US, "%.2f", given)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "查看统计",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterDialog(
    filterDirection: RenQingDirection?,
    filterTagId: Long?,
    tags: List<RenQingTag>,
    onDismiss: () -> Unit,
    onApply: (RenQingDirection?, Long?) -> Unit
) {
    var direction by remember { mutableStateOf(filterDirection) }
    var tagId by remember { mutableStateOf(filterTagId) }

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = "筛选",
        content = {
            Column {
                Text("按方向", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = direction == null,
                        onClick = { direction = null },
                        label = { Text("全部") }
                    )
                    RenQingDirection.entries.forEach { d ->
                        FilterChip(
                            selected = direction == d,
                            onClick = { direction = if (direction == d) null else d },
                            label = { Text(d.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("按标签", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = tagId == null,
                            onClick = { tagId = null },
                            label = { Text("全部") }
                        )
                    }
                    items(tags) { tag ->
                        FilterChip(
                            selected = tagId == tag.id,
                            onClick = { tagId = if (tagId == tag.id) null else tag.id },
                            label = { Text(tag.name) },
                            leadingIcon = {
                                Icon(
                                    RenQingIcons.iconForTagIconValue(tag.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        },
        buttons = listOf(
            AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { onDismiss() },
            AppleDialogButton("确认", AppleDialogButtonStyle.DEFAULT) { onApply(direction, tagId) }
        )
    )
}

@Composable
internal fun RenQingEventsList(events: List<RenQingEvent>, tags: List<RenQingTag>, viewModel: RenQingViewModel) {
    if (events.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                RenQingIcons.eventTypeIcon(RenQingEventType.OTHER),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "还没有人情记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "点右上角「+」记一笔：选联系人、填金额即可。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        val grouped = events.groupBy { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date)) }
        val safeAreaBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), contentPadding = PaddingValues(bottom = safeAreaBottom + floatingContentBottomInset)) {
            grouped.forEach { (month, monthEvents) ->
                item {
                    Text(
                        text = month,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(monthEvents, key = { it.id }) { event ->
                    val tag = tags.find { it.id == event.tagId }
                    SwipeableRenQingEventCard(event, tag, viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/** 左滑编辑/删除（与主账本交互一致） */
@Composable
internal fun SwipeableRenQingEventCard(
    event: RenQingEvent,
    tag: RenQingTag?,
    viewModel: RenQingViewModel
) {
    val density = LocalDensity.current
    val menuWidth = 120.dp
    val menuWidthPx = with(density) { menuWidth.toPx() }
    var offsetX by remember(event.id) { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        val newOffset = (offsetX + delta).coerceIn(-menuWidthPx, 0f)
        offsetX = newOffset
    }
    val menuProgress = if (menuWidthPx <= 0f) 0f else (-offsetX / menuWidthPx).coerceIn(0f, 1f)
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditRenQingEventDialog(
            event,
            viewModel.allContacts.collectAsState().value,
            viewModel.allTags.collectAsState().value,
            onDismiss = { showEditDialog = false }
        ) { updated, _ ->
            viewModel.updateEvent(updated)
            showEditDialog = false
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除记录") },
            text = { Text("确定删除「${event.contactName}」的这条人情记录？此操作不可撤销。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteEvent(event)
                    showDeleteConfirm = false
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(com.inkqilin.ledger.ui.theme.Corners.Md)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        if (menuProgress > 0.02f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(menuWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                    .graphicsLayer { alpha = menuProgress },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    offsetX = 0f
                    showEditDialog = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    offsetX = 0f
                    showDeleteConfirm = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val target = if (offsetX < -menuWidthPx / 2) -menuWidthPx else 0f
                        animate(
                            initialValue = offsetX,
                            targetValue = target,
                            animationSpec = MotionSprings.interactive()
                        ) { value, _ -> offsetX = value }
                    }
                )
        ) {
            RenQingEventCard(event, tag, viewModel, showMenuButton = false)
        }
    }
}

@Composable
internal fun RenQingEventCard(
    event: RenQingEvent,
    tag: RenQingTag?,
    viewModel: RenQingViewModel,
    showMenuButton: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditRenQingEventDialog(event, viewModel.allContacts.collectAsState().value, viewModel.allTags.collectAsState().value, onDismiss = { showEditDialog = false }) { updated, _ ->
            viewModel.updateEvent(updated)
            showEditDialog = false
        }
    }

    val iconVector = if (tag != null) {
        RenQingIcons.iconForTagIconValue(tag.icon)
    } else {
        RenQingIcons.eventTypeIcon(event.eventType)
    }
    val tagColor = try { Color(android.graphics.Color.parseColor(tag?.color ?: "#715CFF")) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    val isGiven = event.direction == RenQingDirection.GIVEN

    Card(modifier = Modifier.fillMaxWidth(), shape = com.inkqilin.ledger.ui.theme.Corners.Md, elevation = CardDefaults.cardElevation(0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tagColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconVector,
                    contentDescription = null,
                    tint = tagColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.contactName, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isGiven) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            event.direction.label,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGiven) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    listOfNotNull(
                        event.eventType.label.takeIf { it.isNotBlank() },
                        SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(event.date))
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val extraLine = listOf(
                    event.giftDescription,
                    event.location,
                    event.note
                ).filter { it.isNotBlank() }.joinToString(" · ")
                if (extraLine.isNotBlank()) {
                    Text(extraLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Text(
                "${if (isGiven) "-" else "+"}¥${String.format("%.2f", event.amount)}",
                color = if (isGiven) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (showMenuButton) {
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("编辑") }, onClick = { showMenu = false; showEditDialog = true }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text("删除") }, onClick = { showMenu = false; viewModel.deleteEvent(event) }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }
        }
    }
}
