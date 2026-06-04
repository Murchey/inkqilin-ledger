package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserAssetType(val label: String, val icon: String) {
    REAL_ESTATE("房产", "Home"),
    STOCK("股票", "TrendingUp"),
    FUND("基金", "ShowChart"),
    BOND("债券", "AccountBalance"),
    DEPOSIT("存款", "Savings"),
    INSURANCE("保险", "Security"),
    CRYPTO("数字货币", "CurrencyBitcoin"),
    OTHER("其他", "Category")
}

@Entity(tableName = "user_assets")
data class UserAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: UserAssetType,
    val currentValue: Double,
    val purchasePrice: Double = 0.0,
    val note: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)
