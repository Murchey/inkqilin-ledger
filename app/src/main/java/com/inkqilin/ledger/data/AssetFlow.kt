package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AssetFlowType(val label: String) {
    INCREASE("存入/增值"),
    DECREASE("取出/减值"),
    REVALUATION("重新估值")
}

@Entity(tableName = "asset_flows")
data class AssetFlow(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetId: Long,
    val assetName: String,
    val flowType: AssetFlowType,
    val amount: Double,
    val newValue: Double,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)
