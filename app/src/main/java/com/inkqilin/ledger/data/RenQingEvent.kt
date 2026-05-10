package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RenQingEventType(val label: String, val icon: String) {
    WEDDING("婚礼", "\uD83D\uDC92"),
    FUNERAL("丧礼", "\uD83D\uDE4F"),
    BIRTHDAY("生日", "\uD83C\uDF82"),
    MOVING("乔迁", "\uD83C\uDFE0"),
    GRADUATION("升学", "\uD83C\uDF93"),
    BABY("满月", "\uD83D\uDC76"),
    OTHER("其他", "\uD83C\uDF81")
}

enum class RenQingDirection(val label: String) {
    RECEIVED("收到"), GIVEN("送出")
}

@Entity(tableName = "renqing_events")
data class RenQingEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val contactName: String,
    val eventType: RenQingEventType = RenQingEventType.OTHER,
    val tagId: Long = 0,
    val tagName: String = "",
    val direction: RenQingDirection = RenQingDirection.GIVEN,
    val amount: Double,
    val giftDescription: String = "",
    val date: Long,
    val location: String = "",
    val note: String = "",
    val photoUri: String? = null
)
