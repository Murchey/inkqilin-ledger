package com.inkqilin.ledger.ui.theme

import androidx.compose.ui.graphics.Color
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

val CardColorPresets = listOf(
    CardColorPreset("#007AFF", "#5AC8FA", "蓝"),
    CardColorPreset("#FF9500", "#FF9F0A", "橙"),
    CardColorPreset("#AF52DE", "#BF5AF2", "紫"),
    CardColorPreset("#34C759", "#30D158", "绿"),
    CardColorPreset("#FF3B30", "#FF453A", "红"),
    CardColorPreset("#5856D6", "#5E5CE6", "靛"),
    CardColorPreset("#FF2D55", "#FF375F", "粉"),
    CardColorPreset("#00C7BE", "#30B0C7", "青")
)

fun resolveCardColor(asset: CurrencyAsset, isDark: Boolean): Color {
    return try {
        val hex = if (isDark) asset.cardColor else (asset.cardColorLight ?: asset.cardColor)
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    }
}
