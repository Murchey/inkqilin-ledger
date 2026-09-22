package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.roundToInt
enum class CalcType { COMPOUND_INTEREST, INCOME_TAX, SAVINGS_GOAL, INSTALLMENT, DCA, MATH_DOCS }
enum class DcaFrequency(val label: String, val periodsPerYear: Int) {
    WEEKLY(
        "每周",
        52
    ),
    BIWEEKLY("每两周", 26), MONTHLY("每月", 12)
}

data class DcaYearRecord(
    val year: Int,
    val balance: BigDecimal,
    val totalInvested: BigDecimal,
    val totalProfit: BigDecimal
)

internal val calcItems = listOf(
    Triple(CalcType.COMPOUND_INTEREST, "复利计算器", "计算投资收益和复利增长"),
    Triple(CalcType.INCOME_TAX, "个人所得税", "计算中国个税和税后收入"),
    Triple(CalcType.SAVINGS_GOAL, "储蓄目标", "算进度与管现金流"),
    Triple(CalcType.INSTALLMENT, "分期付款", "揭穿低息幻觉，反推真实APR"),
    Triple(CalcType.DCA, "定投计算器", "长期复利增值与目标规划"),
    Triple(CalcType.MATH_DOCS, "数学原理", "查看所有计算器的公式与算法说明")
)

@Composable
fun CalculatorScreen(
    initialType: String?,
    onUpdateTopBar: (String, (() -> Unit)?) -> Unit = { _, _ -> }
) {
    val initialSelectedCalc = remember(initialType) {
        when (initialType) {
            "compound_interest" -> CalcType.COMPOUND_INTEREST
            "income_tax" -> CalcType.INCOME_TAX
            "savings_goal" -> CalcType.SAVINGS_GOAL
            "installment" -> CalcType.INSTALLMENT
            "dca" -> CalcType.DCA
            else -> null
        }
    }

    var selectedCalc by remember { mutableStateOf<CalcType?>(initialSelectedCalc) }

    // 安全更新 top bar，避免在 compose 树中直接修改导致递归 recomposition
    val currentOnUpdateTopBar by rememberUpdatedState(onUpdateTopBar)
    LaunchedEffect(selectedCalc) {
        val title = when (selectedCalc) {
            null -> "多功能计算"
            CalcType.COMPOUND_INTEREST -> "复利计算器"
            CalcType.INCOME_TAX -> "个人所得税"
            CalcType.SAVINGS_GOAL -> "储蓄目标"
            CalcType.INSTALLMENT -> "分期付款"
            CalcType.DCA -> "定投计算器"
            CalcType.MATH_DOCS -> "数学原理"
        }
        val onBack = selectedCalc?.let { { selectedCalc = null } }
        try {
            currentOnUpdateTopBar(title, onBack)
        } catch (_: Exception) {
            // 忽略 top bar 更新失败，不影响页面渲染
        }
    }

    // 系统返回：计算器详情 → 计算器列表 → 再退出本页
    BackHandler(enabled = selectedCalc != null) {
        selectedCalc = null
    }

    AnimatedContent(
        targetState = selectedCalc,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "CalculatorContent"
    ) { calc ->
        when (calc) {
            null -> CalculatorHubScreen(onSelect = { selectedCalc = it })
            CalcType.COMPOUND_INTEREST -> CompoundInterestScreen()
            CalcType.INCOME_TAX -> PersonalIncomeTaxScreen()
            CalcType.SAVINGS_GOAL -> SavingsGoalScreen()
            CalcType.INSTALLMENT -> InstallmentScreen()
            CalcType.DCA -> DcaScreen()
            CalcType.MATH_DOCS -> MathDocsScreen()
        }
    }
}


@Composable
fun CalculatorHubScreen(onSelect: (CalcType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        calcItems.forEach { (type, title, desc) ->
            val (icon, color) = when (type) {
                CalcType.COMPOUND_INTEREST -> Pair(Icons.Default.Star, Color(0xFF4CAF50))
                CalcType.INCOME_TAX -> Pair(
                    Icons.Default.Info,
                    Color(0xFFFF9800)
                ); CalcType.SAVINGS_GOAL -> Pair(Icons.Default.Favorite, Color(0xFF2196F3))
                CalcType.INSTALLMENT -> Pair(
                    Icons.Default.ShoppingCart,
                    Color(0xFF9C27B0)
                ); CalcType.DCA -> Pair(Icons.Default.Refresh, Color(0xFF00897B))
                CalcType.MATH_DOCS -> Pair(Icons.Default.Info, Color(0xFF607D8B))
            }
            Card(
                onClick = { onSelect(type) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, null, modifier = Modifier.size(26.dp), tint = color) }
                    Spacer(Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.Medium
                    ); Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                }
                }
            }
        }
    }
}


@Composable
fun OutputRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun ResultRow(label: String, value: String, color: Color = Color.Unspecified, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}


@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer(content = content)
}

