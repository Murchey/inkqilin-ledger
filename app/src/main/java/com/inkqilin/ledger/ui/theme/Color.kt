package com.inkqilin.ledger.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.CurrencyAsset

// ─── Apple HIG Primary Colors ───
val InkPrimary = Color(0xFF34C759)
val InkPrimaryLight = Color(0xFF34C759)
val InkPrimaryDark = Color(0xFF30D158)

// ─── Apple HIG Secondary Colors ───
val InkSecondary = Color(0xFF007AFF)
val InkSecondaryDark = Color(0xFF0A84FF)

// ─── Apple HIG Accent Colors ───
val AppleGreen = Color(0xFF34C759)
val AppleBlue = Color(0xFF007AFF)
val AppleOrange = Color(0xFFFF9F0A)
val AppleRed = Color(0xFFFF3B30)
val AppleYellow = Color(0xFFFFCC00)
val ApplePurple = Color(0xFFAF52DE)
val ApplePink = Color(0xFFFF2D55)
val AppleTeal = Color(0xFF5AC8FA)
val AppleIndigo = Color(0xFF5856D6)

// ─── Apple HIG Light Mode Colors ───
val BackgroundLight = Color(0xFFF5F5F7)
val SurfaceLight = Color(0xFFFFFFFF)
val SecondarySurfaceLight = Color(0xFFEFEFF4)
val OnSurfaceLight = Color(0xFF1D1D1F)
val OnSurfaceVariantLight = Color(0xFF6E6E73)
val OutlineLight = Color(0xFFD1D1D6)
val SurfaceVariantLight = Color(0xFFE5E5EA)

// ─── Apple HIG Dark Mode Colors ───
val BackgroundDark = Color(0xFF0B0B0F)
val SurfaceDark = Color(0xFF111318)
val SecondarySurfaceDark = Color(0xFF161A22)
val OnSurfaceDark = Color(0xFFFFFFFF)
val OnSurfaceVariantDark = Color(0xB3FFFFFF) // rgba(255,255,255,0.65)
val OutlineDark = Color(0xFF2E2F32)
val SurfaceVariantDark = Color(0xFF1C1E24)

// ─── Frosted Glass (Apple-style) ───
val FrostedLight = Color(0xFFF2F2F7)
val FrostedDark = Color(0xFF1C1C1E)
val FrostedBorderLight = Color(0xFFD1D1D6)
val FrostedBorderDark = Color(0xFF38383A)

// ─── Legacy aliases (kept for compatibility) ───
val GoogleBlue = InkPrimaryLight
val GoogleBlueLight = Color(0xFF5AC8FA)
val GoogleGreen = AppleGreen
val GoogleYellow = AppleYellow
val GoogleRed = AppleRed
val GoogleGrey100 = Color(0xFFF2F2F7)
val GoogleGrey200 = Color(0xFFE5E5EA)
val GoogleGrey300 = Color(0xFFD1D1D6)
val GoogleGrey600 = Color(0xFF8E8E93)
val GoogleGrey800 = Color(0xFF48484A)
val GoogleGrey900 = Color(0xFF1C1C1E)

val InkGreen = AppleGreen
val InkRed = AppleRed
val InkYellow = AppleYellow

val NeonGreen = AppleGreen
val NeonBlue = AppleBlue
val NeonPurple = ApplePurple
val NeonCyan = AppleTeal
val NeonPink = ApplePink

data class CardColorPreset(val dark: String, val light: String, val label: String)

/** 币种卡片预置：亮色更明快，暗色低饱和（用于深色界面/深色卡片） */
val CardColorPresets = listOf(
    CardColorPreset("#2E6B52", "#2E9E6A", "青松"),
    CardColorPreset("#2C4F7A", "#2F74D0", "晴蓝"),
    CardColorPreset("#7A5A32", "#D8873E", "暖橙"),
    CardColorPreset("#7A3E3A", "#D75B52", "珊瑚"),
    CardColorPreset("#4A3F72", "#6B5CC4", "藤紫"),
    CardColorPreset("#2A5E62", "#2A8A92", "湖青"),
    CardColorPreset("#2C4864", "#3E6E9E", "藏青"),
    CardColorPreset("#4E4262", "#7B6BA0", "黛紫")
)

/** 主页币种卡片亮色预置（卡片底，白字可读） */
val BrightHomeCardPresets = CardColorPresets.map { it.light }

