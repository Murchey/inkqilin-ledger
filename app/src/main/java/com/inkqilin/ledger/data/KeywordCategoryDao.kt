package com.inkqilin.ledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordCategoryDao {
    @Query("SELECT * FROM keyword_categories")
    fun getAllKeywordCategories(): Flow<List<KeywordCategory>>

    @Query("SELECT * FROM keyword_categories")
    suspend fun getAllKeywordCategoriesOnce(): List<KeywordCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywordCategory(keywordCategory: KeywordCategory)

    @Update
    suspend fun updateKeywordCategory(keywordCategory: KeywordCategory)

    @Delete
    suspend fun deleteKeywordCategory(keywordCategory: KeywordCategory)

    @Query("DELETE FROM keyword_categories")
    suspend fun deleteAll()
}