package com.inkqilin.ledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.util.ThemeManager
import com.inkqilin.ledger.util.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val themeManager: ThemeManager
) : ViewModel() {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()
    val totalExpense: Flow<Double?> = transactionDao.getTotalExpense()

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val themeMode: StateFlow<ThemeMode> = themeManager.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemeMode.AUTO
    )

    init {
        viewModelScope.launch {
            if (categoryDao.getAllCategories().first().isEmpty()) {
                initializeDefaultCategories()
            }
        }
    }

    private suspend fun initializeDefaultCategories() {
        val defaults = listOf(
            Category(name = "餐饮", icon = "🍜", type = TransactionType.EXPENSE),
            Category(name = "交通", icon = "🚗", type = TransactionType.EXPENSE),
            Category(name = "购物", icon = "🛒", type = TransactionType.EXPENSE),
            Category(name = "娱乐", icon = "🎮", type = TransactionType.EXPENSE),
            Category(name = "居住", icon = "🏠", type = TransactionType.EXPENSE),
            Category(name = "其他", icon = "📦", type = TransactionType.EXPENSE),
            Category(name = "工资", icon = "💰", type = TransactionType.INCOME),
            Category(name = "奖金", icon = "🎁", type = TransactionType.INCOME),
            Category(name = "理财", icon = "📈", type = TransactionType.INCOME),
            Category(name = "其他", icon = "📦", type = TransactionType.INCOME)
        )
        defaults.forEach { categoryDao.insertCategory(it) }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.insertTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.updateTransaction(transaction)
        }
    }

    fun addCategory(name: String, icon: String, type: TransactionType) {
        viewModelScope.launch {
            categoryDao.insertCategory(Category(name = name, icon = icon, type = type))
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeManager.setThemeMode(mode)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.deleteTransaction(transaction)
        }
    }

    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query)
    }

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category)
    }

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type)
    }
}

class TransactionViewModelFactory(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val themeManager: ThemeManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(transactionDao, categoryDao, themeManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
