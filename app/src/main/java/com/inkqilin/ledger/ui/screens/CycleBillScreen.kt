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
fun CycleBillScreen(
    onBack: () -> Unit, 
    onNavigateAddBill: () -> Unit, 
    onNavigateEditBill: (Long) -> Unit,
    onNavigateRecycleBin: () -> Unit,
    onCreateTransaction: (CycleBill) -> Unit,
    onUpdateTopBar: ((String?, (() -> Unit)?) -> Unit)? = null,
    context: Context = LocalContext.current
) {
    val repos = remember { com.inkqilin.ledger.data.repository.LedgerRepositories.get(context) }
    val cycleBillDao = remember { repos.cycleBills }
    val transactionDao = remember { repos.transactions }
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
