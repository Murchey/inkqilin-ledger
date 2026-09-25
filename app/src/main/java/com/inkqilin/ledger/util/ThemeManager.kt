package com.inkqilin.ledger.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

const val DEFAULT_PRIMARY_COLOR_HEX = "#34C759"
const val DEFAULT_INCOME_COLOR_HEX = "#34C759"
const val DEFAULT_EXPENSE_COLOR_HEX = "#FF9500"
const val DEFAULT_UPDATE_REPO = "Murchey/inkqinlin-ledger"
const val DEFAULT_GITHUB_REPO = "Murchey/inkqilin-ledger"

enum class ThemeMode {
    AUTO, LIGHT, DARK
}

enum class AppMode {
    BASIC, SMART
}

enum class AiDataRange(val label: String) {
    THIS_WEEK_AND_LAST("本周和上周"),
    TODAY_AND_YESTERDAY("本日和昨日"),
    THIS_MONTH_AND_LAST("本月和上月")
}

/**
 * 将用户输入规范化为 `owner/repo`。
 * 支持：owner/repo、https://host/owner/repo、https://host/owner/repo/releases 等。
 */
fun normalizeRepoPath(input: String, vararg hosts: String): String {
    var s = input.trim().trimEnd('/')
    if (s.isBlank()) return ""
    hosts.forEach { host ->
        s = s.removePrefix("https://$host/")
            .removePrefix("http://$host/")
            .removePrefix("https://www.$host/")
            .removePrefix("http://www.$host/")
    }
    s = s.substringBefore("/releases")
        .substringBefore("/tree")
        .substringBefore("/blob")
        .trim('/')
    val parts = s.split("/").filter { it.isNotBlank() }
    return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else s
}

fun normalizeGiteeRepo(input: String): String =
    normalizeRepoPath(input, "gitee.com")

fun normalizeGithubRepo(input: String): String =
    normalizeRepoPath(input, "github.com")

