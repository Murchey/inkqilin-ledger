package com.inkqilin.ledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class CycleType { DAILY, WEEKLY, MONTHLY, YEARLY }
enum class GenerationMode { AUTO_BEFORE, AUTO_START, NOTIFY_ONLY }

@Entity(tableName = "cycle_bills")
data class CycleBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val currency: String = "CNY",
    val cycleType: CycleType,
    val startDate: Long,
    val enabled: Boolean = true,
    val reminderEnabled: Boolean = true,
    val advanceMinutes: Int = 60,
    val generationMode: GenerationMode = GenerationMode.AUTO_BEFORE,
    val note: String = "",
    val colorHex: Int? = null,
    // Lifecycle tracking fields (updated by Worker)
    val currentCycleStart: Long = 0L,
    val currentCycleEnd: Long = 0L,
    val lastGeneratedDate: Long? = null,
    val nextTriggerDate: Long = 0L,
    val overdue: Boolean = false
)

@Entity(
    tableName = "recycled_cycle_bills",
    primaryKeys = ["originalId"]
)
data class RecycledCycleBill(
    @field: androidx.room.ColumnInfo(name = "originalId") val originalId: Long,
    val recycleTime: Long,
    val name: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val currency: String = "CNY",
    val cycleType: CycleType,
    val startDate: Long,
    val enabled: Boolean = true,
    val reminderEnabled: Boolean = true,
    val advanceMinutes: Int = 60,
    val generationMode: GenerationMode = GenerationMode.AUTO_BEFORE,
    val note: String = "",
    val colorHex: Int? = null
)

@Dao
interface CycleBillDao {
    // ── Queries ──
    @Query("SELECT * FROM cycle_bills ORDER BY enabled DESC, id ASC")
    fun getAllCycleBills(): Flow<List<CycleBill>>

    @Query("SELECT * FROM cycle_bills WHERE enabled = :enabled ORDER BY id ASC")
    fun getEnabledCycleBills(enabled: Boolean): Flow<List<CycleBill>>

    @Query("SELECT * FROM cycle_bills WHERE id = :id")
    suspend fun getCycleBillById(id: Long): CycleBill?

    // 获取即将到期（7天内）的周期账单
    @Query("""
        SELECT * FROM cycle_bills WHERE enabled = 1
        AND nextTriggerDate BETWEEN :now AND :future
        ORDER BY nextTriggerDate ASC
    """)
    fun getDueSoonBills(now: Long, future: Long): Flow<List<CycleBill>>

    // 已过期的周期账单（当前时间已超过当前周期结束但未生成）
    @Query("""
        SELECT * FROM cycle_bills WHERE enabled = 1
        AND nextTriggerDate < :now
        ORDER BY nextTriggerDate DESC
    """)
    fun getOverdueBills(now: Long): Flow<List<CycleBill>>

    // WorkManager 用：每天扫描，获取所有已启用的周期账单
    @Query("SELECT * FROM cycle_bills WHERE enabled = 1")
    suspend fun getAllEnabledBillsSync(): List<CycleBill>

    // 获取当天应该触发的周期账单（用于 App 启动补偿）
    @Query("SELECT * FROM cycle_bills WHERE enabled = 1 AND nextTriggerDate > :currentTime ORDER BY nextTriggerDate ASC LIMIT 50")
    suspend fun getDueByToday(currentTime: Long): List<CycleBill>

    // ── Insert/Update/Delete ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycleBill(cycleBill: CycleBill): Long

    @Update
    suspend fun updateCycleBill(cycleBill: CycleBill)

    @Delete
    suspend fun deleteCycleBill(cycleBill: CycleBill)

    // ── Recycle ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecycledCycleBill(recycled: RecycledCycleBill)

    @Query("SELECT * FROM recycled_cycle_bills ORDER BY recycleTime DESC")
    fun getAllRecycledCycleBills(): Flow<List<RecycledCycleBill>>

    @Query("SELECT * FROM recycled_cycle_bills WHERE recycleTime >= :since ORDER BY recycleTime DESC")
    fun getRecycledSince(since: Long): Flow<List<RecycledCycleBill>>

    @Query("DELETE FROM recycled_cycle_bills")
    suspend fun clearAllRecycled()

    @Query("SELECT * FROM recycled_cycle_bills WHERE originalId = :id")
    suspend fun getRecycledById(id: Long): RecycledCycleBill?

    @Query("DELETE FROM recycled_cycle_bills WHERE originalId = :originalId")
    suspend fun deleteRecycledCycleBill(originalId: Long)

    // ── Lifecycle tracking (updated from Worker) ──
    @Query("""
        UPDATE cycle_bills SET 
            currentCycleStart = :cycleStart,
            currentCycleEnd = :cycleEnd,
            lastGeneratedDate = :lastGeneratedDate,
            nextTriggerDate = :nextTriggerDate,
            overdue = CASE WHEN :now > :cycleEnd AND (lastGeneratedDate IS NULL OR lastGeneratedDate != :now) THEN 1 ELSE 0 END
        WHERE id = :id
    """)
    suspend fun updateLifecycleTracking(
        id: Long, cycleStart: Long, cycleEnd: Long,
        lastGeneratedDate: Long?, nextTriggerDate: Long, now: Long
    )

    // ── Status update for overdue detection ──
    @Query("""
        UPDATE cycle_bills SET 
            currentCycleStart = :cycleStart,
            currentCycleEnd = :cycleEnd,
            nextTriggerDate = :nextTriggerDate,
            overdue = CASE WHEN :overdue THEN 1 ELSE 0 END
        WHERE id = :id
    """)
    suspend fun updateStatusAndNextTrigger(
        id: Long, cycleStart: Long, cycleEnd: Long,
        nextTriggerDate: Long, overdue: Boolean
    )

    // ── AppWidget 用：同步拉取已启用周期账单（按下次触发时间升序）──
    @Query("SELECT * FROM cycle_bills WHERE enabled = 1 ORDER BY nextTriggerDate ASC, id ASC LIMIT :limit")
    suspend fun getWidgetBillsSync(limit: Int): List<CycleBill>
}
