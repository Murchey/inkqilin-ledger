package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_assets")
data class CurrencyAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val symbol: String,
    val name: String,
    val cardColor: String,
    val isDefault: Boolean = false
)
