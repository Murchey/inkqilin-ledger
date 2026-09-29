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
import androidx.compose.foundation.lazy.LazyListScope

fun LazyListScope.StatisticsAssetSummaryItem(
    allUserAssets: List<com.inkqilin.ledger.data.UserAsset>,
    userAssetTotalValue: Double,
    assetValueChange: Double,
    assetChangePercent: Double,
    assetPeriodEndValue: Double,
    onNavigateAssets: () -> Unit,
    allAssetFlows: List<com.inkqilin.ledger.data.AssetFlow>,
    periodStartMs: Long,
    periodEndMs: Long,
) {
            item(key = "asset_summary") {
                Spacer(modifier = Modifier.height(16.dp))
                val shape = com.inkqilin.ledger.ui.theme.Corners.Lg
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .frostedGlass(shape, isDark)
                        .clickable { onNavigateAssets() },
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "资产统计",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "共 ${allUserAssets.size} 项",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // 期间增值/贬值总和（大字显示）
                        val changePrefix = if (assetValueChange >= 0) "+" else ""
                        val changeColor = when {
                            assetValueChange > 0 -> Color(0xFF4CAF50)
                            assetValueChange < 0 -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = "${changePrefix}¥${String.format("%,.2f", assetValueChange)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = changeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // 左下：变动百分比 / 右下：期末总价值
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val percentPrefix = if (assetChangePercent >= 0) "+" else ""
                            Text(
                                text = "${percentPrefix}${String.format("%.1f", assetChangePercent)}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = changeColor.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "¥${String.format("%,.2f", assetPeriodEndValue)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 按各类型资产在时间段内的变动绝对值排序，只取前 3
                        val typeChangeMap = remember(allAssetFlows, allUserAssets, periodStartMs, periodEndMs) {
                            val periodFlows = allAssetFlows.filter { it.date in periodStartMs..periodEndMs }
                            val assetTypeMap = allUserAssets.associate { it.id to it.type }
                            mutableMapOf<UserAssetType, Double>().apply {
                                periodFlows.forEach { flow ->
                                    val aType = assetTypeMap[flow.assetId] ?: return@forEach
                                    val change = when (flow.flowType) {
                                        AssetFlowType.INCREASE -> flow.amount
                                        AssetFlowType.DECREASE -> -flow.amount
                                        AssetFlowType.REVALUATION -> {
                                            val prevFlow = allAssetFlows
                                                .filter { it.assetId == flow.assetId && it.date < flow.date }
                                                .maxByOrNull { it.date }
                                            val prevValue = prevFlow?.newValue
                                                ?: allUserAssets.find { it.id == flow.assetId }?.currentValue
                                                ?: 0.0
                                            flow.newValue - prevValue
                                        }
                                    }
                                    this[aType] = (this[aType] ?: 0.0) + change
                                }
                            }
                        }
                        val grouped = allUserAssets.groupBy { it.type }
                        grouped.entries
                            .sortedByDescending { (type, _) -> abs(typeChangeMap[type] ?: 0.0) }
                            .take(3)
                            .forEach { (type, assets) ->
                            val typeTotal = assets.sumOf { it.currentValue }
                            val typeChange = typeChangeMap[type] ?: 0.0
                            val percent = if (userAssetTotalValue > 0) typeTotal / userAssetTotalValue * 100 else 0.0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = type.label,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 期间变动
                                    val prefix = if (typeChange >= 0) "+" else ""
                                    Text(
                                        text = "${prefix}¥${String.format("%,.0f", typeChange)}",
                                        fontSize = 12.sp,
                                        color = if (typeChange > 0) Color(0xFF4CAF50)
                                                else if (typeChange < 0) Color(0xFFF44336)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "¥${String.format("%,.0f", typeTotal)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${String.format("%.1f", percent)}%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
}
