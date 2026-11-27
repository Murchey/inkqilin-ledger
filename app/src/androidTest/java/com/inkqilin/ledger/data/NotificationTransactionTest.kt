package com.inkqilin.ledger.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationTransactionTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).build()
        dao = database.transactionDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun transaction(uuid: String) = Transaction(
        amount = 10.0,
        category = "其他",
        note = "[自动] 测试通知",
        date = 1_000L,
        type = TransactionType.EXPENSE,
        uuid = uuid,
    )

    @Test
    fun repeatedDeliveryIsSavedOnceAndNewEventIsSaved() = runBlocking {
        assertTrue(dao.insertNotificationIfAbsent(transaction("event-1")))
        assertFalse(dao.insertNotificationIfAbsent(transaction("event-1")))
        assertTrue(dao.insertNotificationIfAbsent(transaction("event-2")))
        assertEquals(2, dao.getAllTransactions().first().size)
    }

    @Test
    fun concurrentDeliveriesAreCheckedAndInsertedAtomically() = runBlocking {
        val inserted = (1..8).map {
            async(Dispatchers.IO) { dao.insertNotificationIfAbsent(transaction("same-event")) }
        }.awaitAll()
        assertEquals(1, inserted.count { it })
        assertEquals(1, dao.countByUuid("same-event"))
    }

    @Test
    fun failedWriteCanBeRetriedWithSameEventIdentity() = runBlocking {
        val sql = database.openHelper.writableDatabase
        sql.execSQL("CREATE TRIGGER fail_notification BEFORE INSERT ON transactions BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        val failed = runCatching { dao.insertNotificationIfAbsent(transaction("retry-event")) }
        assertTrue(failed.isFailure)
        assertEquals(0, dao.countByUuid("retry-event"))
        sql.execSQL("DROP TRIGGER fail_notification")
        assertTrue(dao.insertNotificationIfAbsent(transaction("retry-event")))
        assertEquals(1, dao.countByUuid("retry-event"))
    }
}
