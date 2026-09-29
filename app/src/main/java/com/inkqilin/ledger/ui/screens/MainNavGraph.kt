@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.util.DEFAULT_PRIMARY_COLOR_HEX
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.inkqilin.ledger.data.CycleType
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.util.NotificationHelper
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

@Composable
internal fun MainNavGraph(
    navController: androidx.navigation.NavHostController,
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel,
    enableAnimations: Boolean,
    innerPadding: PaddingValues,
    currentRoute: String?,
    pagerState: androidx.compose.foundation.pager.PagerState,
    bottomItems: List<BottomNavItem>,
    albumFabTrigger: androidx.compose.runtime.MutableState<Boolean>,
    cloudBackupOpenSettings: androidx.compose.runtime.MutableState<Boolean>,
    customTopBarTitle: androidx.compose.runtime.MutableState<String?>,
    customBackAction: androidx.compose.runtime.MutableState<(() -> Unit)?>,
) {
    val scope = rememberCoroutineScope()
    val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
    val albumEnabled by viewModel.albumEnabled.collectAsState()
    val isAlbumInteracting by viewModel.isAlbumInteracting.collectAsState()
        NavHost(
            navController = navController,
            startDestination = "main",
            // Only apply top padding (for TopAppBar). Bottom is handled by each screen.
            // Content extends behind the floating glass tab bar.
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            enterTransition = {
                if (enableAnimations) {
                    fadeIn(animationSpec = MotionSprings.appearanceTween()) +
                        slideInHorizontally(
                            animationSpec = MotionSprings.appearanceTween(),
                            initialOffsetX = { it }
                        )
                } else {
                    EnterTransition.None
                }
            },
            exitTransition = {
                if (enableAnimations) {
                    fadeOut(animationSpec = MotionSprings.appearanceTween()) +
                        slideOutHorizontally(
                            animationSpec = MotionSprings.appearanceTween(),
                            targetOffsetX = { -it / 4 }
                        )
                } else {
                    ExitTransition.None
                }
            },
            popEnterTransition = {
                if (enableAnimations) {
                    fadeIn(animationSpec = MotionSprings.appearanceTween()) +
                        slideInHorizontally(
                            animationSpec = MotionSprings.appearanceTween(),
                            initialOffsetX = { -it / 4 }
                        )
                } else {
                    EnterTransition.None
                }
            },
            popExitTransition = {
                if (enableAnimations) {
                    fadeOut(animationSpec = MotionSprings.appearanceTween()) +
                        slideOutHorizontally(
                            animationSpec = MotionSprings.appearanceTween(),
                            targetOffsetX = { it }
                        )
                } else {
                    ExitTransition.None
                }
            }
        ) {
            composable("main") {
                // 按路由保存各 Tab 的 rememberSaveable（如设置抽屉展开态），
                // 开关人情/相册导致页下标变化时不丢失
                val saveableStateHolder = rememberSaveableStateHolder()
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isAlbumInteracting,
                    beyondBoundsPageCount = 0
                ) { page ->
                    val pageRoute = bottomItems[page].route
                    saveableStateHolder.SaveableStateProvider(key = pageRoute) {
                    when (pageRoute) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToAddTransaction = {
                                navController.navigateSingle("add_transaction")
                            },
                            onNavigateToStatistics = {
                                scope.launch {
                                    val statsIndex = bottomItems.indexOfFirst { it.route == "statistics" }
                                    if (statsIndex != -1) pagerState.animateScrollToPage(statsIndex)
                                }
                            },
                            onNavigateToEditTransaction = { transaction ->
                                viewModel.setPendingEditTransaction(transaction)
                                navController.navigateSingle("edit_transaction/${transaction.id}")
                            },
                            onNavigateToSearch = {
                                navController.navigateSingle("search")
                            },
                            onNavigateToOcrRecognition = {
                                navController.navigateSingle("ocr_batch_recognition")
                            },
                            onNavigateToAssetManagement = {
                                navController.navigateSingle("asset_management")
                            }
                        )
                        "statistics" -> StatisticsScreen(viewModel, navController)
                        "album" -> AlbumScreen(
                            viewModel = viewModel,
                            isActive = pagerState.currentPage == bottomItems.indexOfFirst { it.route == "album" },
                            fabTrigger = albumFabTrigger.value,
                            onFabTriggered = { albumFabTrigger.value = false }
                        )
                        "renqing" -> RenQingMainScreen(
                            viewModel = renQingViewModel,
                            onNavigateToContactDetail = { contactId ->
                                navController.navigateSingle("renqing_contact_detail/$contactId")
                            },
                            onNavigateToMonthDetail = { year, month ->
                                navController.navigateSingle("renqing_month_detail/$year/$month")
                            },
                            onNavigateToTagStats = { year ->
                                navController.navigateSingle("renqing_tag_stats/$year")
                            },
                            onNavigateToContactAnalysis = { year ->
                                navController.navigateSingle("renqing_contact_analysis/$year")
                            },
                            onNavigateToRenQingStats = {
                                navController.navigateSingle("renqing_stats")
                            }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            renQingViewModel = renQingViewModel,
                            onNavigateToCategoryManagement = {
                                navController.navigateSingle("category_management")
                            },
                            onNavigateToKeywordCategoryManagement = {
                                navController.navigateSingle("keyword_category_management")
                            },
                            onNavigateToContactManagement = {
                                navController.navigateSingle("contact_management")
                            },
                            onNavigateToCurrencyManagement = {
                                navController.navigateSingle("currency_management")
                            },
                            onNavigateToAIConfig = {
                                navController.navigateSingle("ai_config")
                            },
                            onNavigateToOCRConfig = {
                                navController.navigateSingle("ocr_config")
                            },
                            onNavigateToBillImport = {
                                navController.navigateSingle("bill_import")
                            },
                            onNavigateToCloudBackup = {
                                navController.navigateSingle("cloud_backup")
                            }
                        )
                    }
                    }
                }
            }
            composable("search") { SearchScreen(viewModel) }
            composable("ai_config") {
                AIConfigScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("ocr_config") {
                OCRConfigScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("cloud_backup") {
                CloudBackupScreen(
                    viewModel = viewModel,
                    openSettings = cloudBackupOpenSettings.value,
                    onOpenSettingsConsumed = { cloudBackupOpenSettings.value = false },
                    onNavigateAutoBackup = { target ->
                        navController.navigateSingle("backup_auto_schedule/$target")
                    }
                )
                DisposableEffect(Unit) {
                    onDispose { cloudBackupOpenSettings.value = false }
                }
            }
            composable(
                route = "backup_auto_schedule/{target}",
                arguments = listOf(navArgument("target") {
                    type = NavType.StringType
                    defaultValue = "local"
                })
            ) { entry ->
                val target = entry.arguments?.getString("target") ?: "local"
                BackupAutoScheduleScreen(viewModel = viewModel, target = target)
            }
            composable("asset_management") {
                AssetManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle.value = title
                        customBackAction.value = backAction
                    }
                )
                DisposableEffect(Unit) {
                    onDispose {
                        customTopBarTitle.value = null
                        customBackAction.value = null
                    }
                }
            }
            composable("ocr_batch_recognition") {
                OcrBatchRecognitionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("bill_import") {
                BillImportScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "add_transaction?category={category}&type={type}",
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType; defaultValue = "" },
                    navArgument("type") { type = NavType.StringType; defaultValue = "EXPENSE" }
                )
            ) { backStackEntry ->
                val initialCategory = backStackEntry.arguments?.getString("category").orEmpty()
                val initialTypeStr = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
                AddTransactionScreen(
                    viewModel = viewModel,
                    renQingViewModel = renQingViewModel,
                    initialCategory = initialCategory,
                    initialType = runCatching { TransactionType.valueOf(initialTypeStr) }
                        .getOrDefault(TransactionType.EXPENSE),
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                val pendingTx by viewModel.pendingEditTransaction.collectAsState()
                val transactions by viewModel.allTransactions.collectAsState()
                // 优先用 Flow 已有数据；未就绪时用导航前缓存，保证转场首帧就有内容
                val transaction = transactions.firstOrNull { it.id == transactionId }
                    ?: pendingTx?.takeIf { it.id == transactionId }
                transaction?.let { tx ->
                    AddTransactionScreen(
                        viewModel = viewModel,
                        renQingViewModel = renQingViewModel,
                        existingTransaction = tx,
                        onSaved = { navController.popBackStack() }
                    )
                }
                DisposableEffect(transactionId) {
                    onDispose {
                        viewModel.setPendingEditTransaction(null)
                    }
                }
            }
            composable("category_management") {
                CategoryManagementScreen(
                    viewModel = viewModel,
                    renQingViewModel = renQingViewModel
                )
            }
            composable("contact_management") {
                ContactManagementScreen(viewModel = renQingViewModel)
            }
            composable("currency_management") {
                CurrencyManagementScreen(viewModel = viewModel)
            }
            composable("cycle_bill_list") {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                CycleBillScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateAddBill = { navController.navigateSingle("cycle_bill_edit/0") },
                    onNavigateEditBill = { billId -> navController.navigateSingle("cycle_bill_edit/$billId") },
                    onNavigateRecycleBin = { navController.navigateSingle("recycle_bin") },
                    onCreateTransaction = { bill ->
                        val txDate = System.currentTimeMillis()
                        val nextCycleEnd = CycleBoundary(txDate, bill.cycleType)
                        val updatedBill = bill.copy(
                            lastGeneratedDate = txDate,
                            currentCycleStart = txDate,
                            currentCycleEnd = nextCycleEnd,
                            nextTriggerDate = nextCycleEnd,
                            overdue = false
                        )
                        scope.launch {
                            val repos = com.inkqilin.ledger.data.repository.LedgerRepositories.get(ctx)
                            repos.transactions.insertTransaction(
                                Transaction(
                                    amount = bill.amount,
                                    category = bill.category,
                                    note = "",
                                    date = txDate,
                                    type = bill.type,
                                    currency = bill.currency,
                                    uuid = null,
                                    cycleBillId = bill.id
                                )
                            )
                            repos.cycleBills.updateCycleBill(updatedBill)
                            if (bill.reminderEnabled && bill.advanceMinutes > 0) {
                                NotificationHelper.scheduleCycleBillReminder(
                                    ctx,
                                    bill.id, bill.name, bill.amount,
                                    if (bill.type == TransactionType.EXPENSE) "支出" else "收入",
                                    updatedBill.nextTriggerDate - bill.advanceMinutes * 60_000L,
                                    bill.advanceMinutes
                                )
                            }
                            Toast.makeText(ctx, "已生成 ${bill.name} ¥${String.format("%.2f", bill.amount)}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle.value = title
                        customBackAction.value = backAction
                    }
                )
            }
            composable("cycle_bill_edit/{billId}", arguments = listOf(
                androidx.navigation.navArgument("billId") { type = NavType.LongType }
            )) { backStackEntry ->
                val billId = backStackEntry.arguments?.getLong("billId") ?: 0L
                CycleBillEditScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { navController.popBackStack() },
                    editBillId = if (billId > 0) billId else null,
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle.value = title
                        customBackAction.value = backAction
                    }
                )
            }
            composable("recycle_bin") {
                RecycleBinScreen(
                    onBack = { navController.popBackStack() },
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle.value = title
                        customBackAction.value = backAction
                    }
                )
            }
            composable("calculator_hub") {
                CalculatorScreen(
                    initialType = null,
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle.value = title
                        customBackAction.value = backAction
                    }
                )
                DisposableEffect(Unit) {
                    onDispose {
                        customTopBarTitle.value = null
                        customBackAction.value = null
                    }
                }
            }
            composable("calculator/{type}", arguments = listOf(
                androidx.navigation.navArgument("type") { type = NavType.StringType; defaultValue = "" }
            )) { backStackEntry ->
                val calcType = backStackEntry.arguments?.getString("type") ?: ""
                CalculatorScreen(
                    initialType = calcType,
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle.value = title
                        customBackAction.value = backAction
                    }
                )
                DisposableEffect(Unit) {
                    onDispose {
                        customTopBarTitle.value = null
                        customBackAction.value = null
                    }
                }
            }
            composable("keyword_category_management") {
                KeywordCategoryManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("add_renqing_event") {
                AddRenQingEventScreen(
                    viewModel = renQingViewModel,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "renqing_contact_detail/{contactId}",
                arguments = listOf(navArgument("contactId") { type = NavType.LongType })
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
                RenQingContactDetailScreen(
                    viewModel = renQingViewModel,
                    contactId = contactId
                )
            }
            composable("renqing_stats") {
                RenQingStatsScreen(
                    viewModel = renQingViewModel,
                    tags = renQingViewModel.allTags.collectAsState().value,
                    onNavigateToMonthDetail = { year, month ->
                        navController.navigateSingle("renqing_month_detail/$year/$month")
                    },
                    onNavigateToTagStats = { year ->
                        navController.navigateSingle("renqing_tag_stats/$year")
                    },
                    onNavigateToContactAnalysis = { year ->
                        navController.navigateSingle("renqing_contact_analysis/$year")
                    }
                )
            }
            composable(
                route = "renqing_month_detail/{year}/{month}",
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: 2026
                val month = backStackEntry.arguments?.getInt("month") ?: 0
                RenQingMonthDetailScreen(
                    viewModel = renQingViewModel,
                    year = year,
                    month = month
                )
            }
            composable(
                route = "renqing_tag_stats/{year}",
                arguments = listOf(navArgument("year") { type = NavType.IntType })
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: 2026
                RenQingTagStatsScreen(
                    viewModel = renQingViewModel,
                    year = year
                )
            }
            composable(
                route = "renqing_contact_analysis/{year}",
                arguments = listOf(navArgument("year") { type = NavType.IntType })
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: 2026
                RenQingContactAnalysisScreen(
                    viewModel = renQingViewModel,
                    year = year
                )
            }
            composable(
                route = "category_transactions/{categoryName}/{type}?startDate={startDate}&endDate={endDate}",
                arguments = listOf(
                    navArgument("categoryName") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType },
                    navArgument("startDate") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("endDate") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
                val startDate = backStackEntry.arguments?.getLong("startDate") ?: 0L
                val endDate = backStackEntry.arguments?.getLong("endDate") ?: 0L
                CategoryTransactionsScreen(
                    viewModel = viewModel,
                    categoryName = categoryName,
                    type = type,
                    startDate = startDate,
                    endDate = endDate
                )
            }
        }
}
