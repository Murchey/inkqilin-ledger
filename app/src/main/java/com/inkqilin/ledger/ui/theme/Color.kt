package com.inkqilin.ledger.ui.theme

import androidx.compose.ui.graphics.Color
import com.inkqilin.ledger.data.CurrencyAsset

val InkPrimary = Color(0xFF04BE02)
val InkPrimaryLight = Color(0xFF04BE02)
val InkPrimaryDark = Color(0xFF04BE02)

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
    CardColorPreset("#1859CC", "#496CCC", "蓝"),
    CardColorPreset("#CC6325", "#CC763D", "橙"),
    CardColorPreset("#7C4ACC", "#926FCC", "紫"),
    CardColorPreset("#24ABA1", "#2EB1A7", "青"),
    CardColorPreset("#CC3D57", "#CC4A5C", "红"),
    CardColorPreset("#229F54", "#31B96B", "绿"),
    CardColorPreset("#CCAB3D", "#CCB352", "黄"),
    CardColorPreset("#CC689D", "#CC79A7", "粉")
)

fun resolveCardColor(asset: CurrencyAsset, isDark: Boolean): Color {
    return try {
        val hex = if (isDark) asset.cardColor else (asset.cardColorLight ?: asset.cardColor)
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF6C63FF)
    }
}
