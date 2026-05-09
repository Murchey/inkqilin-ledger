package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val type: TransactionType,
    val color: String = "#715CFF" // 默认颜色
)
