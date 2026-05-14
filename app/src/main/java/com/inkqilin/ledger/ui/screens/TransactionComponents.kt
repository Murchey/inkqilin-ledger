package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    viewModel: TransactionViewModel,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current
    val menuWidth = 120.dp
    val menuWidthPx = with(density) { menuWidth.toPx() }
    
    var offsetX by remember(transaction.id) { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        val newOffset = (offsetX + delta).coerceIn(-menuWidthPx, 0f)
        offsetX = newOffset
    }

    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(menuWidth)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = {
                    offsetX = 0f
                    onEdit()
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = {
                    offsetX = 0f
                    onDelete()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = expenseColor)
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val target = if (offsetX < -menuWidthPx / 2) -menuWidthPx else 0f
                        animate(
                            initialValue = offsetX,
                            targetValue = target,
                            animationSpec = MotionSprings.interactive() // iOS-like bouncy menu snap
                        ) { value, _ -> offsetX = value }
                    }
                )
        ) {
            TransactionItem(transaction, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: com.inkqilin.ledger.data.Category? = null,
    @Suppress("UNUSED_PARAMETER") type: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "📦") }
    var color by remember { mutableStateOf(category?.color ?: "#715CFF") }

    val emojiList = listOf(
        "🍜", "🚗", "🛒", "🎮", "🏠", "📦", "💰", "🎁", "📈", "💼",
        "💳", "🚌", "✈️", "🏥", "📚", "🎵", "🎬", "⚽", "🐱", "🐶",
        "☕", "🍺", "🛍️", "💄", "💇", "🔧", "📱", "💻", "🎓", "🎉",
        "🌿", "🏋️", "🍕", "🍰", "🧋", "🚕", "⛽", "🏡", "🏢", "🏦",
        "👶", "🧹", "💊", "📌", "💡", "🔥", "⭐", "❤️", "✅", "🆕"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "添加分类" else "修改分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("选择图标", style = MaterialTheme.typography.bodyMedium)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(emojiList) { emoji ->
                        val selected = icon == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { icon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("或手动输入 Emoji") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("选择颜色", style = MaterialTheme.typography.bodyMedium)
                val colors = listOf("#715CFF", "#51B4FF", "#4CAF50", "#F44336", "#FF9800", "#9C27B0", "#E91E63", "#00BCD4")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .clickable { color = colorHex }
                                .then(
                                    if (color == colorHex) Modifier.background(Color.Black.copy(alpha = 0.1f), CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == colorHex) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, icon, color) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(transaction: Transaction, viewModel: TransactionViewModel) {
    val sdf = SimpleDateFormat("MM月dd日", Locale.getDefault())
    val dateStr = sdf.format(Date(transaction.date))

    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val category = allCategories.find { it.name == transaction.category && it.type == transaction.type }
    val icon = category?.icon ?: "📋"
    val isIncome = transaction.type == TransactionType.INCOME

    val allAssets by viewModel.allAssets.collectAsState()
    val currencySymbol = allAssets.firstOrNull { it.code == transaction.currency }?.symbol ?: "¥"

    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val incomeColor = Color(android.graphics.Color.parseColor(incomeColorHex))
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))

    val interactionSource = remember { MutableInteractionSource() }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource), // Use our custom iOS-style press down
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        interactionSource = interactionSource,
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isIncome) incomeColor.copy(alpha = 0.1f)
                        else expenseColor.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${currencySymbol}${String.format("%.2f", transaction.amount)}",
                    color = if (isIncome) incomeColor else expenseColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionItemPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("账单条目预览", style = MaterialTheme.typography.titleSmall)
                val sdf = java.text.SimpleDateFormat("MM月dd日", java.util.Locale.getDefault())
                val now = System.currentTimeMillis()
                val previewTransactions = listOf(
                    Transaction(1, 35.50, "餐饮", "午餐", now, TransactionType.EXPENSE, "CNY"),
                    Transaction(2, 5000.00, "工资", "", now - 86400000, TransactionType.INCOME, "CNY"),
                    Transaction(3, 128.00, "购物", "超市", now - 86400000 * 2, TransactionType.EXPENSE, "CNY")
                )
                previewTransactions.forEach { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isIncome = tx.type == TransactionType.INCOME
                            val iconColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                            val iconEmoji = when(tx.category) { "餐饮" -> "🍜"; "购物" -> "🛒"; "工资" -> "💰"; else -> "📋" }
                            Box(
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                                    .background(iconColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Text(text = iconEmoji, fontSize = 20.sp) }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = tx.category, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (tx.note.isNotBlank()) {
                                    Text(text = tx.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isIncome) "+" else "-"}¥${String.format("%.2f", tx.amount)}",
                                    color = iconColor, fontWeight = FontWeight.Bold, fontSize = 15.sp
                                )
                                Text(
                                    text = sdf.format(java.util.Date(tx.date)),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
