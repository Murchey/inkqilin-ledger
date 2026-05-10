package com.inkqilin.ledger.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode {
    AUTO, LIGHT, DARK
}

class ThemeManager(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val INCOME_COLOR_KEY = stringPreferencesKey("income_color")
    private val EXPENSE_COLOR_KEY = stringPreferencesKey("expense_color")
    private val RENQING_ENABLED_KEY = booleanPreferencesKey("renqing_enabled")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val mode = preferences[THEME_KEY] ?: ThemeMode.AUTO.name
        ThemeMode.valueOf(mode)
    }

    val incomeColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[INCOME_COLOR_KEY] ?: "#4CAF50"
    }

    val expenseColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[EXPENSE_COLOR_KEY] ?: "#F44336"
    }

    val renQingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[RENQING_ENABLED_KEY] ?: false
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
}
