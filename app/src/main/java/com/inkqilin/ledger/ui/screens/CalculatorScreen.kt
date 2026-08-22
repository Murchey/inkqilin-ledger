package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class CalcType { COMPOUND_INTEREST, INCOME_TAX, SAVINGS_GOAL, INSTALLMENT, DCA, MATH_DOCS }
private enum class DcaFrequency(val label: String, val periodsPerYear: Int) {
    WEEKLY(
        "每周",
        52
    ),
    BIWEEKLY("每两周", 26), MONTHLY("每月", 12)
}

private data class DcaYearRecord(
    val year: Int,
    val balance: BigDecimal,
    val totalInvested: BigDecimal,
    val totalProfit: BigDecimal
)

private val calcItems = listOf(
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
private fun CalculatorHubScreen(onSelect: (CalcType) -> Unit) {
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

private fun fmtAmt(v: Double): String =
    if (v == v.toLong().toDouble()) String.format("%,.0f", v) else String.format("%,.2f", v)

private fun Double.f2(): String = String.format("%.2f", this)
@Composable
private fun CalcInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    leadingIcon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            Text(
                suffix,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OutputRow(label: String, value: String, isHighlight: Boolean = false) {
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
private fun CalcButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}


@Composable
private fun CompoundInterestScreen() {
    var principalA by remember { mutableFloatStateOf(100000f) }
    var rateA by remember { mutableFloatStateOf(5f) }
    var yearsA by remember { mutableFloatStateOf(10f) }
    var showCompare by remember { mutableStateOf(false) }
    var principalB by remember { mutableFloatStateOf(100000f) }
    var rateB by remember { mutableFloatStateOf(8f) }
    var yearsB by remember { mutableFloatStateOf(20f) }
    var principalAText by remember { mutableStateOf("100000") }
    var rateAText by remember { mutableStateOf("5.0") }
    var yearsAText by remember { mutableStateOf("10") }
    var principalBText by remember { mutableStateOf("100000") }
    var rateBText by remember { mutableStateOf("8.0") }
    var yearsBText by remember { mutableStateOf("20") }

    fun calcSafe(p: Float, r: Float, y: Int): Double {
        return try {
            p.toDouble() * ((1 + r.toDouble() / 100).pow(y))
        } catch (_: Exception) {
            p.toDouble()
        }
    }

    val finalA = calcSafe(principalA, rateA, yearsA.toInt())
    val profitA = finalA - principalA
    val multipleA = if (principalA > 0) finalA / principalA else 1.0
    val finalB = calcSafe(principalB, rateB, yearsB.toInt())
    val maxYears = if (showCompare) maxOf(yearsA.toInt(), yearsB.toInt()) else yearsA.toInt()
    val dataA = (0..maxYears).map { y -> y to calcSafe(principalA, rateA, y) }
    val dataB = if (showCompare) (0..maxYears).map { y -> y to calcSafe(principalB, rateB, y) } else emptyList<Pair<Int, Double>>()
    val allVals = dataA.map { it.second } + dataB.map { it.second }
    val maxVal = allVals.maxOrNull() ?: 1.0
    val rule72A = if (rateA > 0) 72.0 / rateA else 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // A 组参数卡
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("A 组参数", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        value = principalAText, onValueChange = { v -> principalAText = v; principalA = v.replace(",", "").toFloatOrNull()?.coerceIn(10000f, 1000000f) ?: principalA },
                        label = { Text("初始本金") }, leadingIcon = { Text("¥", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rateAText, onValueChange = { v -> rateAText = v; rateA = v.toFloatOrNull()?.coerceIn(0f, 20f) ?: rateA },
                        label = { Text("年化收益率 (%)") }, trailingIcon = { Text("%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = yearsAText, onValueChange = { v -> if (v.all { it.isDigit() }) { yearsAText = v; yearsA = v.toFloatOrNull()?.coerceIn(1f, 50f) ?: yearsA } },
                        label = { Text("投资年限 (年)") }, trailingIcon = { Text("年", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 对比开关
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Surface(onClick = { showCompare = !showCompare }, shape = RoundedCornerShape(24.dp), color = if (showCompare) Color(0xFFFF6D00) else MaterialTheme.colorScheme.surfaceVariant) {
                    Text(text = if (showCompare) "关闭对比" else "添加对比组 B", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = if (showCompare) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            // B 组参数（仅对比时显示）
            if (showCompare) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFF6D00).copy(alpha = 0.3f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFF6D00), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("B 组参数", fontWeight = FontWeight.SemiBold, color = Color(0xFFFF6D00))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            listOf(Triple("费率之差", Triple(100000f, 8f, 20f), Triple(100000f, 7f, 20f)), Triple("时间之力", Triple(100000f, 6f, 20f), Triple(100000f, 6f, 30f)), Triple("通胀侵蚀", Triple(100000f, 5f, 15f), Triple(100000f, 2f, 15f)), Triple("72 法则", Triple(100000f, 7.2f, 10f), Triple(100000f, 3.6f, 20f))).forEach { (name, a, b) ->
                                Surface(onClick = { principalA = a.first; principalAText = a.first.toInt().toString(); rateA = a.second; rateAText = String.format("%.1f", a.second); yearsA = a.third; yearsAText = a.third.toInt().toString(); principalB = b.first; principalBText = b.first.toInt().toString(); rateB = b.second; rateBText = String.format("%.1f", b.second); yearsB = b.third; yearsBText = b.third.toInt().toString() }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(text = name, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        OutlinedTextField(value = principalBText, onValueChange = { v -> principalBText = v; principalB = v.replace(",", "").toFloatOrNull()?.coerceIn(10000f, 1000000f) ?: principalB }, label = { Text("初始本金") }, leadingIcon = { Text("¥", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF6D00)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = rateBText, onValueChange = { v -> rateBText = v; rateB = v.toFloatOrNull()?.coerceIn(0f, 20f) ?: rateB }, label = { Text("年化收益率 (%)") }, trailingIcon = { Text("%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = yearsBText, onValueChange = { v -> if (v.all { it.isDigit() }) { yearsBText = v; yearsB = v.toFloatOrNull()?.coerceIn(1f, 50f) ?: yearsB } }, label = { Text("投资年限 (年)") }, trailingIcon = { Text("年", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // 曲线图 - 固定高度避免挤压
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("资产增长曲线", fontWeight = FontWeight.SemiBold)
                    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        val chartW = size.width - 80f
                        val chartH = size.height - 60f
                        val padX = 40f
                        val padY = 10f
                        // Y 轴网格
                        for (i in 0..4) {
                            val y = padY + chartH * i / 4
                            drawLine(start = Offset(padX, y), end = Offset(size.width - padX, y), color = Color.Gray.copy(alpha = 0.15f), strokeWidth = 1f)
                        }
                        // A 曲线
                        if (dataA.size > 1) {
                            val pathA = Path().apply {
                                dataA.forEachIndexed { index, (_, v) ->
                                    val x = padX + chartW * index.toFloat() / (dataA.lastIndex.toFloat())
                                    val y = padY + chartH - (v / maxVal * chartH).toFloat()
                                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                                }
                            }
                            drawPath(pathA, Color(0xFF4CAF50), style = Stroke(width = 3f))
                        }
                        // B 曲线
                        if (showCompare && dataB.size > 1) {
                            val pathB = Path().apply {
                                dataB.forEachIndexed { index, (_, v) ->
                                    val x = padX + chartW * index.toFloat() / (dataB.lastIndex.toFloat())
                                    val y = padY + chartH - (v / maxVal * chartH).toFloat()
                                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                                }
                            }
                            drawPath(pathB, Color(0xFFFF6D00), style = Stroke(width = 3f))
                        }
                        // 简化：移除 Canvas 内直接绘制文本（Compose Canvas API 不支持文本）
                        // 年份标注和图例改为在 Canvas 外使用 Text 组件显示
                    }
                }
            }

            // 计算结果
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("计算结果", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    ResultRow(label = "A 终值", value = "¥${String.format("%,.0f", finalA)}", color = Color(0xFF4CAF50))
                    ResultRow(label = "A 收益", value = "¥${String.format("%,.0f", profitA)}", color = Color(0xFF4CAF50))
                    ResultRow(label = "A 倍数", value = String.format("%.2fx", multipleA), bold = true)
                    if (showCompare && principalB > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ResultRow(label = "B 终值", value = "¥${String.format("%,.0f", finalB)}", color = Color(0xFFFF6D00))
                        val diff = finalB - finalA
                        ResultRow(label = "差额 (B-A)", value = "¥${String.format("%,.0f", diff)}", color = if (diff > 0) Color(0xFFFF6D00) else Color(0xFF4CAF50))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ResultRow(label = "72 法则翻倍", value = if (rule72A > 0) String.format("%.1f 年", rule72A) else "—")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}


@Composable
private fun PersonalIncomeTaxScreen() {
    var incomeText by remember { mutableStateOf("10000") }
    var socialInsuranceText by remember { mutableStateOf("3000") }
    var specialDeductionText by remember { mutableStateOf("2000") }
    var monthlyIncome by remember { mutableFloatStateOf(10000f) }
    var socialInsurance by remember { mutableFloatStateOf(3000f) }
    var specialDeduction by remember { mutableFloatStateOf(2000f) }
    
    fun calcMonthlyTax(income: Double, si: Double, sd: Double): Triple<Double, Double, Double> {
        val taxableIncome = income - si - sd - 5000
        if (taxableIncome <= 0) return Triple(0.0, 0.0, income - si - sd)
        
        var tax = 0.0
        var remaining = taxableIncome
        
        val brackets = doubleArrayOf(
            36000.0, 144000.0, 300000.0, 420000.0, 660000.0, 960000.0
        )
        val rates = doubleArrayOf(0.03, 0.10, 0.20, 0.25, 0.30, 0.35, 0.45)
        
        for (i in brackets.indices) {
            val bracketSize = if (i == 0) brackets[i] else brackets[i] - brackets[i - 1]
            if (remaining <= 0) break
            
            val taxableInBracket = remaining.coerceAtMost(bracketSize)
            tax += taxableInBracket * rates[i + 1] // rates shifted by 1 since index 0 is 0.03 start
            remaining -= taxableInBracket
        }
        
        val takeHome = income - si - sd - tax
        return Triple(tax, taxableIncome, takeHome)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("个人所得税", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = incomeText, onValueChange = { v -> incomeText = v; monthlyIncome = v.toFloatOrNull()?.coerceAtLeast(0f) ?: monthlyIncome },
            label = { Text("税前月薪") }, leadingIcon = { Text("¥", style = MaterialTheme.typography.titleMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = socialInsuranceText, onValueChange = { v -> socialInsuranceText = v; socialInsurance = v.toFloatOrNull()?.coerceAtLeast(0f) ?: socialInsurance },
            label = { Text("五险一金（个人）") }, leadingIcon = { Text("🛡️", style = MaterialTheme.typography.titleMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = specialDeductionText, onValueChange = { v -> specialDeductionText = v; specialDeduction = v.toFloatOrNull()?.coerceAtLeast(0f) ?: specialDeduction },
            label = { Text("专项附加扣除") }, leadingIcon = { Text("📋", style = MaterialTheme.typography.titleMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val taxResult = calcMonthlyTax(monthlyIncome.toDouble(), socialInsurance.toDouble(), specialDeduction.toDouble())
                Text("个税计算结果", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("应纳税所得额")
                    Text("¥")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("应缴税额")
                    Text("¥", color = Color(0xFFFF5722))
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("税后到手")
                    Text("¥", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, color: Color = Color.Unspecified, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun SavingsGoalScreen() {
    var goalText by remember { mutableStateOf("50000") };
    var targetMonths by remember { mutableIntStateOf(12) };
    var savedText by remember { mutableStateOf("0") }
    var showSaved by remember { mutableStateOf(false) };
    var monthlyText by remember { mutableStateOf("") };
    var rateText by remember { mutableStateOf("2") };
    var isModeB by remember { mutableStateOf(false) }
    var resultMonthly by remember { mutableStateOf<BigDecimal?>(null) };
    var resultMonths by remember { mutableIntStateOf(0) };
    var resultProgress by remember { mutableFloatStateOf(0f) }
    var resultTargetDate by remember { mutableStateOf("") };
    var resultDaily by remember { mutableStateOf<BigDecimal?>(null) }
    fun calc() {
        val goal = goalText.toBigDecimalOrNull() ?: return;
        val saved = if (showSaved) savedText.toBigDecimalOrNull()
            ?: BigDecimal.ZERO else BigDecimal.ZERO
        val rate =
            (rateText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 20.0) / 100;
        val mr = rate / 12
        if (isModeB) {
            val monthly = monthlyText.toBigDecimalOrNull()
                ?: return; if (monthly <= BigDecimal.ZERO) return
            var bal = saved;
            var m = 0;
            val max = 120; while (bal < goal && m < max) {
                bal =
                    bal.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(mr)))
                        .add(monthly).setScale(2, RoundingMode.HALF_UP); m++
            }; resultMonths = m; resultMonthly = monthly
        } else {
            val n = targetMonths; if (n <= 0) return;
            val remaining = goal.subtract(
                saved.multiply(
                    BigDecimal.ONE.add(
                        BigDecimal.valueOf(mr)
                    ).pow(n)
                )
            ).coerceAtLeast(BigDecimal.ZERO)
            if (mr == 0.0) {
                resultMonthly =
                    remaining.divide(BigDecimal(n), 2, RoundingMode.CEILING)
            } else {
                val factor =
                    BigDecimal.ONE.add(BigDecimal.valueOf(mr)).pow(n);
                val annuity = factor.subtract(BigDecimal.ONE).divide(
                    BigDecimal.valueOf(mr),
                    10,
                    RoundingMode.HALF_UP
                ); resultMonthly =
                    remaining.divide(annuity, 2, RoundingMode.CEILING)
            }; resultMonths = n
        }
        val cal = java.util.Calendar.getInstance(); cal.add(
            java.util.Calendar.MONTH,
            resultMonths
        ); resultTargetDate =
            cal.get(java.util.Calendar.YEAR).toString() + "年" + (cal.get(
                java.util.Calendar.MONTH
            ) + 1) + "月"
        resultProgress =
            if (goal > BigDecimal.ZERO) saved.toFloat() / goal.toFloat() else 0f; resultDaily =
            resultMonthly?.divide(BigDecimal(30), 0, RoundingMode.CEILING)
    }
    LaunchedEffect(
        goalText,
        targetMonths,
        savedText,
        showSaved,
        monthlyText,
        rateText,
        isModeB
    ) { delay(150); calc() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(22.dp)
                    ); Spacer(Modifier.width(8.dp)); Text(
                    "储蓄目标",
                    fontWeight = FontWeight.SemiBold
                )
                }
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text("目标金额") },
                    leadingIcon = {
                        Text(
                            "¥",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2196F3)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetMonths.toString(),
                    onValueChange = { v ->
                        if (v.all { it.isDigit() } && v.length <= 2) targetMonths =
                            v.toIntOrNull() ?: targetMonths
                    },
                    label = { Text("期望达成（月）") },
                    trailingIcon = {
                        Text(
                            "个月后（" + resultTargetDate + "）",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2196F3)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    showSaved = !showSaved; if (!showSaved) savedText = "0"
                }) { Text(if (showSaved) "收起" else "已有积蓄") }
                if (showSaved) OutlinedTextField(
                    value = savedText,
                    onValueChange = { savedText = it },
                    label = { Text("已存金额") },
                    leadingIcon = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isModeB) OutlinedTextField(
                    value = monthlyText,
                    onValueChange = { monthlyText = it },
                    label = { Text("每月可存") },
                    leadingIcon = {
                        Text(
                            "¥",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFF6D00)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("预估年化") },
                        trailingIcon = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "0" to "不计息",
                            "2" to "货币基金"
                        ).forEach { (v, l) ->
                            val sel = rateText == v; Surface(
                            onClick = { rateText = v },
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) Color(0xFF2196F3) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                l,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                fontSize = 12.sp,
                                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = { isModeB = !isModeB },
                shape = RoundedCornerShape(24.dp),
                color = if (isModeB) Color(0xFFFF6D00) else Color(0xFF2196F3)
            ) {
                Text(
                    if (isModeB) "切换到算月存" else "切换到达标日期",
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 10.dp
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
        if (resultMonthly != null && resultMonthly!! > BigDecimal.ZERO) {
            val saved = if (showSaved) savedText.toBigDecimalOrNull()
                ?: BigDecimal.ZERO else BigDecimal.ZERO;
            val goal = goalText.toBigDecimalOrNull() ?: BigDecimal.ZERO;
            val progressPct = (resultProgress * 100).toInt()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "储蓄进度",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ); Text(
                        progressPct.toString() + "%",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    }
                    LinearProgressIndicator(
                        progress = {
                            resultProgress.coerceIn(
                                0f,
                                1f
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF2196F3),
                        trackColor = Color(0xFF2196F3).copy(alpha = 0.15f)
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "已存 ¥" + dcaSmartFormat(saved),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ); Text(
                        "目标 ¥" + dcaSmartFormat(goal),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2196F3).copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (isModeB) "预计达成" else "每月需存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isModeB) {
                        Text(
                            resultTargetDate,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    } else {
                        Text(
                            "¥" + resultMonthly!!.toPlainString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        ); Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                12.dp
                            )
                        ) {
                            Text(
                                "约 ¥" + resultDaily!!.toPlainString() + "/天",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(); Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("目标金额"); Text(
                    "¥" + dcaSmartFormat(goal),
                    fontWeight = FontWeight.SemiBold
                )
                }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("达成时间"); Text(
                        resultMonths.toString() + "个月后（" + resultTargetDate + "）",
                        fontWeight = FontWeight.SemiBold
                    )
                    }
                }
            }
            if (!isModeB) {
                val rate = (rateText.toDoubleOrNull() ?: 0.0).coerceIn(
                    0.0,
                    20.0
                ) / 100;
                val less500 = resultMonthly!!.subtract(BigDecimal(500))
                    .coerceAtLeast(BigDecimal.ONE)
                var bal = saved;
                var m = 0;
                val mr = rate / 12;
                val max = 120; while (bal < goal && m < max) {
                    bal = bal.multiply(
                        BigDecimal.ONE.add(
                            BigDecimal.valueOf(mr)
                        )
                    ).add(less500).setScale(2, RoundingMode.HALF_UP); m++
                }
                if (m > resultMonths) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF9800).copy(alpha = 0.08f)
                        )
                    ) {
                        Text(
                            "每月少存500元，将推迟 " + (m - resultMonths) + " 个月达成",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
            Text(
                "提示：利率默认2%（货币基金水平），储蓄重在纪律而非收益",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.6f
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // 底部导航栏留白
        Spacer(modifier = Modifier.height(76.dp))
    }
}

@Composable
private fun InstallmentScreen() {
    var amountText by remember { mutableStateOf("5000") };
    var periods by remember { mutableIntStateOf(12) };
    var monthlyText by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) };
    var feeText by remember { mutableStateOf("0") };
    var feeMode by remember { mutableStateOf(0) }
    var resultAPR by remember { mutableStateOf<BigDecimal?>(null) };
    var resultMonthly by remember { mutableStateOf<BigDecimal?>(null) };
    var resultTotalInterest by remember { mutableStateOf<BigDecimal?>(null) };
    var resultTotal by remember { mutableStateOf<BigDecimal?>(null) };
    var errorMsg by remember { mutableStateOf<String?>(null) }
    fun calc() {
        val P = amountText.toBigDecimalOrNull() ?: return;
        val n = periods;
        val M = monthlyText.toBigDecimalOrNull();
        val fee =
            feeText.toBigDecimalOrNull() ?: BigDecimal.ZERO; errorMsg = null
        if (M != null && M > BigDecimal.ZERO) {
            val eP = if (feeMode == 1) P.subtract(fee)
                .coerceAtLeast(BigDecimal.ONE) else P;
            val eM = if (feeMode == 0) M.add(fee) else M
            if (eM.multiply(BigDecimal(n)) < eP) {
                errorMsg = "月供*期数 < 本金"; resultAPR =
                    null; resultMonthly = null; return
            }
            val apr = solveAPR(eP, eM, n); if (apr == null) {
                errorMsg = "无法收敛"; resultAPR = null; resultMonthly =
                    null; return
            }
            resultAPR = apr.multiply(BigDecimal(1200))
                .setScale(2, RoundingMode.HALF_UP); resultMonthly =
                M.setScale(2, RoundingMode.HALF_UP); resultTotal =
                M.multiply(BigDecimal(n)).setScale(
                    2,
                    RoundingMode.HALF_UP
                ); resultTotalInterest =
                resultTotal!!.subtract(P).setScale(2, RoundingMode.HALF_UP)
        } else {
            val linear = P.divide(
                BigDecimal(n),
                2,
                RoundingMode.CEILING
            ); resultMonthly = linear; resultTotal =
                linear.multiply(BigDecimal(n)).setScale(
                    2,
                    RoundingMode.HALF_UP
                ); resultTotalInterest = resultTotal!!.subtract(P)
                .setScale(2, RoundingMode.HALF_UP); resultAPR = null
        }
    }
    LaunchedEffect(
        amountText,
        periods,
        monthlyText,
        feeText,
        feeMode
    ) { delay(150); calc() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(22.dp)
                    ); Spacer(Modifier.width(8.dp)); Text(
                    "分期信息",
                    fontWeight = FontWeight.SemiBold
                )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("分期总金额") },
                    leadingIcon = {
                        Text(
                            "¥",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "分期期数"
                    ); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        3,
                        6,
                        12,
                        24
                    ).forEach { v ->
                        val sel = periods == v; Surface(
                        onClick = { periods = v },
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) Color(0xFF9C27B0) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            v.toString() + "期",
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            fontSize = 13.sp,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    }
                }
                }
                OutlinedTextField(
                    value = monthlyText,
                    onValueChange = { monthlyText = it },
                    label = { Text("每期还款额（必填）") },
                    leadingIcon = {
                        Text(
                            "¥",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFF6D00)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    showAdvanced = !showAdvanced
                }) { Text(if (showAdvanced) "收起高级选项" else "高级选项：手续费") }
                if (showAdvanced) {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        label = { Text("手续费/服务费") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to "每期收取",
                            1 to "一次性收取"
                        ).forEach { (v, l) ->
                            val sel = feeMode == v; Surface(
                            onClick = { feeMode = v },
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) Color(0xFF9C27B0) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                l,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                fontSize = 12.sp,
                                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        }
                    }
                }
            }
        }
        if (errorMsg != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(
                        alpha = 0.08f
                    )
                )
            ) {
                Text(
                    errorMsg!!,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (resultAPR != null) {
            val ir =
                if (resultTotal!! > BigDecimal.ZERO) resultTotalInterest!!.multiply(
                    BigDecimal(100)
                ).divide(
                    resultTotal!!,
                    1,
                    RoundingMode.HALF_UP
                ) else BigDecimal.ZERO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF6D00).copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "真实年化利率",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ); Text(
                    resultAPR!!.toPlainString() + "%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6D00)
                )
                    HorizontalDivider(); Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("总利息"); Text(
                        "¥" + resultTotalInterest!!.toPlainString(),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    }; Column(horizontalAlignment = Alignment.End) {
                    Text("利息占比"); Text(
                    ir.toPlainString() + "%",
                    fontWeight = FontWeight.SemiBold
                )
                }
                }
                }
            }
        } else if (resultMonthly != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF9C27B0).copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutputRow(
                        "每期还款",
                        "¥" + resultMonthly!!.toPlainString(),
                        isHighlight = true
                    ); HorizontalDivider(); OutputRow(
                    "还款总额",
                    "¥" + resultTotal!!.toPlainString()
                ); OutputRow(
                    "总利息",
                    "¥" + resultTotalInterest!!.toPlainString()
                )
                }
            }
        }
        if (resultMonthly != null && errorMsg == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("每月还款"); Text(
                        "¥" + resultMonthly!!.toPlainString(),
                        fontWeight = FontWeight.SemiBold
                    )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("还款期数"); Text(
                        periods.toString() + "期",
                        fontWeight = FontWeight.SemiBold
                    )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("到期总还款"); Text(
                        "¥" + resultTotal!!.toPlainString(),
                        fontWeight = FontWeight.SemiBold
                    )
                    }
                }
            }
        }
        Text(
            "提示：输入每期还款额可反推真实年化利率",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // 底部导航栏留白
        Spacer(modifier = Modifier.height(76.dp))
    }
}

private fun solveAPR(P: BigDecimal, M: BigDecimal, n: Int): BigDecimal? {
    if (M.multiply(BigDecimal(n)).compareTo(P) < 0) return null;
    val lin =
        P.divide(BigDecimal(n), 10, RoundingMode.HALF_UP); if (M.compareTo(
            lin
        ) <= 0
    ) return BigDecimal.ZERO;
    var lo = BigDecimal.ZERO;
    var hi = BigDecimal("0.1"); for (i in 1..100) {
        val mid = lo.add(hi)
            .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        val f = aprFunc(
            P,
            mid,
            n,
            M
        ); if (f.abs() < BigDecimal("0.00000001")) return mid; if (f > BigDecimal.ZERO) hi =
            mid else lo = mid
    }; return lo.add(hi)
        .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP)
}

private fun aprFunc(
    P: BigDecimal,
    r: BigDecimal,
    n: Int,
    M: BigDecimal
): BigDecimal {
    if (r.compareTo(BigDecimal.ZERO) == 0) return P.divide(
        BigDecimal(n),
        10,
        RoundingMode.HALF_UP
    ).subtract(M);
    val op = BigDecimal.ONE.add(r);
    val pw = op.pow(n); return P.multiply(r).multiply(pw)
        .divide(pw.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP)
        .subtract(M)
}

@Composable
private fun DcaScreen() {
    var amountText by remember { mutableStateOf("1000") };
    var yearsSlider by remember { mutableFloatStateOf(10f) };
    var rateText by remember { mutableStateOf("6") }
    var frequency by remember { mutableStateOf(DcaFrequency.MONTHLY) };
    var showAdvanced by remember { mutableStateOf(false) };
    var initialText by remember { mutableStateOf("0") }
    var schedule by remember { mutableStateOf<List<DcaYearRecord>>(emptyList()) };
    var showAllYears by remember { mutableStateOf(false) }
    var isGoalMode by remember { mutableStateOf(false) };
    var goalAmountText by remember { mutableStateOf("1000000") }
    var requiredPMT by remember { mutableStateOf<BigDecimal?>(null) };
    var goalSchedule by remember {
        mutableStateOf<List<DcaYearRecord>>(
            emptyList()
        )
    }

    fun triggerCalc() {
        val rate =
            (rateText.toDoubleOrNull() ?: return).coerceIn(0.0, 20.0) / 100;
        val y = yearsSlider.toInt().coerceIn(1, 50);
        val init = if (showAdvanced) initialText.toBigDecimalOrNull()
            ?: BigDecimal.ZERO else BigDecimal.ZERO
        if (isGoalMode) {
            val goal = goalAmountText.toBigDecimalOrNull() ?: return;
            val pmt = dcaCalcRequiredPMT(
                goal,
                rate,
                y,
                frequency,
                init
            ); requiredPMT =
                pmt; if (pmt != null && pmt >= BigDecimal.ZERO) goalSchedule =
                dcaCalculate(
                    pmt,
                    rate,
                    y,
                    frequency,
                    init
                ) else goalSchedule = emptyList(); schedule = emptyList()
        } else {
            val amt = amountText.toBigDecimalOrNull() ?: return; schedule =
                dcaCalculate(amt, rate, y, frequency, init); requiredPMT =
                null; goalSchedule = emptyList()
        }
    }
    LaunchedEffect(
        amountText,
        yearsSlider,
        rateText,
        frequency,
        showAdvanced,
        initialText,
        isGoalMode,
        goalAmountText
    ) { delay(150); triggerCalc() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isGoalMode) OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("每期投入金额") },
                    leadingIcon = {
                        Text(
                            "¥",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                else OutlinedTextField(
                    value = goalAmountText,
                    onValueChange = { goalAmountText = it },
                    label = { Text("目标金额") },
                    leadingIcon = {
                        Text(
                            "¥",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFF6D00)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("投资年限"); Text(
                        yearsSlider.toInt().toString() + " 年",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    }; Slider(
                    value = yearsSlider,
                    onValueChange = { yearsSlider = it },
                    valueRange = 1f..50f,
                    steps = 48
                )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("预期年化收益率") },
                        trailingIcon = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "4" to "保守",
                            "6" to "稳健",
                            "8" to "积极"
                        ).forEach { (v, l) ->
                            val sel = rateText == v; Surface(
                            onClick = { rateText = v },
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                l + " " + v + "%",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                fontSize = 12.sp,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "定投频率"
                    ); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DcaFrequency.entries.forEach { f ->
                        val sel = frequency == f; Surface(
                        onClick = { frequency = f },
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            f.label,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            fontSize = 13.sp,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    }
                }
                }
                TextButton(onClick = {
                    showAdvanced =
                        !showAdvanced; if (!showAdvanced) initialText = "0"
                }) { Text(if (showAdvanced) "收起高级选项" else "高级选项：初始本金") }
                if (showAdvanced) OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it },
                    label = { Text("初始本金") },
                    leadingIcon = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = { isGoalMode = !isGoalMode },
                shape = RoundedCornerShape(24.dp),
                color = if (isGoalMode) Color(0xFFFF6D00) else MaterialTheme.colorScheme.primary
            ) {
                Text(
                    if (isGoalMode) "切换到正向计算" else "设定目标",
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 10.dp
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
        if (!isGoalMode && schedule.isNotEmpty()) {
            val last = schedule.last();
            val totalFmt = dcaSmartFormat(last.balance);
            val profitFmt = dcaSmartFormat(last.totalProfit);
            val ratePct =
                if (last.totalInvested > BigDecimal.ZERO) last.totalProfit.divide(
                    last.totalInvested,
                    4,
                    RoundingMode.HALF_UP
                ).multiply(BigDecimal(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    .toPlainString() else "0"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00897B).copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "最终总资产",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ); Text(
                    "¥$totalFmt",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                    HorizontalDivider(); Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("总收益"); Text(
                        "+¥$profitFmt",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                    }; Column(horizontalAlignment = Alignment.End) {
                    Text("收益率"); Text(
                    "+$ratePct%",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold
                )
                }
                }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "资产增长曲线",
                        fontWeight = FontWeight.SemiBold
                    ); Spacer(Modifier.height(12.dp)); DcaStackedChart(
                    schedule,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ); Spacer(Modifier.height(8.dp)); Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        16.dp
                    )
                ) {
                    Row {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF00897B))
                        ); Spacer(Modifier.width(4.dp)); Text(
                        "累计投入",
                        fontSize = 11.sp
                    )
                    }; Row {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF4CAF50))
                    ); Spacer(Modifier.width(4.dp)); Text(
                    "累计收益",
                    fontSize = 11.sp
                )
                }
                }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "年度明细",
                        fontWeight = FontWeight.SemiBold
                    ); Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth()) {
                    listOf(
                        "年份" to 1f,
                        "年末资产" to 1.3f,
                        "累计投入" to 1.2f,
                        "累计收益" to 1.2f
                    ).forEach { (h, w) ->
                        Text(
                            h,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(w),
                            textAlign = TextAlign.Center
                        )
                    }
                }; HorizontalDivider()
                    val display =
                        if (showAllYears) schedule else schedule.filterIndexed { i, r -> i == 0 || r.year == 5 || r.year == 10 || r.year == schedule.last().year || (schedule.size > 20 && r.year % 10 == 0) }
                    display.forEach { r ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                "第" + r.year + "年",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            ); Text(
                            "¥" + dcaSmartFormat(r.balance),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.3f),
                            textAlign = TextAlign.Center
                        ); Text(
                            "¥" + dcaSmartFormat(r.totalInvested),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center
                        ); Text(
                            "¥" + dcaSmartFormat(r.totalProfit),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center
                        )
                        }
                    }
                    if (schedule.size > 2 && !showAllYears) TextButton(
                        onClick = { showAllYears = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("展开详细") }
                }
            }
        }
        if (isGoalMode && requiredPMT != null) {
            val pmt = requiredPMT!!;
            val goal =
                goalAmountText.toBigDecimalOrNull() ?: BigDecimal.ZERO;
            val init = if (showAdvanced) initialText.toBigDecimalOrNull()
                ?: BigDecimal.ZERO else BigDecimal.ZERO;
            val y = yearsSlider.toInt().coerceIn(1, 50);
            val ppy = frequency.periodsPerYear
            if (pmt < BigDecimal.ZERO || goal <= init) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "现有本金已足够达成目标",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50)
                        ); Text(
                        "无需额外定投",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                }
            } else {
                val pmtR = pmt.setScale(0, RoundingMode.CEILING)
                    .divide(BigDecimal.TEN).multiply(BigDecimal.TEN)
                    .setScale(0, RoundingMode.CEILING)
                val wk = pmtR.multiply(BigDecimal(ppy.toLong()))
                    .divide(BigDecimal(52), 0, RoundingMode.CEILING);
                val dy = pmtR.multiply(BigDecimal(ppy.toLong()))
                    .divide(BigDecimal(365), 0, RoundingMode.CEILING)
                val cal = java.util.Calendar.getInstance(); cal.add(
                    java.util.Calendar.YEAR,
                    y
                );
                val tY = cal.get(java.util.Calendar.YEAR);
                val tM = cal.get(java.util.Calendar.MONTH) + 1
                val tInv =
                    pmt.multiply(BigDecimal((y * ppy).toLong())).add(init);
                val ratio = if (goal > BigDecimal.ZERO) tInv.divide(
                    goal,
                    4,
                    RoundingMode.HALF_UP
                ).multiply(BigDecimal(100))
                    .setScale(1, RoundingMode.HALF_UP) else BigDecimal.ZERO
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF6D00).copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "当月应投",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ); Text(
                        "¥$pmtR",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6D00)
                    )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "约 ¥$wk/周",
                                style = MaterialTheme.typography.bodySmall
                            ); Text(
                            "约 ¥$dy/天",
                            style = MaterialTheme.typography.bodySmall
                        )
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("目标金额"); Text(
                            "¥" + dcaSmartFormat(goal),
                            fontWeight = FontWeight.SemiBold
                        )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("预计达标"); Text(
                            tY.toString() + "年" + tM + "月",
                            fontWeight = FontWeight.SemiBold
                        )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) { Text("累计投入"); Text("¥" + dcaSmartFormat(tInv) + "（占" + ratio + "%）") }
                    }
                }
            }
        }
        Text(
            "注：未考虑通胀与交易费用",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // 底部导航栏留白
        Spacer(modifier = Modifier.height(76.dp))
    }
}

