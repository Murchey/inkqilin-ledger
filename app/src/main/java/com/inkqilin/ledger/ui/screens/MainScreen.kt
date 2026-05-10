package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel

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
        route == "search"
    } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (currentRoute == "home") {
                        IconButton(onClick = { navController.navigate("search") }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
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
            modifier = Modifier.padding(innerPadding)
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
                    }
                )
            }
            composable("category_management") {
                CategoryManagementScreen(
                    viewModel = viewModel,
                    renQingViewModel = renQingViewModel
                )
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
