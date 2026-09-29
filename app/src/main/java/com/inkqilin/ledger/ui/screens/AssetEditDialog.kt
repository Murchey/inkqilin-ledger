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
internal fun AssetEditDialog(
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
