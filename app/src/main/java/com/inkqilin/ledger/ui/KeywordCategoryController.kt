package com.inkqilin.ledger.ui

import com.inkqilin.ledger.data.KeywordCategory
import com.inkqilin.ledger.data.repository.KeywordCategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** 关键词分类映射。 */
class KeywordCategoryController(
    private val dao: KeywordCategoryRepository,
    private val scope: CoroutineScope,
) {
    val allKeywordCategories: Flow<List<KeywordCategory>> = dao.getAllKeywordCategories()

    fun Add(keyword: String, categoryName: String) {
        scope.launch {
            dao.insertKeywordCategory(KeywordCategory(keyword = keyword, categoryName = categoryName))
        }
    }

    fun Update(keywordCategory: KeywordCategory) {
        scope.launch { dao.updateKeywordCategory(keywordCategory) }
    }

    fun Delete(keywordCategory: KeywordCategory) {
        scope.launch { dao.deleteKeywordCategory(keywordCategory) }
    }

    suspend fun MatchCategoryByKeyword(note: String): String? {
        if (note.isBlank()) return null
        val keywords = dao.getAllKeywordCategoriesOnce()
        for (kc in keywords) {
            if (note.contains(kc.keyword, ignoreCase = true)) {
                return kc.categoryName
            }
        }
        return null
    }
}
