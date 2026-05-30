package com.inkqilin.ledger.ui

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.service.AIAnalysisService
import com.inkqilin.ledger.service.AiAlert
import com.inkqilin.ledger.service.AiAnalysisResult
import com.inkqilin.ledger.util.DEFAULT_EXPENSE_COLOR_HEX
import com.inkqilin.ledger.util.DEFAULT_INCOME_COLOR_HEX
import com.inkqilin.ledger.util.ExcelImporter
import com.inkqilin.ledger.util.AiDataRange
import com.inkqilin.ledger.util.AppMode
import com.inkqilin.ledger.util.ThemeManager
import com.inkqilin.ledger.util.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class TransactionViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val currencyAssetDao: CurrencyAssetDao,
    private val albumPhotoDao: AlbumPhotoDao,
    private val keywordCategoryDao: KeywordCategoryDao,
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
            if (keywordCategoryDao.getAllKeywordCategoriesOnce().isEmpty()) {
                initializeDefaultKeywordCategories()
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
            CurrencyAsset(code = "CNY", symbol = "¥", name = "人民币", cardColor = "#43A047", isDefault = true),
            CurrencyAsset(code = "USD", symbol = "$", name = "美元", cardColor = "#1565C0")
        )
        defaults.forEach { currencyAssetDao.insertAsset(it) }
    }

    private suspend fun initializeDefaultKeywordCategories() {
        val defaults = listOf(
            KeywordCategory(keyword = "外卖", categoryName = "餐饮"),
            KeywordCategory(keyword = "饿了么", categoryName = "餐饮"),
            KeywordCategory(keyword = "美团", categoryName = "餐饮"),
            KeywordCategory(keyword = "餐厅", categoryName = "餐饮"),
            KeywordCategory(keyword = "咖啡", categoryName = "餐饮"),
            KeywordCategory(keyword = "奶茶", categoryName = "餐饮"),
            KeywordCategory(keyword = "肯德基", categoryName = "餐饮"),
            KeywordCategory(keyword = "麦当劳", categoryName = "餐饮"),
            KeywordCategory(keyword = "星巴克", categoryName = "餐饮"),
            KeywordCategory(keyword = "瑞幸", categoryName = "餐饮"),
            KeywordCategory(keyword = "食堂", categoryName = "餐饮"),
            KeywordCategory(keyword = "公交", categoryName = "交通"),
            KeywordCategory(keyword = "地铁", categoryName = "交通"),
            KeywordCategory(keyword = "滴滴", categoryName = "交通"),
            KeywordCategory(keyword = "出租车", categoryName = "交通"),
            KeywordCategory(keyword = "加油", categoryName = "交通"),
            KeywordCategory(keyword = "高铁", categoryName = "交通"),
            KeywordCategory(keyword = "机票", categoryName = "交通"),
            KeywordCategory(keyword = "12306", categoryName = "交通"),
            KeywordCategory(keyword = "哈啰", categoryName = "交通"),
            KeywordCategory(keyword = "单车", categoryName = "交通"),
            KeywordCategory(keyword = "超市", categoryName = "购物"),
            KeywordCategory(keyword = "淘宝", categoryName = "购物"),
            KeywordCategory(keyword = "京东", categoryName = "购物"),
            KeywordCategory(keyword = "拼多多", categoryName = "购物"),
            KeywordCategory(keyword = "便利店", categoryName = "购物"),
            KeywordCategory(keyword = "天猫", categoryName = "购物"),
            KeywordCategory(keyword = "盒马", categoryName = "购物"),
            KeywordCategory(keyword = "唯品会", categoryName = "购物"),
            KeywordCategory(keyword = "沃尔玛", categoryName = "购物"),
            KeywordCategory(keyword = "电影", categoryName = "娱乐"),
            KeywordCategory(keyword = "游戏", categoryName = "娱乐"),
            KeywordCategory(keyword = "KTV", categoryName = "娱乐"),
            KeywordCategory(keyword = "网易云", categoryName = "娱乐"),
            KeywordCategory(keyword = "腾讯视频", categoryName = "娱乐"),
            KeywordCategory(keyword = "爱奇艺", categoryName = "娱乐"),
            KeywordCategory(keyword = "B站", categoryName = "娱乐"),
            KeywordCategory(keyword = "房租", categoryName = "居住"),
            KeywordCategory(keyword = "水电", categoryName = "居住"),
            KeywordCategory(keyword = "物业", categoryName = "居住"),
            KeywordCategory(keyword = "煤气", categoryName = "居住"),
            KeywordCategory(keyword = "燃气", categoryName = "居住"),
            KeywordCategory(keyword = "供暖", categoryName = "居住"),
            KeywordCategory(keyword = "薪水", categoryName = "工资"),
            KeywordCategory(keyword = "转账", categoryName = "工资"),
            KeywordCategory(keyword = "分红", categoryName = "工资"),
            KeywordCategory(keyword = "基金", categoryName = "理财"),
            KeywordCategory(keyword = "股票", categoryName = "理财"),
            KeywordCategory(keyword = "收益", categoryName = "理财"),
            KeywordCategory(keyword = "利息", categoryName = "理财"),
            KeywordCategory(keyword = "余额宝", categoryName = "理财"),
            KeywordCategory(keyword = "零钱通", categoryName = "理财")
        )
        defaults.forEach { keywordCategoryDao.insertKeywordCategory(it) }
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

    val ocrApiKey: StateFlow<String> = themeManager.ocrApiKey.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val ocrBaseUrl: StateFlow<String> = themeManager.ocrBaseUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "https://api.openai.com/v1"
    )

    val ocrModel: StateFlow<String> = themeManager.ocrModel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "gpt-4o"
    )

    fun setOcrApiKey(apiKey: String) {
        viewModelScope.launch { themeManager.setOcrApiKey(apiKey) }
    }

    fun setOcrBaseUrl(baseUrl: String) {
        viewModelScope.launch { themeManager.setOcrBaseUrl(baseUrl) }
    }

    fun setOcrModel(model: String) {
        viewModelScope.launch { themeManager.setOcrModel(model) }
    }

    val appMode: StateFlow<AppMode> = themeManager.appMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppMode.BASIC
    )

    fun setAppMode(mode: AppMode) {
        viewModelScope.launch { themeManager.setAppMode(mode) }
    }

    val aiDataRange: StateFlow<AiDataRange> = themeManager.aiDataRange.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AiDataRange.THIS_WEEK_AND_LAST
    )

    fun setAiDataRange(range: AiDataRange) {
        viewModelScope.launch { themeManager.setAiDataRange(range) }
    }

    private val _aiAnalysisResult = MutableStateFlow<AiAnalysisResult?>(null)
    val aiAnalysisResult: StateFlow<AiAnalysisResult?> = _aiAnalysisResult.asStateFlow()

    private val _aiAnalysisLoading = MutableStateFlow(false)
    val aiAnalysisLoading: StateFlow<Boolean> = _aiAnalysisLoading.asStateFlow()

    private val _aiAnalysisFailed = MutableStateFlow(false)
    val aiAnalysisFailed: StateFlow<Boolean> = _aiAnalysisFailed.asStateFlow()

    init {
        loadCachedAiResult()
    }

    private fun loadCachedAiResult() {
        viewModelScope.launch {
            val score = themeManager.aiScore.first()
            val label = themeManager.aiScoreLabel.first()
            val explanation = themeManager.aiScoreExplanation.first()
            val alertsJson = themeManager.aiAlertsJson.first()
            val failed = themeManager.aiAnalysisFailed.first()

            if (score != null) {
                _aiAnalysisResult.value = AiAnalysisResult(
                    score = score,
                    scoreLabel = label,
                    scoreExplanation = explanation,
                    alerts = AIAnalysisService.deserializeAlerts(alertsJson)
                )
            }
            _aiAnalysisFailed.value = failed
        }
    }

    fun checkAndRunDailyAnalysis() {
        viewModelScope.launch {
            val mode = appMode.value
            if (mode != AppMode.SMART) return@launch

            val apiKey = aiApiKey.value
            if (apiKey.isBlank()) return@launch

            val lastDate = themeManager.aiLastAnalysisDate.first()
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (lastDate >= today) return@launch

            runAiAnalysis()
        }
    }

    fun runAiAnalysis() {
        if (_aiAnalysisLoading.value) return
        viewModelScope.launch {
            val apiKey = aiApiKey.value
            val baseUrl = aiBaseUrl.value
            val model = aiModel.value
            if (apiKey.isBlank()) {
                _aiAnalysisFailed.value = true
                return@launch
            }

            _aiAnalysisLoading.value = true
            _aiAnalysisFailed.value = false

            try {
                val transactions = allTransactions.first()
                val dataRange = aiDataRange.value
                val result = AIAnalysisService.analyze(transactions, dataRange, apiKey, baseUrl, model)
                val alertsJson = AIAnalysisService.serializeAlerts(result.alerts)
                themeManager.saveAiAnalysisResult(result.score, result.scoreLabel, result.scoreExplanation, alertsJson)
                _aiAnalysisResult.value = result
                _aiAnalysisFailed.value = false
            } catch (_: Exception) {
                themeManager.markAiAnalysisFailed()
                _aiAnalysisFailed.value = true
            } finally {
                _aiAnalysisLoading.value = false
            }
        }
    }

    val recentNotes: StateFlow<List<String>> = themeManager.recentNotes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun addRecentNote(note: String) {
        viewModelScope.launch { themeManager.addRecentNote(note) }
    }

    fun clearRecentNotes() {
        viewModelScope.launch { themeManager.clearRecentNotes() }
    }

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

    data class CleanupResult(val deletedCount: Int, val freedBytes: Long)

    suspend fun cleanupOrphanedAlbumFiles(context: Context): CleanupResult {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val imageDir = File(context.filesDir, "album_photos")
            if (!imageDir.exists()) return@withContext CleanupResult(0, 0)

            val dbUris = albumPhotoDao.getAllPhotosOnce().map { photo ->
                Uri.parse(photo.uri).path
            }.toSet()

            var deletedCount = 0
            var freedBytes = 0L

            imageDir.listFiles()?.forEach { file ->
                val filePath = file.absolutePath
                if (filePath !in dbUris) {
                    freedBytes += file.length()
                    if (file.delete()) deletedCount++
                }
            }

            CleanupResult(deletedCount, freedBytes)
        }
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

    val allKeywordCategories: Flow<List<KeywordCategory>> = keywordCategoryDao.getAllKeywordCategories()

    fun addKeywordCategory(keyword: String, categoryName: String) {
        viewModelScope.launch {
            keywordCategoryDao.insertKeywordCategory(KeywordCategory(keyword = keyword, categoryName = categoryName))
        }
    }

    fun updateKeywordCategory(keywordCategory: KeywordCategory) {
        viewModelScope.launch {
            keywordCategoryDao.updateKeywordCategory(keywordCategory)
        }
    }

    fun deleteKeywordCategory(keywordCategory: KeywordCategory) {
        viewModelScope.launch {
            keywordCategoryDao.deleteKeywordCategory(keywordCategory)
        }
    }

    suspend fun matchCategoryByKeyword(note: String): String? {
        if (note.isBlank()) return null
        val keywords = keywordCategoryDao.getAllKeywordCategoriesOnce()
        for (kc in keywords) {
            if (note.contains(kc.keyword, ignoreCase = true)) {
                return kc.categoryName
            }
        }
        return null
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
    private val keywordCategoryDao: KeywordCategoryDao,
    private val themeManager: ThemeManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(transactionDao, categoryDao, currencyAssetDao, albumPhotoDao, keywordCategoryDao, themeManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
