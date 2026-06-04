package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.UserAsset
import com.inkqilin.ledger.data.UserAssetType
import com.inkqilin.ledger.ui.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetManagementScreen(
    viewModel: TransactionViewModel
) {
    val allUserAssets by viewModel.allUserAssets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<UserAsset?>(null) }
    var assetToDelete by remember { mutableStateOf<UserAsset?>(null) }

    val groupedAssets = allUserAssets.groupBy { it.type }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            if (allUserAssets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "暂无资产记录",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击右下角 + 添加资产",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            UserAssetType.entries.forEach { type ->
                val assets = groupedAssets[type]
                if (!assets.isNullOrEmpty()) {
                    item {
                        Text(
                            text = "${type.label} (${assets.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(assets, key = { it.id }) { asset ->
                        AssetItem(
                            asset = asset,
                            onEdit = { editingAsset = asset },
                            onDelete = { assetToDelete = asset }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加资产")
        }
    }

    if (showAddDialog) {
        AssetEditDialog(
            title = "添加资产",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, value, price, note ->
                viewModel.addUserAsset(
                    UserAsset(
                        name = name,
                        type = type,
                        currentValue = value,
                        purchasePrice = price,
                        note = note
                    )
                )
                showAddDialog = false
            }
        )
    }

    editingAsset?.let { asset ->
        AssetEditDialog(
            title = "编辑资产",
            initialAsset = asset,
            onDismiss = { editingAsset = null },
            onConfirm = { name, type, value, price, note ->
                viewModel.updateUserAsset(
                    asset.copy(
                        name = name,
                        type = type,
                        currentValue = value,
                        purchasePrice = price,
                        note = note
                    )
                )
                editingAsset = null
            }
        )
    }

    assetToDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            title = { Text("删除资产") },
            text = { Text("确定要删除「${asset.name}」吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUserAsset(asset)
                        assetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { assetToDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AssetItem(
    asset: UserAsset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        iconForAssetType(asset.type),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (asset.note.isNotBlank()) {
                    Text(
                        text = asset.note,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥${String.format("%,.2f", asset.currentValue)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (asset.purchasePrice > 0) {
                    val change = asset.currentValue - asset.purchasePrice
                    val changePercent = if (asset.purchasePrice > 0) change / asset.purchasePrice * 100 else 0.0
                    Text(
                        text = "${if (change >= 0) "+" else ""}${String.format("%.1f", changePercent)}%",
                        fontSize = 11.sp,
                        color = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetEditDialog(
    title: String,
    initialAsset: UserAsset? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: UserAssetType, value: Double, price: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialAsset?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialAsset?.type ?: UserAssetType.DEPOSIT) }
    var valueText by remember { mutableStateOf(initialAsset?.currentValue?.let { String.format("%.2f", it) } ?: "") }
    var priceText by remember { mutableStateOf(initialAsset?.purchasePrice?.let { if (it > 0) String.format("%.2f", it) else "" } ?: "") }
    var note by remember { mutableStateOf(initialAsset?.note ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("资产名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Box {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        label = { Text("资产类型") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    )
                    // Invisible clickable overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { typeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        UserAssetType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("当前价值 (¥)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("购入价格 (¥)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = valueText.toDoubleOrNull() ?: 0.0
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedType, value, price, note)
                    }
                },
                enabled = name.isNotBlank() && valueText.toDoubleOrNull() != null
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun iconForAssetType(type: UserAssetType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        UserAssetType.REAL_ESTATE -> Icons.Default.Home
        UserAssetType.STOCK -> Icons.Default.Star
        UserAssetType.FUND -> Icons.Default.List
        UserAssetType.BOND -> Icons.Default.Lock
        UserAssetType.DEPOSIT -> Icons.Default.Lock
        UserAssetType.INSURANCE -> Icons.Default.Info
        UserAssetType.CRYPTO -> Icons.Default.Star
        UserAssetType.OTHER -> Icons.Default.Menu
    }
}
