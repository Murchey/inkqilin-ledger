@file:OptIn(ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.AssetFlowType
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.UserAsset
import com.inkqilin.ledger.data.UserAssetType
import com.inkqilin.ledger.ui.TransactionViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs



@Composable
internal fun AssetFlowScreen(
    asset: UserAsset,
    viewModel: TransactionViewModel
) {
    val flows by viewModel.getAssetFlows(asset.id)
        .collectAsState(initial = emptyList())
    var showAddFlowDialog by remember { mutableStateOf(false) }
    var editingFlow by remember { mutableStateOf<AssetFlow?>(null) }

    // 从 ViewModel 观察最新的资产数据，确保流转操作后价值实时更新
    val allAssets by viewModel.allUserAssets.collectAsState()
    val currencyList by viewModel.allAssets.collectAsState()
    val currentAsset = allAssets.find { it.id == asset.id } ?: asset
    val netChange = flows.sumOf { flow ->
        when (flow.flowType) {
            AssetFlowType.INCREASE -> flow.amount
            AssetFlowType.DECREASE -> -flow.amount
            AssetFlowType.REVALUATION -> flow.amount - (flows
                .filter { it.assetId == flow.assetId && it.date < flow.date }
                .maxByOrNull { it.date }
                ?.newValue ?: flow.newValue)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 资产信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currentAsset.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            currentAsset.type.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${currencySymbolOf(currentAsset.currency, currencyList)} ${amountFormat.format(currentAsset.currentValue)}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AssetSummaryCard(
                currentValue = currentAsset.currentValue,
                netChange = netChange,
                flowCount = flows.size,
                trendValues = flows.sortedBy { it.date }.map { it.newValue },
                symbol = currencySymbolOf(currentAsset.currency, currencyList)
            )

            if (flows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "暂无流转记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            "记录资产的每次价值变动",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 0.dp, bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(flows, key = { it.id }) { flow ->
                        FlowItem(
                            flow = flow,
                            symbol = currencySymbolOf(flow.currency, currencyList),
                            onEdit = { editingFlow = it },
                            onDelete = { viewModel.deleteAssetFlow(flow) }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddFlowDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加流转记录")
        }
    }

    if (showAddFlowDialog || editingFlow != null) {
        AssetFlowEditDialog(
            flow = editingFlow,
            assetId = asset.id,
            assetName = asset.name,
            currentValue = currentAsset.currentValue,
            currency = currentAsset.currency,
            symbol = currencySymbolOf(currentAsset.currency, currencyList),
            onDismiss = {
                showAddFlowDialog = false
                editingFlow = null
            },
            onSave = { flow ->
                if (editingFlow != null) {
                    viewModel.updateAssetFlow(flow.copy(id = editingFlow!!.id))
                } else {
                    viewModel.addAssetFlow(flow)
                }
                showAddFlowDialog = false
                editingFlow = null
            }
        )
    }
}
