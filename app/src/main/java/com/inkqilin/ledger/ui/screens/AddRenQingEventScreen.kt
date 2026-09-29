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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

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
internal fun AddRenQingEventForm(
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
                ?: tags.firstOrNull() ?: RenQingTag(name = "其他", icon = "gift", color = "#715CFF")
            else tags.firstOrNull() ?: RenQingTag(name = "其他", icon = "gift", color = "#715CFF")
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
    var newTagIcon by remember { mutableStateOf("gift") }
    var contactExpanded by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    // 精简表单：默认折叠可选区；编辑模式展开
    var showMoreOptions by remember { mutableStateOf(isEdit) }

    if (showAddContactDialog) {
        AddRenQingContactDialog(onDismiss = { showAddContactDialog = false }) { contact ->
            viewModel?.addContact(contact)
            selectedContact = contact
            showAddContactDialog = false
        }
    }

    if (showNewTagDialog) {
        AppleAlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = "快速添加标签",
            content = {
                Column {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("标签名称") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("图标", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RenQingIcons.tagIconOptions) { (key, vector) ->
                            FilterChip(
                                selected = newTagIcon == key,
                                onClick = { newTagIcon = key },
                                label = {
                                    Icon(vector, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
            },
            buttons = listOf(
                AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { showNewTagDialog = false },
                AppleDialogButton("添加", AppleDialogButtonStyle.DEFAULT) {
                    if (newTagName.isNotBlank()) {
                        val newTag = RenQingTag(name = newTagName.trim(), icon = newTagIcon)
                        selectedTag = newTag
                        newTagName = ""
                        newTagIcon = "gift"
                        showNewTagDialog = false
                    }
                }
            )
        )
    }

    val canSave = selectedContact != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0
    val sortedContacts = remember(contacts) { contacts.sortedBy { it.name } }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            if (isEdit) "编辑人情" else "记一笔人情",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "选人 → 收/送 → 金额即可保存；更多细节可展开。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = contactExpanded,
            onExpandedChange = { contactExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedContact?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("联系人 *") },
                placeholder = { Text("请选择联系人") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contactExpanded) }
            )
            ExposedDropdownMenu(
                expanded = contactExpanded,
                onDismissRequest = { contactExpanded = false }
            ) {
                sortedContacts.forEach { contact ->
                    DropdownMenuItem(
                        text = { Text("${contact.name} (${contact.relationship.label})") },
                        onClick = {
                            selectedContact = contact
                            contactExpanded = false
                        }
                    )
                }
                if (contacts.isNotEmpty()) {
                    HorizontalDivider()
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

        Spacer(modifier = Modifier.height(14.dp))
        Text("方向", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RenQingDirection.entries.forEach { d ->
                FilterChip(selected = direction == d, onClick = { direction = d }, label = { Text(d.label) })
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
            label = { Text("金额 *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Spacer(modifier = Modifier.height(16.dp))
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
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave,
            elevation = appButtonElevation()
        ) {
            Text(if (isEdit) "保存" else "保存这一笔")
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showMoreOptions = !showMoreOptions },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showMoreOptions) "收起更多选项" else "更多选项（标签/日期/备注…）")
        }

        AnimatedVisibility(visible = showMoreOptions) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text("标签", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags) { tag ->
                            FilterChip(
                                selected = selectedTag.id == tag.id,
                                onClick = { selectedTag = tag },
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
                    IconButton(onClick = { showNewTagDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "添加标签", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("事件类型", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RenQingEventType.entries) { t ->
                        FilterChip(
                            selected = eventType == t,
                            onClick = { eventType = t },
                            label = { Text(t.label) },
                            leadingIcon = {
                                Icon(
                                    RenQingIcons.eventTypeIcon(t),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

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
                    AppleDatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        state = datePickerState,
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { selectedDate = it }
                                showDatePicker = false
                            }) { Text("确定") }
                        },
                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
                    )
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("取消")
        }
        Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp) + 76.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRenQingEventDialog(
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
            ?: tags.firstOrNull() ?: RenQingTag(name = "其他", icon = "gift", color = "#715CFF"))
    }
    var direction by remember { mutableStateOf(event.direction) }
    var amount by remember { mutableStateOf(String.format("%.2f", event.amount)) }
    var giftDesc by remember { mutableStateOf(event.giftDescription) }
    var location by remember { mutableStateOf(event.location) }
    var note by remember { mutableStateOf(event.note) }
    var selectedDate by remember { mutableLongStateOf(event.date) }
    var contactExpanded by remember { mutableStateOf(false) }

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = "编辑事件",
        content = {
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
                    AppleDatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        state = datePickerState,
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { selectedDate = it }
                                showDatePicker = false
                            }) { Text("确定") }
                        },
                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
                    )
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
        buttons = listOf(
            AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { onDismiss() },
            AppleDialogButton("保存", AppleDialogButtonStyle.DEFAULT) {
                val amt = amount.toDoubleOrNull() ?: return@AppleDialogButton
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
            }
        )
    )
}
