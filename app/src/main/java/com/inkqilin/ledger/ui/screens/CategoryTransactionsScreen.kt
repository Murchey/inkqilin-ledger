package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.Transaction
import java.util.Locale
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme

@Composable
fun CategoryTransactionsScreen(
    viewModel: TransactionViewModel,
    categoryName: String,
    type: String,
    startDate: Long = 0L,
    endDate: Long = 0L
) {
    val transactionType = if (type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
    val transactions by viewModel.getTransactionsByCategory(categoryName).collectAsState(initial = emptyList())
    val filteredTransactions = transactions.filter {
        it.type == transactionType && (startDate == 0L || it.date in startDate..endDate)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "$categoryName 的所有账单",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无账单记录")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryTransactionsScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val now = System.currentTimeMillis()
            val transactions = listOf(
                Transaction(1, 35.50, "餐饮", "午餐", now, TransactionType.EXPENSE, "CNY"),
                Transaction(2, 128.00, "餐饮", "聚餐", now - 86400000, TransactionType.EXPENSE, "CNY")
            )
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("餐饮 的所有账单", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(transactions) { tx ->
                        val accent = Color(0xFFF44336)
                        val emoji = "🍜"
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.5.dp)) {
                            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) { Text(tx.category, fontWeight = FontWeight.Medium, fontSize = 15.sp); if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Text("-¥${String.format(Locale.getDefault(), "%.2f", tx.amount)}", color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}