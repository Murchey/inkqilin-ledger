package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.BackHandler
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    viewModel: TransactionViewModel,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit = onEdit
) {
    val density = LocalDensity.current
    val menuWidth = 120.dp
    val menuWidthPx = with(density) { menuWidth.toPx() }
    
    var offsetX by remember(transaction.id) { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        val newOffset = (offsetX + delta).coerceIn(-menuWidthPx, 0f)
        offsetX = newOffset
    }

    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val expenseColor = Color(expenseColorHex.toColorInt())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(menuWidth)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = {
                    offsetX = 0f
                    onEdit()
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = {
                    offsetX = 0f
                    onDelete()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = expenseColor)
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val target = if (offsetX < -menuWidthPx / 2) -menuWidthPx else 0f
                        animate(
                            initialValue = offsetX,
                            targetValue = target,
                            animationSpec = MotionSprings.interactive() // iOS-like bouncy menu snap
                        ) { value, _ -> offsetX = value }
                    }
                )
        ) {
            TransactionItem(transaction, viewModel, onClick = onClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: com.inkqilin.ledger.data.Category? = null,
    @Suppress("UNUSED_PARAMETER") type: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "📦") }
    var color by remember { mutableStateOf(category?.color ?: "#715CFF") }

    val emojiList = listOf(
        "🍜", "🚗", "🛒", "🎮", "🏠", "📦", "💰", "🎁", "📈", "💼",
        "💳", "🚌", "✈️", "🏥", "📚", "🎵", "🎬", "⚽", "🐱", "🐶",
        "☕", "🍺", "🛍️", "💄", "💇", "🔧", "📱", "💻", "🎓", "🎉",
        "🌿", "🏋️", "🍕", "🍰", "🧋", "🚕", "⛽", "🏡", "🏢", "🏦",
        "👶", "🧹", "💊", "📌", "💡", "🔥", "⭐", "❤️", "✅", "🆕"
    )

    val bgColor = MaterialTheme.colorScheme.background
    val isDark = (bgColor.red * 0.299f + bgColor.green * 0.587f + bgColor.blue * 0.114f) < 0.5f

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = if (category == null) "添加分类" else "修改分类",
        content = {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF007AFF),
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                focusedLabelColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF007AFF),
                unfocusedLabelColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6E6E73),
                cursorColor = if (isDark) Color.White else Color(0xFF1D1D1F),
                focusedTextColor = if (isDark) Color.White else Color(0xFF1D1D1F),
                unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1D1F)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )
                Text(
                    "选择图标",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6E6E73)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(emojiList) { emoji ->
                        val selected = icon == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Color(0xFF007AFF).copy(alpha = 0.12f)
                                    else if (isDark) Color.White.copy(alpha = 0.08f)
                                    else Color.Black.copy(alpha = 0.05f)
                                )
                                .clickable { icon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("或手动输入 Emoji") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )
                Text(
                    "选择颜色",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6E6E73)
                )
                val presetColors = listOf("#715CFF", "#51B4FF", "#4CAF50", "#F44336", "#FF9800", "#9C27B0", "#E91E63")
                var showCategoryColorPicker by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .clickable { color = colorHex }
                                .then(
                                    if (color == colorHex) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == colorHex) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                            }
                        }
                    }
                    // Custom color button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                )
                            )
                            .clickable { showCategoryColorPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "自定义颜色",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (showCategoryColorPicker) {
                    ColorPickerDialog(
                        initialColor = color,
                        onColorSelected = {
                            color = it
                            showCategoryColorPicker = false
                        },
                        onDismiss = { showCategoryColorPicker = false }
                    )
                }
            }
        },
        buttons = listOf(
            AppleDialogButton(
                text = "取消",
                style = AppleDialogButtonStyle.CANCEL,
                onClick = onDismiss
            ),
            AppleDialogButton(
                text = "确定",
                style = AppleDialogButtonStyle.DEFAULT,
                onClick = { if (name.isNotBlank()) onConfirm(name, icon, color) }
            )
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    viewModel: TransactionViewModel,
    onClick: () -> Unit = {}
) {
    val sdf = SimpleDateFormat("MM月dd日", Locale.getDefault())
    val dateStr = sdf.format(Date(transaction.date))

    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val category = allCategories.find { it.name == transaction.category && it.type == transaction.type }
    val icon = category?.icon ?: "📋"
    val isIncome = transaction.type == TransactionType.INCOME

    val allAssets by viewModel.allAssets.collectAsState()
    val currencySymbol = allAssets.firstOrNull { it.code == transaction.currency }?.symbol ?: "¥"

    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()
    val incomeColor = Color(android.graphics.Color.parseColor(incomeColorHex))
    val expenseColor = Color(android.graphics.Color.parseColor(expenseColorHex))

    val interactionSource = remember { MutableInteractionSource() }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource), // Use our custom iOS-style press down
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        interactionSource = interactionSource,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isIncome) incomeColor.copy(alpha = 0.1f)
                        else expenseColor.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${currencySymbol}${String.format("%.2f", transaction.amount)}",
                    color = if (isIncome) incomeColor else expenseColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Apple-style dialog button definition
 */
