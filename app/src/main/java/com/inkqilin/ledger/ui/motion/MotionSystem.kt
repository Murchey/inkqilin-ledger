package com.inkqilin.ledger.ui.motion

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object MotionDurations {
    const val FAST = 200
    const val SHORT = 300
    const val MEDIUM = 400
}

object MotionCurves {
    val FastOutSlowIn = CubicBezierEasing(0.2f, 0f, 0f, 1f)
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

fun Modifier.shimmer(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = MotionCurves.FastOutSlowIn),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    this.background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.LightGray.copy(alpha = 0.3f),
                Color.LightGray.copy(alpha = 0.5f),
                Color.LightGray.copy(alpha = 0.3f),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

fun Modifier.staggeredAppearance(
    index: Int,
    visible: Boolean = true
): Modifier = composed {
    var isActuallyVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 50L) // iOS-style staggered delay
            isActuallyVisible = true
        } else {
            isActuallyVisible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isActuallyVisible) 1f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "staggeredAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (isActuallyVisible) 0.dp else 20.dp,
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
