package com.inkqilin.ledger.data.repository

import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.CategoryDao
import com.inkqilin.ledger.data.TransactionType
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {
    fun getAllCategories(): Flow<List<Category>> = dao.getAllCategories()
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> = dao.getCategoriesByType(type)
    suspend fun insertCategory(category: Category) = dao.insertCategory(category)
    suspend fun updateCategory(category: Category) = dao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = dao.deleteCategory(category)
    suspend fun getCategoriesByTypeSync(type: TransactionType): List<Category> =
        dao.getCategoriesByTypeSync(type)
}
