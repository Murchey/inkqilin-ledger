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
import com.inkqilin.ledger.data.AppDatabase
import com.inkqilin.ledger.data.CycleType
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.util.NotificationHelper
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/** 二级页导航：避免同一路由重复压栈，导致系统返回需要连按多次 */
fun androidx.navigation.NavController.navigateSingle(route: String) {
    navigate(route) { launchSingleTop = true }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel,
    enableAnimations: Boolean = true,
    externalNavTarget: String? = null,
    onExternalTargetHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
    val albumEnabled by viewModel.albumEnabled.collectAsState()
    val isAlbumInteracting by viewModel.isAlbumInteracting.collectAsState()
    val customPrimaryColorHex by viewModel.customPrimaryColorHex.collectAsState()
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

    val bottomItems = remember(renQingEnabled, albumEnabled) {
        buildList {
            add(baseItems[0])
            add(baseItems[1])
            if (albumEnabled) {
                add(BottomNavItem("album", Icons.Default.Email, "相册"))
            }
            if (renQingEnabled) {
                add(BottomNavItem("renqing", Icons.Default.Favorite, "人情"))
            }
            add(baseItems[2])
        }
    }

    val pagerState = rememberPagerState { bottomItems.size }

    LaunchedEffect(bottomItems.size) {
        if (pagerState.currentPage >= bottomItems.size) {
            pagerState.scrollToPage(0)
        }
    }

    // 桌面小部件外部导航目标（冷启动/热启动均可到达）
    LaunchedEffect(externalNavTarget) {
        val target = externalNavTarget ?: return@LaunchedEffect
        when {
            target == "main" || target == "home" -> {
                if (navController.currentDestination?.route != "main") {
                    navController.navigate("main") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            target == "settings" -> {
                val index = bottomItems.indexOfFirst { it.route == "settings" }
                if (index != -1) {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                        if (navController.currentDestination?.route != "main") {
                            navController.navigate("main") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
            else -> runCatching { navController.navigateSingle(target) }
        }
        onExternalTargetHandled()
    }

    val currentPageRoute = if (pagerState.currentPage < bottomItems.size) {
        bottomItems[pagerState.currentPage].route
    } else {
        "home"
    }
    // 仅在「相册 Tab 且交互中」隐藏底栏，避免 isAlbumInteracting 卡死后无法恢复
    val showBottomBar = currentRoute == "main" && !(currentPageRoute == "album" && isAlbumInteracting)

    // 主页非「首页」Tab 时，系统返回先回首页 Tab，而不是直接退出 App
    BackHandler(enabled = currentRoute == "main" && currentPageRoute != "home") {
        viewModel.setAlbumInteracting(false)
        scope.launch {
            val homeIndex = bottomItems.indexOfFirst { it.route == "home" }
            if (homeIndex != -1) {
                pagerState.animateScrollToPage(homeIndex)
            }
        }
    }

    // 离开相册 Tab 后强制复位底栏隐藏标记
    LaunchedEffect(currentPageRoute, currentRoute) {
        if (currentRoute != "main" || currentPageRoute != "album") {
            viewModel.setAlbumInteracting(false)
        }
    }

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
                "album" -> "记账相册"
                "settings" -> "设置"
                else -> "墨麒麟记账"
            }
        }
        currentRoute == "search" -> "搜索"
        currentRoute == "add_transaction" -> "记一笔"
        currentRoute?.startsWith("edit_transaction") == true -> "编辑账单"
        currentRoute == "add_renqing_event" -> "添加事件"
        currentRoute == "category_management" -> "分类管理"
        currentRoute?.startsWith("renqing_contact_detail") == true -> "联系人详情"
        currentRoute?.startsWith("renqing_month_detail") == true -> "月度详情"
        currentRoute?.startsWith("renqing_tag_stats") == true -> "标签统计"
        currentRoute?.startsWith("renqing_contact_analysis") == true -> "关系分析"
        currentRoute == "renqing_stats" -> "人情统计"
        currentRoute?.startsWith("category_transactions") == true -> "分类账单"
        currentRoute == "contact_management" -> "联系人管理"
        currentRoute == "currency_management" -> "币种卡片管理"
        currentRoute == "keyword_category_management" -> "关键词管理"
        currentRoute == "ai_config" -> "AI API 配置"
        currentRoute == "cloud_backup" -> "数据备份"
        currentRoute == "ocr_batch_recognition" -> "OCR 批量识别"
        currentRoute == "asset_management" -> "资产管理"
        currentRoute?.startsWith("cycle_bill_edit") == true -> {
            val billId = navBackStackEntry?.arguments?.getLong("billId") ?: 0L
            if (billId > 0L) "编辑周期账单" else "新建周期账单"
        }
        currentRoute == "cycle_bill_list" -> "周期账单"
        currentRoute == "recycle_bin" -> "回收站"
        else -> "墨麒麟记账"
    }

    val showBackButton = currentRoute != "main"

    val showTopBar = currentRoute != "main" || currentPageRoute != "album"

    // FAB menu state (rendered in bottom bar, shared across pages)
    var showFabMenu by remember { mutableStateOf(false) }
    
    // 子页面可覆盖的 TopAppBar 状态
    var customTopBarTitle by remember { mutableStateOf<String?>(null) }
    var customBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var cloudBackupOpenSettings by remember { mutableStateOf(false) }
    var albumFabTrigger by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ocrEnabled by viewModel.ocrEnabled.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AnimatedVisibility(
                visible = showTopBar,
                enter = if (enableAnimations) {
                    fadeIn(MotionSprings.appearanceTween()) + slideInVertically(
                        animationSpec = MotionSprings.appearanceTween(),
                        initialOffsetY = { -it }
                    )
                } else EnterTransition.None,
                exit = if (enableAnimations) {
                    fadeOut(MotionSprings.appearanceTween()) + slideOutVertically(
                        animationSpec = MotionSprings.appearanceTween(),
                        targetOffsetY = { -it }
                    )
                } else ExitTransition.None
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        AnimatedContent(
                            targetState = customTopBarTitle ?: topBarTitle,
                            transitionSpec = {
                                if (enableAnimations) {
                                    fadeIn(tween(MotionDurations.FAST)) togetherWith
                                        fadeOut(tween(MotionDurations.FAST))
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
                            IconButton(onClick = { (customBackAction ?: { navController.popBackStack() })() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        // 主页搜索按钮
                        AnimatedVisibility(
                            visible = currentRoute == "main" && currentPageRoute == "home",
                            enter = if (enableAnimations) fadeIn(MotionSprings.interactive()) else EnterTransition.None,
                            exit = if (enableAnimations) fadeOut(MotionSprings.interactive()) else ExitTransition.None
                        ) {
                            IconButton(onClick = { navController.navigateSingle("search") }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        }
                        // 人情账本添加按钮
                        AnimatedVisibility(
                            visible = currentRoute == "main" && currentPageRoute == "renqing",
                            enter = if (enableAnimations) fadeIn(MotionSprings.interactive()) else EnterTransition.None,
                            exit = if (enableAnimations) fadeOut(MotionSprings.interactive()) else ExitTransition.None
                        ) {
                            IconButton(onClick = { navController.navigateSingle("add_renqing_event") }) {
                                Icon(Icons.Default.Add, contentDescription = "添加事件")
                            }
                        }
                        // 云备份：COS 设置
                        AnimatedVisibility(
                            visible = currentRoute == "cloud_backup",
                            enter = if (enableAnimations) fadeIn(MotionSprings.interactive()) else EnterTransition.None,
                            exit = if (enableAnimations) fadeOut(MotionSprings.interactive()) else ExitTransition.None
                        ) {
                            IconButton(onClick = { cloudBackupOpenSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "COS 设置")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = if (enableAnimations) {
                    slideInVertically(
                        animationSpec = MotionSprings.appearanceTween(),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = MotionSprings.appearanceTween())
                } else {
                    EnterTransition.None
                },
                exit = if (enableAnimations) {
                    slideOutVertically(
                        animationSpec = MotionSprings.appearanceTween(),
                        targetOffsetY = { it }
                    ) + fadeOut(animationSpec = MotionSprings.appearanceTween())
                } else {
                    ExitTransition.None
                }
            ) {
                // ══════════════════════════════════════════════
                //  Apple Music Style Floating Tab Bar
                //  Lightweight · Minimal · Subtle
                // ══════════════════════════════════════════════
                val bgLuminance = MaterialTheme.colorScheme.background.let {
                    it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f
                }
                val isDarkMode = bgLuminance < 0.5f

                // Apple Music colors: subtle in dark, clearly elevated in light
                val unselectedColor = if (isDarkMode) Color.White.copy(alpha = 0.55f) else Color(0xFF8E8E93)
                val selectedColor = if (isDarkMode) Color.White else Color(0xFF1D1D1F)

                // Compact container
                val barRadius = 22.dp
                val density = androidx.compose.ui.platform.LocalDensity.current
                val fabSize = 44.dp
                val fabRadius = 22.dp

                val showFab = currentPageRoute == "home" || currentPageRoute == "album"
                val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)

                // ── Smooth indicator position：只在值变化时写入，避免 layout 回环重组 ──
                var indicatorCenterX by remember { mutableFloatStateOf(0f) }
                var indicatorWidth by remember { mutableStateOf(0.dp) }
                val animIndicatorX by animateFloatAsState(
                    targetValue = indicatorCenterX,
                    animationSpec = if (enableAnimations)
                        tween(durationMillis = MotionDurations.FAST, easing = MotionCurves.FastOutSlowIn)
                    else snap(),
                    label = "tabIndicatorX"
                )
                val animIndicatorW by animateDpAsState(
                    targetValue = indicatorWidth,
                    animationSpec = if (enableAnimations)
                        tween(durationMillis = MotionDurations.FAST, easing = MotionCurves.FastOutSlowIn)
                    else snap(),
                    label = "tabIndicatorW"
                )
                val selectedIndex by remember { derivedStateOf { pagerState.currentPage } }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = navBottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Apple Music Tab Bar Container ──
                    // 静态边框 + 填充，去掉 shadowElevation（低端机阴影渲染昂贵）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(barRadius))
                            .background(
                                if (isDarkMode) Color(0xFF1C1C1E).copy(alpha = 0.96f)
                                else Color.White.copy(alpha = 0.98f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.16f)
                                        else Color(0xFFD1D1D6).copy(alpha = 0.9f),
                                shape = RoundedCornerShape(barRadius)
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        // ── Apple Photos Style Indicator ──
                        if (animIndicatorW > 0.dp) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        val centerXPx = animIndicatorX.toInt()
                                        val halfW = animIndicatorW.roundToPx() / 2
                                        IntOffset(centerXPx - halfW, 0)
                                    }
                                    .size(width = animIndicatorW, height = 34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isDarkMode) Color.White.copy(alpha = 0.08f)
                                        else Color.Black.copy(alpha = 0.08f)
                                    )
                            )
                        }

                        // ── Tab Items ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomItems.forEachIndexed { index, item ->
                                val selected = selectedIndex == index
                                // 滑动/切页不再逐 tab 跑颜色弹簧，直接取色
                                val iconColor = if (selected) selectedColor else unselectedColor

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coords ->
                                            if (selected) {
                                                val parentCoords = coords.parentCoordinates
                                                if (parentCoords != null) {
                                                    val localCenter = coords.size.width / 2
                                                    val posInParent = parentCoords.localPositionOf(
                                                        coords,
                                                        androidx.compose.ui.geometry.Offset(localCenter.toFloat(), 0f)
                                                    )
                                                    val cx = posInParent.x
                                                    val w = with(density) { (coords.size.width * 0.9f).toDp() }
                                                    // 仅在变化时写 state，打断 onGloballyPositioned → recompose 回环
                                                    if (cx != indicatorCenterX) indicatorCenterX = cx
                                                    if (w != indicatorWidth) indicatorWidth = w
                                                }
                                            }
                                        }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                        .padding(vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(21.dp),
                                        tint = iconColor
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = iconColor
                                    )
                                }
                            }
                        }
                    }

                    // ── Apple Music Style FAB ──
                    if (showFab) {
                        val fabInteractionSource = remember { MutableInteractionSource() }
                        val fabScale = animatePressScale(fabInteractionSource)
                        val fabColor = remember(customPrimaryColorHex) {
                            val hex = customPrimaryColorHex ?: DEFAULT_PRIMARY_COLOR_HEX
                            try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF34C759) }
                        }

                        Box(
                            modifier = Modifier
                                .size(fabSize)
                                .clip(RoundedCornerShape(fabRadius))
                                .background(fabColor)
                                .clickable(
                                    interactionSource = fabInteractionSource,
                                    indication = null
                                ) {
                                    when (currentPageRoute) {
                                        "home" -> showFabMenu = true
                                        "album" -> albumFabTrigger = true
                                    }
                                }
                                .pressScale(fabScale),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "记一笔",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isAlbumInteracting,
                    beyondBoundsPageCount = 0
                ) { page ->
                    when (bottomItems[page].route) {
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
                            fabTrigger = albumFabTrigger,
                            onFabTriggered = { albumFabTrigger = false }
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
                    openSettings = cloudBackupOpenSettings,
                    onOpenSettingsConsumed = { cloudBackupOpenSettings = false }
                )
                DisposableEffect(Unit) {
                    onDispose { cloudBackupOpenSettings = false }
                }
            }
            composable("asset_management") {
                AssetManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle = title
                        customBackAction = backAction
                    }
                )
                DisposableEffect(Unit) {
                    onDispose {
                        customTopBarTitle = null
                        customBackAction = null
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
                        val nextCycleEnd = cycleBoundary(txDate, bill.cycleType)
                        val updatedBill = bill.copy(
                            lastGeneratedDate = txDate,
                            currentCycleStart = txDate,
                            currentCycleEnd = nextCycleEnd,
                            nextTriggerDate = nextCycleEnd,
                            overdue = false
                        )
                        scope.launch {
                            val appDb = AppDatabase.getDatabase(ctx)
                            appDb.transactionDao().insertTransaction(
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
                            appDb.cycleBillDao().updateCycleBill(updatedBill)
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
                        customTopBarTitle = title
                        customBackAction = backAction
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
                        customTopBarTitle = title
                        customBackAction = backAction
                    }
                )
            }
            composable("recycle_bin") {
                RecycleBinScreen(
                    onBack = { navController.popBackStack() },
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle = title
                        customBackAction = backAction
                    }
                )
            }
            composable("calculator_hub") {
                CalculatorScreen(
                    initialType = null,
                    onUpdateTopBar = { title, backAction ->
                        customTopBarTitle = title
                        customBackAction = backAction
                    }
                )
                DisposableEffect(Unit) {
                    onDispose {
                        customTopBarTitle = null
                        customBackAction = null
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
                        customTopBarTitle = title
                        customBackAction = backAction
                    }
                )
                DisposableEffect(Unit) {
                    onDispose {
                        customTopBarTitle = null
                        customBackAction = null
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

    // FAB Bottom Sheet (shared across pages, triggered from bottom bar)
    if (showFabMenu) {
        ModalBottomSheet(
            onDismissRequest = { showFabMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(36.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                if (ocrEnabled) {
                    Surface(
                        onClick = {
                            showFabMenu = false
                            navController.navigateSingle("ocr_batch_recognition")
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("OCR 批量识别", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("拍照或选择图片自动识别账单", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Surface(
                    onClick = {
                        showFabMenu = false
                        navController.navigateSingle("asset_management")
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("资产管理", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("管理多币种资产和账户", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = {
                        showFabMenu = false
                        navController.navigateSingle("cycle_bill_list")
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("周期账单", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("管理周期性账单和提醒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = {
                        showFabMenu = false
                        navController.navigateSingle("calculator_hub")
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("多功能计算", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("复利、个税、储蓄、分期计算器", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = {
                        showFabMenu = false
                        navController.navigateSingle("add_transaction")
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("手动记一笔", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                            Text("手动输入单条账单", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}
