package com.inkqilin.ledger.data.repository

import com.inkqilin.ledger.data.RenQingContact
import com.inkqilin.ledger.data.RenQingContactDao
import com.inkqilin.ledger.data.RenQingDirection
import com.inkqilin.ledger.data.RenQingEvent
import com.inkqilin.ledger.data.RenQingEventDao
import com.inkqilin.ledger.data.RenQingEventType
import com.inkqilin.ledger.data.RenQingTag
import com.inkqilin.ledger.data.RenQingTagDao
import kotlinx.coroutines.flow.Flow

class RenQingRepository(
    private val contactDao: RenQingContactDao,
    private val eventDao: RenQingEventDao,
    private val tagDao: RenQingTagDao,
) {
    fun getAllContacts(): Flow<List<RenQingContact>> = contactDao.getAllContacts()
    suspend fun getContactById(id: Long): RenQingContact? = contactDao.getContactById(id)
    fun searchContacts(query: String): Flow<List<RenQingContact>> = contactDao.searchContacts(query)
    suspend fun insertContact(contact: RenQingContact): Long = contactDao.insertContact(contact)
    suspend fun updateContact(contact: RenQingContact) = contactDao.updateContact(contact)
    suspend fun deleteContact(contact: RenQingContact) = contactDao.deleteContact(contact)
    suspend fun deleteContactById(id: Long) = contactDao.deleteContactById(id)

    fun getAllEvents(): Flow<List<RenQingEvent>> = eventDao.getAllEvents()
    fun getEventsByContact(contactId: Long): Flow<List<RenQingEvent>> = eventDao.getEventsByContact(contactId)
    fun getEventsByType(type: RenQingEventType): Flow<List<RenQingEvent>> = eventDao.getEventsByType(type)
    fun getEventsByDirection(direction: RenQingDirection): Flow<List<RenQingEvent>> =
        eventDao.getEventsByDirection(direction)

    fun getEventsByDateRange(startTime: Long, endTime: Long): Flow<List<RenQingEvent>> =
        eventDao.getEventsByDateRange(startTime, endTime)

    fun searchEvents(query: String): Flow<List<RenQingEvent>> = eventDao.searchEvents(query)
    fun getTotalGiven(startTime: Long, endTime: Long): Flow<Double?> = eventDao.getTotalGiven(startTime, endTime)
    fun getTotalReceived(startTime: Long, endTime: Long): Flow<Double?> =
        eventDao.getTotalReceived(startTime, endTime)

    fun getTotalByDirectionAndType(
        direction: RenQingDirection,
        type: RenQingEventType,
        startTime: Long,
        endTime: Long,
    ): Flow<Double?> = eventDao.getTotalByDirectionAndType(direction, type, startTime, endTime)

    suspend fun insertEvent(event: RenQingEvent): Long = eventDao.insertEvent(event)
    suspend fun updateEvent(event: RenQingEvent) = eventDao.updateEvent(event)
    suspend fun deleteEvent(event: RenQingEvent) = eventDao.deleteEvent(event)
    suspend fun deleteEventById(id: Long) = eventDao.deleteEventById(id)

    fun getAllTags(): Flow<List<RenQingTag>> = tagDao.getAllTags()
    suspend fun insertTag(tag: RenQingTag): Long = tagDao.insertTag(tag)
    suspend fun updateTag(tag: RenQingTag) = tagDao.updateTag(tag)
    suspend fun deleteTag(tag: RenQingTag) = tagDao.deleteTag(tag)
}
