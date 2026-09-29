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
private fun TransactionItemPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("账单条目预览", style = MaterialTheme.typography.titleSmall)
                val sdf = SimpleDateFormat("MM月dd日", Locale.getDefault())
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
                            val iconColor = if (isIncome) Color(0xFF34C759) else Color(0xFFFF3B30)
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
                                    text = sdf.format(Date(tx.date)),
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