data class AppleDialogButton(
    val text: String,
    val style: AppleDialogButtonStyle = AppleDialogButtonStyle.DEFAULT,
    val onClick: () -> Unit
)

enum class AppleDialogButtonStyle {
    DEFAULT,     // System blue
    DESTRUCTIVE, // System red
    CANCEL       // Bold system blue
}

// ─── Apple iOS EaseOutCubic ───
private val AppleEaseOutCubic = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)

/**
 * Apple iOS system-grade AlertDialog
 *
 * Uses a full-screen overlay instead of platform Dialog to avoid
 * the white window background that causes the white border issue.
 *
 * Material: frosted glass (light: White@0.82, dark: #1C1C1E)
 * Corner: 14dp continuous curvature (Squircle)
 * No elevation — uses 0.5dp gradient border instead
 * Typography: 17sp SemiBold title, 13sp Normal body (lineHeight 1.4)
 * Buttons: system blue #007AFF, destructive red #FF3B30
 * Press: no ripple, instant gray overlay (alpha=0.1)
 * Entry: scale(1.1)+alpha(0) → scale(1)+alpha(1), 250ms EaseOutCubic
 * Scrim: black alpha 0 → 0.4
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppleAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String? = null,
    content: @Composable (() -> Unit)? = null,
    buttons: List<AppleDialogButton>
) {
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = (bgColor.red * 0.299f + bgColor.green * 0.587f + bgColor.blue * 0.114f) < 0.5f

    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 1.1f,
        animationSpec = tween(durationMillis = 250, easing = AppleEaseOutCubic),
        label = "dialogScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = AppleEaseOutCubic),
        label = "dialogAlpha"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (animateIn) 0.4f else 0f,
        animationSpec = tween(durationMillis = 250, easing = AppleEaseOutCubic),
        label = "scrimAlpha"
    )

    val glassColor = if (isDark) Color(0xFF1C1C1E)
                     else Color.White.copy(alpha = 0.82f)

    val view = LocalView.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LaunchedEffect(Unit) {
            (view.context as? android.app.Activity)?.window?.let { window ->
                window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )

            // Dialog card
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = contentAlpha
                    }
                    .widthIn(min = 260.dp, max = 300.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(glassColor)
                    .drawBehind {
                        val h = size.height
                        val cr = 14.dp.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.12f else 0.35f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f)
                        )
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Content area
                    Column(
                        modifier = Modifier.padding(
                            top = 20.dp, start = 16.dp, end = 16.dp, bottom = 16.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = if (isDark) Color.White else Color(0xFF1D1D1F),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (message != null) {
                            Text(
                                text = message,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                color = if (isDark) Color.White.copy(alpha = 0.65f)
                                        else Color(0xFF6E6E73),
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    lineHeight = (13 * 1.4).sp
                                )
                            )
                        }
                        if (content != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            content()
                        }
                    }

                    // Horizontal divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.1f)
                                else Color.Black.copy(alpha = 0.1f)
                            )
                    )

                    // Button row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        buttons.forEachIndexed { index, button ->
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(0.5.dp)
                                        .height(44.dp)
                                        .background(
                                            if (isDark) Color.White.copy(alpha = 0.1f)
                                            else Color.Black.copy(alpha = 0.1f)
                                        )
                                )
                            }
                            val textColor = when (button.style) {
                                AppleDialogButtonStyle.DESTRUCTIVE -> Color(0xFFFF3B30)
                                else -> if (isDark) MaterialTheme.colorScheme.primary
                                        else Color(0xFF007AFF)
                            }
                            val btnFontWeight = when (button.style) {
                                AppleDialogButtonStyle.CANCEL -> FontWeight.SemiBold
                                else -> FontWeight.Normal
                            }

                            val interactionSource = remember { MutableInteractionSource() }
                            var isPressed by remember { mutableStateOf(false) }
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect { interaction ->
                                    when (interaction) {
                                        is PressInteraction.Press -> isPressed = true
                                        is PressInteraction.Release -> isPressed = false
                                        is PressInteraction.Cancel -> isPressed = false
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = button.onClick
                                    )
                                    .background(
                                        if (isPressed) Color.Gray.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = button.text,
                                    color = textColor,
                                    fontWeight = btnFontWeight,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Transparent-background DatePickerDialog.
 * Uses Dialog with transparent window to eliminate the white border.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppleDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    state: DatePickerState,
    title: String = "选择日期"
) {
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = (bgColor.red * 0.299f + bgColor.green * 0.587f + bgColor.blue * 0.114f) < 0.5f

    // Entry animation
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }
    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 1.1f,
        animationSpec = tween(durationMillis = 250, easing = AppleEaseOutCubic),
        label = "datePickerScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = AppleEaseOutCubic),
        label = "datePickerAlpha"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (animateIn) 0.4f else 0f,
        animationSpec = tween(durationMillis = 250, easing = AppleEaseOutCubic),
        label = "datePickerScrim"
    )

    val view = LocalView.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LaunchedEffect(Unit) {
            (view.context as? android.app.Activity)?.window?.let { window ->
                window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )
            // Dialog card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (isDark) Color(0xFF1C1C1E) else MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = contentAlpha
                    }
                    .widthIn(min = 328.dp, max = 360.dp)
                    .drawBehind {
                        val cr = 28.dp.toPx()
                        val h = size.height
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.12f else 0.35f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f, endY = h
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f)
                        )
                    }
            ) {
                Column {
                    // Title
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 0.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    // Date picker
                    DatePicker(
                        state = state,
                        title = null,
                        headline = null,
                        showModeToggle = true,
                        colors = DatePickerDefaults.colors(
                            containerColor = if (isDark) Color(0xFF1C1C1E) else MaterialTheme.colorScheme.surface,
                            titleContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            headlineContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            weekdayContentColor = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            subheadContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            yearContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            currentYearContentColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            selectedYearContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimary,
                            selectedYearContainerColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            dayContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            disabledDayContentColor = if (isDark) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            selectedDayContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimary,
                            selectedDayContainerColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            todayContentColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            todayDateBorderColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            dayInSelectionRangeContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                            dayInSelectionRangeContainerColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.1f)
                                else Color.Black.copy(alpha = 0.1f)
                            )
                    )
                    // Button row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        dismissButton()
                        Spacer(modifier = Modifier.width(8.dp))
                        confirmButton()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionItemPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("账单条目预览", style = MaterialTheme.typography.titleSmall)
                val sdf = SimpleDateFormat("MM月dd日", Locale.getDefault())
                val now = System.currentTimeMillis()
                val previewTransactions = listOf(
                    Transaction(1, 35.50, "餐饮", "午餐", now, TransactionType.EXPENSE, "CNY"),
                    Transaction(2, 5000.00, "工资", "", now - 86400000, TransactionType.INCOME, "CNY"),
                    Transaction(3, 128.00, "购物", "超市", now - 86400000 * 2, TransactionType.EXPENSE, "CNY")
                )
                previewTransactions.forEach { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isIncome = tx.type == TransactionType.INCOME
                            val iconColor = if (isIncome) Color(0xFF34C759) else Color(0xFFFF3B30)
                            val iconEmoji = when(tx.category) { "餐饮" -> "🍜"; "购物" -> "🛒"; "工资" -> "💰"; else -> "📋" }
                            Box(
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                                    .background(iconColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Text(text = iconEmoji, fontSize = 20.sp) }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = tx.category, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (tx.note.isNotBlank()) {
                                    Text(text = tx.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isIncome) "+" else "-"}¥${String.format("%.2f", tx.amount)}",
                                    color = iconColor, fontWeight = FontWeight.Bold, fontSize = 15.sp
                                )
                                Text(
                                    text = sdf.format(Date(tx.date)),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
