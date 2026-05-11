package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel

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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
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