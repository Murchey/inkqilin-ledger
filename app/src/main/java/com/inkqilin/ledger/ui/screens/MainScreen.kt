package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import kotlinx.coroutines.launch

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel,
    enableAnimations: Boolean = true
) {
    val navController = rememberNavController()
    val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    val baseItems = remember {
        listOf(
            BottomNavItem("home", Icons.Default.Home, "首页"),
            BottomNavItem("statistics", Icons.Default.List, "统计"),
            BottomNavItem("settings", Icons.Default.Settings, "设置")
        )
    }

    val bottomItems = remember(renQingEnabled) {
        buildList {
            if (renQingEnabled) {
                addAll(baseItems.subList(0, 2))
                add(BottomNavItem("renqing", Icons.Default.Favorite, "人情"))
                addAll(baseItems.subList(2, baseItems.size))
            } else {
                addAll(baseItems)
            }
        }
    }

    val pagerState = rememberPagerState { bottomItems.size }
    val showBottomBar = currentRoute == "main"
    val currentPageRoute = bottomItems[pagerState.currentPage].route

    // Sync Pager with Bottom Nav selection (initial sync)
    LaunchedEffect(currentRoute) {
        if (currentRoute != "main") {
            // If we are on a sub-page, we don't sync
        }
    }

    val topBarTitle = when {
        currentRoute == "main" -> {
            when (currentPageRoute) {
                "home" -> "墨麒麟记账"
                "statistics" -> "统计"
                "renqing" -> "人情账本"
                "settings" -> "设置"
                else -> "墨麒麟记账"
            }
        }
        currentRoute == "search" -> "搜索"
        currentRoute == "add_transaction" -> "记一笔"
        currentRoute == "add_renqing_event" -> "添加事件"
        currentRoute == "category_management" -> "分类管理"
        currentRoute?.startsWith("renqing_contact_detail") == true -> "联系人详情"
        currentRoute?.startsWith("renqing_month_detail") == true -> "月度详情"
        currentRoute?.startsWith("renqing_tag_stats") == true -> "标签统计"
        currentRoute?.startsWith("renqing_contact_analysis") == true -> "关系分析"
        currentRoute?.startsWith("category_transactions") == true -> "分类账单"
        currentRoute == "contact_management" -> "联系人管理"
        currentRoute == "currency_management" -> "币种卡片管理"
        currentRoute == "ai_config" -> "AI API 配置"
        currentRoute == "ocr_batch_recognition" -> "OCR 批量识别"
        else -> "墨麒麟记账"
    }

    val showBackButton = currentRoute != "main"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = topBarTitle,
                        transitionSpec = {
                            if (enableAnimations) {
                                (fadeIn(animationSpec = MotionSprings.appearance()) +
                                        slideInVertically(animationSpec = MotionSprings.appearance()) { -it / 4 }) togetherWith
                                        fadeOut(animationSpec = MotionSprings.appearance())
                            } else {
                                EnterTransition.None togetherWith ExitTransition.None
                            }
                        },
                        label = "topBarTitle"
                    ) { title ->
                        Text(title, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    AnimatedVisibility(
                        visible = showBackButton,
                        enter = if (enableAnimations) {
                            fadeIn(MotionSprings.interactive()) + scaleIn(
                                animationSpec = MotionSprings.interactive(),
                                initialScale = 0.8f
                            )
                        } else {
                            EnterTransition.None
                        },
                        exit = if (enableAnimations) {
                            fadeOut(MotionSprings.interactive()) + scaleOut(
                                animationSpec = MotionSprings.interactive(),
                                targetScale = 0.8f
                            )
                        } else {
                            ExitTransition.None
                        }
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = currentRoute == "main" && currentPageRoute == "home",
                        enter = if (enableAnimations) fadeIn(MotionSprings.interactive()) else EnterTransition.None,
                        exit = if (enableAnimations) fadeOut(MotionSprings.interactive()) else ExitTransition.None
                    ) {
                        IconButton(onClick = { navController.navigate("search") }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = if (enableAnimations) {
                    slideInVertically(
                        animationSpec = MotionSprings.appearance(),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = MotionSprings.appearance())
                } else {
                    EnterTransition.None
                },
                exit = if (enableAnimations) {
                    slideOutVertically(
                        animationSpec = MotionSprings.appearance(),
                        targetOffsetY = { it }
                    ) + fadeOut(animationSpec = MotionSprings.appearance())
                } else {
                    ExitTransition.None
                }
            ) {
                val bgLuminance = MaterialTheme.colorScheme.background.let {
                    it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f
                }
                val isDarkMode = bgLuminance < 0.5f
                val unselectedColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF666666)

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomItems.forEachIndexed { index, item ->
                        val selected = pagerState.currentPage == index
                        val itemScale by animateFloatAsState(
                            targetValue = if (selected) 1.05f else 1f,
                            animationSpec = if (enableAnimations) {
                                MotionSprings.interactive()
                            } else {
                                snap()
                            },
                            label = "navScale_${item.route}"
                        )
                        val itemAlpha by animateFloatAsState(
                            targetValue = if (selected) 1f else 0.7f,
                            animationSpec = if (enableAnimations) {
                                MotionSprings.interactive()
                            } else {
                                snap()
                            },
                            label = "navAlpha_${item.route}"
                        )

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .scale(itemScale)
                                        .alpha(itemAlpha)
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .scale(itemScale)
                                        .alpha(itemAlpha)
                                )
                            },
                            selected = selected,
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = unselectedColor,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = unselectedColor,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                if (enableAnimations) {
                    fadeIn(animationSpec = MotionSprings.appearance()) +
                        slideInHorizontally(
                            animationSpec = MotionSprings.appearance(),
                            initialOffsetX = { it }
                        )
                } else {
                    EnterTransition.None
                }
            },
            exitTransition = {
                if (enableAnimations) {
                    fadeOut(animationSpec = MotionSprings.appearance()) +
                        slideOutHorizontally(
                            animationSpec = MotionSprings.appearance(),
                            targetOffsetX = { -it / 3 }
                        )
                } else {
                    ExitTransition.None
                }
            },
            popEnterTransition = {
                if (enableAnimations) {
                    fadeIn(animationSpec = MotionSprings.appearance()) +
                        slideInHorizontally(
                            animationSpec = MotionSprings.appearance(),
                            initialOffsetX = { -it / 3 }
                        )
                } else {
                    EnterTransition.None
                }
            },
            popExitTransition = {
                if (enableAnimations) {
                    fadeOut(animationSpec = MotionSprings.appearance()) +
                        slideOutHorizontally(
                            animationSpec = MotionSprings.appearance(),
                            targetOffsetX = { it }
                        )
                } else {
                    ExitTransition.None
                }
            }
        ) {
            composable("main") {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondBoundsPageCount = 1
                ) { page ->
                    when (bottomItems[page].route) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToAddTransaction = {
                                navController.navigate("add_transaction")
                            },
                            onNavigateToStatistics = {
                                scope.launch {
                                    val statsIndex = bottomItems.indexOfFirst { it.route == "statistics" }
                                    if (statsIndex != -1) pagerState.animateScrollToPage(statsIndex)
                                }
                            },
                            onNavigateToSearch = {
                                navController.navigate("search")
                            },
                            onNavigateToOcrRecognition = {
                                navController.navigate("ocr_batch_recognition")
                            }
                        )
                        "statistics" -> StatisticsScreen(viewModel, navController)
                        "renqing" -> RenQingMainScreen(
                            viewModel = renQingViewModel,
                            onNavigateToContactDetail = { contactId ->
                                navController.navigate("renqing_contact_detail/$contactId")
                            },
                            onNavigateToMonthDetail = { year, month ->
                                navController.navigate("renqing_month_detail/$year/$month")
                            },
                            onNavigateToAddEvent = {
                                navController.navigate("add_renqing_event")
                            },
                            onNavigateToTagStats = { year ->
                                navController.navigate("renqing_tag_stats/$year")
                            },
                            onNavigateToContactAnalysis = { year ->
                                navController.navigate("renqing_contact_analysis/$year")
                            }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            renQingViewModel = renQingViewModel,
                            onNavigateToCategoryManagement = {
                                navController.navigate("category_management")
                            },
                            onNavigateToContactManagement = {
                                navController.navigate("contact_management")
                            },
                            onNavigateToCurrencyManagement = {
                                navController.navigate("currency_management")
                            },
                            onNavigateToAIConfig = {
                                navController.navigate("ai_config")
                            }
                        )
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
            composable("ocr_batch_recognition") {
                OcrBatchRecognitionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("add_transaction") {
                AddTransactionScreen(
                    viewModel = viewModel,
                    renQingViewModel = renQingViewModel,
                    onSaved = { navController.popBackStack() }
                )
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
}
