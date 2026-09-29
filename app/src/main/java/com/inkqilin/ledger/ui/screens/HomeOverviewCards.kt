package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.AppMode
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*


@Composable
internal fun SingleCurrencyOverviewCard(
    periodIncome: Double,
    periodExpense: Double,
    monthlyBudget: Double,
    displayCalendar: Calendar,
    defaultAsset: CurrencyAsset?,
    onMonthClick: () -> Unit,
    enableAnimations: Boolean,
    customColorHex: String? = null,
    translucent: Boolean = false
) {
    val symbol = defaultAsset?.symbol ?: "¥"
    val balance = periodIncome - periodExpense
    val assetAccent = if (customColorHex != null) {
        try { Color(android.graphics.Color.parseColor(customColorHex)) } catch (_: Exception) {
            Color(android.graphics.Color.parseColor(com.inkqilin.ledger.util.DEFAULT_HOME_CARD_COLOR_HEX))
        }
    } else {
        Color(android.graphics.Color.parseColor(com.inkqilin.ledger.util.DEFAULT_HOME_CARD_COLOR_HEX))
    }
    val cardColor by animateColorAsState(
        targetValue = assetAccent,
        animationSpec = if (enableAnimations) MotionSprings.interactive() else snap(),
        label = "singleCardColor"
    )

    // Apple Card style: dark gradient background, data is the hero
    // translucent 时降低不透明度，让首页背景图透出
    val gradTop = if (translucent) 0.72f else 0.95f
    val gradBottom = if (translucent) 0.52f else 0.75f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        cardColor.copy(alpha = gradTop),
                        cardColor.copy(alpha = gradBottom).copy(red = (cardColor.red * 0.6f).coerceIn(0f, 1f))
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
            // Lightweight header: asset name + month picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = defaultAsset?.name ?: "个人",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    onClick = onMonthClick
                ) {
                    Text(
                        text = "${displayCalendar.get(Calendar.YEAR)}.${String.format("%02d", displayCalendar.get(Calendar.MONTH) + 1)} ▾",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Balance hero number - the visual protagonist
            Text(
                text = "${symbol}${String.format("%.2f", balance)}",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )

            // Budget progress bar (if budget set)
            if (monthlyBudget > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val progress = (periodExpense / monthlyBudget).coerceIn(0.0, 1.0).toFloat()
                val remaining = monthlyBudget - periodExpense
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "预算剩余 ${symbol}${String.format("%.0f", remaining.coerceAtLeast(0.0))}",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${String.format("%.0f", progress * 100)}%",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (progress > 0.9f) Color(0xFFFF453A) else Color.White.copy(alpha = 0.7f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compact income/expense row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column {
                    Text(text = "收入", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${symbol}${String.format("%.2f", periodIncome)}",
                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(text = "支出", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${symbol}${String.format("%.2f", periodExpense)}",
                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MultiCurrencyOverviewCards(
    allAssets: List<CurrencyAsset>,
    currencySummaries: Map<String, CurrencyPeriodSummary>,
    displayCalendar: Calendar,
    onMonthClick: () -> Unit,
    enableAnimations: Boolean,
    translucent: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gradTop = if (translucent) 0.72f else 0.95f
    val gradBottom = if (translucent) 0.52f else 0.75f
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(allAssets, key = { it.id }) { asset ->
            val resolvedColor = resolveCardColor(asset, isDark)
            val cardColor by animateColorAsState(
                targetValue = resolvedColor,
                animationSpec = if (enableAnimations) MotionSprings.interactive() else snap(),
                label = "multiCardColor_${asset.id}"
            )
            val summary = currencySummaries[asset.code] ?: CurrencyPeriodSummary()
            val income = summary.income
            val expense = summary.expense
            val isDefault = asset.isDefault

            val interactionSource = remember { MutableInteractionSource() }
            // Apple Card style: dark gradient
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .pressScale(interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {}
                    )
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                cardColor.copy(alpha = gradTop),
                                cardColor.copy(alpha = gradBottom).copy(red = (cardColor.red * 0.6f).coerceIn(0f, 1f))
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = asset.name,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "默认",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            onClick = onMonthClick
                        ) {
                            Text(
                                text = "${displayCalendar.get(Calendar.YEAR)}.${String.format("%02d", displayCalendar.get(Calendar.MONTH) + 1)} ▾",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${asset.symbol}${String.format("%.2f", income - expense)}",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Column {
                            Text(
                                text = "收入",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${asset.symbol}${String.format("%.2f", income)}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column {
                            Text(
                                text = "支出",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${asset.symbol}${String.format("%.2f", expense)}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OverviewCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmer())
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(40.dp).clip(RoundedCornerShape(8.dp)).shimmer())
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).shimmer())
        }
    }
}

@Composable
internal fun TrendChartSkeleton() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().frostedGlass(RoundedCornerShape(24.dp), isDark)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).shimmer())
            }
        }
    }
}

@Composable
internal fun TransactionItemSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmer()
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }
        }
    }
}
