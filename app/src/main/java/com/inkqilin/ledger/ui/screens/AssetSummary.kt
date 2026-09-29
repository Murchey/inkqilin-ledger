@file:OptIn(ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.AssetFlowType
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.UserAsset
import com.inkqilin.ledger.data.UserAssetType
import com.inkqilin.ledger.ui.TransactionViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs



@Composable
internal fun AssetSummaryCard(
    currentValue: Double,
    netChange: Double,
    flowCount: Int,
    trendValues: List<Double>,
    symbol: String = "¥"
) {
    val changeColor = when {
        netChange > 0 -> ComposeColor(0xFF34C759)
        netChange < 0 -> ComposeColor(0xFFFF3B30)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryMetric("当前估值", "$symbol ${amountFormat.format(currentValue)}", MaterialTheme.colorScheme.onSurface)
                SummaryMetric("累计变化", "${if (netChange >= 0) "+" else ""}$symbol ${amountFormat.format(netChange)}", changeColor)
                SummaryMetric("流转次数", "$flowCount 次", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trendValues.size >= 2) {
                Spacer(modifier = Modifier.height(16.dp))
                AssetTrendChart(values = trendValues, lineColor = changeColor)
            }
        }
    }
}

@Composable
internal fun SummaryMetric(label: String, value: String, valueColor: ComposeColor) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
internal fun AssetTrendChart(values: List<Double>, lineColor: ComposeColor) {
    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: minValue
    val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
    Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) 0f else size.width * index / (values.size - 1)
            val y = size.height - ((value - minValue) / range).toFloat() * size.height
            Offset(x, y)
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(color = lineColor, start = start, end = end, strokeWidth = 4f)
        }
    }
}
