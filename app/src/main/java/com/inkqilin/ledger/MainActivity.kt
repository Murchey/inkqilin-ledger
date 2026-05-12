package com.inkqilin.ledger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.AppDatabase
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.TransactionViewModelFactory
import com.inkqilin.ledger.ui.screens.MainScreen
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import com.inkqilin.ledger.util.AppUpdateChecker
import com.inkqilin.ledger.util.ThemeManager
import com.inkqilin.ledger.util.ThemeMode
import com.inkqilin.ledger.util.UpdateInfo

import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val themeManager by lazy { ThemeManager(this) }
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(
            database.transactionDao(),
            database.categoryDao(),
            database.currencyAssetDao(),
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
            val customPrimaryColorHex by viewModel.customPrimaryColorHex.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            InkQilinLedgerTheme(darkTheme = darkTheme, customPrimaryColorHex = customPrimaryColorHex) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val checkUpdateEnabled by viewModel.checkUpdateEnabled.collectAsState()
                    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                    val context = LocalContext.current

                    LaunchedEffect(checkUpdateEnabled) {
                        if (checkUpdateEnabled) {
                            val result = AppUpdateChecker.checkForUpdate(context)
                            if (result != null) {
                                updateInfo = result
                            }
                        }
                    }

                    updateInfo?.let { info ->
                        AlertDialog(
                            onDismissRequest = { updateInfo = null },
                            title = { Text("发现新版本 v${info.versionName}") },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "当前版本: v${viewModel.getCurrentVersionName(context)}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("更新内容:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = info.releaseNotes.ifBlank { "暂无更新说明" },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                                    context.startActivity(intent)
                                    updateInfo = null
                                }) { Text("前往更新") }
                            },
                            dismissButton = {
                                TextButton(onClick = { updateInfo = null }) { Text("稍后再说") }
                            }
                        )
                    }

                    MainScreen(viewModel, renQingViewModel)
                }
            }
        }
    }
}
