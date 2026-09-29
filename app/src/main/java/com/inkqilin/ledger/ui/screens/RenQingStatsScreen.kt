@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.appButtonElevation
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun RenQingStatsScreen(
    viewModel: RenQingViewModel,
    tags: List<RenQingTag>,
    onNavigateToMonthDetail: (Int, Int) -> Unit,
    onNavigateToTagStats: (Int) -> Unit = {},
    onNavigateToContactAnalysis: (Int) -> Unit = {}
) {
    val dataLoaded by viewModel.dataLoaded.collectAsState()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    val allEvents by viewModel.allEvents.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val yearRange = remember(selectedYear) { viewModel.getYearRange(selectedYear) }
    val yearEvents = remember(allEvents, selectedYear) {
        allEvents.filter { it.date in yearRange.first..yearRange.second }
    }
    val yearGiven = remember(yearEvents) {
        yearEvents.filter { it.direction == RenQingDirection.GIVEN }.sumOf { it.amount }
    }
    val yearReceived = remember(yearEvents) {
        yearEvents.filter { it.direction == RenQingDirection.RECEIVED }.sumOf { it.amount }
    }

    if (!dataLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppleLoadingIndicator()
        }
        return
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = navBarPadding + 76.dp)
    ) {
        // 年份切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selectedYear-- }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一年")
            }
            Text("$selectedYear 年", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { selectedYear++ }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一年")
            }
        }

        // 总览
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("年度总览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatColumn("收到", yearReceived, MaterialTheme.colorScheme.primary)
                    StatColumn("送出", yearGiven, MaterialTheme.colorScheme.error)
                    val net = yearReceived - yearGiven
                    StatColumn(
                        "净额",
                        net,
                        if (net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        showSign = true
                    )
                }
                if (yearEvents.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "该年还没有人情记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 月度趋势
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("月度趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "点柱状图看当月明细",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                val monthlyData = remember(yearEvents) {
                    (0..11).map { month ->
                        val mEvents = yearEvents.filter {
                            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.MONTH) == month
                        }
                        Triple(
                            month,
                            mEvents.filter { it.direction == RenQingDirection.GIVEN }.sumOf { it.amount },
                            mEvents.filter { it.direction == RenQingDirection.RECEIVED }.sumOf { it.amount }
                        )
                    }
                }
                val maxAmount = remember(monthlyData) {
                    monthlyData.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyData.forEach { (month, given, received) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                                .clickable { onNavigateToMonthDetail(selectedYear, month) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(104.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    if (given > 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(9.dp)
                                                .height(((given / maxAmount) * 96).dp)
                                                .background(MaterialTheme.colorScheme.error, RoundedCornerShape(3.dp))
                                        )
                                    }
                                    if (received > 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(9.dp)
                                                .height(((received / maxAmount) * 96).dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${month + 1}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(3.dp)))
                    Text("  送出", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
                    Text("  收到", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 标签 Top
        val tagStats = remember(yearEvents, tags) {
            tags.mapNotNull { tag ->
                val list = yearEvents.filter { it.tagId == tag.id }
                if (list.isEmpty()) null
                else Triple(tag, list.size, list.sumOf { it.amount })
            }.sortedByDescending { it.third }.take(5)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("标签 Top 5", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onNavigateToTagStats(selectedYear) }) { Text("全部") }
                }
                if (tagStats.isEmpty()) {
                    Text("暂无数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val maxAmount = tagStats.maxOf { it.third }.coerceAtLeast(1.0)
                    tagStats.forEach { (tag, count, total) ->
                        val tagColor = try {
                            Color(android.graphics.Color.parseColor(tag.color))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                RenQingIcons.iconForTagIconValue(tag.icon),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = tagColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tag.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp), maxLines = 1)
                            LinearProgressIndicator(
                                progress = (total / maxAmount).toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = tagColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "¥${String.format(Locale.US, "%.0f", total)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 联系人 Top
        val contactStats = remember(yearEvents, allContacts) {
            yearEvents.groupBy { it.contactName }.map { (name, events) ->
                val contact = allContacts.find { it.name == name }
                Triple(name, contact?.relationship?.label ?: "未知", events.sumOf { it.amount })
            }.sortedByDescending { it.third }.take(5)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("联系人 Top 5", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onNavigateToContactAnalysis(selectedYear) }) { Text("全部") }
                }
                if (contactStats.isEmpty()) {
                    Text("暂无数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    contactStats.forEach { (name, relation, total) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name.take(1),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text(
                                    relation,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "¥${String.format(Locale.US, "%.0f", total)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun StatColumn(
    label: String,
    value: Double,
    color: Color,
    showSign: Boolean = false
) {
    StatColumn(
        label = label,
        value = buildString {
            if (showSign && value >= 0) append("+")
            append("¥")
            append(String.format(Locale.US, "%.2f", value))
        },
        color = color
    )
}
