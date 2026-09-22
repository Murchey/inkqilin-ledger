package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "renqing_tags")
data class RenQingTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "gift",
    val color: String = "#715CFF"
)
