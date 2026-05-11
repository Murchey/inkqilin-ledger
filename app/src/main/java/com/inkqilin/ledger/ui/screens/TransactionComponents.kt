package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.MotionDurations
import com.inkqilin.ledger.ui.motion.MotionSprings
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
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                .background(MaterialTheme.colorScheme.surface)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val target = if (offsetX < -menuWidthPx / 2) -menuWidthPx else 0f
                        animate(
                            initialValue = offsetX,
                            targetValue = target,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
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
                                .clip(RoundedCornerShape(8.dp))
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

    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val incomeColor = Color(android.graphics.Color.parseColor(incomeColorHex))
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = if (isPressed) tween(MotionDurations.FAST) else MotionSprings.gentle(),
        label = "txScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        interactionSource = interactionSource,
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isIncome) incomeColor.copy(alpha = 0.12f)
                        else expenseColor.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                if (transaction.note.isNotBlank()) {
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
                    text = "${if (isIncome) "+" else "-"}¥${String.format("%.2f", transaction.amount)}",
                    color = if (isIncome) incomeColor else expenseColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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
