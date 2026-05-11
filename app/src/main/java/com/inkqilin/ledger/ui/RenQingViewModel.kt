package com.inkqilin.ledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class RenQingViewModel(
    private val contactDao: RenQingContactDao,
    private val eventDao: RenQingEventDao,
    private val tagDao: RenQingTagDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val themeManager: ThemeManager
) : ViewModel() {

    val allContacts: StateFlow<List<RenQingContact>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvents: StateFlow<List<RenQingEvent>> = eventDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<RenQingTag>> = tagDao.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val renQingEnabled: StateFlow<Boolean> = themeManager.renQingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dataLoaded: StateFlow<Boolean> = combine(allContacts, allEvents, allTags) { _, _, _ ->
        true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            if (tagDao.getAllTags().first().isEmpty()) {
                initializeDefaultTags()
            }
            initializeRenQingCategories()
        }
    }

    private suspend fun initializeDefaultTags() {
        val defaults = listOf(
            RenQingTag(name = "婚礼", icon = "\uD83D\uDC92", color = "#E91E63"),
            RenQingTag(name = "丧礼", icon = "\uD83D\uDE4F", color = "#607D8B"),
            RenQingTag(name = "生日", icon = "\uD83C\uDF82", color = "#FF9800"),
            RenQingTag(name = "乔迁", icon = "\uD83C\uDFE0", color = "#4CAF50"),
            RenQingTag(name = "升学", icon = "\uD83C\uDF93", color = "#2196F3"),
            RenQingTag(name = "满月", icon = "\uD83D\uDC76", color = "#9C27B0"),
            RenQingTag(name = "其他", icon = "\uD83C\uDF81", color = "#715CFF")
        )
        defaults.forEach { tagDao.insertTag(it) }
    }

    private suspend fun initializeRenQingCategories() {
        val existing = categoryDao.getAllCategories().first()
        if (existing.none { it.name == "人情收入" }) {
            categoryDao.insertCategory(Category(name = "人情收入", icon = "\u2764\uFE0F", type = TransactionType.INCOME, color = "#4CAF50"))
        }
        if (existing.none { it.name == "人情支出" }) {
            categoryDao.insertCategory(Category(name = "人情支出", icon = "\u2764\uFE0F", type = TransactionType.EXPENSE, color = "#F44336"))
        }
    }

    fun setRenQingEnabled(enabled: Boolean) {
        viewModelScope.launch { themeManager.setRenQingEnabled(enabled) }
    }

    fun addTag(tag: RenQingTag) {
        viewModelScope.launch { tagDao.insertTag(tag) }
    }

    fun updateTag(tag: RenQingTag) {
        viewModelScope.launch { tagDao.updateTag(tag) }
    }

    fun deleteTag(tag: RenQingTag) {
        viewModelScope.launch { tagDao.deleteTag(tag) }
    }

    fun addContact(contact: RenQingContact) {
        viewModelScope.launch { contactDao.insertContact(contact) }
    }

    fun updateContact(contact: RenQingContact) {
        viewModelScope.launch { contactDao.updateContact(contact) }
    }

    fun deleteContact(contact: RenQingContact) {
        viewModelScope.launch { contactDao.deleteContact(contact) }
    }

    fun getContactById(id: Long): Flow<RenQingContact?> = flow {
        emit(contactDao.getContactById(id))
    }

    fun getEventsByContact(contactId: Long): StateFlow<List<RenQingEvent>> =
        eventDao.getEventsByContact(contactId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEvent(event: RenQingEvent, syncToTransaction: Boolean = true) {
        viewModelScope.launch {
            eventDao.insertEvent(event)
            if (syncToTransaction) {
                syncEventToTransaction(event)
            }
        }
    }

    fun updateEvent(event: RenQingEvent) {
        viewModelScope.launch { eventDao.updateEvent(event) }
    }

    fun deleteEvent(event: RenQingEvent) {
        viewModelScope.launch { eventDao.deleteEvent(event) }
    }

    private suspend fun syncEventToTransaction(event: RenQingEvent) {
        val category = if (event.direction == RenQingDirection.RECEIVED) "人情收入" else "人情支出"
        val type = if (event.direction == RenQingDirection.RECEIVED) TransactionType.INCOME else TransactionType.EXPENSE
        val syncNote = "${event.contactName} · ${event.eventType.label}${if (event.tagName.isNotBlank() && event.tagName != event.eventType.label) " · ${event.tagName}" else ""}"
        transactionDao.insertTransaction(
            Transaction(
                amount = event.amount,
                category = category,
                note = syncNote,
                date = event.date,
                type = type
            )
        )
    }

    fun addRenQingEventFromTransaction(amount: Double, type: TransactionType, @Suppress("UNUSED_PARAMETER") category: String, note: String, date: Long, contactId: Long = 0, contactName: String = "") {
        viewModelScope.launch {
            val direction = if (type == TransactionType.INCOME) RenQingDirection.RECEIVED else RenQingDirection.GIVEN
            val event = RenQingEvent(
                contactId = contactId,
                contactName = contactName,
                eventType = RenQingEventType.OTHER,
                direction = direction,
                amount = amount,
                date = date,
                note = note
            )
            eventDao.insertEvent(event)
        }
    }

    fun searchEvents(query: String): StateFlow<List<RenQingEvent>> =
        eventDao.searchEvents(query)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getEventsByDateRange(startTime: Long, endTime: Long): StateFlow<List<RenQingEvent>> =
        eventDao.getEventsByDateRange(startTime, endTime)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTotalGiven(startTime: Long, endTime: Long): StateFlow<Double> =
        eventDao.getTotalGiven(startTime, endTime)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun getTotalReceived(startTime: Long, endTime: Long): StateFlow<Double> =
        eventDao.getTotalReceived(startTime, endTime)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun getYearRange(year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val end = cal.timeInMillis - 1
        return start to end
    }

    fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.MONTH, month + 1)
        val end = cal.timeInMillis - 1
        return start to end
    }

    class Factory(
        private val contactDao: RenQingContactDao,
        private val eventDao: RenQingEventDao,
        private val tagDao: RenQingTagDao,
        private val transactionDao: TransactionDao,
        private val categoryDao: CategoryDao,
        private val themeManager: ThemeManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RenQingViewModel::class.java)) {
                return RenQingViewModel(contactDao, eventDao, tagDao, transactionDao, categoryDao, themeManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
