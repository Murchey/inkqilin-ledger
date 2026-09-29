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
            AppleLoadingIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        when {
            !dataLoaded -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppleLoadingIndicator()
                }
            }
            contact != null -> {
                val relColor = when (contact.relationship) {
                    RelationshipType.RELATIVE -> Color(0xFFFF2D55)
                    RelationshipType.FRIEND -> Color(0xFF007AFF)
                    RelationshipType.COLLEAGUE -> Color(0xFFFF9F0A)
                    RelationshipType.OTHER -> Color(0xFF8E8E93)
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
            AppleLoadingIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("${year}年按标签统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (yearEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
                                    Icon(
                                        RenQingIcons.iconForTagIconValue(tag.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
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
                        Row(
                            modifier = Modifier.width(96.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                RenQingIcons.iconForTagIconValue(tag.icon),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                tag.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
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
            AppleLoadingIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("${year}年关系分析", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (contactStats.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
        AppleAlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = "删除联系人",
            message = "确定要删除联系人「${contact.name}」吗？该联系人相关的人情记录不会被删除。",
            buttons = listOf(
                AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { showDeleteConfirm = null },
                AppleDialogButton("删除", AppleDialogButtonStyle.DESTRUCTIVE) {
                    viewModel.deleteContact(contact)
                    showDeleteConfirm = null
                }
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共 ${allContacts.size} 位联系人", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { showAddDialog = true },
                elevation = appButtonElevation()
            ) {
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
                        RelationshipType.RELATIVE -> Color(0xFFFF2D55)
                        RelationshipType.FRIEND -> Color(0xFF007AFF)
                        RelationshipType.COLLEAGUE -> Color(0xFFFF9F0A)
                        RelationshipType.OTHER -> Color(0xFF8E8E93)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
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
                                        shape = RoundedCornerShape(6.dp),
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
