package com.inkqilin.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RelationshipType(val label: String) {
    RELATIVE("亲属"), FRIEND("朋友"), COLLEAGUE("同事"), OTHER("其他")
}

@Entity(tableName = "renqing_contacts")
data class RenQingContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationship: RelationshipType = RelationshipType.OTHER,
    val phone: String = "",
    val birthday: Long? = null,
    val note: String = ""
)
