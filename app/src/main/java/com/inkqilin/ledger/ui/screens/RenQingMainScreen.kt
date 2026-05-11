package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenQingMainScreen(
    viewModel: RenQingViewModel,
    onNavigateToContactDetail: (Long) -> Unit = {},
    onNavigateToMonthDetail: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToAddEvent: () -> Unit = {},
    onNavigateToTagStats: (Int) -> Unit = {},
    onNavigateToContactAnalysis: (Int) -> Unit = {}
) {
    val allEvents by viewModel.allEvents.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var filterDirection by remember { mutableStateOf<RenQingDirection?>(null) }
    var filterTagId by remember { mutableStateOf<Long?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val contactsListState = rememberLazyListState()

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

    Scaffold(
        floatingActionButton = {
            if (selectedTab != 2) {
                FloatingActionButton(onClick = onNavigateToAddEvent) {
                    Icon(Icons.Default.Add, contentDescription = "添加事件")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                        singleLine = true
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

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("事件") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("联系人") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("统计") })
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (selectedTab) {
                    0 -> RenQingEventsList(filteredEvents, allTags, viewModel)
                    1 -> RenQingContactsList(allContacts, viewModel, onNavigateToContactDetail, contactsListState)
                    2 -> RenQingStatsScreen(viewModel, allTags, onNavigateToMonthDetail, onNavigateToTagStats, onNavigateToContactAnalysis)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    filterDirection: RenQingDirection?,
    filterTagId: Long?,
    tags: List<RenQingTag>,
    onDismiss: () -> Unit,
    onApply: (RenQingDirection?, Long?) -> Unit
) {
    var direction by remember { mutableStateOf(filterDirection) }
    var tagId by remember { mutableStateOf(filterTagId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选") },
        text = {
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
                            label = { Text("${tag.icon} ${tag.name}") }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(direction, tagId) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RenQingEventsList(events: List<RenQingEvent>, tags: List<RenQingTag>, viewModel: RenQingViewModel) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无事件记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val grouped = events.groupBy { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date)) }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
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
                    RenQingEventCard(event, tag, viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RenQingEventCard(event: RenQingEvent, tag: RenQingTag?, viewModel: RenQingViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditRenQingEventDialog(event, viewModel.allContacts.collectAsState().value, viewModel.allTags.collectAsState().value, onDismiss = { showEditDialog = false }) { updated, _ ->
            viewModel.updateEvent(updated)
            showEditDialog = false
        }
    }

    val icon = tag?.icon ?: event.eventType.icon
    val tagColor = try { Color(android.graphics.Color.parseColor(tag?.color ?: "#715CFF")) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    val isGiven = event.direction == RenQingDirection.GIVEN

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(tagColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.contactName, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
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
                    "${event.eventType.label}${if (event.tagName.isNotBlank() && event.tagName != event.eventType.label) " · ${event.tagName}" else ""} · ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(event.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.note.isNotBlank()) {
                    Text(event.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "${if (isGiven) "-" else "+"}¥${String.format("%.2f", event.amount)}",
                color = if (isGiven) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRenQingEventScreen(
    viewModel: RenQingViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val contacts by viewModel.allContacts.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    AddRenQingEventForm(
        contacts = contacts,
        tags = tags,
        viewModel = viewModel,
        onBack = onBack,
        onSaved = onSaved
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRenQingEventForm(
    contacts: List<RenQingContact>,
    tags: List<RenQingTag>,
    viewModel: RenQingViewModel?,
    isEdit: Boolean = false,
    initialEvent: RenQingEvent? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onSaveEvent: ((RenQingEvent, Boolean) -> Unit)? = null
) {
    var selectedContact by remember {
        mutableStateOf(contacts.find { it.id == initialEvent?.contactId })
    }
    var eventType by remember { mutableStateOf(initialEvent?.eventType ?: RenQingEventType.OTHER) }
    var selectedTag by remember {
        mutableStateOf(
            if (initialEvent != null) tags.find { it.id == initialEvent.tagId }
                ?: tags.firstOrNull() ?: RenQingTag(name = "其他", icon = "\uD83C\uDF81", color = "#715CFF")
            else tags.firstOrNull() ?: RenQingTag(name = "其他", icon = "\uD83C\uDF81", color = "#715CFF")
        )
    }
    var direction by remember { mutableStateOf(initialEvent?.direction ?: RenQingDirection.GIVEN) }
    var amount by remember { mutableStateOf(initialEvent?.amount?.let { String.format("%.2f", it) } ?: "") }
    var giftDesc by remember { mutableStateOf(initialEvent?.giftDescription ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var note by remember { mutableStateOf(initialEvent?.note ?: "") }
    var selectedDate by remember { mutableLongStateOf(initialEvent?.date ?: System.currentTimeMillis()) }
    var syncToTransaction by remember { mutableStateOf(true) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var newTagIcon by remember { mutableStateOf("\uD83C\uDF81") }
    var contactExpanded by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    if (showAddContactDialog) {
        AddRenQingContactDialog(onDismiss = { showAddContactDialog = false }) { contact ->
            viewModel?.addContact(contact)
            selectedContact = contact
            showAddContactDialog = false
        }
    }

    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text("快速添加标签") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("标签名称") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTagIcon,
                        onValueChange = { newTagIcon = it },
                        label = { Text("图标 (emoji)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagName.isNotBlank()) {
                        val newTag = RenQingTag(name = newTagName.trim(), icon = newTagIcon)
                        selectedTag = newTag
                        newTagName = ""
                        newTagIcon = "\uD83C\uDF81"
                        showNewTagDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showNewTagDialog = false }) { Text("取消") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ExposedDropdownMenuBox(
            expanded = contactExpanded,
            onExpandedChange = { contactExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedContact?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("联系人") },
                placeholder = { Text("请选择联系人") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contactExpanded) }
            )
            ExposedDropdownMenu(
                expanded = contactExpanded,
                onDismissRequest = { contactExpanded = false }
            ) {
                contacts.forEach { contact ->
                    DropdownMenuItem(
                        text = { Text("${contact.name} (${contact.relationship.label})") },
                        onClick = {
                            selectedContact = contact
                            contactExpanded = false
                        }
                    )
                }
                if (contacts.isNotEmpty()) {
                    Divider()
                }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("新建联系人", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        contactExpanded = false
                        showAddContactDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("标签", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    FilterChip(
                        selected = selectedTag.id == tag.id,
                        onClick = { selectedTag = tag },
                        label = { Text("${tag.icon} ${tag.name}") }
                    )
                }
            }
            IconButton(onClick = { showNewTagDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "添加标签", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("方向", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RenQingDirection.entries.forEach { d ->
                FilterChip(selected = direction == d, onClick = { direction = d }, label = { Text(d.label) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
            label = { Text("金额") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = giftDesc,
            onValueChange = { giftDesc = it },
            label = { Text("礼物描述（可选）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("地点（可选）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
        var showDatePicker by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("日期") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, contentDescription = "选择日期") }
        }
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate = it }
                        showDatePicker = false
                    }) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
            ) { DatePicker(state = datePickerState) }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("备注（可选）") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        if (!isEdit) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = syncToTransaction, onCheckedChange = { syncToTransaction = it })
                Text("同步添加到首页账单", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("取消")
            }
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    val cid = selectedContact?.id ?: 0
                    val cname = selectedContact?.name ?: ""
                    val event = RenQingEvent(
                        id = initialEvent?.id ?: 0,
                        contactId = cid,
                        contactName = cname,
                        eventType = eventType,
                        tagId = selectedTag.id,
                        tagName = selectedTag.name,
                        direction = direction,
                        amount = amt,
                        giftDescription = giftDesc,
                        date = selectedDate,
                        location = location,
                        note = note,
                        photoUri = initialEvent?.photoUri
                    )
                    if (onSaveEvent != null) {
                        onSaveEvent(event, syncToTransaction)
                    } else {
                        viewModel?.addEvent(event, syncToTransaction)
                    }
                    onSaved()
                },
                modifier = Modifier.weight(1f),
                enabled = amount.toDoubleOrNull() != null
            ) {
                Text(if (isEdit) "保存" else "添加")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRenQingEventDialog(
    event: RenQingEvent,
    contacts: List<RenQingContact>,
    tags: List<RenQingTag>,
    onDismiss: () -> Unit,
    onConfirm: (RenQingEvent, Boolean) -> Unit
) {
    var selectedContact by remember { mutableStateOf(contacts.find { it.id == event.contactId }) }
    var eventType by remember { mutableStateOf(event.eventType) }
    var selectedTag by remember {
        mutableStateOf(tags.find { it.id == event.tagId }
            ?: tags.firstOrNull() ?: RenQingTag(name = "其他", icon = "\uD83C\uDF81", color = "#715CFF"))
    }
    var direction by remember { mutableStateOf(event.direction) }
    var amount by remember { mutableStateOf(String.format("%.2f", event.amount)) }
    var giftDesc by remember { mutableStateOf(event.giftDescription) }
    var location by remember { mutableStateOf(event.location) }
    var note by remember { mutableStateOf(event.note) }
    var selectedDate by remember { mutableLongStateOf(event.date) }
    var contactExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑事件") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(
                    expanded = contactExpanded,
                    onExpandedChange = { contactExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedContact?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("联系人") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contactExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = contactExpanded,
                        onDismissRequest = { contactExpanded = false }
                    ) {
                        contacts.forEach { contact ->
                            DropdownMenuItem(
                                text = { Text("${contact.name} (${contact.relationship.label})") },
                                onClick = {
                                    selectedContact = contact
                                    contactExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("标签", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags) { tag ->
                        FilterChip(
                            selected = selectedTag.id == tag.id,
                            onClick = { selectedTag = tag },
                            label = { Text("${tag.icon} ${tag.name}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("方向", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RenQingDirection.entries.forEach { d ->
                        FilterChip(selected = direction == d, onClick = { direction = d }, label = { Text(d.label) })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = giftDesc,
                    onValueChange = { giftDesc = it },
                    label = { Text("礼物描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                var showDatePicker by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("日期") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, contentDescription = "选择日期") }
                }
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { selectedDate = it }
                                showDatePicker = false
                            }) { Text("确定") }
                        },
                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
                    ) { DatePicker(state = datePickerState) }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@TextButton
                    val cid = selectedContact?.id ?: 0
                    val cname = selectedContact?.name ?: ""
                    val updated = event.copy(
                        contactId = cid,
                        contactName = cname,
                        eventType = eventType,
                        tagId = selectedTag.id,
                        tagName = selectedTag.name,
                        direction = direction,
                        amount = amt,
                        giftDescription = giftDesc,
                        date = selectedDate,
                        location = location,
                        note = note
                    )
                    onConfirm(updated, false)
                },
                enabled = amount.toDoubleOrNull() != null
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RenQingContactsList(
    contacts: List<RenQingContact>,
    viewModel: RenQingViewModel,
    onNavigateToContactDetail: (Long) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddRenQingContactDialog(onDismiss = { showAddDialog = false }) { contact ->
            viewModel.addContact(contact)
            showAddDialog = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加联系人")
            }
        }
        if (contacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无联系人", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(contacts, key = { it.id }) { contact ->
                    ContactCard(contact, viewModel, onNavigateToContactDetail)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: RenQingContact,
    viewModel: RenQingViewModel,
    onNavigateToContactDetail: (Long) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val relationshipColor = when (contact.relationship) {
        RelationshipType.RELATIVE -> Color(0xFFE91E63)
        RelationshipType.FRIEND -> Color(0xFF2196F3)
        RelationshipType.COLLEAGUE -> Color(0xFFFF9800)
        RelationshipType.OTHER -> Color(0xFF9E9E9E)
    }

    if (showEditDialog) {
        AddRenQingContactDialog(editContact = contact, onDismiss = { showEditDialog = false }) { updated ->
            viewModel.updateContact(updated)
            showEditDialog = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigateToContactDetail(contact.id) },
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(relationshipColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(contact.name.take(1), fontWeight = FontWeight.Bold, color = relationshipColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.Medium)
                Text(
                    "${contact.relationship.label}${if (contact.phone.isNotBlank()) " · ${contact.phone}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { showMenu = false; showEditDialog = true }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { showMenu = false; viewModel.deleteContact(contact) }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRenQingContactDialog(
    editContact: RenQingContact? = null,
    onDismiss: () -> Unit,
    onConfirm: (RenQingContact) -> Unit
) {
    var name by remember { mutableStateOf(editContact?.name ?: "") }
    var relationship by remember { mutableStateOf(editContact?.relationship ?: RelationshipType.RELATIVE) }
    var phone by remember { mutableStateOf(editContact?.phone ?: "") }
    var birthday by remember { mutableLongStateOf(editContact?.birthday ?: 0L) }
    var note by remember { mutableStateOf(editContact?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editContact != null) "编辑联系人" else "添加联系人") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                Text("关系", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RelationshipType.entries.forEach { type ->
                        FilterChip(selected = relationship == type, onClick = { relationship = type }, label = { Text(type.label) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("联系方式（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(RenQingContact(id = editContact?.id ?: 0, name = name.trim(), relationship = relationship, phone = phone.trim(), birthday = if (birthday > 0) birthday else null, note = note.trim()))
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(if (editContact != null) "保存" else "添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun RenQingStatsScreen(
    viewModel: RenQingViewModel,
    tags: List<RenQingTag>,
    onNavigateToMonthDetail: (Int, Int) -> Unit,
    onNavigateToTagStats: (Int) -> Unit = {},
    onNavigateToContactAnalysis: (Int) -> Unit = {}
) {
    val dataLoaded by viewModel.dataLoaded.collectAsState()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    val allEvents by viewModel.allEvents.collectAsState()
    val yearRange = remember(selectedYear) { viewModel.getYearRange(selectedYear) }
    val yearGivenFlow = remember(yearRange) { viewModel.getTotalGiven(yearRange.first, yearRange.second) }
    val yearGiven by yearGivenFlow.collectAsState()
    val yearReceivedFlow = remember(yearRange) { viewModel.getTotalReceived(yearRange.first, yearRange.second) }
    val yearReceived by yearReceivedFlow.collectAsState()
    val yearEvents = remember(allEvents, selectedYear) { allEvents.filter { it.date in yearRange.first..yearRange.second } }

    if (!dataLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { selectedYear-- }) { Icon(Icons.Default.KeyboardArrowLeft, "上一年") }
            Text("$selectedYear 年", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { selectedYear++ }) { Icon(Icons.Default.KeyboardArrowRight, "下一年") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("${selectedYear}年来往总览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatColumn("支出", String.format("%.2f", yearGiven), MaterialTheme.colorScheme.error)
            StatColumn("收入", String.format("%.2f", yearReceived), MaterialTheme.colorScheme.primary)
            val balance = yearReceived - yearGiven
            StatColumn("结余", String.format("%.2f", balance), if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("按标签统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToTagStats(selectedYear) },
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val activeTags = tags.filter { tag -> yearEvents.any { it.tagId == tag.id } }
                    if (activeTags.isNotEmpty()) {
                        Text("${activeTags.size} 个标签", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            activeTags.take(3).joinToString(" ") { it.icon },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("月度趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val monthlyData = remember(yearEvents) {
            (0..11).map { month ->
                val mEvents = yearEvents.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    cal.get(Calendar.MONTH) == month
                }
                Triple(month, mEvents.filter { it.direction == RenQingDirection.GIVEN }.sumOf { it.amount }, mEvents.filter { it.direction == RenQingDirection.RECEIVED }.sumOf { it.amount })
            }
        }
        val maxAmount = remember(monthlyData) { monthlyData.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0) }
        Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            monthlyData.forEach { (month, given, received) ->
                Column(
                    modifier = Modifier.weight(1f).clickable { onNavigateToMonthDetail(selectedYear, month) }.height(150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.BottomCenter) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                            if (given > 0) {
                                Box(modifier = Modifier.width(8.dp).height(((given / maxAmount) * 100).dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                            }
                            if (received > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(modifier = Modifier.width(8.dp).height(((received / maxAmount) * 100).dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                            }
                        }
                    }
                    Text("${month + 1}月", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp)))
            Text(" 支出  ", style = MaterialTheme.typography.labelSmall)
            Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
            Text(" 收入", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("关系分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val allContacts by viewModel.allContacts.collectAsState()
        val contactStats = remember(yearEvents, allContacts) {
            yearEvents.groupBy { it.contactName }.map { (name, events) ->
                val contact = allContacts.find { it.name == name }
                Triple(name, contact?.relationship?.label ?: "未知", events.sumOf { it.amount })
            }.sortedByDescending { it.third }
        }
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToContactAnalysis(selectedYear) },
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (contactStats.isNotEmpty()) {
                        Text("${contactStats.size} 位联系人", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Top: ${contactStats.firstOrNull()?.first ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("¥$value", style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryStatRow(label: String, value: String, percentage: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Text("¥$value", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(8.dp))
        Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenQingMonthDetailScreen(viewModel: RenQingViewModel, year: Int, month: Int) {
    val dataLoaded by viewModel.dataLoaded.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val range = remember { viewModel.getMonthRange(year, month) }
    val monthEvents = remember(allEvents, year, month) { allEvents.filter { it.date in range.first..range.second } }
    val totalGiven = remember(monthEvents) { monthEvents.filter { it.direction == RenQingDirection.GIVEN }.sumOf { it.amount } }
    val totalReceived = remember(monthEvents) { monthEvents.filter { it.direction == RenQingDirection.RECEIVED }.sumOf { it.amount } }

    if (!dataLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("${year}年${month + 1}月详情", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatColumn("支出", String.format("%.2f", totalGiven), MaterialTheme.colorScheme.error)
            StatColumn("收入", String.format("%.2f", totalReceived), MaterialTheme.colorScheme.primary)
            StatColumn("笔数", "${monthEvents.size}", MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (monthEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("该月暂无事件记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(monthEvents, key = { it.id }) { event ->
                    val tag = allTags.find { it.id == event.tagId }
                    RenQingEventCard(event, tag, viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun RenQingContactDetailScreen(viewModel: RenQingViewModel, contactId: Long) {
    val dataLoaded by viewModel.dataLoaded.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val contactEventsFlow = remember(contactId) { viewModel.getEventsByContact(contactId) }
    val contactEvents by contactEventsFlow.collectAsState()

    val contact = if (dataLoaded) allContacts.find { it.id == contactId } else null

    val totalGiven = remember(contactEvents) { contactEvents.filter { it.direction == RenQingDirection.GIVEN }.sumOf { it.amount } }
    val totalReceived = remember(contactEvents) { contactEvents.filter { it.direction == RenQingDirection.RECEIVED }.sumOf { it.amount } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            !dataLoaded -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            contact != null -> {
                val relColor = when (contact.relationship) {
                    RelationshipType.RELATIVE -> Color(0xFFE91E63)
                    RelationshipType.FRIEND -> Color(0xFF2196F3)
                    RelationshipType.COLLEAGUE -> Color(0xFFFF9800)
                    RelationshipType.OTHER -> Color(0xFF9E9E9E)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(relColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(contact.name.take(1), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = relColor)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(contact.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${contact.relationship.label}${if (contact.phone.isNotBlank()) " · ${contact.phone}" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatColumn("支出", String.format("%.2f", totalGiven), MaterialTheme.colorScheme.error)
                    StatColumn("收入", String.format("%.2f", totalReceived), MaterialTheme.colorScheme.primary)
                    val balance = totalReceived - totalGiven
                    StatColumn("结余", String.format("%.2f", balance), if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    StatColumn("笔数", "${contactEvents.size}", MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("来往记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (contactEvents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("暂无来往记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                        items(contactEvents, key = { it.id }) { event ->
                            val tag = allTags.find { it.id == event.tagId }
                            RenQingEventCard(event, tag, viewModel)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("联系人不存在", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RenQingTagStatsScreen(viewModel: RenQingViewModel, year: Int) {
    val dataLoaded by viewModel.dataLoaded.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val yearRange = remember(year) { viewModel.getYearRange(year) }
    val yearEvents = remember(allEvents, year) { allEvents.filter { it.date in yearRange.first..yearRange.second } }

    if (!dataLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("${year}年按标签统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (yearEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    allTags.forEach { tag ->
                        val tagEvents = yearEvents.filter { it.tagId == tag.id }
                        if (tagEvents.isNotEmpty()) {
                            val tagTotal = tagEvents.sumOf { it.amount }
                            val tagGiven = tagEvents.filter { it.direction == RenQingDirection.GIVEN }.sumOf { it.amount }
                            val tagReceived = tagEvents.filter { it.direction == RenQingDirection.RECEIVED }.sumOf { it.amount }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tag.icon, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tag.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${tagEvents.size}笔 · 支出¥${String.format("%.2f", tagGiven)} 收入¥${String.format("%.2f", tagReceived)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "¥${String.format("%.2f", tagTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("标签分布", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val totalAmount = yearEvents.sumOf { it.amount }.coerceAtLeast(1.0)
            allTags.forEach { tag ->
                val tagEvents = yearEvents.filter { it.tagId == tag.id }
                if (tagEvents.isNotEmpty()) {
                    val tagTotal = tagEvents.sumOf { it.amount }
                    val ratio = (tagTotal / totalAmount).toFloat()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${tag.icon} ${tag.name}", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = ratio,
                            modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${(ratio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RenQingContactAnalysisScreen(viewModel: RenQingViewModel, year: Int) {
    val dataLoaded by viewModel.dataLoaded.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val yearRange = remember(year) { viewModel.getYearRange(year) }
    val yearEvents = remember(allEvents, year) { allEvents.filter { it.date in yearRange.first..yearRange.second } }

    val contactStats = remember(yearEvents, allContacts) {
        yearEvents.groupBy { it.contactName }.map { (name, events) ->
            val contact = allContacts.find { it.name == name }
            val given = events.filter { it.direction == RenQingDirection.GIVEN }.sumOf { it.amount }
            val received = events.filter { it.direction == RenQingDirection.RECEIVED }.sumOf { it.amount }
            Triple(name, contact?.relationship?.label ?: "未知", Triple(given, received, events.size))
        }.sortedByDescending { it.third.first + it.third.second }
    }

    if (!dataLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("${year}年关系分析", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (contactStats.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    contactStats.forEach { (name, rel, stats) ->
                        val (given, received, count) = stats
                        val balance = received - given
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Medium)
                                Text(
                                    "$rel · ${count}笔",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "支出¥${String.format("%.2f", given)} 收入¥${String.format("%.2f", received)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                if (balance >= 0) "+¥${String.format("%.2f", balance)}" else "-¥${String.format("%.2f", -balance)}",
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ContactManagementScreen(viewModel: RenQingViewModel) {
    val allContacts by viewModel.allContacts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<RenQingContact?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<RenQingContact?>(null) }

    if (showAddDialog) {
        AddRenQingContactDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { contact ->
                viewModel.addContact(contact)
                showAddDialog = false
            }
        )
    }

    editingContact?.let { contact ->
        AddRenQingContactDialog(
            editContact = contact,
            onDismiss = { editingContact = null },
            onConfirm = { updated ->
                viewModel.updateContact(updated)
                editingContact = null
            }
        )
    }

    showDeleteConfirm?.let { contact ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除联系人") },
            text = { Text("确定要删除联系人「${contact.name}」吗？该联系人相关的人情记录不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact(contact)
                    showDeleteConfirm = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共 ${allContacts.size} 位联系人", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加联系人")
            }
        }

        if (allContacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无联系人", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击上方按钮添加", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allContacts, key = { it.id }) { contact ->
                    val relColor = when (contact.relationship) {
                        RelationshipType.RELATIVE -> Color(0xFFE91E63)
                        RelationshipType.FRIEND -> Color(0xFF2196F3)
                        RelationshipType.COLLEAGUE -> Color(0xFFFF9800)
                        RelationshipType.OTHER -> Color(0xFF9E9E9E)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(relColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(contact.name.take(1), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = relColor)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = relColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            contact.relationship.label,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 12.sp,
                                            color = relColor
                                        )
                                    }
                                    if (contact.phone.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            IconButton(onClick = { editingContact = contact }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { showDeleteConfirm = contact }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun RenQingMainScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text("人情往来", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF715CFF))) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("本月人情 · 随礼", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("收到", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp); Text("¥2,000.00", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Column(horizontalAlignment = Alignment.End) { Text("给出", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp); Text("¥500.00", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("全部", "本月", "本年").forEach { FilterChip(selected = it == "本月", onClick = {}, label = { Text(it) }) }
                }
                val contacts = listOf("张三" to "朋友", "李四" to "同事", "王五" to "亲属")
                contacts.forEach { (name, rel) ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.5.dp)) {
                        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF2196F3).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Text(name.take(1), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3)) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(rel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("🎁 随礼 ¥200", fontSize = 12.sp, color = Color(0xFFF44336))
                        }
                    }
                }
            }
        }
    }
}
