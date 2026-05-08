package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inkqilin.ledger.ui.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TransactionViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val items = listOf("home" to "首页", "statistics" to "统计", "settings" to "设置")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.List, Icons.Filled.Settings)
    
    val isMainScreen = currentRoute in listOf("home", "statistics", "settings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(when(currentRoute) {
                        "home" -> "墨麒麟记账"
                        "statistics" -> "收支统计"
                        "settings" -> "设置"
                        "add_transaction" -> "记一笔"
                        "search" -> "搜索"
                        else -> "墨麒麟记账"
                    })
                },
                navigationIcon = {
                    if (!isMainScreen) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentRoute == "home") {
                        IconButton(onClick = { navController.navigate("search") }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isMainScreen) {
                NavigationBar {
                    items.forEachIndexed { index, (route, label) ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = { navController.navigate("add_transaction") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -300 },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 300 },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable("home") { HomeScreen(viewModel) }
            composable("statistics") { StatisticsScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }
            composable("add_transaction") { AddTransactionScreen(viewModel, navController) }
            composable("search") { SearchScreen(viewModel) }
        }
    }
}
