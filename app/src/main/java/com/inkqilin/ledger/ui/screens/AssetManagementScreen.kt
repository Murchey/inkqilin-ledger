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

private val amountFormat = DecimalFormat("#,###.##")
private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

/** 由币种 code 解析符号；未知 code 回退为 code 前缀 */
private fun currencySymbolOf(code: String, currencies: List<CurrencyAsset>): String =
    currencies.firstOrNull { it.code == code }?.symbol
        ?: if (code.equals("CNY", ignoreCase = true)) "¥" else "$code "

private enum class AssetSortMode(val label: String) {
    BY_VALUE("按总值排序"),
    BY_CHANGE("按增值排序")
}

@OptIn(ExperimentalMaterial3Api::class)
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

@Composable
private fun AssetCard(
    asset: UserAsset,
    currencies: List<CurrencyAsset>,
    onClick: () -> Unit,
    onEdit: (UserAsset) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconForAssetType(asset.type),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    asset.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (asset.note.isNotBlank()) {
                    Text(
                        asset.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${currencySymbolOf(asset.currency, currencies)} ${amountFormat.format(asset.currentValue)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    "修改于 ${dateFormat.format(Date(asset.lastUpdated))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            IconButton(onClick = { onEdit(asset) }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${asset.name}」吗？相关的流转记录不会自动删除。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetEditDialog(
    asset: UserAsset?,
    currencies: List<CurrencyAsset>,
    onDismiss: () -> Unit,
    onSave: (UserAsset) -> Unit
) {
    var name by remember { mutableStateOf(asset?.name ?: "") }
    var selectedType by remember { mutableStateOf(asset?.type ?: UserAssetType.OTHER) }
    var selectedCurrency by remember(asset?.id) {
        mutableStateOf(asset?.currency ?: currencies.firstOrNull { it.isDefault }?.code ?: "CNY")
    }
    var valueStr by remember { mutableStateOf(if (asset != null) asset.currentValue.toString() else "") }
    var note by remember { mutableStateOf(asset?.note ?: "") }

    val symbol = currencies.firstOrNull { it.code == selectedCurrency }?.symbol
        ?: if (selectedCurrency == "CNY") "¥" else "$selectedCurrency "
    val value = AmountExpressionEvaluator.evaluate(valueStr) ?: 0.0
    val isValid = name.isNotBlank() && value >= 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 640.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (asset != null) "编辑资产" else "添加资产",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("资产名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserAssetType.entries.chunked(2).forEach { rowTypes ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowTypes.forEach { type ->
                                    val selected = selectedType == type
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedType = type },
                                        label = { Text(type.label) },
                                        leadingIcon = {
                                            Icon(
                                                iconForAssetType(type),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowTypes.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (currencies.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            currencies.forEach { cur ->
                                val selected = selectedCurrency == cur.code
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedCurrency = cur.code },
                                    label = { Text("${cur.symbol} ${cur.code}") },
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            }
                        }
                    }
                    var showAmountKeypad by remember { mutableStateOf(false) }
                    fun evaluateAmount() {
                        AmountExpressionEvaluator.evaluate(valueStr)?.let { result ->
                            if (result >= 0) valueStr = result.toString()
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = valueStr,
                            onValueChange = { valueStr = it },
                            label = { Text("当前估值") },
                            singleLine = true,
                            readOnly = true,
                            prefix = { Text("$symbol ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(1f)
                                .clickable { showAmountKeypad = true }
                        )
                    }
                    if (showAmountKeypad) {
                        AmountKeypad(
                            value = valueStr,
                            onValueChange = { valueStr = it },
                            onEvaluate = ::evaluateAmount,
                            onDismiss = { showAmountKeypad = false }
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            onSave(
                                UserAsset(
                                    name = name.trim(),
                                    type = selectedType,
                                    currentValue = value,
                                    note = note.trim(),
                                    currency = selectedCurrency,
                                    createdAt = asset?.createdAt ?: now,
                                    lastUpdated = now
                                )
                            )
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f)
                    ) { Text("保存") }
                }
            }
        }
    }
}

// ========== 流转记录页面 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetFlowScreen(
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

@Composable
private fun AssetSummaryCard(
    currentValue: Double,
    netChange: Double,
    flowCount: Int,
    trendValues: List<Double>,
    symbol: String = "¥"
) {
    val changeColor = when {
        netChange > 0 -> ComposeColor(0xFF34C759)
        netChange < 0 -> ComposeColor(0xFFFF3B30)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryMetric("当前估值", "$symbol ${amountFormat.format(currentValue)}", MaterialTheme.colorScheme.onSurface)
                SummaryMetric("累计变化", "${if (netChange >= 0) "+" else ""}$symbol ${amountFormat.format(netChange)}", changeColor)
                SummaryMetric("流转次数", "$flowCount 次", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trendValues.size >= 2) {
                Spacer(modifier = Modifier.height(16.dp))
                AssetTrendChart(values = trendValues, lineColor = changeColor)
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, valueColor: ComposeColor) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun AssetTrendChart(values: List<Double>, lineColor: ComposeColor) {
    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: minValue
    val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
    Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) 0f else size.width * index / (values.size - 1)
            val y = size.height - ((value - minValue) / range).toFloat() * size.height
            Offset(x, y)
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(color = lineColor, start = start, end = end, strokeWidth = 4f)
        }
    }
}

