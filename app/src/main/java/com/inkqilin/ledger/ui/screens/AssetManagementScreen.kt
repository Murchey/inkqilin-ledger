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
fun AssetManagementScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onUpdateTopBar: (String, (() -> Unit)?) -> Unit = { _, _ -> }
) {
    val allAssets by viewModel.allUserAssets.collectAsState()
    val allFlows by viewModel.allAssetFlows.collectAsState()
    val currencyList by viewModel.allAssets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<UserAsset?>(null) }
    var selectedAssetForFlow by remember { mutableStateOf<UserAsset?>(null) }
    var sortMode by remember { mutableStateOf(AssetSortMode.BY_VALUE) }
    var sortExpanded by remember { mutableStateOf(false) }

    // 计算每个资产的累计增值
    val assetAppreciation = remember(allFlows, allAssets) {
        val map = mutableMapOf<Long, Double>()
        allFlows.forEach { flow ->
            val change = when (flow.flowType) {
                AssetFlowType.INCREASE -> flow.amount
                AssetFlowType.DECREASE -> -flow.amount
                AssetFlowType.REVALUATION -> {
                    val prevFlow = allFlows
                        .filter { it.assetId == flow.assetId && it.date < flow.date }
                        .maxByOrNull { it.date }
                    val prevValue = prevFlow?.newValue
                        ?: allAssets.find { it.id == flow.assetId }?.currentValue
                        ?: 0.0
                    flow.newValue - prevValue
                }
            }
            map[flow.assetId] = (map[flow.assetId] ?: 0.0) + change
        }
        map
    }

    val grouped = remember(allAssets, sortMode, assetAppreciation) {
        allAssets.groupBy { it.type }.mapValues { (_, assets) ->
            when (sortMode) {
                AssetSortMode.BY_VALUE -> assets.sortedByDescending { it.currentValue }
                AssetSortMode.BY_CHANGE -> assets.sortedByDescending { abs(assetAppreciation[it.id] ?: 0.0) }
            }
        }
    }

    // 统一管理 TopAppBar 标题和返回行为
    LaunchedEffect(selectedAssetForFlow) {
        if (selectedAssetForFlow != null) {
            onUpdateTopBar(selectedAssetForFlow!!.name) { selectedAssetForFlow = null }
        } else {
            onUpdateTopBar("资产管理", onBack)
        }
    }

    // 系统返回：资产流转详情 → 资产列表 → 再退出本页（与顶栏返回一致）
    BackHandler(enabled = selectedAssetForFlow != null) {
        selectedAssetForFlow = null
    }

    // 流转记录子页面（替换整个界面）
    if (selectedAssetForFlow != null) {
        AssetFlowScreen(
            asset = selectedAssetForFlow!!,
            viewModel = viewModel
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (allAssets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有资产记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        "点击右下角添加你的资产",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 总资产卡片（多币种分行汇总，不跨币种直接相加）
                item {
                    val symbolOf: (String) -> String = { code -> currencySymbolOf(code, currencyList) }
                    val totalsByCurrency = allAssets.groupBy { it.currency }
                        .mapValues { entry -> entry.value.sumOf { it.currentValue } }
                        .toList()
                        .sortedByDescending { it.second }
                    Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (totalsByCurrency.size > 1) "总资产（分币种）" else "总资产",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (totalsByCurrency.size <= 1) {
                                val only = totalsByCurrency.firstOrNull()
                                Text(
                                    "${symbolOf(only?.first ?: "CNY")} ${amountFormat.format(only?.second ?: 0.0)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                totalsByCurrency.forEach { (code, sum) ->
                                    Text(
                                        "${symbolOf(code)} ${amountFormat.format(sum)}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "共 ${allAssets.size} 项资产",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 排序切换
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box {
                            TextButton(onClick = { sortExpanded = true }) {
                                Text(
                                    sortMode.label,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false }
                            ) {
                                AssetSortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.label) },
                                        onClick = {
                                            sortMode = mode
                                            sortExpanded = false
                                        },
                                        leadingIcon = if (mode == sortMode) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }

                // 按类型分组
                UserAssetType.entries.forEach { type ->
                    val assetsOfType = grouped[type] ?: return@forEach
                    if (assetsOfType.isEmpty()) return@forEach

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                iconForAssetType(type),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${type.label} (${assetsOfType.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    items(assetsOfType, key = { it.id }) { asset ->
                        AssetCard(
                            asset = asset,
                            currencies = currencyList,
                            onClick = { selectedAssetForFlow = asset },
                            onEdit = { editingAsset = it },
                            onDelete = { viewModel.deleteUserAsset(asset) }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加资产")
        }
    }

    // 添加/编辑资产对话框
    if (showAddDialog || editingAsset != null) {
        AssetEditDialog(
            asset = editingAsset,
            currencies = currencyList,
            onDismiss = {
                showAddDialog = false
                editingAsset = null
            },
            onSave = { asset ->
                if (editingAsset != null) {
                    viewModel.updateUserAsset(asset.copy(id = editingAsset!!.id, createdAt = editingAsset!!.createdAt))
                } else {
                    viewModel.addUserAsset(asset)
                }
                showAddDialog = false
                editingAsset = null
            }
        )
    }
}
