package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "keyword_categories",
    indices = [Index("keyword")]
)
data class KeywordCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val categoryName: String
)