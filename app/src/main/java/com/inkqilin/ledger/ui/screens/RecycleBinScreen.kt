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
fun RecycleBinScreen(
    onBack: () -> Unit, 
    onUpdateTopBar: ((String?, (() -> Unit)?) -> Unit)? = null,
    context: Context = LocalContext.current
) {
    val repos = com.inkqilin.ledger.data.repository.LedgerRepositories.get(context)
    val cycleBillDao by remember { derivedStateOf { repos.cycleBills } }
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
                                    Text("被回收于 ${FormatDate(recycled.recycleTime)}", style = MaterialTheme.typography.labelSmall,
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
