package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE, INCOME
}

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("type"),
        Index("category"),
        Index("currency"),
        Index("uuid"),
        Index("cycleBillId"),
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val note: String,
    val date: Long,
    val type: TransactionType,
    val currency: String = "CNY",
    val uuid: String? = null,
    val cycleBillId: Long? = null
)
