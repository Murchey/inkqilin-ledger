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

const val DEFAULT_PRIMARY_COLOR_HEX = "#04BE02"
const val DEFAULT_INCOME_COLOR_HEX = "#4CAF50"
const val DEFAULT_EXPENSE_COLOR_HEX = "#FF9800"

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

class ThemeManager(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val INCOME_COLOR_KEY = stringPreferencesKey("income_color")
    private val EXPENSE_COLOR_KEY = stringPreferencesKey("expense_color")
    private val RENQING_ENABLED_KEY = booleanPreferencesKey("renqing_enabled")
    private val MULTI_CURRENCY_ENABLED_KEY = booleanPreferencesKey("multi_currency_enabled")
    private val MONTHLY_BUDGET_KEY = doublePreferencesKey("monthly_budget")
    private val CHECK_UPDATE_ENABLED_KEY = booleanPreferencesKey("check_update_enabled")
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
}
