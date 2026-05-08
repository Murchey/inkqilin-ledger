package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class TransactionType {
    EXPENSE, INCOME
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val note: String,
    val date: Long, // Timestamp
    val type: TransactionType
)
