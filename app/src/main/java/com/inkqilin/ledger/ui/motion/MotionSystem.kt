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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

object MotionDurations {
    const val FAST = 200
    const val SHORT = 300
    const val MEDIUM = 400

    /** 全屏/导航转场目标时长 */
    const val APPEARANCE = 280
}

object MotionCurves {
    val FastOutSlowIn = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

object MotionSprings {
    /**
     * 交互反馈（按下/释放）：MediumBouncy + Medium，保持手感。
     */
    fun <T> interactive(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * 出现/转场：接近临界阻尼 + 中刚度，短促无超调。
     * 禁止再用 StiffnessLow + LowBouncy（低端机上会拖 400–800ms）。
     */
    fun <T> appearance(): SpringSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMedium
    )

    /** 转场优先使用 tween，比弹簧更可预测、帧路径更短。 */
    fun <T> appearanceTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionDurations.APPEARANCE,
        easing = MotionCurves.FastOutSlowIn
    )

    fun <T> default(): SpringSpec<T> = interactive()
}

@Composable
fun animatePressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.98f
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (isPressed) {
            spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)
        } else {
            MotionSprings.interactive()
        },
        label = "pressScale"
    )
}

/** 无状态按压缩放：状态外提后走 graphicsLayer，零 composed、零重组。 */
fun Modifier.pressScale(scale: Float): Modifier =
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

/** 同上，但持有 State：scale.value 只在绘制阶段读取，不订阅组合。 */
fun Modifier.pressScale(scale: State<Float>): Modifier =
    this.graphicsLayer {
        val s = scale.value
        scaleX = s
        scaleY = s
    }

/**
 * 便捷重载。内部仍需组合读取按压态，但实现收敛为单个 animateFloat + graphicsLayer。
 * 热点调用方请改用 [animatePressScale] + [pressScale] 外提状态。
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.98f
): Modifier = composed {
    val scaleState = animatePressScale(interactionSource, pressedScale)
    graphicsLayer {
        scaleX = scaleState.value
        scaleY = scaleState.value
    }
}

// ─── Shimmer ────────────────────────────────────────────────────────────────

/**
 * 全局共享一个 shimmer 进度（0..1）。
 * 宿主 Composition 提供一次；每个骨架块只读该 State 重绘，不再各自开无限动画。
 */
val LocalShimmerProgress = staticCompositionLocalOf<State<Float>> {
    error("Wrap content in ShimmerHost or pass progress explicitly")
}

@Composable
fun rememberShimmerProgress(): State<Float> {
    val transition = rememberInfiniteTransition(label = "shimmer-shared")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
}

/**
 * 骨架高光。[progress] 为 0..1 共享进度：
 * - 渐变 Brush 在 size 不变时缓存（drawWithCache）
 * - 每帧只做 translate，不新建 Brush / 不 onGloballyPositioned
 */
fun Modifier.shimmer(progress: State<Float>): Modifier =
    this.drawWithCache {
        val w = size.width
        val h = size.height
        val gradient = Brush.linearGradient(
            colors = listOf(
                Color.LightGray.copy(alpha = 0.3f),
                Color.LightGray.copy(alpha = 0.5f),
                Color.LightGray.copy(alpha = 0.3f)
            ),
            start = Offset(-w, 0f),
            end = Offset(w, h)
        )
        onDrawBehind {
            val shift = (progress.value * 2f - 1f) * w
            translate(left = shift) {
                drawRect(brush = gradient)
            }
        }
    }

/** 兼容旧签名：从 CompositionLocal 取共享进度。 */
@Composable
fun Modifier.shimmer(): Modifier {
    val progress = LocalShimmerProgress.current
    return this.shimmer(progress)
}

// ─── Staggered appearance ───────────────────────────────────────────────────

/** 首次入场只对前 N 条做 stagger，其余直接显示。 */
object MotionStagger {
    const val VISIBLE_COUNT = 8
    const val DELAY_CAP_MS = 240
    const val DURATION_MS = 220
}

/** 兼容常量别名（与 MotionStagger 同值） */
@Deprecated("Use MotionStagger", ReplaceWith("MotionStagger.VISIBLE_COUNT"))
const val STAGGER_VISIBLE_COUNT = MotionStagger.VISIBLE_COUNT

/** 单条最大延迟（ms），避免 index * delay 无上限。 */
const val STAGGER_MAX_DELAY_MS = MotionStagger.DELAY_CAP_MS

/** 入场单 tween 时长。 */
const val STAGGER_DURATION_MS = MotionStagger.DURATION_MS

/**
 * 列表项入场。
 * - 只有 `visible && index < [STAGGER_VISIBLE_COUNT]` 才进入组合动画
 * - 延迟 = `min(index * 48ms, 240ms)`，单 tween，无双弹簧
 * - 滚动复用 / 二级页返回的重组不重放（由调用方一次性传 visible）
 */
fun Modifier.staggeredAppearance(
    index: Int,
    visible: Boolean = true
): Modifier {
    // 绝大多数 item 走此短路：零 composed、零动画
    if (!visible || index >= STAGGER_VISIBLE_COUNT) return this
    return composed {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay((index * 48L).coerceAtMost(STAGGER_MAX_DELAY_MS.toLong()))
            shown = true
        }
        val tweenSpec = tween<Float>(
            durationMillis = STAGGER_DURATION_MS,
            easing = MotionCurves.FastOutSlowIn
        )
        val alpha by animateFloatAsState(
            targetValue = if (shown) 1f else 0f,
            animationSpec = tweenSpec,
            label = "staggerAlpha"
        )
        val ty by animateFloatAsState(
            targetValue = if (shown) 0f else 20f,
            animationSpec = tweenSpec,
            label = "staggerY"
        )
        graphicsLayer {
            this.alpha = alpha
            translationY = ty
        }
    }
}

// ─── Misc light modifiers ───────────────────────────────────────────────────

/** 一次性淡入，不随滚动重放。 */
fun Modifier.appearOnce(): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(MotionDurations.FAST, easing = MotionCurves.FastOutSlowIn),
        label = "appearOnce"
    )
    graphicsLayer { this.alpha = alpha }
}

fun Modifier.shimmerLegacyPlaceholder() = this.background(Color.LightGray.copy(alpha = 0.35f))
