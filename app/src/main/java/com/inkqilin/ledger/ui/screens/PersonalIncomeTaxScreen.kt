package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.launch

/** 格式化为两位小数 */
fun Double.f2(): String = String.format("%.2f", this)

// ──── 默认税率配置（中国现行标准） ────
private data class TaxBracket(val limit: Double, val rate: Double, val quickDeduction: Double)

private data class TaxConfig(
    val threshold: Double = 5000.0,
    val brackets: List<TaxBracket> = defaultBrackets()
)

private fun defaultBrackets() = listOf(
    TaxBracket(36000.0, 0.03, 0.0),
    TaxBracket(144000.0, 0.10, 2520.0),
    TaxBracket(300000.0, 0.20, 16920.0),
    TaxBracket(420000.0, 0.25, 31920.0),
    TaxBracket(660000.0, 0.30, 52920.0),
    TaxBracket(960000.0, 0.35, 85920.0),
    TaxBracket(Double.MAX_VALUE, 0.45, 181920.0)
)

/** 根据各档限额和税率自动计算速算扣除数 */
private fun computeQuickDeductions(brackets: List<TaxBracket>): List<TaxBracket> {
    val result = mutableListOf<TaxBracket>()
    var prevQD = 0.0
    for (i in brackets.indices) {
        val qd = if (i == 0) 0.0
        else prevQD + (brackets[i].rate - brackets[i - 1].rate) * brackets[i - 1].limit
        result.add(TaxBracket(brackets[i].limit, brackets[i].rate, qd))
        prevQD = qd
    }
    return result
}

private fun TaxConfig.toJson(): String {
    val sb = StringBuilder()
    sb.append("{\"threshold\":$threshold,\"brackets\":[")
    brackets.forEachIndexed { i, b ->
        if (i > 0) sb.append(",")
        val limitStr = if (b.limit == Double.MAX_VALUE) "999999999" else b.limit.toString()
        sb.append("{\"limit\":$limitStr,\"rate\":${b.rate},\"qd\":${b.quickDeduction}}")
    }
    sb.append("]}")
    return sb.toString()
}

private fun parseTaxConfig(json: String): TaxConfig? {
    if (json.isBlank()) return null
    return try {
        val threshold = Regex("\"threshold\":([0-9.]+)").find(json)?.groupValues?.get(1)?.toDoubleOrNull() ?: 5000.0
        val bracketMatches = Regex("\"limit\":([0-9.]+),\"rate\":([0-9.]+),\"qd\":([0-9.]+)").findAll(json)
        val brackets = bracketMatches.map { m ->
            val limit = m.groupValues[1].toDouble()
            val rate = m.groupValues[2].toDouble()
            val qd = m.groupValues[3].toDouble()
            TaxBracket(limit, rate, qd)
        }.toList()
        if (brackets.isNotEmpty()) TaxConfig(threshold, brackets) else null
    } catch (_: Exception) { null }
}

