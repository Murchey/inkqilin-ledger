package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.AppMode
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*


@Preview(showBackground = true, heightDp = 800)
@Composable
internal fun HomeScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val transactions = listOf(
                Transaction(1, 35.50, "餐饮", "午餐", now, TransactionType.EXPENSE, "CNY"),
                Transaction(2, 12.00, "交通", "地铁", now, TransactionType.EXPENSE, "CNY"),
                Transaction(3, 5000.00, "工资", "", now - dayMs, TransactionType.INCOME, "CNY"),
                Transaction(4, 89.00, "购物", "", now - dayMs, TransactionType.EXPENSE, "CNY"),
                Transaction(5, 200.00, "餐饮", "聚餐", now - dayMs * 2, TransactionType.EXPENSE, "CNY")
            )
            val groupedTransactions = transactions.groupBy { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }.toSortedMap(compareByDescending { it })
            val daySdf = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("总览", color = Color.White, fontSize = 13.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text("收入", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp); Text("¥5,000.00", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                                Column(horizontalAlignment = Alignment.End) { Text("支出", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp); Text("¥336.50", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("结余 ¥4,663.50", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                groupedTransactions.forEach { (dateKey, txs) ->
                    val dayIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val dayExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val dayBalance = dayIncome - dayExpense
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(daySdf.format(Date(dateKey)), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val balanceText = when {
                                dayBalance > 0 -> "+¥${String.format("%.2f", dayBalance)}"
                                dayBalance < 0 -> "-¥${String.format("%.2f", -dayBalance)}"
                                else -> "¥0.00"
                            }
                            val balanceColor = when {
                                dayBalance > 0 -> Color(0xFF4CAF50)
                                dayBalance < 0 -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(balanceText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = balanceColor)
                        }
                    }
                    items(txs) { tx ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val isIncome = tx.type == TransactionType.INCOME
                                val accent = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                                val emoji = mapOf("餐饮" to "🍜", "交通" to "🚌", "购物" to "🛒", "工资" to "💰")
                                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text(emoji[tx.category] ?: "📋", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.category, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${if (isIncome) "+" else "-"}¥${String.format("%.2f", tx.amount)}", color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
