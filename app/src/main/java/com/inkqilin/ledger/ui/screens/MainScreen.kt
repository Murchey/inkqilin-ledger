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

    // 以路由而非下标记录当前 Tab：开关人情/相册会增减底部项，下标会错位
    var selectedTabRoute by rememberSaveable { mutableStateOf("home") }
    var aligningPagerToRoute by remember { mutableStateOf(false) }

    // Tab 集合变化后把 pager 对齐到当前路由，避免停留在错误页（跳转走）
    LaunchedEffect(renQingEnabled, albumEnabled) {
        val target = bottomItems.indexOfFirst { it.route == selectedTabRoute }
        val page = if (target >= 0) target else 0
        if (pagerState.currentPage != page || pagerState.settledPage != page) {
            aligningPagerToRoute = true
            try {
                pagerState.scrollToPage(page)
            } finally {
                aligningPagerToRoute = false
            }
        }
    }

    // 用户滑动停稳后记录路由；对齐过程中不回写，防止选中项被错位下标覆盖
    LaunchedEffect(pagerState.settledPage) {
        if (aligningPagerToRoute) return@LaunchedEffect
        bottomItems.getOrNull(pagerState.settledPage)?.let { selectedTabRoute = it.route }
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
                    selectedTabRoute = "settings"
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
        selectedTabRoute = "home"
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
        currentRoute?.startsWith("backup_auto_schedule") == true -> "自动备份设置"
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
    val customTopBarTitle = remember { mutableStateOf<String?>(null) }
    val customBackAction = remember { mutableStateOf<(() -> Unit)?>(null) }
    val cloudBackupOpenSettings = remember { mutableStateOf(false) }
    val albumFabTrigger = remember { mutableStateOf(false) }
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
                            targetState = customTopBarTitle.value ?: topBarTitle,
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
                            IconButton(onClick = { (customBackAction.value ?: { navController.popBackStack() })() }) {
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
                            IconButton(onClick = { cloudBackupOpenSettings.value = true }) {
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
                // 外框与选中指示器共用胶囊圆角，避免「大方角 bar + 小圆角选中块」形状打架
                val capsuleShape = RoundedCornerShape(percent = 50)
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
                    // 静态边框 + 填充，去掉 shadowElevation（低端机阴影渲染昂贵）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(capsuleShape)
                            .background(
                                if (isDarkMode) Color(0xFF1C1C1E).copy(alpha = 0.96f)
                                else Color.White.copy(alpha = 0.98f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.16f)
                                        else Color(0xFFD1D1D6).copy(alpha = 0.9f),
                                shape = capsuleShape
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        // 内容高度由 Tab 行决定；指示器用 IntrinsicSize.Min 对齐同高，再 clip 成胶囊
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
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
                                        .fillMaxHeight()
                                        .width(animIndicatorW)
                                        .clip(capsuleShape)
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
                                                selectedTabRoute = item.route
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
                    }

                    // ── Apple Music Style FAB ──
                    if (showFab) {
                        val fabInteractionSource = remember { MutableInteractionSource() }
                        val fabScale = animatePressScale(fabInteractionSource)
                        val fabColor = remember(customPrimaryColorHex) {
                            val hex = customPrimaryColorHex ?: DEFAULT_PRIMARY_COLOR_HEX
                            try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF007AFF) }
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
                                        "album" -> albumFabTrigger.value = true
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
        MainNavGraph(
            navController = navController,
            viewModel = viewModel,
            renQingViewModel = renQingViewModel,
            enableAnimations = enableAnimations,
            innerPadding = innerPadding,
            currentRoute = currentRoute,
            pagerState = pagerState,
            bottomItems = bottomItems,
            albumFabTrigger = albumFabTrigger,
            cloudBackupOpenSettings = cloudBackupOpenSettings,
            customTopBarTitle = customTopBarTitle,
            customBackAction = customBackAction,
        )
    }

    // FAB Bottom Sheet (shared across pages, triggered from bottom bar)
    if (showFabMenu) {
        ModalBottomSheet(
            onDismissRequest = { showFabMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = com.inkqilin.ledger.ui.theme.Corners.SheetTop,
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

