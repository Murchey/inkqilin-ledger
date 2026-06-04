package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
    val albumEnabled by viewModel.albumEnabled.collectAsState()
    val isAlbumInteracting by viewModel.isAlbumInteracting.collectAsState()
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
                add(BottomNavItem("album", Icons.Default.Star, "相册"))
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

    val showBottomBar = currentRoute == "main"
    val currentPageRoute = if (pagerState.currentPage < bottomItems.size) {
        bottomItems[pagerState.currentPage].route
    } else {
        "home"
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
        currentRoute == "add_renqing_event" -> "添加事件"
        currentRoute == "category_management" -> "分类管理"
        currentRoute?.startsWith("renqing_contact_detail") == true -> "联系人详情"
        currentRoute?.startsWith("renqing_month_detail") == true -> "月度详情"
        currentRoute?.startsWith("renqing_tag_stats") == true -> "标签统计"
        currentRoute?.startsWith("renqing_contact_analysis") == true -> "关系分析"
        currentRoute?.startsWith("category_transactions") == true -> "分类账单"
        currentRoute == "contact_management" -> "联系人管理"
        currentRoute == "currency_management" -> "币种卡片管理"
        currentRoute == "keyword_category_management" -> "关键词管理"
        currentRoute == "ai_config" -> "AI API 配置"
        currentRoute == "ocr_batch_recognition" -> "OCR 批量识别"
        else -> "墨麒麟记账"
    }

    val showBackButton = currentRoute != "main"

    val showTopBar = currentRoute != "main" || currentPageRoute != "album"

    // FAB menu state (rendered in bottom bar, shared across pages)
    var showFabMenu by remember { mutableStateOf(false) }
    var albumFabTrigger by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ocrEnabled by viewModel.ocrEnabled.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AnimatedVisibility(
                visible = showTopBar,
                enter = if (enableAnimations) {
                    fadeIn(MotionSprings.appearance()) + slideInVertically(
                        animationSpec = MotionSprings.appearance(),
                        initialOffsetY = { -it }
                    )
                } else EnterTransition.None,
                exit = if (enableAnimations) {
                    fadeOut(MotionSprings.appearance()) + slideOutVertically(
                        animationSpec = MotionSprings.appearance(),
                        targetOffsetY = { -it }
                    )
                } else ExitTransition.None
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
            }
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
                // ══════════════════════════════════════════════
                //  Apple Liquid Glass Navigation Bar
                //  Optical glass · Refraction · Depth
                // ══════════════════════════════════════════════
                val bgLuminance = MaterialTheme.colorScheme.background.let {
                    it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f
                }
                val isDarkMode = bgLuminance < 0.5f
                val unselectedColor = if (isDarkMode) Color(0xFFAEAEB2) else Color(0xFF636366)
                val selectedColor = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF1D1D1F)
                val barRadius = 36.dp
                val density = androidx.compose.ui.platform.LocalDensity.current
                val barCornerPx = with(density) { barRadius.toPx() }
                val fabSize = 48.dp
                val fabRadius = 24.dp

                val showFab = currentPageRoute == "home" || currentPageRoute == "album"
                val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)

                // ── Glass lens indicator position ──
                var indicatorCenterX by remember { mutableFloatStateOf(0f) }
                var indicatorWidth by remember { mutableStateOf(0.dp) }
                val animIndicatorX by animateFloatAsState(
                    targetValue = indicatorCenterX,
                    animationSpec = if (enableAnimations)
                        spring(dampingRatio = 0.65f, stiffness = 280f)
                    else snap(),
                    label = "glassBubbleX"
                )
                val animIndicatorW by animateDpAsState(
                    targetValue = indicatorWidth,
                    animationSpec = if (enableAnimations)
                        spring(dampingRatio = 0.7f, stiffness = 320f)
                    else snap(),
                    label = "glassBubbleW"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 4.dp, bottom = navBottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Liquid Glass Container ──
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                shadowElevation = if (isDarkMode) 8f else 4f
                                shape = RoundedCornerShape(barRadius)
                                clip = false
                                ambientShadowColor = Color.Black.copy(alpha = if (isDarkMode) 0.20f else 0.06f)
                                spotShadowColor = Color.Black.copy(alpha = if (isDarkMode) 0.15f else 0.04f)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(barRadius))
                                .drawBehind {
                                    val w = size.width
                                    val h = size.height
                                    val cr = barCornerPx

                                    // ── L1: Glass Base ──
                                    // Barely tinted — content shows through
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = if (isDarkMode) 0.10f else 0.35f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr)
                                    )

                                    // ── L2: Environmental Light (top-left) ──
                                    // Simulates directional ambient light
                                    drawRoundRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkMode) 0.08f else 0.22f),
                                                Color.Transparent
                                            ),
                                            center = androidx.compose.ui.geometry.Offset(w * 0.25f, 0f),
                                            radius = w * 0.55f
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr)
                                    )

                                    // ── L3: Specular Band ──
                                    // Bright concentrated reflection where light hits the curved glass
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkMode) 0.25f else 0.50f),
                                                Color.White.copy(alpha = if (isDarkMode) 0.04f else 0.08f),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = h * 0.38f
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr)
                                    )

                                    // ── L4: Edge Refraction ──
                                    // Light bends at curved glass boundaries
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkMode) 0.07f else 0.14f),
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.White.copy(alpha = if (isDarkMode) 0.07f else 0.14f)
                                            )
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr)
                                    )

                                    // ── L5: Internal Depth Shadow ──
                                    // Glass physically thick — bottom is darker
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = if (isDarkMode) 0.15f else 0.08f)
                                            ),
                                            startY = h * 0.55f,
                                            endY = h
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr)
                                    )

                                    // ── L6: Reflective Rim ──
                                    // Thin glass boundary definition
                                    drawRoundRect(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkMode) 0.06f else 0.18f),
                                                Color.White.copy(alpha = if (isDarkMode) 0.14f else 0.35f),
                                                Color.White.copy(alpha = if (isDarkMode) 0.06f else 0.18f),
                                                Color.White.copy(alpha = if (isDarkMode) 0.10f else 0.25f),
                                                Color.White.copy(alpha = if (isDarkMode) 0.06f else 0.18f)
                                            )
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8f)
                                    )

                                    // ── L7: Top Razor Highlight ──
                                    // Brightest edge — catches direct light
                                    drawLine(
                                        color = Color.White.copy(alpha = if (isDarkMode) 0.28f else 0.70f),
                                        start = androidx.compose.ui.geometry.Offset(cr * 0.8f, 0.5f),
                                        end = androidx.compose.ui.geometry.Offset(w - cr * 0.8f, 0.5f),
                                        strokeWidth = 1.0f
                                    )

                                    // ── L8: Bottom Subtle Edge ──
                                    // Thin dark line defines glass thickness
                                    drawLine(
                                        color = Color.Black.copy(alpha = if (isDarkMode) 0.12f else 0.05f),
                                        start = androidx.compose.ui.geometry.Offset(cr * 0.8f, h - 0.5f),
                                        end = androidx.compose.ui.geometry.Offset(w - cr * 0.8f, h - 0.5f),
                                        strokeWidth = 0.5f
                                    )
                                }
                                .padding(horizontal = 4.dp, vertical = 5.dp)
                        ) {
                            // ── Convex Glass Lens Indicator ──
                            // Transparent optical glass — defined by highlights, not fill
                            if (animIndicatorW > 0.dp) {
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            val centerXPx = animIndicatorX.toInt()
                                            val halfW = (animIndicatorW.roundToPx() / 2)
                                            IntOffset(centerXPx - halfW, 0)
                                        }
                                        .size(width = animIndicatorW, height = 42.dp)
                                        .drawBehind {
                                            val bcr = size.height / 2f
                                            val bh = size.height
                                            val bw = size.width

                                            // L1: Near-invisible glass body
                                            // The lens shape is defined by light, not fill
                                            drawRoundRect(
                                                color = Color.White.copy(alpha = if (isDarkMode) 0.04f else 0.10f),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bcr)
                                            )

                                            // L2: Central convex glow
                                            // Simulates light focused through a curved lens
                                            drawRoundRect(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = if (isDarkMode) 0.20f else 0.38f),
                                                        Color.White.copy(alpha = if (isDarkMode) 0.04f else 0.08f),
                                                        Color.Transparent
                                                    ),
                                                    center = androidx.compose.ui.geometry.Offset(bw * 0.5f, bh * 0.38f),
                                                    radius = bw * 0.42f
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bcr)
                                            )

                                            // L3: Top specular arc
                                            // Bright crescent where light hits the convex dome
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = if (isDarkMode) 0.40f else 0.68f),
                                                        Color.White.copy(alpha = if (isDarkMode) 0.10f else 0.18f),
                                                        Color.Transparent
                                                    ),
                                                    startY = 0f,
                                                    endY = bh * 0.38f
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bcr)
                                            )

                                            // L4: Left edge refraction band
                                            drawRoundRect(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = if (isDarkMode) 0.18f else 0.32f),
                                                        Color.White.copy(alpha = if (isDarkMode) 0.02f else 0.04f),
                                                        Color.Transparent,
                                                        Color.Transparent
                                                    ),
                                                    endX = bw * 0.35f
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bcr)
                                            )

                                            // L5: Right edge refraction band
                                            drawRoundRect(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Transparent,
                                                        Color.White.copy(alpha = if (isDarkMode) 0.02f else 0.04f),
                                                        Color.White.copy(alpha = if (isDarkMode) 0.18f else 0.32f)
                                                    ),
                                                    startX = bw * 0.65f
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bcr)
                                            )

                                            // L6: Bottom depth shadow (glass thickness)
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = if (isDarkMode) 0.12f else 0.06f)
                                                    ),
                                                    startY = bh * 0.55f,
                                                    endY = bh
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bcr)
                                            )

                                            // L7: Bright inner edge — glass boundary catches light
                                            drawRoundRect(
                                                brush = Brush.sweepGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = if (isDarkMode) 0.08f else 0.20f),
                                                        Color.White.copy(alpha = if (isDarkMode) 0.22f else 0.45f),
                                                        Color.White.copy(alpha = if (isDarkMode) 0.08f else 0.20f),
                                                        Color.White.copy(alpha = if (isDarkMode) 0.15f else 0.32f),
                                                        Color.White.copy(alpha = if (isDarkMode) 0.08f else 0.20f)
                                                    )
                                                ),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bcr),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8f)
                                            )

                                            // L8: Top razor highlight
                                            drawLine(
                                                color = Color.White.copy(alpha = if (isDarkMode) 0.45f else 0.85f),
                                                start = androidx.compose.ui.geometry.Offset(bcr * 0.6f, 0.5f),
                                                end = androidx.compose.ui.geometry.Offset(bw - bcr * 0.6f, 0.5f),
                                                strokeWidth = 1.0f
                                            )

                                            // L9: Second highlight band (convex double-reflection)
                                            drawLine(
                                                color = Color.White.copy(alpha = if (isDarkMode) 0.12f else 0.25f),
                                                start = androidx.compose.ui.geometry.Offset(bcr * 0.8f, 3.0f),
                                                end = androidx.compose.ui.geometry.Offset(bw - bcr * 0.8f, 3.0f),
                                                strokeWidth = 0.6f
                                            )
                                        }
                                )
                            }

                            // ── Tab Icons ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                bottomItems.forEachIndexed { index, item ->
                                    val selected = pagerState.currentPage == index
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (selected) 1.1f else 1f,
                                        animationSpec = if (enableAnimations) MotionSprings.interactive() else snap(),
                                        label = "navIconScale_${item.route}"
                                    )
                                    val iconColor by animateColorAsState(
                                        targetValue = if (selected) selectedColor else unselectedColor,
                                        animationSpec = if (enableAnimations) MotionSprings.interactive() else snap(),
                                        label = "navColor_${item.route}"
                                    )
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
                                                            coords, androidx.compose.ui.geometry.Offset(localCenter.toFloat(), 0f)
                                                        )
                                                        indicatorCenterX = posInParent.x
                                                        indicatorWidth = with(density) { (coords.size.width * 1.2f).toDp() }
                                                    }
                                                }
                                            }
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                scope.launch { pagerState.animateScrollToPage(index) }
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(21.dp).scale(iconScale),
                                            tint = iconColor
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = iconColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Liquid Glass FAB ──
                    if (showFab) {
                        val fabInteractionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(fabSize)
                                .graphicsLayer {
                                    shadowElevation = if (isDarkMode) 8f else 4f
                                    shape = RoundedCornerShape(fabRadius)
                                    clip = false
                                    ambientShadowColor = Color.Black.copy(alpha = if (isDarkMode) 0.20f else 0.06f)
                                    spotShadowColor = Color.Black.copy(alpha = if (isDarkMode) 0.15f else 0.04f)
                                }
                                .clip(RoundedCornerShape(fabRadius))
                                .drawBehind {
                                    val w = size.width
                                    val h = size.height
                                    val fcr = fabRadius.toPx()

                                    // Glass base
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = if (isDarkMode) 0.10f else 0.35f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(fcr)
                                    )

                                    // Subtle green tint
                                    drawRoundRect(
                                        color = (if (isDarkMode) Color(0xFF30D158) else Color(0xFF34C759))
                                            .copy(alpha = if (isDarkMode) 0.20f else 0.14f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(fcr)
                                    )

                                    // Specular band
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkMode) 0.25f else 0.50f),
                                                Color.White.copy(alpha = if (isDarkMode) 0.04f else 0.08f),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = h * 0.40f
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(fcr)
                                    )

                                    // Edge refraction
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkMode) 0.08f else 0.15f),
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.White.copy(alpha = if (isDarkMode) 0.08f else 0.15f)
                                            )
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(fcr)
                                    )

                                    // Bottom depth
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = if (isDarkMode) 0.12f else 0.06f)
                                            ),
                                            startY = h * 0.55f,
                                            endY = h
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(fcr)
                                    )

                                    // Reflective rim
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = if (isDarkMode) 0.12f else 0.30f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(fcr),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.6f)
                                    )

                                    // Top razor highlight
                                    drawLine(
                                        color = Color.White.copy(alpha = if (isDarkMode) 0.28f else 0.70f),
                                        start = androidx.compose.ui.geometry.Offset(fcr * 0.8f, 0.5f),
                                        end = androidx.compose.ui.geometry.Offset(w - fcr * 0.8f, 0.5f),
                                        strokeWidth = 0.8f
                                    )
                                }
                                .clickable(
                                    interactionSource = fabInteractionSource,
                                    indication = null
                                ) {
                                    when (currentPageRoute) {
                                        "home" -> showFabMenu = true
                                        "album" -> albumFabTrigger = true
                                    }
                                }
                                .pressScale(fabInteractionSource),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "记一笔",
                                tint = if (isDarkMode) Color(0xFF30D158) else Color(0xFF34C759),
                                modifier = Modifier.size(22.dp)
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
                    userScrollEnabled = !isAlbumInteracting,
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
                            },
                            onNavigateToAssetManagement = {
                                navController.navigate("asset_management")
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
                            onNavigateToKeywordCategoryManagement = {
                                navController.navigate("keyword_category_management")
                            },
                            onNavigateToContactManagement = {
                                navController.navigate("contact_management")
                            },
                            onNavigateToCurrencyManagement = {
                                navController.navigate("currency_management")
                            },
                            onNavigateToAIConfig = {
                                navController.navigate("ai_config")
                            },
                            onNavigateToOCRConfig = {
                                navController.navigate("ocr_config")
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
            composable("asset_management") {
                AssetManagementScreen(
                    viewModel = viewModel
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
                            navController.navigate("ocr_batch_recognition")
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
                        navController.navigate("asset_management")
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
                        navController.navigate("add_transaction")
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