@Composable
fun PersonalIncomeTaxScreen() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val scope = rememberCoroutineScope()

    // 从 DataStore 读取税率配置
    val savedConfigJson by themeManager.taxConfig.collectAsState(initial = "")
    val taxConfig = remember(savedConfigJson) { parseTaxConfig(savedConfigJson) ?: TaxConfig() }

    var incomeText by remember { mutableStateOf("10000") }
    var socialInsuranceText by remember { mutableStateOf("3000") }
    var specialDeductionText by remember { mutableStateOf("2000") }
    var monthlyIncome by remember { mutableFloatStateOf(10000f) }
    var socialInsurance by remember { mutableFloatStateOf(3000f) }
    var specialDeduction by remember { mutableFloatStateOf(2000f) }
    var showSettings by remember { mutableStateOf(false) }

    fun calcMonthlyTax(income: Double, si: Double, sd: Double): Triple<Double, Double, Double> {
        val monthlyTaxable = income - si - sd - taxConfig.threshold
        if (monthlyTaxable <= 0) return Triple(0.0, 0.0, income - si - sd)
        val annualTaxable = monthlyTaxable * 12
        var annualTax = 0.0
        for (b in taxConfig.brackets) {
            if (annualTaxable <= b.limit) {
                annualTax = annualTaxable * b.rate - b.quickDeduction; break
            }
        }
        val monthlyTax = (annualTax / 12).coerceAtLeast(0.0)
        return Triple(monthlyTax, monthlyTaxable, income - si - sd - monthlyTax)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ─── 标题行 + 设置按钮 ───
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("个人所得税", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "税率设置", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = incomeText, onValueChange = { v -> incomeText = v; monthlyIncome = v.toFloatOrNull()?.coerceAtLeast(0f) ?: monthlyIncome },
            label = { Text("税前月薪") }, leadingIcon = { Text("¥", style = MaterialTheme.typography.titleMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = socialInsuranceText, onValueChange = { v -> socialInsuranceText = v; socialInsurance = v.toFloatOrNull()?.coerceAtLeast(0f) ?: socialInsurance },
            label = { Text("五险一金（个人）") }, leadingIcon = { Text("\uD83D\uDEE1\uFE0F", style = MaterialTheme.typography.titleMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = specialDeductionText, onValueChange = { v -> specialDeductionText = v; specialDeduction = v.toFloatOrNull()?.coerceAtLeast(0f) ?: specialDeduction },
            label = { Text("专项附加扣除") }, leadingIcon = { Text("\uD83D\uDCCB", style = MaterialTheme.typography.titleMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val taxResult = calcMonthlyTax(monthlyIncome.toDouble(), socialInsurance.toDouble(), specialDeduction.toDouble())
                Text("个税计算结果", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("月应纳税所得额"); Text("¥${taxResult.second.f2()}")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("每月应缴税额"); Text("¥${taxResult.first.f2()}", color = Color(0xFFFF5722))
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("税后到手"); Text("¥${taxResult.third.f2()}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        }
    }

    // ─── 税率设置弹窗 ───
    if (showSettings) {
        TaxSettingsDialog(taxConfig) { newConfig ->
            showSettings = false
            if (newConfig != null) {
                scope.launch { themeManager.setTaxConfig(newConfig.toJson()) }
            }
        }
    }
}
// ──── 税率设置弹窗 ────
@Composable
private fun TaxSettingsDialog(current: TaxConfig, onDismiss: (TaxConfig?) -> Unit) {
    var thresholdText by remember { mutableStateOf(current.threshold.toInt().toString()) }
    // 每档：限额、税率%
    val editableBrackets = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            current.brackets.forEach { b ->
                val limitStr = if (b.limit == Double.MAX_VALUE) "" else b.limit.toInt().toString()
                add(Pair(limitStr, (b.rate * 100).toString()))
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss(null) },
        title = { Text("税率设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("起征点（月）", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = thresholdText, onValueChange = { thresholdText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("超额累进税率表（年应纳税所得额）", style = MaterialTheme.typography.labelMedium)

                editableBrackets.forEachIndexed { i, (limit, rate) ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("第${i + 1}档", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = limit, onValueChange = { v -> editableBrackets[i] = Pair(v, editableBrackets[i].second) },
                                    label = { Text("上限") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true, modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = rate, onValueChange = { v -> editableBrackets[i] = Pair(editableBrackets[i].first, v) },
                                    label = { Text("税率%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true, modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                TextButton(onClick = {
                    thresholdText = "5000"
                    editableBrackets.clear()
                    defaultBrackets().forEach { b ->
                        val limitStr = if (b.limit == Double.MAX_VALUE) "" else b.limit.toInt().toString()
                        editableBrackets.add(Pair(limitStr, (b.rate * 100).toString()))
                    }
                }) { Text("恢复默认值", fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val threshold = thresholdText.toDoubleOrNull() ?: 5000.0
                val rawBrackets = editableBrackets.mapNotNull { (limitStr, rateStr) ->
                    val limit = limitStr.toDoubleOrNull() ?: Double.MAX_VALUE
                    val rate = (rateStr.toDoubleOrNull() ?: 0.0) / 100.0
                    if (rate > 0) TaxBracket(limit, rate, 0.0) else null
                }.sortedBy { it.limit }
                // 自动计算速算扣除数
                val brackets = computeQuickDeductions(rawBrackets)
                val config = if (brackets.isEmpty()) TaxConfig() else TaxConfig(threshold, brackets)
                onDismiss(config)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(null) }) { Text("取消") }
        }
    )
}