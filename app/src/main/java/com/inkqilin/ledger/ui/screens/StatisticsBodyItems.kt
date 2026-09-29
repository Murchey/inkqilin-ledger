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

fun LazyListScope.StatisticsBodyItems(
    totalAmount: Double,
    selectedType: com.inkqilin.ledger.data.TransactionType,
    expenseColor: Color,
    incomeColor: Color,
    currencySymbol: String,
    categoryTotals: List<Pair<String, Double>>,
    barChartData: List<Pair<String, Double>>,
    previousTotal: Double,
    changePercent: Double,
    selectedPeriod: TimePeriod,
    startDate: Long,
    endDate: Long,
    categories: List<com.inkqilin.ledger.data.Category>,
    categoryToEdit: androidx.compose.runtime.MutableState<com.inkqilin.ledger.data.Category?>,
    aiAnalysisResult: com.inkqilin.ledger.service.AiAnalysisResult?,
    aiAnalysisFailed: Boolean,
    aiAnalysisLoading: Boolean,
    onRefreshAi: () -> Unit,
    filteredTransactions: List<com.inkqilin.ledger.data.Transaction>,
    navController: NavController,
    viewModel: TransactionViewModel,
    showPieChart: androidx.compose.runtime.MutableState<Boolean>,
    appMode: com.inkqilin.ledger.util.AppMode,
    subFiltered: List<com.inkqilin.ledger.data.Transaction>,
    monthlyBudget: Double,
) {
        item(key = "total_card") {
            Spacer(modifier = Modifier.height(24.dp))
            val totalShape = RoundedCornerShape(20.dp)
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val totalInteractionSource = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .pressScale(totalInteractionSource)
                    .frostedGlass(totalShape, isDark),
                shape = totalShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = selectedPeriod.label + "总" + if (selectedType == TransactionType.EXPENSE) "支出" else "收入",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currencySymbol}${String.format("%.2f", totalAmount)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "共 ${filteredTransactions.size} 笔记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            val compShape = RoundedCornerShape(20.dp)
            val isDarkComp = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val compInteractionSource = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .pressScale(compInteractionSource)
                    .frostedGlass(compShape, isDarkComp),
                shape = compShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "环比上期",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isUp = changePercent >= 0
                            Icon(
                                imageVector = if (isUp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isUp) expenseColor else incomeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (isUp) "+" else ""}${String.format("%.1f", changePercent)}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isUp) expenseColor else incomeColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "上期 ${currencySymbol}${String.format("%.2f", previousTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val avgDailyExpense = remember(filteredTransactions, selectedPeriod) {
                        val days = when (selectedPeriod) {
                            TimePeriod.WEEK -> 7
                            TimePeriod.MONTH -> Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
                            TimePeriod.YEAR -> 365
                            TimePeriod.CUSTOM -> {
                                val diff = endDate - startDate
                                (diff / 86400000L).toInt().coerceAtLeast(1)
                            }
                        }
                        if (days > 0) totalAmount / days else 0.0
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "日均支出",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currencySymbol}${String.format("%.2f", avgDailyExpense)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (barChartData.isNotEmpty()) {
            item(key = "chart_section") {
                Spacer(modifier = Modifier.height(16.dp))

                // Title + toggle buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showPieChart.value) "分类占比" else "趋势图",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (!showPieChart.value) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { showPieChart.value = false }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "趋势",
                                fontSize = 12.sp,
                                fontWeight = if (!showPieChart.value) FontWeight.Bold else FontWeight.Normal,
                                color = if (!showPieChart.value) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (showPieChart.value) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { showPieChart.value = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "占比",
                                fontSize = 12.sp,
                                fontWeight = if (showPieChart.value) FontWeight.Bold else FontWeight.Normal,
                                color = if (showPieChart.value) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                val accentColor = if (selectedType == TransactionType.EXPENSE) expenseColor else incomeColor
                var tooltipIndex by remember { mutableStateOf<Int?>(null) }
                val hasAnyData = barChartData.any { it.second > 0 }

                val chartShape = com.inkqilin.ledger.ui.theme.Corners.Lg
                val isDarkChart = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val chartInteractionSource = remember { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .pressScale(chartInteractionSource)
                        .frostedGlass(chartShape, isDarkChart),
                    shape = chartShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    if (!showPieChart.value) {
                        // Bar chart mode
                        if (hasAnyData) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                AnimatedBarChart(
                                    data = barChartData,
                                    accentColor = accentColor,
                                    onBarLongPress = { index -> tooltipIndex = index },
                                    onBarRelease = { tooltipIndex = null },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .horizontalScroll(rememberScrollState())
                                )
                                if (tooltipIndex != null && tooltipIndex!! < barChartData.size) {
                                    val (label, value) = barChartData[tooltipIndex!!]
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.inverseSurface,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text(
                                            text = "$label · ${currencySymbol}${String.format("%.2f", value)}",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.inverseOnSurface,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无数据",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Pie chart mode
                        if (categoryTotals.isNotEmpty()) {
                            CategoryPieChart(
                                categoryTotals = categoryTotals,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无数据",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (appMode == AppMode.SMART) {
            item(key = "score_card") {
                Spacer(modifier = Modifier.height(16.dp))
                if (aiAnalysisResult != null) {
                    AiFinancialScoreCard(
                        result = aiAnalysisResult!!,
                        isFailed = false
                    )
                } else if (aiAnalysisFailed) {
                    AiFinancialScoreCard(
                        result = null,
                        isFailed = true
                    )
                } else {
                    FinancialScoreCard(
                        income = subFiltered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        expense = subFiltered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                        transactions = subFiltered,
                        monthlyBudget = monthlyBudget
                    )
                }
            }
        }

        item(key = "category_rank_title") {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "分类排行",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (categoryTotals.isEmpty()) {
            item(key = "category_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无数据",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(categoryTotals, key = { "cat_${it.first}" }) { (categoryName, total) ->
                val percentage = if (totalAmount > 0) (total / totalAmount).toFloat() else 0f
                val accentColor = if (selectedType == TransactionType.EXPENSE) expenseColor else incomeColor
                val category = categories.find { it.name == categoryName && it.type == selectedType }
                val displayColor = category?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: accentColor

                val density = LocalDensity.current
                val menuWidth = 80.dp
                val menuWidthPx = with(density) { menuWidth.toPx() }
                var offsetX by remember(categoryName, selectedType) { mutableFloatStateOf(0f) }
                val draggableState = rememberDraggableState { delta ->
                    val newOffset = (offsetX + delta).coerceIn(-menuWidthPx, 0f)
                    offsetX = newOffset
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(com.inkqilin.ledger.ui.theme.Corners.Md)
                ) {
                    // 滑动展示的编辑按钮
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(menuWidth)
                            .fillMaxHeight()
                            .clickable {
                                offsetX = 0f
                                categoryToEdit.value = category
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            Text("编辑", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // 前景内容
                    val cardInteractionSource = remember { MutableInteractionSource() }
                    Card(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                            .fillMaxWidth()
                            .pressScale(cardInteractionSource) // iOS-style interactive feedback
                            .draggable(
                                state = draggableState,
                                orientation = Orientation.Horizontal,
                                onDragStopped = {
                                    val target = if (offsetX < -menuWidthPx / 2) -menuWidthPx else 0f
                                    animate(
                                        initialValue = offsetX,
                                        targetValue = target,
                                        animationSpec = MotionSprings.interactive() // iOS-like bouncy menu snap
                                    ) { value, _ -> offsetX = value }
                                }
                            )
                            .clickable(
                                interactionSource = cardInteractionSource,
                                indication = null
                            ) {
                                val dateRange = getDateRangeForPeriod(selectedPeriod, startDate, endDate)
                                navController.navigateSingle("category_transactions/$categoryName/${selectedType.name}?startDate=${dateRange.first}&endDate=${dateRange.second}")
                            },
                        shape = com.inkqilin.ledger.ui.theme.Corners.Md,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(category?.icon ?: "📋", modifier = Modifier.padding(end = 8.dp))
                                    Text(
                                        text = categoryName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = "${currencySymbol}${String.format("%.2f", total)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = displayColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val animatedPercentage by animateFloatAsState(
                                targetValue = percentage,
                                animationSpec = MotionSprings.interactive(), // iOS-like bouncy progress
                                label = "categoryPercentage"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = animatedPercentage)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(displayColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.1f", percentage * 100)}% · ${filteredTransactions.count { it.category == categoryName }} 笔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
}
