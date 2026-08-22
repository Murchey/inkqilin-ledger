package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode

private enum class SavingsMode(val label: String, val desc: String) {
    MONTHLY("算月存", "目标 → 每月存多少"),
    TIMELINE("算达标", "每月存 → 多久达标"),
    FORWARD("正向预测", "每月存 → 到期多少")
}

@Composable
fun SavingsGoalScreen() {
    var mode by remember { mutableStateOf(SavingsMode.MONTHLY) }
    var rateText by remember { mutableStateOf("2.0") }
    var savedText by remember { mutableStateOf("0") }
    var goalText by remember { mutableStateOf("50000") }
    var monthsText by remember { mutableStateOf("12") }
    var monthlyText by remember { mutableStateOf("3000") }
    var fwdMonthlyText by remember { mutableStateOf("2000") }
    var fwdMonthsText by remember { mutableStateOf("24") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SavingsModeTabs(mode) { mode = it }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (mode) {
                    SavingsMode.MONTHLY -> {
                        LabelField("目标金额"); MoneyInput(goalText, { goalText = it }, "¥", Color(0xFF2196F3))
                        LabelField("计划期限"); MonthsInput(monthsText, { monthsText = it })
                    }
                    SavingsMode.TIMELINE -> {
                        LabelField("目标金额"); MoneyInput(goalText, { goalText = it }, "¥", Color(0xFF2196F3))
                        LabelField("每月可存"); MoneyInput(monthlyText, { monthlyText = it }, "¥", Color(0xFFFF6D00))
                    }
                    SavingsMode.FORWARD -> {
                        LabelField("每月存入"); MoneyInput(fwdMonthlyText, { fwdMonthlyText = it }, "¥", Color(0xFF4CAF50))
                        LabelField("计划期限"); MonthsInput(fwdMonthsText, { fwdMonthsText = it })
                    }
                }
                HorizontalDivider()
                LabelField("已有存款"); MoneyInput(savedText, { savedText = it }, "¥", Color.Gray)
                LabelField("预估年化")
                OutlinedTextField(value = rateText, onValueChange = { rateText = it },
                    trailingIcon = { Text("%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("0" to "不计息", "2" to "货币基金", "3" to "定期").forEach { (v, l) ->
                        QuickChip(v, l, rateText) { rateText = v }
                    }
                }
            }
        }

        when (mode) {
            SavingsMode.MONTHLY -> MonthlyResult(goalText, monthsText, savedText, rateText)
            SavingsMode.TIMELINE -> TimelineResult(goalText, monthlyText, savedText, rateText)
            SavingsMode.FORWARD -> ForwardResult(fwdMonthlyText, fwdMonthsText, savedText, rateText)
        }
        Spacer(modifier = Modifier.height(76.dp))
    }
}
// ════ Results ════

@Composable
private fun MonthlyResult(goalText: String, monthsText: String, savedText: String, rateText: String) {
    val goal = goalText.toBigDecimalOrNull() ?: return
    val n = monthsText.toIntOrNull()?.coerceIn(1, 600) ?: return
    val saved = savedText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val rate = (rateText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 30.0) / 100; val mr = rate / 12
    val futureSaved = if (mr == 0.0) saved else saved.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(mr)).pow(n)).setScale(2, RoundingMode.HALF_UP)
    val remaining = (goal - futureSaved).coerceAtLeast(BigDecimal.ZERO)
    val monthly = if (mr == 0.0) remaining.divide(BigDecimal(n), 2, RoundingMode.CEILING) else {
        val factor = BigDecimal.ONE.add(BigDecimal.valueOf(mr)).pow(n)
        val annuity = factor.subtract(BigDecimal.ONE).divide(BigDecimal.valueOf(mr), 10, RoundingMode.HALF_UP)
        remaining.divide(annuity, 2, RoundingMode.CEILING)
    }
    val daily = monthly.divide(BigDecimal(30), 0, RoundingMode.CEILING)
    val totalInterest = goal.subtract(monthly.multiply(BigDecimal(n)).add(saved)).coerceAtLeast(BigDecimal.ZERO)

    BigResult("每月需存", "¥${monthly.toPlainString()}", Color(0xFF2196F3)) {
        InfoRow("每天约", "¥${daily.toPlainString()}")
        InfoRow("目标金额", "¥${smartFmt(goal)}")
        InfoRow("期限", "${n}个月（${futureDate(n)}）")
        if (rate > 0) InfoRow("预计利息", "¥${smartFmt(totalInterest)}")
    }
}

