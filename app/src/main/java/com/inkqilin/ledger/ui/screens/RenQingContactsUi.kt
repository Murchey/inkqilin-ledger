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
internal fun RenQingContactsList(
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
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                Icons.Filled.People,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
                Spacer(Modifier.height(12.dp))
                Text(
                    "还没有联系人",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "先添加常来往的亲友，记一笔时选人更快。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showAddDialog = true }) { Text("添加联系人") }
            }
        } else {
            val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), contentPadding = PaddingValues(bottom = navBarBottomPadding + 76.dp)) {
                items(contacts, key = { it.id }) { contact ->
                    ContactCard(contact, viewModel, onNavigateToContactDetail)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun ContactCard(
    contact: RenQingContact,
    viewModel: RenQingViewModel,
    onNavigateToContactDetail: (Long) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val relationshipColor = when (contact.relationship) {
        RelationshipType.RELATIVE -> Color(0xFFFF2D55)
        RelationshipType.FRIEND -> Color(0xFF007AFF)
        RelationshipType.COLLEAGUE -> Color(0xFFFF9F0A)
        RelationshipType.OTHER -> Color(0xFF8E8E93)
    }

    if (showEditDialog) {
        AddRenQingContactDialog(editContact = contact, onDismiss = { showEditDialog = false }) { updated ->
            viewModel.updateContact(updated)
            showEditDialog = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigateToContactDetail(contact.id) },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
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
internal fun AddRenQingContactDialog(
    editContact: RenQingContact? = null,
    onDismiss: () -> Unit,
    onConfirm: (RenQingContact) -> Unit
) {
    var name by remember { mutableStateOf(editContact?.name ?: "") }
    var relationship by remember { mutableStateOf(editContact?.relationship ?: RelationshipType.RELATIVE) }
    var phone by remember { mutableStateOf(editContact?.phone ?: "") }
    var birthday by remember { mutableLongStateOf(editContact?.birthday ?: 0L) }
    var note by remember { mutableStateOf(editContact?.note ?: "") }

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = if (editContact != null) "编辑联系人" else "添加联系人",
        content = {
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
        buttons = listOf(
            AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { onDismiss() },
            AppleDialogButton(if (editContact != null) "保存" else "添加", AppleDialogButtonStyle.DEFAULT) {
                if (name.isNotBlank()) {
                    onConfirm(RenQingContact(id = editContact?.id ?: 0, name = name.trim(), relationship = relationship, phone = phone.trim(), birthday = if (birthday > 0) birthday else null, note = note.trim()))
                }
            }
        )
    )
}