/** 主页币种卡片暗色预置（低饱和，减轻视觉疲劳） */
val DarkHomeCardPresets = CardColorPresets.map { it.dark }

fun resolveCardColor(asset: CurrencyAsset, isDark: Boolean): Color {
    return try {
        val hex = if (isDark) asset.cardColor else (asset.cardColorLight ?: asset.cardColor)
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    }
}

fun hsvToHex(hue: Float, saturation: Float, value: Float): String {
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    return String.format("#%06X", argb and 0xFFFFFF)
}

@Composable
fun ColorPickerDialog(
    initialColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialArgb = try {
        android.graphics.Color.parseColor(initialColor)
    } catch (_: Exception) {
        0xFF6C63FF.toInt()
    }
    val initialHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialArgb, initialHsv)

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }

    val currentHueColor = Color.hsv(hue, 1f, 1f)
    val currentColor = Color.hsv(hue, saturation, brightness)
    var hexInput by remember { mutableStateOf(hsvToHex(hue, saturation, brightness)) }

    var svPanelSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("自定义颜色", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Saturation-Value panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .onSizeChanged { svPanelSize = it }
                        .pointerInput(Unit) {
                            // 按下即取色，拖动连续更新（detectTapGestures 无法拖拽）
                            val applyOffset: (androidx.compose.ui.geometry.Offset) -> Unit = { offset ->
                                if (svPanelSize.width > 0 && svPanelSize.height > 0) {
                                    saturation = (offset.x / svPanelSize.width).coerceIn(0f, 1f)
                                    brightness = (1f - offset.y / svPanelSize.height).coerceIn(0f, 1f)
                                    hexInput = hsvToHex(hue, saturation, brightness)
                                }
                            }
                            detectDragGestures(
                                onDragStart = applyOffset,
                                onDrag = { change, _ ->
                                    change.consume()
                                    applyOffset(change.position)
                                }
                            )
                        }
                        .drawBehind {
                            // White to hue horizontally
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(Color.White, currentHueColor)
                                )
                            )
                            // Black to transparent vertically
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black)
                                )
                            )
                        }
                ) {
                    // Cursor indicator
                    val cursorX = with(density) { (svPanelSize.width * saturation - 6.dp.toPx()).coerceIn(0f, (svPanelSize.width - 12.dp.toPx()).coerceAtLeast(0f)) }
                    val cursorY = with(density) { (svPanelSize.height * (1f - brightness) - 6.dp.toPx()).coerceIn(0f, (svPanelSize.height - 12.dp.toPx()).coerceAtLeast(0f)) }
                    if (svPanelSize.width > 0) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { cursorX.toDp() },
                                    y = with(density) { cursorY.toDp() }
                                )
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(currentColor)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }

                // Hue bar
                val hueSegments = listOf(
                    0f to Color.Red, 60f to Color.Yellow, 120f to Color.Green,
                    180f to Color.Cyan, 240f to Color.Blue, 300f to Color.Magenta,
                    360f to Color.Red
                )
                var hueBarWidth by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .onSizeChanged { hueBarWidth = it.width.toFloat() }
                        .drawBehind {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    hueSegments.map { it.second }
                                )
                            )
                        }
                        .pointerInput(Unit) {
                            val applyHue: (androidx.compose.ui.geometry.Offset) -> Unit = { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                                hexInput = hsvToHex(hue, saturation, brightness)
                            }
                            detectDragGestures(
                                onDragStart = applyHue,
                                onDrag = { change, _ ->
                                    change.consume()
                                    applyHue(change.position)
                                }
                            )
                        }
                ) {
                    // Hue indicator
                    if (hueBarWidth > 0) {
                        val indicatorXPx = with(density) { (hue / 360f * hueBarWidth - 1.5.dp.toPx()).coerceIn(0f, (hueBarWidth - 3.dp.toPx()).coerceAtLeast(0f)) }
                        Box(
                            modifier = Modifier
                                .offset(x = with(density) { indicatorXPx.toDp() })
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Preview + hex input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { newVal ->
                            hexInput = newVal
                            if (newVal.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                                try {
                                    val argb = android.graphics.Color.parseColor(newVal)
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(argb, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                } catch (_: Exception) {}
                            }
                        },
                        label = { Text("颜色代码", fontSize = 12.sp) },
                        placeholder = { Text("#RRGGBB", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(hsvToHex(hue, saturation, brightness))
                onDismiss()
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
