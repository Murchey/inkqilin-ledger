package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserAssetType(val label: String) {
    REAL_ESTATE("房屋"),
    VEHICLE("载具"),
    STOCK("股票"),
    FUND("基金"),
    INSURANCE("保险"),
    DEPOSIT("存款"),
    DIGITAL("数字货币"),
    OTHER("其他")
}

@Entity(tableName = "user_assets")
data class UserAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: UserAssetType,
    val currentValue: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)
