package com.inkqilin.ledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RenQingContactDao {
    @Query("SELECT * FROM renqing_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<RenQingContact>>

    @Query("SELECT * FROM renqing_contacts WHERE id = :id")
    suspend fun getContactById(id: Long): RenQingContact?

    @Query("SELECT * FROM renqing_contacts WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchContacts(query: String): Flow<List<RenQingContact>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContact(contact: RenQingContact): Long

    @Update
    suspend fun updateContact(contact: RenQingContact)

    @Delete
    suspend fun deleteContact(contact: RenQingContact)

    @Query("DELETE FROM renqing_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
}
