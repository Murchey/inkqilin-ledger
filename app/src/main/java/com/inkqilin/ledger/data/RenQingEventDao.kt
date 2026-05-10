package com.inkqilin.ledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RenQingEventDao {
    @Query("SELECT * FROM renqing_events ORDER BY date DESC")
    fun getAllEvents(): Flow<List<RenQingEvent>>

    @Query("SELECT * FROM renqing_events WHERE contactId = :contactId ORDER BY date DESC")
    fun getEventsByContact(contactId: Long): Flow<List<RenQingEvent>>

    @Query("SELECT * FROM renqing_events WHERE eventType = :type ORDER BY date DESC")
    fun getEventsByType(type: RenQingEventType): Flow<List<RenQingEvent>>

    @Query("SELECT * FROM renqing_events WHERE direction = :direction ORDER BY date DESC")
    fun getEventsByDirection(direction: RenQingDirection): Flow<List<RenQingEvent>>

    @Query("""
        SELECT * FROM renqing_events 
        WHERE date BETWEEN :startTime AND :endTime 
        ORDER BY date DESC
    """)
    fun getEventsByDateRange(startTime: Long, endTime: Long): Flow<List<RenQingEvent>>

    @Query("""
        SELECT * FROM renqing_events 
        WHERE (contactName LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%')
        ORDER BY date DESC
    """)
    fun searchEvents(query: String): Flow<List<RenQingEvent>>

    @Query("SELECT SUM(amount) FROM renqing_events WHERE direction = 'GIVEN' AND date BETWEEN :startTime AND :endTime")
    fun getTotalGiven(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM renqing_events WHERE direction = 'RECEIVED' AND date BETWEEN :startTime AND :endTime")
    fun getTotalReceived(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM renqing_events WHERE direction = :direction AND eventType = :type AND date BETWEEN :startTime AND :endTime")
    fun getTotalByDirectionAndType(direction: RenQingDirection, type: RenQingEventType, startTime: Long, endTime: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: RenQingEvent): Long

    @Update
    suspend fun updateEvent(event: RenQingEvent)

    @Delete
    suspend fun deleteEvent(event: RenQingEvent)

    @Query("DELETE FROM renqing_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
}
