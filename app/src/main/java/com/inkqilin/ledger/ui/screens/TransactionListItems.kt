@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.BackHandler
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    viewModel: TransactionViewModel,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit = onEdit,
    /** 卡片不透明度 0.08–1；配合首页背景图使用 */
    cardOpacity: Float = 0.8f
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
    val expenseColor = Color(expenseColorHex.toColorInt())
    val cardAlpha = cardOpacity.coerceIn(0.08f, 1f)
    val trackAlpha = (cardAlpha * 0.45f).coerceIn(0.06f, 0.55f)
    // 未滑开时不组合操作按钮，避免半透明卡片下图标透出重叠
    val menuProgress = if (menuWidthPx <= 0f) 0f else (-offsetX / menuWidthPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(com.inkqilin.ledger.ui.theme.Corners.Md)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = trackAlpha))
    ) {
        if (menuProgress > 0.02f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(menuWidth)
                    .fillMaxHeight()
                    // 操作区用接近不透明底，滑开后不与背景图/上层内容混叠
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                    .graphicsLayer { alpha = menuProgress },
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
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha))
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val target = if (offsetX < -menuWidthPx / 2) -menuWidthPx else 0f
                        animate(
                            initialValue = offsetX,
                            targetValue = target,
                            animationSpec = MotionSprings.interactive()
                        ) { value, _ -> offsetX = value }
                    }
                )
        ) {
            TransactionItem(
                transaction = transaction,
                viewModel = viewModel,
                onClick = onClick,
                translucent = cardAlpha < 0.95f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun TransactionItem(
    transaction: Transaction,
    viewModel: TransactionViewModel,
    onClick: () -> Unit = {},
    translucent: Boolean = false
) {
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
    // Card 本身必须透明/半透明，否则外层半透明底会被完全盖住
    val cardContainer = if (translucent) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource),
        shape = com.inkqilin.ledger.ui.theme.Corners.Md,
        colors = CardDefaults.cardColors(
            containerColor = cardContainer,
            disabledContainerColor = cardContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        interactionSource = interactionSource,
        onClick = onClick
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
                    .clip(RoundedCornerShape(12.dp))
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
                    fontSize = 16.sp
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
