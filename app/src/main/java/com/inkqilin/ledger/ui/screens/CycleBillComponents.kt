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

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val progress = CalculateProgress(bill, now)
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
                Text(FormatAmount(bill.amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    Text("下次: ${FormatDate(bill.nextTriggerDate)}", style = MaterialTheme.typography.labelSmall,
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
