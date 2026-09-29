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
import com.inkqilin.ledger.data.repository.AlbumRepository
import com.inkqilin.ledger.data.repository.AssetRepository
import com.inkqilin.ledger.data.repository.CategoryRepository
import com.inkqilin.ledger.data.repository.CurrencyAssetRepository
import com.inkqilin.ledger.data.repository.KeywordCategoryRepository
import com.inkqilin.ledger.data.repository.TransactionRepository
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
    private val transactionDao: TransactionRepository,
    private val categoryDao: CategoryRepository,
    private val currencyAssetDao: CurrencyAssetRepository,
    private val albumPhotoDao: AlbumRepository,
    private val keywordCategoryDao: KeywordCategoryRepository,
    private val assets: AssetRepository,
    private val themeManager: ThemeManager
) : ViewModel() {
    private val userAssetDao get() = assets
    private val assetFlowDao get() = assets
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
        DefaultSeedData.categories.forEach { categoryDao.insertCategory(it) }
    }

    private suspend fun initializeDefaultCurrencies() {
        DefaultSeedData.currencies.forEach { currencyAssetDao.insertAsset(it) }
    }

    private suspend fun initializeDefaultKeywordCategories() {
        DefaultSeedData.keywordCategories.forEach { keywordCategoryDao.insertKeywordCategory(it) }
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

    // ── 自动备份计划 ──
    val localBackupSchedule: StateFlow<com.inkqilin.ledger.util.BackupSchedule> =
        themeManager.localBackupSchedule.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.inkqilin.ledger.util.BackupSchedule()
        )

    val cloudBackupSchedule: StateFlow<com.inkqilin.ledger.util.BackupSchedule> =
        themeManager.cloudBackupSchedule.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.inkqilin.ledger.util.BackupSchedule()
        )

    val autoBackupError: StateFlow<String?> = themeManager.autoBackupError.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    fun clearAutoBackupError() {
        viewModelScope.launch { themeManager.clearAutoBackupError() }
    }

    fun setLocalBackupSchedule(schedule: com.inkqilin.ledger.util.BackupSchedule) {
        viewModelScope.launch {
            themeManager.setLocalBackupSchedule(schedule)
            com.inkqilin.ledger.util.AutoBackupRunner.scheduleAutoBackupWorker(LedgerApplication.instance)
        }
    }

    fun setCloudBackupSchedule(schedule: com.inkqilin.ledger.util.BackupSchedule) {
        viewModelScope.launch {
            themeManager.setCloudBackupSchedule(schedule)
            com.inkqilin.ledger.util.AutoBackupRunner.scheduleAutoBackupWorker(LedgerApplication.instance)
        }
    }

    /** 串行更新单个字段，避免输入过程中状态回写导致光标跳动 */
    fun updateLocalBackupSchedule(transform: (com.inkqilin.ledger.util.BackupSchedule) -> com.inkqilin.ledger.util.BackupSchedule) {
        viewModelScope.launch {
            themeManager.updateLocalBackupSchedule(transform)
            com.inkqilin.ledger.util.AutoBackupRunner.scheduleAutoBackupWorker(LedgerApplication.instance)
        }
    }

    fun updateCloudBackupSchedule(transform: (com.inkqilin.ledger.util.BackupSchedule) -> com.inkqilin.ledger.util.BackupSchedule) {
        viewModelScope.launch {
            themeManager.updateCloudBackupSchedule(transform)
            com.inkqilin.ledger.util.AutoBackupRunner.scheduleAutoBackupWorker(LedgerApplication.instance)
        }
    }

    /** 启动时：调度周期任务 + 执行「打开 APP 时」备份 */
    fun kickAutoBackupsOnAppOpen() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val app = LedgerApplication.instance
                com.inkqilin.ledger.util.AutoBackupRunner.scheduleAutoBackupWorker(app)
                com.inkqilin.ledger.util.AutoBackupRunner.runAppOpenBackups(app)
            }
        }
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
                val target = HomeStorageHelpers.CopyHomeBackground(context, uri)
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
            HomeStorageHelpers.DeleteQuietly(path)
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

    private val aiAnalysis = AiAnalysisController(themeManager, viewModelScope) {
        allTransactions.first()
    }

    val aiAnalysisResult: StateFlow<AiAnalysisResult?> = aiAnalysis.aiAnalysisResult
    val aiAnalysisLoading: StateFlow<Boolean> = aiAnalysis.aiAnalysisLoading
    val aiAnalysisFailed: StateFlow<Boolean> = aiAnalysis.aiAnalysisFailed

    init {
        aiAnalysis.loadCachedAiResult()
    }

    fun checkAndRunDailyAnalysis() {
        aiAnalysis.checkAndRunDailyAnalysis(
            isSmartMode = { appMode.value == AppMode.SMART },
            apiKey = { aiApiKey.value },
            runAnalysis = { runAiAnalysis() },
        )
    }

    fun runAiAnalysis() {
        aiAnalysis.runAiAnalysis(
            isSmartMode = { appMode.value == AppMode.SMART },
            apiKey = { aiApiKey.value },
            baseUrl = { aiBaseUrl.value },
            model = { aiModel.value },
            dataRange = { aiDataRange.value },
        )
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
    suspend fun clearAllAlbumPhotos(context: Context): Boolean =
        HomeStorageHelpers.ClearAlbumDirAndDb(context) { albumPhotoDao.deleteAllPhotos() }

    /** 清空本地备份目录（含 pre_restore） */
    suspend fun clearLocalBackups(context: Context): Boolean =
        HomeStorageHelpers.ClearLocalBackupDir(context)

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
    private val assetFlows = AssetFlowController(assets, viewModelScope)

    val allAssetFlows: StateFlow<List<AssetFlow>> = assetFlows.allAssetFlows

    fun getAssetFlows(assetId: Long): Flow<List<AssetFlow>> = assetFlows.GetFlows(assetId)

    fun addAssetFlow(flow: AssetFlow) = assetFlows.Add(flow)

    /** 导入流转时去重 */
    suspend fun importAssetFlowSkipDuplicates(flow: AssetFlow): Boolean =
        assetFlows.ImportSkipDuplicates(flow)

    /** 为存量流转记录补齐 UUID */
    fun backfillAssetFlowUuids() = assetFlows.BackfillUuids()

    fun updateAssetFlow(flow: AssetFlow) = assetFlows.Update(flow)

    fun deleteAssetFlow(flow: AssetFlow) = assetFlows.Delete(flow)

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

    fun getYearRange(year: Int): Pair<Long, Long> = DateRangeUtils.GetYearRange(year)

    fun getMonthRange(year: Int, month: Int): Pair<Long, Long> = DateRangeUtils.GetMonthRange(year, month)

    private val excelImporter = ExcelImportController(viewModelScope, categoryDao, transactionDao) {
        notifyWidgets()
    }

    val excelProgress: StateFlow<Pair<Float, String>?> = excelImporter.excelProgress

    fun importTransactions(
        context: Context,
        uri: Uri,
        onDone: ((imported: Int, newCats: Int) -> Unit)? = null
    ) {
        excelImporter.ImportTransactions(
            context = context,
            uri = uri,
            existingCategories = { allCategories.first() },
            onDone = onDone,
        )
    }

    private val keywordCategories = KeywordCategoryController(keywordCategoryDao, viewModelScope)

    val allKeywordCategories: Flow<List<KeywordCategory>> = keywordCategories.allKeywordCategories

    fun addKeywordCategory(keyword: String, categoryName: String) =
        keywordCategories.Add(keyword, categoryName)

    fun updateKeywordCategory(keywordCategory: KeywordCategory) =
        keywordCategories.Update(keywordCategory)

    fun deleteKeywordCategory(keywordCategory: KeywordCategory) =
        keywordCategories.Delete(keywordCategory)

    suspend fun matchCategoryByKeyword(note: String): String? =
        keywordCategories.MatchCategoryByKeyword(note)

    fun getCurrentVersionName(context: Context): String = AppVersionUtils.Get(context)
}