@Composable
private fun TimelineResult(goalText: String, monthlyText: String, savedText: String, rateText: String) {
    val goal = goalText.toBigDecimalOrNull() ?: return
    val monthly = monthlyText.toBigDecimalOrNull() ?: return; if (monthly <= BigDecimal.ZERO) return
    val saved = savedText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val rate = (rateText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 30.0) / 100; val mr = rate / 12
    var bal = saved; var m = 0
    while (bal < goal && m < 600) { bal = bal.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(mr))).add(monthly).setScale(2, RoundingMode.HALF_UP); m++ }
    val reached = bal >= goal
    val years = m / 12; val rem = m % 12
    val timeStr = if (years > 0) "${years}年${rem}个月" else "${m}个月"
    val totalIn = monthly.multiply(BigDecimal(m)).add(saved)
    val totalInt = (bal - totalIn).coerceAtLeast(BigDecimal.ZERO)

    if (reached) {
        BigResult("预计达标", timeStr, Color(0xFFFF6D00)) {
            InfoRow("达标日期", futureDate(m)); InfoRow("届时总额", "¥${smartFmt(bal)}")
            InfoRow("累计投入", "¥${smartFmt(totalIn)}"); if (rate > 0) InfoRow("累计利息", "¥${smartFmt(totalInt)}")
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f))) {
            Text("按当前存款和利率，50年内无法达成目标。请提高月存金额。", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ForwardResult(monthlyText: String, monthsText: String, savedText: String, rateText: String) {
    val monthly = monthlyText.toBigDecimalOrNull() ?: return
    val n = monthsText.toIntOrNull()?.coerceIn(1, 600) ?: return
    val saved = savedText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val rate = (rateText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 30.0) / 100; val mr = rate / 12
    var bal = saved; var totalIn = saved
    val records = mutableListOf<Triple<Int, BigDecimal, BigDecimal>>()
    for (i in 1..n) {
        bal = bal.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(mr))).add(monthly).setScale(2, RoundingMode.HALF_UP)
        totalIn = totalIn.add(monthly)
        if (i % 12 == 0 || i == n) records.add(Triple(i, bal, totalIn))
    }
    val totalInvested = monthly.multiply(BigDecimal(n)).add(saved)
    val profit = bal - totalInvested
    val profitPct = if (totalInvested > BigDecimal.ZERO) profit.multiply(BigDecimal(100)).divide(totalInvested, 1, RoundingMode.HALF_UP) else BigDecimal.ZERO

    BigResult("到期总额", "¥${smartFmt(bal)}", Color(0xFF4CAF50)) {
        InfoRow("到期日期", futureDate(n)); InfoRow("累计投入", "¥${smartFmt(totalInvested)}")
        if (rate > 0) InfoRow("累计收益", "+¥${smartFmt(profit)}（+${profitPct}%）")
    }

    if (records.size > 1) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("增长明细", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("时间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("总额", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                    Text("投入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
                    Text("收益", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                }
                HorizontalDivider()
                records.forEach { (months, balance, invested) ->
                    val pft = balance - invested
                    val label = if (months % 12 == 0) "第${months / 12}年" else "第${months}月"
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("¥${smartFmt(balance)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                        Text("¥${smartFmt(invested)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
                        Text(if (rate > 0) "+¥${smartFmt(pft)}" else "—", style = MaterialTheme.typography.bodySmall,
                            color = if (pft > BigDecimal.ZERO) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
// ════ UI Helpers ════

@Composable
private fun SavingsModeTabs(current: SavingsMode, onSelect: (SavingsMode) -> Unit) {
    TabRow(selectedTabIndex = current.ordinal,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.primary) {
        SavingsMode.entries.forEach { m ->
            Tab(selected = current == m, onClick = { onSelect(m) }, text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(m.label, fontWeight = if (current == m) FontWeight.Bold else FontWeight.Normal)
                    Text(m.desc, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            })
        }
    }
}

@Composable
private fun LabelField(t: String) {
    Text(t, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MoneyInput(value: String, onChange: (String) -> Unit, prefix: String, tint: Color) {
    OutlinedTextField(value = value, onValueChange = onChange,
        leadingIcon = { Text(prefix, style = MaterialTheme.typography.titleMedium, color = tint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun MonthsInput(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 3) onChange(v) },
        trailingIcon = { Text("个月") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun QuickChip(value: String, label: String, current: String, onSelect: () -> Unit) {
    val sel = current == value
    Surface(onClick = onSelect, shape = RoundedCornerShape(8.dp),
        color = if (sel) Color(0xFF2196F3) else MaterialTheme.colorScheme.surfaceVariant) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp,
            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BigResult(title: String, value: String, color: Color, details: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            HorizontalDivider()
            details()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

// ════ Utils ════

private fun smartFmt(v: BigDecimal): String {
    val d = v.setScale(2, RoundingMode.HALF_UP); val a = d.abs()
    return when {
        a >= BigDecimal("100000000") -> String.format("%.2f", d.toDouble() / 100000000) + "亿"
        a >= BigDecimal("10000") -> String.format("%.2f", d.toDouble() / 10000) + "万"
        else -> String.format("%,.2f", d.toDouble())
    }
}

private fun futureDate(months: Int): String {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.MONTH, months)
    return "${cal.get(java.util.Calendar.YEAR)}年${cal.get(java.util.Calendar.MONTH) + 1}月"
}