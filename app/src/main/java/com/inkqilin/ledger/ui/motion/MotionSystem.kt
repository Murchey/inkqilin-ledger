package com.inkqilin.ledger.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.AnimationSpec
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
import kotlinx.coroutines.flow.collectLatest

object MotionDurations {
    const val FAST = 150
    const val SHORT = 220
    const val MEDIUM = 300
    const val LONG = 400
}

object MotionCurves {
    val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
    val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
    val StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)
}

object MotionSprings {
    fun <T> default(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> gentle(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> snappy(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
}

@Composable
fun animatePressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f
): State<Float> {
    val isPressed = remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed.value = true
                is PressInteraction.Release -> isPressed.value = false
                is PressInteraction.Cancel -> isPressed.value = false
            }
        }
    }

    return animateFloatAsState(
        targetValue = if (isPressed.value) pressedScale else 1f,
        animationSpec = if (isPressed.value) {
            tween(MotionDurations.FAST)
        } else {
            MotionSprings.snappy()
        },
        label = "pressScale"
    )
}

fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val scaleState = animatePressScale(interactionSource, pressedScale)
    scale(scaleState.value)
}

fun Modifier.fadeSlideIn(
    visible: Boolean,
    durationMillis: Int = MotionDurations.MEDIUM
): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis, easing = MotionCurves.EaseOutCubic),
        label = "fadeAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 12.dp,
        animationSpec = tween(durationMillis, easing = MotionCurves.EaseOutCubic),
        label = "slideOffset"
    )
    graphicsLayer {
        this.alpha = alpha
        translationY = offsetY.toPx()
    }
}