private fun dcaCalculate(
    p: BigDecimal,
    r: Double,
    y: Int,
    f: DcaFrequency,
    init: BigDecimal
): List<DcaYearRecord> {
    val ppy = f.periodsPerYear;
    val t = y * ppy;
    val pr = BigDecimal.valueOf(r)
        .divide(BigDecimal.valueOf(ppy.toLong()), 10, RoundingMode.HALF_UP);
    var bal = init.setScale(10, RoundingMode.HALF_UP);
    var inv = init.setScale(10, RoundingMode.HALF_UP);
    val res = mutableListOf<DcaYearRecord>(); for (i in 1..t) {
        val interest =
            bal.multiply(pr).setScale(10, RoundingMode.HALF_UP); bal =
            bal.add(interest).add(p)
                .setScale(10, RoundingMode.HALF_UP); inv = inv.add(p)
            .setScale(
                10,
                RoundingMode.HALF_UP
            ); if (i % ppy == 0 || i == t) res.add(
            DcaYearRecord(
                year = (i + ppy - 1) / ppy,
                balance = bal.setScale(2, RoundingMode.HALF_UP),
                totalInvested = inv.setScale(2, RoundingMode.HALF_UP),
                totalProfit = bal.subtract(inv)
                    .setScale(2, RoundingMode.HALF_UP)
            )
        )
    }; return res
}

