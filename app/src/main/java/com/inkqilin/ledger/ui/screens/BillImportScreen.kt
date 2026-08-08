package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.AssetFlowType
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.data.UserAsset
import com.inkqilin.ledger.data.UserAssetType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.theme.appButtonElevation
import com.inkqilin.ledger.util.BillImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillImportScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existingCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val isDark = isSystemInDarkTheme()
    val dialogTextColor = if (isDark) Color.White else Color.Black

    var parsedBills by remember { mutableStateOf<List<BillImporter.ParsedBill>>(emptyList()) }
    var parsedAssets by remember { mutableStateOf<List<BillImporter.ParsedAssetData>>(emptyList()) }
    var parsedFlows by remember { mutableStateOf<List<BillImporter.ParsedFlowData>>(emptyList()) }
    var isParsing by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(true) }
    var selectedFormat by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val format = selectedFormat
            selectedFormat = null
            scope.launch {
                isParsing = true
                val result = try {
                    withContext(Dispatchers.IO) {
                        when (format) {
                            "alipay" -> BillImporter.FullImportData(BillImporter.parseAlipayCsv(context, it), emptyList(), emptyList())
                            "wechat" -> BillImporter.FullImportData(BillImporter.parseWechatXlsx(context, it), emptyList(), emptyList())
                            else -> BillImporter.parseAppFull(context, it)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BillImport", "解析失败", e)
                    BillImporter.FullImportData(emptyList(), emptyList(), emptyList())
                }
                parsedBills = result.bills
                parsedAssets = result.assets
                parsedFlows = result.flows
                isParsing = false
                if (result.bills.isEmpty()) {
                    Toast.makeText(context, "未能识别到有效账单记录，请检查文件格式", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launch file picker when format is selected
    LaunchedEffect(selectedFormat) {
        if (selectedFormat != null) {
            val format = selectedFormat!!
            val mime = when (format) {
                "alipay" -> "text/*"
                else -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }
            filePicker.launch(mime)
        }
    }

    // Format picker dialog
    if (showFormatDialog && parsedBills.isEmpty()) {

        AppleAlertDialog(
            onDismissRequest = { showFormatDialog = false; onBack() },
            title = "选择导入格式",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请选择要导入的账单文件格式", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            selectedFormat = "app"
                            showFormatDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("本APP格式 (Excel)", color = dialogTextColor)
                    }
                    OutlinedButton(
                        onClick = {
                            selectedFormat = "wechat"
                            showFormatDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("微信账单格式 (Excel)", color = dialogTextColor)
                    }
                    OutlinedButton(
                        onClick = {
                            selectedFormat = "alipay"
                            showFormatDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccountBox, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("支付宝账单格式 (CSV)", color = dialogTextColor)
                    }
                }
            },
            buttons = listOf(
                AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) {
                    showFormatDialog = false
                    onBack()
                }
            )
        )
    }

    // Loading state
    if (isParsing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("操作已确认", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("正在解析账单...", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    // Results state
    if (parsedBills.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "识别结果 (${parsedBills.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "请确认并编辑后点击导入",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(parsedBills) { bill ->
                    BillItemCard(
                        bill = bill,
                        existingCategories = existingCategories.map { it.name },
                        onRemove = {
                            parsedBills = parsedBills.filter { it != bill }
                        },
                        onEdit = { updated ->
                            parsedBills = parsedBills.map {
                                if (it == bill) updated else it
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        scope.launch {
                            var imported = 0
                            var skipped = 0
                            parsedBills.forEach { bill ->
                                val inserted = viewModel.importTransactionSkipDuplicates(
                                    Transaction(
                                        amount = bill.amount,
                                        note = bill.note,
                                        category = bill.category,
                                        type = bill.type,
                                        date = bill.date.time,
                                        currency = "CNY",
                                        uuid = bill.uuid
                                    )
                                )
                                if (inserted) imported++ else skipped++
                            }

                            // 导入资产
                            var assetImported = 0
                            parsedAssets.forEach { asset ->
                                val existing = viewModel.allUserAssets.value.find {
                                    it.name == asset.assetName && it.type.label == asset.assetType
                                }
                                if (existing == null) {
                                    val assetType = UserAssetType.entries.find { it.label == asset.assetType } ?: UserAssetType.OTHER
                                    viewModel.addUserAsset(
                                        UserAsset(
                                            name = asset.assetName,
                                            type = assetType,
                                            currentValue = asset.currentValue,
                                            note = asset.note,
                                            createdAt = asset.createdAt,
                                            lastUpdated = asset.lastUpdated
                                        )
                                    )
                                    assetImported++
                                }
                            }

                            // 导入流转（带去重）
                            var flowImported = 0
                            var flowSkipped = 0
                            parsedFlows.forEach { flow ->
                                val matchedAsset = viewModel.allUserAssets.value.find { it.name == flow.assetName }
                                if (matchedAsset != null) {
                                    val flowType = AssetFlowType.entries.find { it.label == flow.flowType } ?: AssetFlowType.INCREASE
                                    val inserted = viewModel.importAssetFlowSkipDuplicates(
                                        AssetFlow(
                                            assetId = matchedAsset.id,
                                            assetName = flow.assetName,
                                            flowType = flowType,
                                            amount = flow.amount,
                                            newValue = flow.newValue,
                                            note = flow.note,
                                            date = flow.date,
                                            uuid = flow.uuid
                                        )
                                    )
                                    if (inserted) flowImported++ else flowSkipped++
                                }
                            }

                            val summary = buildString {
                                append("已导入 $imported 条账单")
                                if (skipped > 0) append("，跳过 $skipped 条重复")
                                if (assetImported > 0) append("，${assetImported}项资产")
                                if (flowImported > 0) append("，${flowImported}条流转")
                                if (flowSkipped > 0) append("，跳过 $flowSkipped 条重复流转")
                            }
                            Toast.makeText(context, summary, Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    elevation = appButtonElevation()
                ) {
                    Text("确认并导入 (${parsedBills.size})")
                }
            }
        }
        return
    }

    // Empty state (only show after we've tried parsing but got nothing, or user hasn't selected)
    if (!showFormatDialog && !isParsing && parsedBills.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("未识别到账单数据", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onBack) {
                    Text("返回")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BillItemCard(
    bill: BillImporter.ParsedBill,
    existingCategories: List<String>,
    onRemove: () -> Unit,
    onEdit: (BillImporter.ParsedBill) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (bill.type == TransactionType.EXPENSE) "-${bill.amount}" else "+${bill.amount}",
                        color = if (bill.type == TransactionType.EXPENSE) androidx.compose.ui.graphics.Color(0xFFFF5252)
                        else androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = bill.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (bill.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = bill.note, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(bill.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Create, contentDescription = "编辑")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showEditDialog) {
        var editAmount by remember { mutableStateOf(bill.amount.toString()) }
        var editCategory by remember { mutableStateOf(bill.category) }
        var editNote by remember { mutableStateOf(bill.note) }
        var editType by remember { mutableStateOf(bill.type) }
        val categories = (existingCategories + listOf("餐饮", "交通", "购物", "娱乐", "居住", "医疗", "教育", "人情", "投资", "收入", "其他")).distinct()

        AppleAlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = "编辑账单",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("金额") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = editType == TransactionType.EXPENSE,
                            onClick = { editType = TransactionType.EXPENSE },
                            label = { Text("支出") }
                        )
                        FilterChip(
                            selected = editType == TransactionType.INCOME,
                            onClick = { editType = TransactionType.INCOME },
                            label = { Text("收入") }
                        )
                    }
                    Text("分类", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = editCategory == cat,
                                onClick = { editCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("备注") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            buttons = listOf(
                AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { showEditDialog = false },
                AppleDialogButton("保存", AppleDialogButtonStyle.DEFAULT) {
                    val amount = editAmount.toDoubleOrNull() ?: bill.amount
                    onEdit(bill.copy(
                        amount = amount,
                        category = editCategory,
                        note = editNote,
                        type = editType
                    ))
                    showEditDialog = false
                }
            )
        )
    }
}
