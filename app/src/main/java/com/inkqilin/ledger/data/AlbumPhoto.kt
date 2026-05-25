package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_photos")
data class AlbumPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
