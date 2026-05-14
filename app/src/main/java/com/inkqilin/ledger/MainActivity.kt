package com.inkqilin.ledger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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
import kotlinx.coroutines.delay

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
                    var enableStartupAnimations by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        withFrameNanos { }
                        enableStartupAnimations = true
                    }

                    LaunchedEffect(checkUpdateEnabled) {
                        if (checkUpdateEnabled) {
                            delay(1200)
                            val result = AppUpdateChecker.checkForUpdate(context)
                            if (result != null) {
                                updateInfo = result
                            }
                        }
                    }

                    updateInfo?.let { info ->
                        var selectedSourceIndex by remember { mutableStateOf(0) }
                        val sources = listOf("Gitee 镜像 (国内推荐)", "GitHub 仓库 Release")
                        val downloadUrls = listOf(
                            info.downloadUrl,
                            "https://gitee.com/Murchey/inkqinlin-ledger/releases/latest"
                        )
                        var expanded by remember { mutableStateOf(false) }

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
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text("选择下载源:", style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { expanded = true },
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp),
                                            tonalElevation = 1.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = sources[selectedSourceIndex],
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                        
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.fillMaxWidth(0.7f)
                                        ) {
                                            sources.forEachIndexed { index, name ->
                                                DropdownMenuItem(
                                                    text = { Text(name) },
                                                    onClick = {
                                                        selectedSourceIndex = index
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrls[selectedSourceIndex]))
                                    context.startActivity(intent)
                                    updateInfo = null
                                }) { Text("前往更新") }
                            },
                            dismissButton = {
                                TextButton(onClick = { updateInfo = null }) { Text("稍后再说") }
                            }
                        )
                    }

                    MainScreen(
                        viewModel = viewModel,
                        renQingViewModel = renQingViewModel,
                        enableAnimations = enableStartupAnimations
                    )
                }
            }
        }
    }
}