@Composable
private fun FlowItem(
    flow: AssetFlow,
    symbol: String = "¥",
    onEdit: (AssetFlow) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isIncrease = flow.flowType == AssetFlowType.INCREASE
    val iconColor = when (flow.flowType) {
        AssetFlowType.INCREASE -> ComposeColor(0xFF4CAF50)
        AssetFlowType.DECREASE -> ComposeColor(0xFFF44336)
        AssetFlowType.REVALUATION -> ComposeColor(0xFF2196F3)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isIncrease) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        flow.flowType.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        dateFormat.format(Date(flow.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                if (flow.note.isNotBlank()) {
                    Text(
                        flow.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = when (flow.flowType) {
                    AssetFlowType.INCREASE -> "+"
                    AssetFlowType.DECREASE -> "-"
                    AssetFlowType.REVALUATION -> "±"
                }
                Text(
                    "$prefix $symbol ${amountFormat.format(abs(flow.amount))}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = iconColor
                )
                Text(
                    "余额 $symbol ${amountFormat.format(flow.newValue)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            IconButton(onClick = { onEdit(flow) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条流转记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetFlowEditDialog(
    flow: AssetFlow?,
    assetId: Long,
    assetName: String,
    currentValue: Double,
    currency: String,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (AssetFlow) -> Unit
) {
    var selectedType by remember { mutableStateOf(flow?.flowType ?: AssetFlowType.INCREASE) }
    var amountStr by remember { mutableStateOf(if (flow != null) abs(flow.amount).toString() else "") }
    var note by remember { mutableStateOf(flow?.note ?: "") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var flowDate by remember { mutableStateOf(flow?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = AmountExpressionEvaluator.evaluate(amountStr) ?: 0.0
    // 计算新的总价值
    val newValue = when (selectedType) {
        AssetFlowType.INCREASE -> currentValue + amount
        AssetFlowType.DECREASE -> (currentValue - amount).coerceAtLeast(0.0)
        AssetFlowType.REVALUATION -> amount // 估值直接覆盖
    }
    val isValid = amountStr.isNotBlank() && amount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (flow != null) "编辑流转记录" else "添加流转记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 类型选择
                Box {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("变动类型") },
                        trailingIcon = {
                            Icon(
                                if (typeDropdownExpanded) Icons.Default.KeyboardArrowUp
                                else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { typeDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        AssetFlowType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                var showFlowAmountKeypad by remember { mutableStateOf(false) }
                fun evaluateFlowAmount() {
                    AmountExpressionEvaluator.evaluate(amountStr)?.let { result ->
                        if (result >= 0) amountStr = result.toString()
                    }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = {
                            Text(
                                when (selectedType) {
                                    AssetFlowType.INCREASE -> "存入/增值金额"
                                    AssetFlowType.DECREASE -> "取出/减值金额"
                                    AssetFlowType.REVALUATION -> "新估值"
                                }
                            )
                        },
                        singleLine = true,
                        readOnly = true,
                        prefix = { Text("$symbol ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(1f)
                            .clickable { showFlowAmountKeypad = true }
                    )
                }
                if (showFlowAmountKeypad) {
                    AmountKeypad(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        onEvaluate = ::evaluateFlowAmount,
                        onDismiss = { showFlowAmountKeypad = false }
                    )
                }
                // 日期选择
                Box {
                    OutlinedTextField(
                        value = dateFormat.format(Date(flowDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("日期") },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                // 预览新总价值
                if (isValid) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "变动后总价值",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "$symbol ${amountFormat.format(newValue)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalAmount = when (selectedType) {
                        AssetFlowType.INCREASE -> amount
                        AssetFlowType.DECREASE -> -amount
                        AssetFlowType.REVALUATION -> amount
                    }
                    onSave(
                        AssetFlow(
                            assetId = assetId,
                            assetName = assetName,
                            flowType = selectedType,
                            amount = finalAmount,
                            newValue = newValue,
                            note = note.trim(),
                            date = flowDate,
                            currency = currency
                        )
                    )
                },
                enabled = isValid
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = flowDate)
        AppleDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { flowDate = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        )
    }
}

// ========== 工具 ==========

private fun iconForAssetType(type: UserAssetType): ImageVector = when (type) {
    UserAssetType.REAL_ESTATE -> Icons.Default.Home
    UserAssetType.VEHICLE -> Icons.Default.Star
    UserAssetType.STOCK -> Icons.Default.Star
    UserAssetType.FUND -> Icons.Default.Star
    UserAssetType.INSURANCE -> Icons.Default.Lock
    UserAssetType.DEPOSIT -> Icons.Default.Lock
    UserAssetType.DIGITAL -> Icons.Default.Star
    UserAssetType.OTHER -> Icons.Default.MoreVert
}