class ThemeManager(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val INCOME_COLOR_KEY = stringPreferencesKey("income_color")
    private val EXPENSE_COLOR_KEY = stringPreferencesKey("expense_color")
    private val RENQING_ENABLED_KEY = booleanPreferencesKey("renqing_enabled")
    private val MULTI_CURRENCY_ENABLED_KEY = booleanPreferencesKey("multi_currency_enabled")
    private val MONTHLY_BUDGET_KEY = doublePreferencesKey("monthly_budget")
    private val CHECK_UPDATE_ENABLED_KEY = booleanPreferencesKey("check_update_enabled")
    private val UPDATE_PROXY_URL_KEY = stringPreferencesKey("update_proxy_url")
    private val UPDATE_REPO_KEY = stringPreferencesKey("update_repo")
    private val GITHUB_REPO_KEY = stringPreferencesKey("github_repo")
    private val COS_SECRET_ID_KEY = stringPreferencesKey("cos_secret_id")
    private val COS_SECRET_KEY_KEY = stringPreferencesKey("cos_secret_key")
    private val COS_BUCKET_URL_KEY = stringPreferencesKey("cos_bucket_url")
    private val COS_PREFIX_KEY = stringPreferencesKey("cos_prefix")
    private val CUSTOM_PRIMARY_COLOR_KEY = stringPreferencesKey("custom_primary_color")
    private val AUTO_RECORD_ENABLED_KEY = booleanPreferencesKey("auto_record_enabled")
    private val OCR_ENABLED_KEY = booleanPreferencesKey("ocr_enabled")
    private val AI_API_KEY_KEY = stringPreferencesKey("ai_api_key")
    private val AI_BASE_URL_KEY = stringPreferencesKey("ai_base_url")
    private val AI_MODEL_KEY = stringPreferencesKey("ai_model")
    private val ALBUM_ENABLED_KEY = booleanPreferencesKey("album_enabled")
    private val OCR_API_KEY_KEY = stringPreferencesKey("ocr_api_key")
    private val OCR_BASE_URL_KEY = stringPreferencesKey("ocr_base_url")
    private val OCR_MODEL_KEY = stringPreferencesKey("ocr_model")
    private val RECENT_NOTES_KEY = stringPreferencesKey("recent_notes")
    private val APP_MODE_KEY = stringPreferencesKey("app_mode")
    private val AI_DATA_RANGE_KEY = stringPreferencesKey("ai_data_range")
    private val AI_LAST_ANALYSIS_DATE_KEY = longPreferencesKey("ai_last_analysis_date")
    private val AI_SCORE_KEY = intPreferencesKey("ai_score")
    private val AI_SCORE_LABEL_KEY = stringPreferencesKey("ai_score_label")
    private val AI_SCORE_EXPLANATION_KEY = stringPreferencesKey("ai_score_explanation")
    private val AI_ALERTS_JSON_KEY = stringPreferencesKey("ai_alerts_json")
    private val AI_ANALYSIS_FAILED_KEY = booleanPreferencesKey("ai_analysis_failed")
    private val HOME_CARD_COLOR_KEY = stringPreferencesKey("home_card_color")
    private val HOME_BG_IMAGE_PATH_KEY = stringPreferencesKey("home_bg_image_path")
    private val HOME_BG_OPACITY_KEY = doublePreferencesKey("home_bg_opacity")
    private val HOME_TX_CARD_OPACITY_KEY = doublePreferencesKey("home_tx_card_opacity")
private val WIDGET_SHOW_AMOUNT_KEY = booleanPreferencesKey("widget_show_amount")
    private val WIDGET_QUICK_CATEGORIES_KEY = stringPreferencesKey("widget_quick_categories")
    private val HARMONY_COMPAT_ASKED_KEY = booleanPreferencesKey("harmony_compat_asked")
    private val HARMONY_COMPAT_MODE_KEY = booleanPreferencesKey("harmony_compat_mode")
    private val PRIVACY_ACCEPTED_KEY = booleanPreferencesKey("privacy_accepted")

    /** 是否已同意隐私政策 */
    val privacyAccepted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PRIVACY_ACCEPTED_KEY] ?: false
    }

    suspend fun setPrivacyAccepted() {
        context.dataStore.edit { preferences ->
            preferences[PRIVACY_ACCEPTED_KEY] = true
        }
    }

    /** 是否已完成「是否鸿蒙兼容环境」询问 */
    val harmonyCompatAsked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HARMONY_COMPAT_ASKED_KEY] ?: false
    }

    /** true = 鸿蒙/卓易通兼容模式：自动记账降级（通知监听不可用） */
    val harmonyCompatMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HARMONY_COMPAT_MODE_KEY] ?: false
    }

    suspend fun setHarmonyCompatMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HARMONY_COMPAT_MODE_KEY] = enabled
            preferences[HARMONY_COMPAT_ASKED_KEY] = true
        }
    }

    val widgetShowAmount: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WIDGET_SHOW_AMOUNT_KEY] ?: true
    }

    val widgetQuickCategories: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[WIDGET_QUICK_CATEGORIES_KEY]
        if (raw.isNullOrBlank()) listOf("餐饮", "交通", "购物", "娱乐")
        else raw.split("|||").filter { it.isNotBlank() }
    }

    suspend fun setWidgetShowAmount(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIDGET_SHOW_AMOUNT_KEY] = enabled
        }
    }

    suspend fun setWidgetQuickCategories(categories: List<String>) {
        context.dataStore.edit { preferences ->
            if (categories.isEmpty()) preferences.remove(WIDGET_QUICK_CATEGORIES_KEY)
            else preferences[WIDGET_QUICK_CATEGORIES_KEY] =
                categories.filter { it.isNotBlank() }.joinToString("|||")
        }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val mode = preferences[THEME_KEY] ?: ThemeMode.AUTO.name
        ThemeMode.valueOf(mode)
    }

    val incomeColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[INCOME_COLOR_KEY] ?: DEFAULT_INCOME_COLOR_HEX
    }

    val expenseColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[EXPENSE_COLOR_KEY] ?: DEFAULT_EXPENSE_COLOR_HEX
    }

    val renQingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[RENQING_ENABLED_KEY] ?: false
    }

    val multiCurrencyEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MULTI_CURRENCY_ENABLED_KEY] ?: false
    }

    val monthlyBudget: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[MONTHLY_BUDGET_KEY] ?: 0.0
    }

    val checkUpdateEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CHECK_UPDATE_ENABLED_KEY] ?: true
    }

    /** 代理源 URL 前缀，默认使用 gh-proxy.org */
    val updateProxyUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UPDATE_PROXY_URL_KEY] ?: PROXY_SOURCES.first()
    }

    /** 更新检测仓库路径，格式 owner/repo 或完整 Gitee 地址 */
    val updateRepo: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UPDATE_REPO_KEY]?.takeIf { it.isNotBlank() } ?: DEFAULT_UPDATE_REPO
    }

    /** GitHub 下载仓库路径，格式 owner/repo 或完整 GitHub 地址 */
    val githubRepo: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GITHUB_REPO_KEY]?.takeIf { it.isNotBlank() } ?: DEFAULT_GITHUB_REPO
    }

    /** 腾讯云 COS 私有备份配置 */
    val cosConfig: Flow<CosConfig> = context.dataStore.data.map { preferences ->
        CosConfig(
            secretId = preferences[COS_SECRET_ID_KEY] ?: "",
            secretKey = preferences[COS_SECRET_KEY_KEY] ?: "",
            bucketUrl = preferences[COS_BUCKET_URL_KEY] ?: "",
            prefix = preferences[COS_PREFIX_KEY]?.takeIf { it.isNotBlank() } ?: "backups/v1"
        )
    }

    suspend fun setCosConfig(config: CosConfig) {
        context.dataStore.edit { preferences ->
            if (config.secretId.isBlank()) preferences.remove(COS_SECRET_ID_KEY)
            else preferences[COS_SECRET_ID_KEY] = config.secretId.trim()
            if (config.secretKey.isBlank()) preferences.remove(COS_SECRET_KEY_KEY)
            else preferences[COS_SECRET_KEY_KEY] = config.secretKey.trim()
            if (config.bucketUrl.isBlank()) preferences.remove(COS_BUCKET_URL_KEY)
            else preferences[COS_BUCKET_URL_KEY] = config.bucketUrl.trim()
            if (config.prefix.isBlank()) preferences.remove(COS_PREFIX_KEY)
            else preferences[COS_PREFIX_KEY] = config.prefix.trim().ifBlank { "backups/v1" }
        }
    }

    val customPrimaryColor: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_PRIMARY_COLOR_KEY]
    }

    val autoRecordEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_RECORD_ENABLED_KEY] ?: false
    }

    val ocrEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[OCR_ENABLED_KEY] ?: false
    }

    val aiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_API_KEY_KEY] ?: ""
    }

    val aiBaseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_BASE_URL_KEY] ?: "https://api.openai.com/v1"
    }

    val aiModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_MODEL_KEY] ?: "gpt-4o"
    }

    val albumEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ALBUM_ENABLED_KEY] ?: false
    }

    val ocrApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[OCR_API_KEY_KEY] ?: ""
    }

    val ocrBaseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[OCR_BASE_URL_KEY] ?: "https://api.openai.com/v1"
    }

    val ocrModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[OCR_MODEL_KEY] ?: "gpt-4o"
    }

    val recentNotes: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val notesStr = preferences[RECENT_NOTES_KEY] ?: ""
        if (notesStr.isBlank()) emptyList()
        else notesStr.split("|||").filter { it.isNotBlank() }
    }

    val appMode: Flow<AppMode> = context.dataStore.data.map { preferences ->
        val mode = preferences[APP_MODE_KEY] ?: AppMode.BASIC.name
        try { AppMode.valueOf(mode) } catch (_: Exception) { AppMode.BASIC }
    }

    val aiDataRange: Flow<AiDataRange> = context.dataStore.data.map { preferences ->
        val range = preferences[AI_DATA_RANGE_KEY] ?: AiDataRange.THIS_WEEK_AND_LAST.name
        try { AiDataRange.valueOf(range) } catch (_: Exception) { AiDataRange.THIS_WEEK_AND_LAST }
    }

    val aiLastAnalysisDate: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[AI_LAST_ANALYSIS_DATE_KEY] ?: 0L
    }

    val aiScore: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[AI_SCORE_KEY]
    }

    val aiScoreLabel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_SCORE_LABEL_KEY] ?: ""
    }

    val aiScoreExplanation: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_SCORE_EXPLANATION_KEY] ?: ""
    }

    val aiAlertsJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_ALERTS_JSON_KEY] ?: ""
    }

    val aiAnalysisFailed: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AI_ANALYSIS_FAILED_KEY] ?: false
    }

    val homeCardColor: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[HOME_CARD_COLOR_KEY]
    }

    /** 首页背景图本地文件绝对路径；null 表示未设置 */
    val homeBgImagePath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[HOME_BG_IMAGE_PATH_KEY]?.takeIf { it.isNotBlank() }
    }

    /** 首页背景不透明度 0f–1f，默认 0.35 */
    val homeBgOpacity: Flow<Float> = context.dataStore.data.map { preferences ->
        ((preferences[HOME_BG_OPACITY_KEY] ?: 0.35).toFloat()).coerceIn(0.05f, 1f)
    }

    /** 首页账单条目卡片不透明度，默认 0.72 */
    val homeTxCardOpacity: Flow<Float> = context.dataStore.data.map { preferences ->
        ((preferences[HOME_TX_CARD_OPACITY_KEY] ?: 0.72).toFloat()).coerceIn(0.08f, 1f)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }

    suspend fun setIncomeColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[INCOME_COLOR_KEY] = color
        }
    }

    suspend fun setExpenseColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[EXPENSE_COLOR_KEY] = color
        }
    }

    suspend fun setRenQingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RENQING_ENABLED_KEY] = enabled
        }
    }

    suspend fun setMultiCurrencyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MULTI_CURRENCY_ENABLED_KEY] = enabled
        }
    }

    suspend fun setMonthlyBudget(amount: Double) {
        context.dataStore.edit { preferences ->
            preferences[MONTHLY_BUDGET_KEY] = amount
        }
    }

    suspend fun setCheckUpdateEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CHECK_UPDATE_ENABLED_KEY] = enabled
        }
    }

    suspend fun setUpdateProxyUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_PROXY_URL_KEY] = url
        }
    }

    suspend fun setUpdateRepo(repo: String) {
        context.dataStore.edit { preferences ->
            val normalized = normalizeGiteeRepo(repo)
            if (normalized.isBlank()) {
                preferences.remove(UPDATE_REPO_KEY)
            } else {
                preferences[UPDATE_REPO_KEY] = normalized
            }
        }
    }

    suspend fun setGithubRepo(repo: String) {
        context.dataStore.edit { preferences ->
            val normalized = normalizeGithubRepo(repo)
            if (normalized.isBlank()) {
                preferences.remove(GITHUB_REPO_KEY)
            } else {
                preferences[GITHUB_REPO_KEY] = normalized
            }
        }
    }

    suspend fun setCustomPrimaryColor(colorHex: String?) {
        context.dataStore.edit { preferences ->
            if (colorHex == null) {
                preferences.remove(CUSTOM_PRIMARY_COLOR_KEY)
            } else {
                preferences[CUSTOM_PRIMARY_COLOR_KEY] = colorHex
            }
        }
    }

    suspend fun setAutoRecordEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_RECORD_ENABLED_KEY] = enabled
        }
    }

    suspend fun setOcrEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OCR_ENABLED_KEY] = enabled
        }
    }

    suspend fun setAiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_API_KEY_KEY] = apiKey
        }
    }

    suspend fun setAiBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_BASE_URL_KEY] = baseUrl
        }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_MODEL_KEY] = model
        }
    }

    suspend fun setAlbumEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALBUM_ENABLED_KEY] = enabled
        }
    }

    suspend fun setOcrApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[OCR_API_KEY_KEY] = apiKey
        }
    }

    suspend fun setOcrBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[OCR_BASE_URL_KEY] = baseUrl
        }
    }

    suspend fun setOcrModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[OCR_MODEL_KEY] = model
        }
    }

    suspend fun addRecentNote(note: String) {
        if (note.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = preferences[RECENT_NOTES_KEY] ?: ""
            val notes = current.split("|||").filter { it.isNotBlank() }.toMutableList()
            notes.remove(note)
            notes.add(0, note)
            val trimmed = notes.take(5)
            preferences[RECENT_NOTES_KEY] = trimmed.joinToString("|||")
        }
    }

    suspend fun clearRecentNotes() {
        context.dataStore.edit { preferences ->
            preferences.remove(RECENT_NOTES_KEY)
        }
    }

    suspend fun setAppMode(mode: AppMode) {
        context.dataStore.edit { preferences ->
            preferences[APP_MODE_KEY] = mode.name
        }
    }

    suspend fun setAiDataRange(range: AiDataRange) {
        context.dataStore.edit { preferences ->
            preferences[AI_DATA_RANGE_KEY] = range.name
        }
    }

    suspend fun saveAiAnalysisResult(score: Int, scoreLabel: String, scoreExplanation: String, alertsJson: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_SCORE_KEY] = score
            preferences[AI_SCORE_LABEL_KEY] = scoreLabel
            preferences[AI_SCORE_EXPLANATION_KEY] = scoreExplanation
            preferences[AI_ALERTS_JSON_KEY] = alertsJson
            preferences[AI_LAST_ANALYSIS_DATE_KEY] = System.currentTimeMillis()
            preferences[AI_ANALYSIS_FAILED_KEY] = false
        }
    }

    suspend fun markAiAnalysisFailed() {
        context.dataStore.edit { preferences ->
            preferences[AI_ANALYSIS_FAILED_KEY] = true
            preferences[AI_LAST_ANALYSIS_DATE_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun clearAiAnalysisResult() {
        context.dataStore.edit { preferences ->
            preferences.remove(AI_SCORE_KEY)
            preferences.remove(AI_SCORE_LABEL_KEY)
            preferences.remove(AI_SCORE_EXPLANATION_KEY)
            preferences.remove(AI_ALERTS_JSON_KEY)
            preferences.remove(AI_LAST_ANALYSIS_DATE_KEY)
            preferences.remove(AI_ANALYSIS_FAILED_KEY)
        }
    }

    suspend fun setHomeCardColor(colorHex: String?) {
        context.dataStore.edit { preferences ->
            if (colorHex == null) {
                preferences.remove(HOME_CARD_COLOR_KEY)
            } else {
                preferences[HOME_CARD_COLOR_KEY] = colorHex
            }
        }
    }

    suspend fun setHomeBgImagePath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path.isNullOrBlank()) {
                preferences.remove(HOME_BG_IMAGE_PATH_KEY)
            } else {
                preferences[HOME_BG_IMAGE_PATH_KEY] = path
            }
        }
    }

    suspend fun setHomeBgOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[HOME_BG_OPACITY_KEY] = opacity.coerceIn(0.05f, 1f).toDouble()
        }
    }

    suspend fun setHomeTxCardOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[HOME_TX_CARD_OPACITY_KEY] = opacity.coerceIn(0.08f, 1f).toDouble()
        }
    }

    // ──── 个税税率配置 ────
    private val TAX_CONFIG_KEY = stringPreferencesKey("tax_config")

    val taxConfig: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TAX_CONFIG_KEY] ?: ""
    }

    suspend fun setTaxConfig(configJson: String) {
        context.dataStore.edit { preferences ->
            preferences[TAX_CONFIG_KEY] = configJson
        }
    }
}
