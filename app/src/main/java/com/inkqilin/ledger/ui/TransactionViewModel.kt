package com.inkqilin.ledger.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.util.ExcelImporter
import com.inkqilin.ledger.util.ThemeManager
import com.inkqilin.ledger.util.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

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

    fun addCategory(name: String, icon: String, type: TransactionType, color: String = "#715CFF") {
        viewModelScope.launch {
            categoryDao.insertCategory(Category(name = name, icon = icon, type = type, color = color))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }

    val incomeColor: StateFlow<String> = themeManager.incomeColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "#4CAF50"
    )
    val expenseColor: StateFlow<String> = themeManager.expenseColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "#F44336"
    )

    fun setIncomeColor(color: String) {
        viewModelScope.launch { themeManager.setIncomeColor(color) }
    }

    fun setExpenseColor(color: String) {
        viewModelScope.launch { themeManager.setExpenseColor(color) }
    }

    val renQingEnabled: StateFlow<Boolean> = themeManager.renQingEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    fun setRenQingEnabled(enabled: Boolean) {
        viewModelScope.launch { themeManager.setRenQingEnabled(enabled) }
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

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startTime, endTime)
    }

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

    fun importTransactions(context: Context, uri: Uri) {
        viewModelScope.launch {
            val existingCategories = allCategories.first()
            val result = ExcelImporter.importTransactionsFromUri(context, uri, existingCategories)
            
            // 插入新分类
            result.newCategories.forEach { category ->
                categoryDao.insertCategory(category)
            }
            
            // 插入所有账单
            result.transactions.forEach { transaction ->
                transactionDao.insertTransaction(transaction)
            }
        }
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