private fun dcaSmartFormat(v: BigDecimal): String {
    val d = v.setScale(2, RoundingMode.HALF_UP);
    val a = d.abs(); return when {
        a >= BigDecimal("100000000") -> String.format(
            "%.2f",
            d.toDouble() / 100000000
        ) + "亿"; a >= BigDecimal("10000") -> String.format(
            "%.2f",
            d.toDouble() / 10000
        ) + "万"; else -> String.format("%,.2f", d.toDouble())
    }
}

private fun dcaCalcRequiredPMT(
    fv: BigDecimal,
    r: Double,
    y: Int,
    f: DcaFrequency,
    init: BigDecimal
): BigDecimal? {
    val ppy = f.periodsPerYear;
    val t = y * ppy;
    val i = BigDecimal.valueOf(r)
        .divide(BigDecimal.valueOf(ppy.toLong()), 10, RoundingMode.HALF_UP);
    val fvFactor =
        if (r == 0.0) BigDecimal.ONE else BigDecimal.ONE.add(i).pow(t);
    val pvFV = init.multiply(fvFactor).setScale(10, RoundingMode.HALF_UP);
    val num = fv.subtract(pvFV)
        .setScale(10, RoundingMode.HALF_UP); if (r == 0.0) {
        val d =
            BigDecimal(t.toLong()); return if (d > BigDecimal.ZERO) num.divide(
            d,
            2,
            RoundingMode.CEILING
        ) else null
    };
    val den = fvFactor.subtract(BigDecimal.ONE).divide(
        i,
        10,
        RoundingMode.HALF_UP
    ); if (den <= BigDecimal.ZERO) return null; return num.divide(
        den,
        2,
        RoundingMode.CEILING
    )
}

