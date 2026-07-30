package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserAssetType(val label: String) {
    REAL_ESTATE("房产"),
    VEHICLE("车辆"),
    DEPOSIT("存款"),
    INSURANCE("保险"),
    JEWELRY("珠宝首饰"),
    COLLECTION("收藏品"),
    DIGITAL("数字资产"),
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
