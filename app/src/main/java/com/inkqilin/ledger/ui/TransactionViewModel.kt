package com.inkqilin.ledger.ui

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inkqilin.ledger.LedgerApplication
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
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class TransactionViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val currencyAssetDao: CurrencyAssetDao,
    private val albumPhotoDao: AlbumPhotoDao,
    private val keywordCategoryDao: KeywordCategoryDao,
    private val userAssetDao: UserAssetDao,
    private val assetFlowDao: AssetFlowDao,
    private val themeManager: ThemeManager
) : ViewModel() {
    // Eagerly 保持缓存：子页面返回时首帧就能拿到完整账单，避免空列表闪断导致滚动位置丢失
    val allTransactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
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

    // 导航进入编辑页前缓存账单，避免首帧等 Flow 导致动画期间空白
    private val _pendingEditTransaction = MutableStateFlow<Transaction?>(null)
    val pendingEditTransaction: StateFlow<Transaction?> = _pendingEditTransaction.asStateFlow()

    fun setPendingEditTransaction(transaction: Transaction?) {
        _pendingEditTransaction.value = transaction
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // 本版配色改版：仅升级后首次启动重置一次默认色；失败不影响业务数据
            runCatching { themeManager.migrateColorPaletteOnce() }
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

    // ── 桌面小组件设置 ──
    val widgetShowAmount: StateFlow<Boolean> = themeManager.widgetShowAmount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val widgetQuickCategories: StateFlow<List<String>> = themeManager.widgetQuickCategories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("餐饮", "交通", "购物", "娱乐")
    )

    fun setWidgetShowAmount(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setWidgetShowAmount(enabled)
            notifyWidgets()
        }
    }

    fun setWidgetQuickCategories(categories: List<String>) {
        viewModelScope.launch {
            themeManager.setWidgetQuickCategories(categories)
            notifyWidgets()
        }
    }

    /** 记账/周期账单数据变化后，通知桌面小部件刷新（未挂载时零开销） */
    private fun notifyWidgets() {
        runCatching { LedgerApplication.refreshWidgets() }
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
            val tx = if (transaction.uuid == null) transaction.copy(uuid = java.util.UUID.randomUUID().toString()) else transaction
            transactionDao.insertTransaction(tx)
            notifyWidgets()
        }
    }

    /** 为所有缺少 UUID 的存量交易自动生成并填充 UUID */
    fun backfillTransactionUuids() {
        viewModelScope.launch(Dispatchers.IO) {
            val nulls = transactionDao.getTransactionsWithoutUuid()
            if (nulls.isNotEmpty()) {
                val updated = nulls.map { it.copy(uuid = java.util.UUID.randomUUID().toString()) }
                transactionDao.updateTransactions(updated)
            }
        }
    }

    /** 导入时使用：有UUID则跳过重复，无UUID则自动生成后插入。返回 true 表示实际插入 */
    suspend fun importTransactionSkipDuplicates(transaction: Transaction): Boolean {
        if (transaction.uuid != null) {
            val count = transactionDao.countByUuid(transaction.uuid!!)
            if (count == 0) {
                transactionDao.insertTransactionIgnore(transaction)
                return true
            }
            return false
        } else {
            // 无UUID的条目自动生成UUID后插入（保证后续再导入可去重）
            val tx = transaction.copy(uuid = java.util.UUID.randomUUID().toString())
            transactionDao.insertTransaction(tx)
            return true
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.updateTransaction(transaction)
            notifyWidgets()
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

    val updateProxyUrl: StateFlow<String> = themeManager.updateProxyUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        com.inkqilin.ledger.util.PROXY_SOURCES.first()
    )

    val updateRepo: StateFlow<String> = themeManager.updateRepo.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        com.inkqilin.ledger.util.DEFAULT_UPDATE_REPO
    )

    val githubRepo: StateFlow<String> = themeManager.githubRepo.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        com.inkqilin.ledger.util.DEFAULT_GITHUB_REPO
    )

    fun setUpdateRepo(repo: String) {
        viewModelScope.launch { themeManager.setUpdateRepo(repo) }
    }

    fun setGithubRepo(repo: String) {
        viewModelScope.launch { themeManager.setGithubRepo(repo) }
    }

    val cosConfig: StateFlow<com.inkqilin.ledger.util.CosConfig> =
        themeManager.cosConfig.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.inkqilin.ledger.util.CosConfig()
        )

    fun setCosConfig(config: com.inkqilin.ledger.util.CosConfig) {
        viewModelScope.launch { themeManager.setCosConfig(config) }
    }

    /** 设置页「检查更新」→ MainActivity 弹出更新对话框 */
    private val _manualUpdateCheckTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val manualUpdateCheckTrigger: SharedFlow<Unit> = _manualUpdateCheckTrigger.asSharedFlow()

    fun triggerManualUpdateCheck() {
        viewModelScope.launch {
            _manualUpdateCheckTrigger.emit(Unit)
        }
    }

    val customPrimaryColorHex: StateFlow<String?> = themeManager.customPrimaryColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val homeCardColor: StateFlow<String?> = themeManager.homeCardColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val homeBgImagePath: StateFlow<String?> = themeManager.homeBgImagePath.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val homeBgOpacity: StateFlow<Float> = themeManager.homeBgOpacity.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.35f
    )

    val homeTxCardOpacity: StateFlow<Float> = themeManager.homeTxCardOpacity.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.72f
    )

    fun setHomeBgImagePath(path: String?) {
        viewModelScope.launch { themeManager.setHomeBgImagePath(path) }
    }

    fun setHomeBgOpacity(opacity: Float) {
        viewModelScope.launch { themeManager.setHomeBgOpacity(opacity) }
    }

    fun setHomeTxCardOpacity(opacity: Float) {
        viewModelScope.launch { themeManager.setHomeTxCardOpacity(opacity) }
    }

    /** 将用户选择的图片复制到应用私有目录，避免 content URI 失效 */
    fun importHomeBackground(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val target = java.io.File(context.filesDir, "home_bg_image")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("无法读取图片")
                }
                setHomeBgImagePath(target.absolutePath)
            } catch (e: Exception) {
                Log.e("HomeBg", "导入背景失败", e)
            }
        }
    }

    fun clearHomeBackground() {
        viewModelScope.launch {
            val path = homeBgImagePath.value
            setHomeBgImagePath(null)
            if (!path.isNullOrBlank()) {
                runCatching { java.io.File(path).delete() }
            }
        }
    }

    val autoRecordEnabled: StateFlow<Boolean> = themeManager.autoRecordEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    /** 鸿蒙/卓易通兼容模式：自动记账降级（拍照已统一系统相机） */
    val harmonyCompatMode: StateFlow<Boolean> = themeManager.harmonyCompatMode.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    /** 隐私政策是否已确认 */
    val privacyAccepted: StateFlow<Boolean> = themeManager.privacyAccepted.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )
    private val _privacyAcceptedLoaded = MutableStateFlow(false)
    val privacyAcceptedLoaded: StateFlow<Boolean> = _privacyAcceptedLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            themeManager.privacyAccepted.collect { _privacyAcceptedLoaded.value = true }
        }
    }

    fun acceptPrivacyPolicy() {
        viewModelScope.launch { themeManager.setPrivacyAccepted() }
    }

    /** 是否已完成「是否鸿蒙」询问 */
    val harmonyCompatAsked: StateFlow<Boolean> = themeManager.harmonyCompatAsked.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    /** DataStore 真值是否已读入（首帧假值防护） */
    private val _harmonyAskedLoaded = MutableStateFlow(false)
    val harmonyAskedLoaded: StateFlow<Boolean> = _harmonyAskedLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            themeManager.harmonyCompatAsked.collect {
                _harmonyAskedLoaded.value = true
            }
        }
    }

    fun setHarmonyCompatMode(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setHarmonyCompatMode(enabled)
        }
    }

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

    fun setUpdateProxyUrl(url: String) {
        viewModelScope.launch { themeManager.setUpdateProxyUrl(url) }
    }

    fun setCustomPrimaryColor(colorHex: String?) {
        viewModelScope.launch { themeManager.setCustomPrimaryColor(colorHex) }
    }

    fun setHomeCardColor(colorHex: String?) {
        viewModelScope.launch { themeManager.setHomeCardColor(colorHex) }
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
        if (appMode.value != AppMode.SMART) return
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
            try {
                albumPhotoDao.insertPhoto(AlbumPhoto(uri = uri, note = note))
            } catch (t: Throwable) {
                Log.e("Album", "保存照片记录失败", t)
            }
        }
    }

    fun updateAlbumPhoto(photo: AlbumPhoto) {
        viewModelScope.launch {
            albumPhotoDao.updatePhoto(photo)
        }
    }

    fun deleteAlbumPhoto(photo: AlbumPhoto) {
        viewModelScope.launch {
            // 先删除数据库记录
            albumPhotoDao.deletePhoto(photo)
            // 同步删除磁盘文件
            try {
                val path = Uri.parse(photo.uri).path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                        Log.d("TransactionVM", "Deleted photo file: $path")
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionVM", "Failed to delete photo file: ${photo.uri}", e)
            }
        }
    }

    suspend fun getAlbumPhotoById(id: Long): AlbumPhoto? {
        return albumPhotoDao.getPhotoById(id)
    }

    /** 清空记账相册：删文件 + 清数据库 */
    suspend fun clearAllAlbumPhotos(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            com.inkqilin.ledger.util.StorageUsageManager.clearDir(
                java.io.File(context.filesDir, "album_photos")
            )
            albumPhotoDao.deleteAllPhotos()
            true
        } catch (e: Exception) {
            Log.e("TransactionVM", "clear album failed", e)
            false
        }
    }

    /** 清空本地备份目录（含 pre_restore） */
    suspend fun clearLocalBackups(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            com.inkqilin.ledger.util.StorageUsageManager.clearDir(
                com.inkqilin.ledger.util.CloudBackupManager.localBackupDir(context)
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    val allUserAssets: StateFlow<List<UserAsset>> = userAssetDao.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userAssetTotalValue: StateFlow<Double> = userAssetDao.getTotalValue()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addUserAsset(asset: UserAsset) {
        viewModelScope.launch { userAssetDao.insertAsset(asset) }
    }

    fun updateUserAsset(asset: UserAsset) {
        viewModelScope.launch { userAssetDao.updateAsset(asset.copy(lastUpdated = System.currentTimeMillis())) }
    }

    fun deleteUserAsset(asset: UserAsset) {
        viewModelScope.launch { userAssetDao.deleteAsset(asset) }
    }

    // --- AssetFlow ---
    val allAssetFlows: StateFlow<List<AssetFlow>> = assetFlowDao.getAllFlows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAssetFlows(assetId: Long): Flow<List<AssetFlow>> =
        assetFlowDao.getFlowsByAssetId(assetId)

    fun addAssetFlow(flow: AssetFlow) {
        viewModelScope.launch {
            val f = if (flow.uuid == null) flow.copy(uuid = java.util.UUID.randomUUID().toString()) else flow
            assetFlowDao.insertFlow(f)
            // 同步更新资产的当前估值
            userAssetDao.getAssetById(f.assetId)?.let { asset ->
                userAssetDao.updateAsset(asset.copy(
                    currentValue = f.newValue,
                    lastUpdated = System.currentTimeMillis()
                ))
            }
        }
    }

    /** 导入流转时去重 */
    suspend fun importAssetFlowSkipDuplicates(flow: AssetFlow): Boolean {
        if (flow.uuid != null) {
            val count = assetFlowDao.countByUuid(flow.uuid!!)
            if (count == 0) {
                assetFlowDao.insertFlowIgnore(flow)
                userAssetDao.getAssetById(flow.assetId)?.let { asset ->
                    userAssetDao.updateAsset(asset.copy(
                        currentValue = flow.newValue,
                        lastUpdated = System.currentTimeMillis()
                    ))
                }
                return true
            }
            return false
        } else {
            val f = flow.copy(uuid = java.util.UUID.randomUUID().toString())
            assetFlowDao.insertFlow(f)
            userAssetDao.getAssetById(f.assetId)?.let { asset ->
                userAssetDao.updateAsset(asset.copy(
                    currentValue = f.newValue,
                    lastUpdated = System.currentTimeMillis()
                ))
            }
            return true
        }
    }

    /** 为存量流转记录补齐 UUID */
    fun backfillAssetFlowUuids() {
        viewModelScope.launch(Dispatchers.IO) {
            val nulls = assetFlowDao.getFlowsWithoutUuid()
            if (nulls.isNotEmpty()) {
                val updated = nulls.map { it.copy(uuid = java.util.UUID.randomUUID().toString()) }
                assetFlowDao.updateFlows(updated)
            }
        }
    }

    fun updateAssetFlow(flow: AssetFlow) {
        viewModelScope.launch {
            assetFlowDao.updateFlow(flow)
            // 同步更新资产的当前估值
            userAssetDao.getAssetById(flow.assetId)?.let { asset ->
                userAssetDao.updateAsset(asset.copy(
                    currentValue = flow.newValue,
                    lastUpdated = System.currentTimeMillis()
                ))
            }
        }
    }

    fun deleteAssetFlow(flow: AssetFlow) {
        viewModelScope.launch {
            assetFlowDao.deleteFlow(flow)
            // 删除后，找到该资产最新的流转记录，以其 newValue 更新资产估值
            val latestFlow = assetFlowDao.getLatestFlowByAssetId(flow.assetId)
            userAssetDao.getAssetById(flow.assetId)?.let { asset ->
                userAssetDao.updateAsset(asset.copy(
                    currentValue = latestFlow?.newValue ?: asset.currentValue,
                    lastUpdated = System.currentTimeMillis()
                ))
            }
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
            // Room @Delete 为硬删除；库已开启 PRAGMA secure_delete，行内容会被覆盖
            transactionDao.deleteTransaction(transaction)
            notifyWidgets()
        }
    }

    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query)
    }

    /** 搜索聚合：支出/收入合计（一次 SQL），供搜索结果头部展示 */
    fun searchSummary(query: String): Flow<SearchSummary> {
        return transactionDao.searchSummary(query)
    }

    /**
     * 支持日期范围的搜索。
     * @param start/end 为 null 时退化为纯关键词搜索；query 为空且有日期时只按日期筛选。
     */
    fun searchTransactionsFiltered(query: String, start: Long?, end: Long?): Flow<List<Transaction>> {
        val q = query.trim()
        return if (start != null && end != null) {
            transactionDao.searchTransactionsFiltered(q, start, end)
        } else if (q.isBlank()) {
            flowOf(emptyList())
        } else {
            transactionDao.searchTransactions(q)
        }
    }

    fun searchSummaryFiltered(query: String, start: Long?, end: Long?): Flow<SearchSummary> {
        val q = query.trim()
        return if (start != null && end != null) {
            transactionDao.searchSummaryFiltered(q, start, end)
        } else if (q.isBlank()) {
            flowOf(SearchSummary(0.0, 0.0))
        } else {
            transactionDao.searchSummary(q)
        }
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

    /** Excel 导入进度：fraction 0–1 + 描述；null = 未在导入 */
    private val _excelProgress = MutableStateFlow<Pair<Float, String>?>(null)
    val excelProgress: StateFlow<Pair<Float, String>?> = _excelProgress.asStateFlow()

    fun importTransactions(
        context: Context,
        uri: Uri,
        onDone: ((imported: Int, newCats: Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _excelProgress.value = 0f to "开始导入…"
            try {
                val existingCategories = allCategories.first()
                val result = withContext(Dispatchers.IO) {
                    ExcelImporter.importTransactionsFromUri(context, uri, existingCategories) { fraction, msg ->
                        _excelProgress.value = fraction.coerceIn(0f, 1f) to msg
                    }
                }
                _excelProgress.value = 0.96f to "写入数据库…"
                result.newCategories.forEach { category ->
                    categoryDao.insertCategory(category)
                }
                result.transactions.forEach { transaction ->
                    transactionDao.insertTransaction(transaction)
                }
                notifyWidgets()
                _excelProgress.value = 1f to "导入完成"
                onDone?.invoke(result.transactions.size, result.newCategories.size)
                kotlinx.coroutines.delay(400)
            } catch (e: Exception) {
                Log.e("TransactionVM", "import failed", e)
                onDone?.invoke(0, 0)
            } finally {
                _excelProgress.value = null
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
    private val userAssetDao: UserAssetDao,
    private val assetFlowDao: AssetFlowDao,
    private val themeManager: ThemeManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(transactionDao, categoryDao, currencyAssetDao, albumPhotoDao, keywordCategoryDao, userAssetDao, assetFlowDao, themeManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