@Composable
private fun DcaStackedChart(
    schedule: List<DcaYearRecord>,
    modifier: Modifier = Modifier
) {
    if (schedule.size < 2) return;
    val ic = Color(0xFF00897B);
    val pc = Color(0xFF4CAF50);
    val mv =
        schedule.last().balance.toFloat(); if (mv <= 0f) return; Canvas(
        modifier = modifier
    ) {
        val w = size.width;
        val h = size.height;
        val n = schedule.size;
        val dx = w / (n - 1); drawPath(
        Path().apply {
            moveTo(
                0f,
                h
            ); schedule.forEachIndexed { idx, r ->
            lineTo(
                idx * dx,
                h - (r.totalInvested.toFloat() / mv) * h
            )
        }; lineTo((n - 1) * dx, h); close()
        },
        color = ic.copy(alpha = 0.35f),
        style = Fill
    ); drawPath(
        Path().apply {
            moveTo(
                0f,
                h
            ); schedule.forEachIndexed { idx, r ->
            lineTo(
                idx * dx,
                h - (r.balance.toFloat() / mv) * h
            )
        }; lineTo((n - 1) * dx, h); close()
        },
        color = pc.copy(alpha = 0.25f),
        style = Fill
    ); drawPath(Path().apply {
        moveTo(
            0f,
            h - (schedule[0].balance.toFloat() / mv) * h
        ); for (idx in 1 until n) lineTo(
        idx * dx,
        h - (schedule[idx].balance.toFloat() / mv) * h
    )
    }, color = pc, style = Stroke(width = 3f))
    }
}

@Composable
private fun MathDocsScreen() {
    val context = LocalContext.current
    val markdownText = remember {
        try {
            context.assets.open("math_docs.md").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "文档加载失败："
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📖 数学原理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("各计算器的公式、算法与验证说明", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            SelectionContainer {
                val styledContent = com.inkqilin.ledger.util.MarkdownRenderer.renderMarkdown(markdownText)
                Text(text = styledContent, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp, modifier = Modifier.padding(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(76.dp))
    }
}

@Composable
private fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer(content = content)
}
