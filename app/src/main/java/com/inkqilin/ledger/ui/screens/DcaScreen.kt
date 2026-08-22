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
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun DcaScreen() {
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


fun dcaCalculate(
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


fun dcaSmartFormat(v: BigDecimal): String {
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


fun dcaCalcRequiredPMT(
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
fun DcaStackedChart(
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

