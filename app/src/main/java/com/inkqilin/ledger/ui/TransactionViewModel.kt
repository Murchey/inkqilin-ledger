package com.inkqilin.ledger.ui

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.util.DEFAULT_EXPENSE_COLOR_HEX
import com.inkqilin.ledger.util.DEFAULT_INCOME_COLOR_HEX
import com.inkqilin.ledger.util.ExcelImporter
import com.inkqilin.ledger.util.ThemeManager
import com.inkqilin.ledger.util.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val currencyAssetDao: CurrencyAssetDao,
    private val albumPhotoDao: AlbumPhotoDao,
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

    val multiCurrencyEnabled: StateFlow<Boolean> = themeManager.multiCurrencyEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val allAssets: StateFlow<List<CurrencyAsset>> = currencyAssetDao.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAlbumInteracting = MutableStateFlow(false)
    val isAlbumInteracting: StateFlow<Boolean> = _isAlbumInteracting.asStateFlow()

    fun setAlbumInteracting(interacting: Boolean) {
        _isAlbumInteracting.value = interacting
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (categoryDao.getAllCategories().first().isEmpty()) {
                initializeDefaultCategories()
            }
            if (currencyAssetDao.getCount() == 0) {
                initializeDefaultCurrencies()
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

    private suspend fun initializeDefaultCurrencies() {
        val defaults = listOf(
            CurrencyAsset(code = "CNY", symbol = "¥", name = "人民币", cardColor = "#D32F2F", isDefault = true),
            CurrencyAsset(code = "USD", symbol = "$", name = "美元", cardColor = "#1565C0")
        )
        defaults.forEach { currencyAssetDao.insertAsset(it) }
    }

    fun setMultiCurrencyEnabled(enabled: Boolean) {
        viewModelScope.launch { themeManager.setMultiCurrencyEnabled(enabled) }
    }

    val monthlyBudget: StateFlow<Double> = themeManager.monthlyBudget.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0
    )

    fun setMonthlyBudget(amount: Double) {
        viewModelScope.launch { themeManager.setMonthlyBudget(amount) }
    }

    fun addCurrencyAsset(asset: CurrencyAsset) {
        viewModelScope.launch { currencyAssetDao.insertAsset(asset) }
    }

    fun updateCurrencyAsset(asset: CurrencyAsset) {
        viewModelScope.launch { currencyAssetDao.updateAsset(asset) }
    }

    fun deleteCurrencyAsset(asset: CurrencyAsset) {
        viewModelScope.launch { currencyAssetDao.deleteAsset(asset) }
    }

    fun getTotalIncomeByCurrency(currency: String): Flow<Double?> {
        return transactionDao.getTotalIncomeByCurrency(currency)
    }

    fun getTotalExpenseByCurrency(currency: String): Flow<Double?> {
        return transactionDao.getTotalExpenseByCurrency(currency)
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
        viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_INCOME_COLOR_HEX
    )
    val expenseColor: StateFlow<String> = themeManager.expenseColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_EXPENSE_COLOR_HEX
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

    val checkUpdateEnabled: StateFlow<Boolean> = themeManager.checkUpdateEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val customPrimaryColorHex: StateFlow<String?> = themeManager.customPrimaryColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val autoRecordEnabled: StateFlow<Boolean> = themeManager.autoRecordEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val ocrEnabled: StateFlow<Boolean> = themeManager.ocrEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val aiApiKey: StateFlow<String> = themeManager.aiApiKey.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val aiBaseUrl: StateFlow<String> = themeManager.aiBaseUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "https://api.openai.com/v1"
    )

    val aiModel: StateFlow<String> = themeManager.aiModel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "gpt-4o"
    )

    fun setCheckUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch { themeManager.setCheckUpdateEnabled(enabled) }
    }

    fun setCustomPrimaryColor(colorHex: String?) {
        viewModelScope.launch { themeManager.setCustomPrimaryColor(colorHex) }
    }

    fun setRenQingEnabled(enabled: Boolean) {
        viewModelScope.launch { themeManager.setRenQingEnabled(enabled) }
    }

    fun setAutoRecordEnabled(enabled: Boolean) {
        viewModelScope.launch { themeManager.setAutoRecordEnabled(enabled) }
    }

    fun setOcrEnabled(enabled: Boolean) {
        viewModelScope.launch { themeManager.setOcrEnabled(enabled) }
    }

    val albumEnabled: StateFlow<Boolean> = themeManager.albumEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    fun setAlbumEnabled(enabled: Boolean) {
        viewModelScope.launch { themeManager.setAlbumEnabled(enabled) }
    }

    val allAlbumPhotos: StateFlow<List<AlbumPhoto>> = albumPhotoDao.getAllPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAlbumPhoto(uri: String, note: String = "") {
        viewModelScope.launch {
            albumPhotoDao.insertPhoto(AlbumPhoto(uri = uri, note = note))
        }
    }

    fun updateAlbumPhoto(photo: AlbumPhoto) {
        viewModelScope.launch {
            albumPhotoDao.updatePhoto(photo)
        }
    }

    fun deleteAlbumPhoto(photo: AlbumPhoto) {
        viewModelScope.launch {
            albumPhotoDao.deletePhoto(photo)
        }
    }

    suspend fun getAlbumPhotoById(id: Long): AlbumPhoto? {
        return albumPhotoDao.getPhotoById(id)
    }

    fun setAiApiKey(apiKey: String) {
        viewModelScope.launch { themeManager.setAiApiKey(apiKey) }
    }

    fun setAiBaseUrl(baseUrl: String) {
        viewModelScope.launch { themeManager.setAiBaseUrl(baseUrl) }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch { themeManager.setAiModel(model) }
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
            
            result.newCategories.forEach { category ->
                categoryDao.insertCategory(category)
            }
            
            result.transactions.forEach { transaction ->
                transactionDao.insertTransaction(transaction)
            }
        }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }
}

class TransactionViewModelFactory(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val currencyAssetDao: CurrencyAssetDao,
    private val albumPhotoDao: AlbumPhotoDao,
    private val themeManager: ThemeManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(transactionDao, categoryDao, currencyAssetDao, albumPhotoDao, themeManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
