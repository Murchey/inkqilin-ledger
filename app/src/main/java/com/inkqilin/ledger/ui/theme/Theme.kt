package com.inkqilin.ledger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.inkqilin.ledger.ui.motion.*

private fun dynamicDarkColorScheme(primary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.15f),
    onPrimaryContainer = primary,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = NeonPurple.copy(alpha = 0.15f),
    onSecondaryContainer = NeonPurple,
    tertiary = NeonCyan,
    onTertiary = Color.Black,
    tertiaryContainer = NeonCyan.copy(alpha = 0.15f),
    onTertiaryContainer = NeonCyan,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = FrostedDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceDark.copy(alpha = 0.65f),
    outline = FrostedBorderDark,
    outlineVariant = OutlineDark.copy(alpha = 0.5f),
    error = NeonPink,
    onError = Color.White,
)

private fun dynamicLightColorScheme(primary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.1f),
    onPrimaryContainer = primary,
    secondary = InkSecondary,
    onSecondary = Color.White,
    secondaryContainer = InkSecondary.copy(alpha = 0.1f),
    onSecondaryContainer = InkSecondary,
    tertiary = GoogleGreen,
    onTertiary = Color.White,
    tertiaryContainer = GoogleGreen.copy(alpha = 0.1f),
    onTertiaryContainer = GoogleGreen,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = FrostedLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceLight.copy(alpha = 0.65f),
    outline = FrostedBorderLight,
    outlineVariant = OutlineLight.copy(alpha = 0.5f),
    error = InkRed,
    onError = Color.White,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

fun parseHexColor(hex: String): Color? {
    return try {
        val clean = hex.removePrefix("#")
        if (clean.length == 6 || clean.length == 8) {
            Color(android.graphics.Color.parseColor(if (clean.length == 6) "#FF$clean" else "#$clean"))
        } else null
    } catch (_: Exception) {
        null
    }
}

val DarkDefaultPrimary = Color(0xFF04BE02)
val LightDefaultPrimary = Color(0xFF04BE02)

@Composable
fun InkQilinLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    customPrimaryColorHex: String? = null,
    content: @Composable () -> Unit
) {
    val customPrimary = remember(customPrimaryColorHex) {
        customPrimaryColorHex?.let { parseHexColor(it) }
    }

    val defaultPrimary = if (darkTheme) DarkDefaultPrimary else LightDefaultPrimary

    val animatedPrimary = animateColorAsState(
        targetValue = customPrimary ?: defaultPrimary,
        animationSpec = MotionSprings.interactive(), // iOS-like bouncy theme transition
        label = "primaryColorTransition"
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> dynamicDarkColorScheme(animatedPrimary.value)
        else -> dynamicLightColorScheme(animatedPrimary.value)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
