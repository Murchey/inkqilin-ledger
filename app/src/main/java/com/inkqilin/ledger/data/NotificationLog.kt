package com.inkqilin.ledger.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "notification_log",
    primaryKeys = ["cycleBillId", "logDate"],
    indices = [Index(value = ["cycleBillId", "logDate"])]
)
data class NotificationLog(
    val cycleBillId: Long,
    val logDate: String            // "yyyy-MM-dd"
)

@Dao
interface NotificationLogDao {
    @Query("SELECT EXISTS(SELECT 1 FROM notification_log WHERE cycleBillId = :cycleBillId AND logDate = :logDate)")
    suspend fun exists(cycleBillId: Long, logDate: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLog)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLogIfNew(log: NotificationLog): Long

    @Query("DELETE FROM notification_log")
    suspend fun clearAll()
}
