package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
import com.inkqilin.ledger.ui.motion.MotionDurations
import com.inkqilin.ledger.ui.motion.MotionCurves
import com.inkqilin.ledger.ui.motion.MotionSprings

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel
) {
    val navController = rememberNavController()
    val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val baseItems = listOf(
        BottomNavItem("home", Icons.Default.Home, "首页"),
        BottomNavItem("statistics", Icons.Default.List, "统计"),
        BottomNavItem("settings", Icons.Default.Settings, "设置")
    )

    val bottomItems = buildList {
        if (renQingEnabled) {
            addAll(baseItems.subList(0, 2))
            add(BottomNavItem("renqing", Icons.Default.Favorite, "人情"))
            addAll(baseItems.subList(2, baseItems.size))
        } else {
            addAll(baseItems)
        }
    }

    val showBottomBar = currentRoute in bottomItems.map { it.route }

    val topBarTitle = when {
        currentRoute == "home" -> "墨麒麟记账"
        currentRoute == "search" -> "搜索"
        currentRoute == "statistics" -> "统计"
        currentRoute == "settings" -> "设置"
        currentRoute == "renqing" -> "人情账本"
        currentRoute == "add_transaction" -> "记一笔"
        currentRoute == "add_renqing_event" -> "添加事件"
        currentRoute == "category_management" -> "分类管理"
        currentRoute?.startsWith("renqing_contact_detail") == true -> "联系人详情"
        currentRoute?.startsWith("renqing_month_detail") == true -> "月度详情"
        currentRoute?.startsWith("renqing_tag_stats") == true -> "标签统计"
        currentRoute?.startsWith("renqing_contact_analysis") == true -> "关系分析"
        currentRoute?.startsWith("category_transactions") == true -> "分类账单"
        currentRoute == "contact_management" -> "联系人管理"
        else -> "墨麒麟记账"
    }

    val showBackButton = currentRoute?.let { route ->
        route.startsWith("category_management") ||
        route.startsWith("renqing_contact_detail") ||
        route.startsWith("renqing_month_detail") ||
        route.startsWith("renqing_tag_stats") ||
        route.startsWith("renqing_contact_analysis") ||
        route.startsWith("category_transactions") ||
        route == "add_transaction" ||
        route == "add_renqing_event" ||
        route == "contact_management" ||
        route == "search"
    } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = topBarTitle,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(MotionDurations.SHORT, easing = MotionCurves.EaseOutCubic)) togetherWith
                            fadeOut(animationSpec = tween(MotionDurations.FAST))
                        },
                        label = "topBarTitle"
                    ) { title ->
                        Text(title)
                    }
                },
                navigationIcon = {
                    AnimatedVisibility(
                        visible = showBackButton,
                        enter = fadeIn(tween(MotionDurations.SHORT)) + scaleIn(
                            animationSpec = tween(MotionDurations.SHORT),
                            initialScale = 0.8f
                        ),
                        exit = fadeOut(tween(MotionDurations.FAST)) + scaleOut(
                            animationSpec = tween(MotionDurations.FAST),
                            targetScale = 0.8f
                        )
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = currentRoute == "home",
                        enter = fadeIn(tween(MotionDurations.SHORT)),
                        exit = fadeOut(tween(MotionDurations.FAST))
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
                enter = slideInVertically(
                    animationSpec = tween(MotionDurations.MEDIUM, easing = MotionCurves.StandardDecelerate),
                    initialOffsetY = { it }
                ) + fadeIn(tween(MotionDurations.MEDIUM)),
                exit = slideOutVertically(
                    animationSpec = tween(MotionDurations.SHORT),
                    targetOffsetY = { it }
                ) + fadeOut(tween(MotionDurations.SHORT))
            ) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.route
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.1f else 1f,
                            animationSpec = MotionSprings.gentle(),
                            label = "navIconScale_${item.route}"
                        )
                        val iconAlpha by animateFloatAsState(
                            targetValue = if (selected) 1f else 0.6f,
                            animationSpec = tween(MotionDurations.SHORT),
                            label = "navIconAlpha_${item.route}"
                        )

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier
                                        .size(if (selected) 26.dp else 24.dp)
                                        .scale(scale)
                                        .alpha(iconAlpha)
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(MotionDurations.MEDIUM, easing = MotionCurves.EaseOutCubic)) +
                slideInHorizontally(
                    animationSpec = tween(MotionDurations.MEDIUM, easing = MotionCurves.EaseOutCubic),
                    initialOffsetX = { (it * 0.05f).toInt() }
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(MotionDurations.FAST))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(MotionDurations.MEDIUM, easing = MotionCurves.EaseOutCubic)) +
                slideInHorizontally(
                    animationSpec = tween(MotionDurations.MEDIUM, easing = MotionCurves.EaseOutCubic),
                    initialOffsetX = { -(it * 0.05f).toInt() }
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(MotionDurations.FAST)) +
                slideOutHorizontally(
                    animationSpec = tween(MotionDurations.MEDIUM, easing = MotionCurves.StandardAccelerate),
                    targetOffsetX = { (it * 0.1f).toInt() }
                )
            }
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = {
                        navController.navigate("add_transaction")
                    },
                    onNavigateToSearch = {
                        navController.navigate("search")
                    }
                )
            }
            composable("search") { SearchScreen(viewModel) }
            composable("statistics") { StatisticsScreen(viewModel, navController) }
            composable("add_transaction") {
                AddTransactionScreen(
                    viewModel = viewModel,
                    renQingViewModel = renQingViewModel,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    renQingViewModel = renQingViewModel,
                    onNavigateToCategoryManagement = {
                        navController.navigate("category_management")
                    },
                    onNavigateToContactManagement = {
                        navController.navigate("contact_management")
                    }
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
            composable("renqing") {
                RenQingMainScreen(
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
                route = "category_transactions/{categoryName}/{type}",
                arguments = listOf(
                    navArgument("categoryName") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
                CategoryTransactionsScreen(
                    viewModel = viewModel,
                    categoryName = categoryName,
                    type = type
                )
            }
        }
    }
}
