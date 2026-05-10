package com.inkqilin.ledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RenQingTagDao {
    @Query("SELECT * FROM renqing_tags ORDER BY id ASC")
    fun getAllTags(): Flow<List<RenQingTag>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: RenQingTag): Long

    @Update
    suspend fun updateTag(tag: RenQingTag)

    @Delete
    suspend fun deleteTag(tag: RenQingTag)
}
