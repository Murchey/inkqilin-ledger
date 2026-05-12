package com.inkqilin.ledger.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
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
    private val MULTI_CURRENCY_ENABLED_KEY = booleanPreferencesKey("multi_currency_enabled")
    private val MONTHLY_BUDGET_KEY = doublePreferencesKey("monthly_budget")
    private val CHECK_UPDATE_ENABLED_KEY = booleanPreferencesKey("check_update_enabled")
    private val CUSTOM_PRIMARY_COLOR_KEY = stringPreferencesKey("custom_primary_color")

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
}
