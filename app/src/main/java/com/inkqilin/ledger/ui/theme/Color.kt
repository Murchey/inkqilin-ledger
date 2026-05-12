package com.inkqilin.ledger.ui.theme

import androidx.compose.ui.graphics.Color
import com.inkqilin.ledger.data.CurrencyAsset

val InkPrimary = Color(0xFF3F51B5)
val InkPrimaryLight = Color(0xFF3F51B5)
val InkPrimaryDark = Color(0xFF3F51B5)

val GoogleBlue = InkPrimaryLight
val GoogleBlueLight = Color(0xFF8C7AFF)
val GoogleGreen = Color(0xFF34A853)
val GoogleYellow = Color(0xFFFBBC04)
val GoogleRed = Color(0xFFEA4335)
val GoogleGrey100 = Color(0xFFF1F3F4)
val GoogleGrey200 = Color(0xFFE8EAED)
val GoogleGrey300 = Color(0xFFDADCE0)
val GoogleGrey600 = Color(0xFF80868B)
val GoogleGrey800 = Color(0xFF3C4043)
val GoogleGrey900 = Color(0xFF202124)

val InkGreen = Color(0xFF34A853)
val InkRed = Color(0xFFEA4335)
val InkYellow = Color(0xFFFBBC04)

val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF191A1C)
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF0F1115)

val SurfaceVariantLight = Color(0xFFEDEDED)
val SurfaceVariantDark = Color(0xFF1F2022)
val OutlineLight = Color(0xFFCCCCCC)
val OutlineDark = Color(0xFF2E2F32)
val OnSurfaceLight = Color(0xFF1C1C1E)
val OnSurfaceDark = Color(0xFFF3F4F6)

val InkSecondary = Color(0xFFFF9F43)

val NeonGreen = Color(0xFF00E676)
val NeonBlue = Color(0xFF448AFF)
val NeonPurple = Color(0xFFBB86FC)
val NeonCyan = Color(0xFF00E5FF)
val NeonPink = Color(0xFFFF4081)

val FrostedDark = Color(0xFF191A1C)
val FrostedLight = Color(0xFFF5F7FA)
val FrostedBorderDark = Color(0xFF2E2F32)
val FrostedBorderLight = Color(0xFFE0E4EA)

data class CardColorPreset(val dark: String, val light: String, val label: String)

val CardColorPresets = listOf(
    CardColorPreset("#1E6FFF", "#5B87FF", "蓝"),
    CardColorPreset("#FF7C2E", "#FF944C", "橙"),
    CardColorPreset("#9B5CFF", "#B78BFF", "紫"),
    CardColorPreset("#2DD6C9", "#3ADDD1", "青"),
    CardColorPreset("#FF4C6D", "#FF5C73", "红"),
    CardColorPreset("#2AC769", "#3DE786", "绿"),
    CardColorPreset("#FFD64C", "#FFE066", "黄"),
    CardColorPreset("#FF82C4", "#FF97D1", "粉")
)

fun resolveCardColor(asset: CurrencyAsset, isDark: Boolean): Color {
    return try {
        val hex = if (isDark) asset.cardColor else (asset.cardColorLight ?: asset.cardColor)
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF6C63FF)
    }
}
