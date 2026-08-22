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
fun InstallmentScreen() {
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


fun solveAPR(P: BigDecimal, M: BigDecimal, n: Int): BigDecimal? {
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


fun aprFunc(
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

