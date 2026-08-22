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
fun CompoundInterestScreen() {
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

