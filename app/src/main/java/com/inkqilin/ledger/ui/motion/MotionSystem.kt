package com.inkqilin.ledger.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlinx.coroutines.flow.collectLatest

object MotionDurations {
    const val EXTRA_FAST = 100
    const val FAST = 200
    const val SHORT = 300
    const val MEDIUM = 400
    const val LONG = 500
}

object MotionCurves {
    val iOSCurve = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val FastOutSlowIn = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
    val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
    val StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)
}

object MotionSprings {
    /**
     * iOS-like spring for interactive property changes (size, position, scale, alpha).
     * Medium bouncy and medium stiffness for a lively, responsive feel.
     */
    fun <T> interactive(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * iOS-like spring for non-interactive appearances (screen navigation, element appearances).
     * Less bouncy and lower stiffness for a smooth, fluid transition.
     */
    fun <T> appearance(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> default(): SpringSpec<T> = interactive()

    fun <T> gentle(): SpringSpec<T> = appearance()

    fun <T> snappy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
}

object MotionAnimSpecs {
    fun <T> fastTween() = tween<T>(MotionDurations.FAST, easing = MotionCurves.iOSCurve)
    fun <T> shortTween() = tween<T>(MotionDurations.SHORT, easing = MotionCurves.iOSCurve)
    fun <T> mediumTween() = tween<T>(MotionDurations.MEDIUM, easing = MotionCurves.iOSCurve)
}

@Composable
fun animatePressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f // iOS-style subtle press down
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()

    return animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (isPressed) {
            // Instant response on press
            spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)
        } else {
            // Smooth bouncy release
            MotionSprings.interactive()
        },
        label = "pressScale"
    )
}

fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.98f
): Modifier = composed {
    val scaleState = animatePressScale(interactionSource, pressedScale)
    scale(scaleState.value)
}

fun Modifier.staggeredAppearance(
    index: Int,
    visible: Boolean = true
): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "staggeredAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 20.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "staggeredOffset"
    )
    
    graphicsLayer {
        this.alpha = alpha
        translationY = offsetY.toPx()
    }
}
