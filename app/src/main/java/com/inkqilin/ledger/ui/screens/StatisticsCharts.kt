@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.toColorInt
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.AssetFlowType
import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.data.UserAssetType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.AppMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun AnimatedBarChart(
    data: List<Pair<String, Double>>,
    accentColor: Color,
    onBarLongPress: (Int) -> Unit,
    onBarRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = data.maxOfOrNull { it.second } ?: 1.0
    val barCount = data.size
    val minBarWidth = 32.dp
    val barSpacing = 6.dp
    val totalBarArea = minBarWidth * barCount + barSpacing * (barCount - 1) + 32.dp
    val chartHeight = 170.dp

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = MotionSprings.appearanceTween() // 短促出现，避免 StiffnessLow 弹簧拖帧
        )
    }

    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .width(totalBarArea)
            .height(chartHeight)
            .pointerInput(data) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val barTotalWidth = size.width / barCount
                        val index = (offset.x / barTotalWidth).toInt().coerceIn(0, barCount - 1)
                        onBarLongPress(index)
                    },
                    onPress = {
                        awaitRelease()
                        onBarRelease()
                    }
                )
            }
    ) {
        val canvasW = size.width
        val canvasH = size.height
        val barAreaWidth = canvasW / barCount
        val barWidthPx = barAreaWidth * 0.55f
        val topPadding = 36f
        val bottomPadding = 32f
        val chartAreaHeight = canvasH - topPadding - bottomPadding

        val labelPaint = android.graphics.Paint().apply {
            color = onSurfaceVariant.toArgb()
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val valuePaint = android.graphics.Paint().apply {
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        data.forEachIndexed { index, (label, value) ->
            val barHeight = if (maxVal > 0) (value / maxVal).toFloat() * chartAreaHeight * animProgress.value else 0f
            val x = barAreaWidth * index + (barAreaWidth - barWidthPx) / 2
            val y = canvasH - bottomPadding - barHeight

            drawRoundRect(
                color = accentColor.copy(alpha = 0.85f),
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )

            val textX = x + barWidthPx / 2

            drawContext.canvas.nativeCanvas.apply {
                drawText(label, textX, canvasH - 6f, labelPaint)

                if (value > 0 && animProgress.value > 0.8f) {
                    val valueText = if (value >= 10000) {
                        "${String.format("%.1f", value / 10000)}w"
                    } else if (value >= 1000) {
                        String.format("%.0f", value)
                    } else {
                        String.format("%.2f", value)
                    }
                    valuePaint.color = accentColor.toArgb()
                    drawText(valueText, textX, y - 8f, valuePaint)
                }
            }
        }
    }
}

@Composable
internal fun CategoryPieChart(
    categoryTotals: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val total = categoryTotals.sumOf { it.second }
    if (total <= 0 || categoryTotals.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val palette = listOf(
        Color(0xFFFF2D55), Color(0xFF007AFF), Color(0xFFFF9F0A),
        Color(0xFF34C759), Color(0xFFAF52DE), Color(0xFFFF3B30),
        Color(0xFF5AC8FA), Color(0xFFFFCC00), Color(0xFF8E8E93),
        Color(0xFF00C7BE), Color(0xFFFF6482), Color(0xFF30B0C7)
    )

    val sweepAngles = categoryTotals.map { (it.second / total * 360f).toFloat() }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(categoryTotals) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val density = LocalDensity.current
        val strokeWidthPx = with(density) { 28.dp.toPx() }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = (size.minDimension - strokeWidthPx) / 2
                val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f
                sweepAngles.forEachIndexed { index, sweep ->
                    val color = palette[index % palette.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep * animProgress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "总计",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.2f", total),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        val displayItems = categoryTotals.take(6)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            displayItems.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEachIndexed { _, (name, amount) ->
                        val globalIdx = categoryTotals.indexOfFirst { it.first == name }
                        val color = palette[globalIdx % palette.size]
                        val pct = (amount / total * 100)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${String.format("%.1f", pct)}%",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
