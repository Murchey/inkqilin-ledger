package com.inkqilin.ledger.data.repository

import com.inkqilin.ledger.data.CycleBill
import com.inkqilin.ledger.data.CycleBillDao
import com.inkqilin.ledger.data.NotificationLog
import com.inkqilin.ledger.data.NotificationLogDao
import com.inkqilin.ledger.data.RecycledCycleBill
import kotlinx.coroutines.flow.Flow

/** 周期账单 + 回收站 + 提醒日志。 */
class CycleBillRepository(
    private val dao: CycleBillDao,
    private val logDao: NotificationLogDao,
) {
    fun getAllCycleBills(): Flow<List<CycleBill>> = dao.getAllCycleBills()
    fun getEnabledCycleBills(enabled: Boolean): Flow<List<CycleBill>> = dao.getEnabledCycleBills(enabled)
    suspend fun getCycleBillById(id: Long): CycleBill? = dao.getCycleBillById(id)
    fun getDueSoonBills(now: Long, future: Long): Flow<List<CycleBill>> = dao.getDueSoonBills(now, future)
    fun getOverdueBills(now: Long): Flow<List<CycleBill>> = dao.getOverdueBills(now)
    suspend fun getAllEnabledBillsSync(): List<CycleBill> = dao.getAllEnabledBillsSync()
    suspend fun getDueByToday(currentTime: Long): List<CycleBill> = dao.getDueByToday(currentTime)
    suspend fun insertCycleBill(cycleBill: CycleBill): Long = dao.insertCycleBill(cycleBill)
    suspend fun updateCycleBill(cycleBill: CycleBill) = dao.updateCycleBill(cycleBill)
    suspend fun deleteCycleBill(cycleBill: CycleBill) = dao.deleteCycleBill(cycleBill)

    suspend fun insertRecycledCycleBill(recycled: RecycledCycleBill) = dao.insertRecycledCycleBill(recycled)
    fun getAllRecycledCycleBills(): Flow<List<RecycledCycleBill>> = dao.getAllRecycledCycleBills()
    fun getRecycledSince(since: Long): Flow<List<RecycledCycleBill>> = dao.getRecycledSince(since)
    suspend fun clearAllRecycled() = dao.clearAllRecycled()
    suspend fun getRecycledById(id: Long): RecycledCycleBill? = dao.getRecycledById(id)
    suspend fun deleteRecycledCycleBill(originalId: Long) = dao.deleteRecycledCycleBill(originalId)

    suspend fun updateLifecycleTracking(
        id: Long,
        cycleStart: Long,
        cycleEnd: Long,
        lastGeneratedDate: Long?,
        nextTriggerDate: Long,
        now: Long,
    ) = dao.updateLifecycleTracking(id, cycleStart, cycleEnd, lastGeneratedDate, nextTriggerDate, now)

    suspend fun updateStatusAndNextTrigger(
        id: Long,
        cycleStart: Long,
        cycleEnd: Long,
        nextTriggerDate: Long,
        overdue: Boolean,
    ) = dao.updateStatusAndNextTrigger(id, cycleStart, cycleEnd, nextTriggerDate, overdue)

    suspend fun getWidgetBillsSync(limit: Int): List<CycleBill> = dao.getWidgetBillsSync(limit)

    suspend fun notificationExists(cycleBillId: Long, logDate: String): Boolean =
        logDao.exists(cycleBillId, logDate)

    suspend fun insertNotificationLog(log: NotificationLog) = logDao.insertLog(log)
    suspend fun insertNotificationLogIfNew(log: NotificationLog): Long = logDao.insertLogIfNew(log)
    suspend fun clearNotificationLogs() = logDao.clearAll()
}
