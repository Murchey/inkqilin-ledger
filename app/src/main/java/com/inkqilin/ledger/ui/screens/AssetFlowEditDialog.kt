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
internal fun AssetFlowEditDialog(
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

internal fun iconForAssetType(type: UserAssetType): ImageVector = when (type) {
    UserAssetType.REAL_ESTATE -> Icons.Default.Home
    UserAssetType.VEHICLE -> Icons.Default.Star
    UserAssetType.STOCK -> Icons.Default.Star
    UserAssetType.FUND -> Icons.Default.Star
    UserAssetType.INSURANCE -> Icons.Default.Lock
    UserAssetType.DEPOSIT -> Icons.Default.Lock
    UserAssetType.DIGITAL -> Icons.Default.Star
    UserAssetType.OTHER -> Icons.Default.MoreVert
}
