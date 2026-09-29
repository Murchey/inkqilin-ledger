package com.inkqilin.ledger.data.repository

import com.inkqilin.ledger.data.KeywordCategory
import com.inkqilin.ledger.data.KeywordCategoryDao
import kotlinx.coroutines.flow.Flow

class KeywordCategoryRepository(private val dao: KeywordCategoryDao) {
    fun getAllKeywordCategories(): Flow<List<KeywordCategory>> = dao.getAllKeywordCategories()
    suspend fun getAllKeywordCategoriesOnce(): List<KeywordCategory> = dao.getAllKeywordCategoriesOnce()
    suspend fun insertKeywordCategory(keywordCategory: KeywordCategory) =
        dao.insertKeywordCategory(keywordCategory)

    suspend fun updateKeywordCategory(keywordCategory: KeywordCategory) =
        dao.updateKeywordCategory(keywordCategory)

    suspend fun deleteKeywordCategory(keywordCategory: KeywordCategory) =
        dao.deleteKeywordCategory(keywordCategory)

    suspend fun deleteAll() = dao.deleteAll()
}
