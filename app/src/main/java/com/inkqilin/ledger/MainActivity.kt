package com.inkqilin.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.inkqilin.ledger.data.AppDatabase
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.TransactionViewModelFactory
import com.inkqilin.ledger.ui.screens.MainScreen
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import com.inkqilin.ledger.util.ThemeManager
import com.inkqilin.ledger.util.ThemeMode

import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val themeManager by lazy { ThemeManager(this) }
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(
            database.transactionDao(),
            database.categoryDao(),
            themeManager
        )
    }
    private val renQingViewModel: RenQingViewModel by viewModels {
        RenQingViewModel.Factory(
            database.renQingContactDao(),
            database.renQingEventDao(),
            database.renQingTagDao(),
            database.transactionDao(),
            database.categoryDao(),
            themeManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            InkQilinLedgerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel, renQingViewModel)
                }
            }
        }
    }
}
