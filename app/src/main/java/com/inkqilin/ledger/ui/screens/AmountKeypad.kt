package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.view.HapticFeedbackConstants
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

/** 记账金额表达式计算器，仅支持安全的四则运算和括号。 */
object AmountExpressionEvaluator {
    fun evaluate(expression: String): Double? {
        val normalized = normalizeExpression(expression)
        if (normalized.isBlank()) return null
        return runCatching {
            Parser(normalized).parse().also {
                if (!it.isFinite()) error("non-finite")
            }
        }.getOrNull()
    }

    /** 键盘/展示用符号 → 解析器可识别的 ASCII */
    fun normalizeExpression(expression: String): String {
        return expression
            .replace('×', '*')
            .replace('✕', '*')
            .replace('x', '*')
            .replace('X', '*')
            .replace('＊', '*')
            .replace('÷', '/')
            .replace('／', '/')
            .replace('−', '-') // U+2212
            .replace('–', '-') // en dash
            .replace('—', '-') // em dash
            .replace('，', '.')
            .replace('。', '.')
    }

    /** 千分位格式化，保留两位小数；非法输入返回原文。 */
    fun formatDisplay(amount: Double): String {
        if (!amount.isFinite()) return amount.toString()
        val rounded = kotlin.math.round(amount * 100.0) / 100.0
        val negative = rounded < 0
        val absStr = String.format(Locale.US, "%.2f", abs(rounded))
        val dot = absStr.indexOf('.')
        val intPart = absStr.substring(0, dot)
        val fracPart = absStr.substring(dot)
        val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
        return (if (negative) "-" else "") + grouped + fracPart
    }

    /** 入库/回填用：尽量短的数字串（去掉多余 .0） */
    fun formatForField(amount: Double): String {
        if (!amount.isFinite()) return "0"
        val rounded = kotlin.math.round(amount * 100.0) / 100.0
        return if (rounded == kotlin.math.floor(rounded) && abs(rounded) < 1e15) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    /**
     * 智能追加：连续运算符则替换、小数点防重复、空表达式拒绝裸乘除。
     */
    fun appendToken(current: String, token: String): String {
        if (token.isEmpty()) return current
        val normalizedToken = when (token) {
            "×", "✕", "x", "X", "＊" -> "*"
            "÷", "／" -> "/"
            "−", "–", "—" -> "-"
            "。", "，" -> "."
            else -> token
        }

        // 括号与数字直接拼
        if (normalizedToken == "(" || normalizedToken == ")") {
            return current + normalizedToken
        }

        if (normalizedToken == ".") {
            val lastNumber = current.takeLastWhile { it.isDigit() || it == '.' }
            if ('.' in lastNumber) return current
            if (lastNumber.isEmpty()) return current + "0."
            return current + "."
        }

        if (normalizedToken.length == 1 && normalizedToken[0] in charArrayOf('+', '-', '*', '/')) {
            if (current.isBlank()) {
                // 允许开头一元正负；不允许以 * / 开头
                return if (normalizedToken == "+" || normalizedToken == "-") normalizedToken else current
            }
            val last = current.last()
            // 连续运算符：替换最后一个（保留一元负号场景：`(-` 后可跟数字）
            if (last in charArrayOf('+', '-', '*', '/', '×', '÷', '−')) {
                // `(` 后允许一元 +/-
                if (last == '(' && (normalizedToken == "+" || normalizedToken == "-")) {
                    return current + normalizedToken
                }
                return current.dropLast(1) + normalizedToken
            }
            if (last == '.') {
                // `1.` 后接运算符 → 补成 `1.0+`
                return current + "0" + normalizedToken
            }
            return current + normalizedToken
        }

        // 数字
        return current + normalizedToken
    }

    private class Parser(private val source: String) {
        private var position = 0

        fun parse(): Double {
            val result = parseExpression()
            skipSpaces()
            if (position != source.length) error("unexpected token")
            return result
        }

        private fun parseExpression(): Double {
            var result = parseTerm()
            while (true) {
                skipSpaces()
                result = when {
                    consume('+') -> result + parseTerm()
                    consume('-') -> result - parseTerm()
                    else -> return result
                }
            }
        }

        private fun parseTerm(): Double {
            var result = parseFactor()
            while (true) {
                skipSpaces()
                result = when {
                    consume('*') -> result * parseFactor()
                    consume('/') -> result / parseFactor()
                    else -> return result
                }
            }
        }

        private fun parseFactor(): Double {
            skipSpaces()
            if (consume('+')) return parseFactor()
            if (consume('-')) return -parseFactor()
            if (consume('(')) {
                val result = parseExpression()
                if (!consume(')')) error("missing parenthesis")
                return result
            }
            val start = position
            while (position < source.length && (source[position].isDigit() || source[position] == '.')) position++
            if (start == position) error("number expected")
            return source.substring(start, position).toDouble()
        }

        private fun consume(character: Char): Boolean {
            if (position < source.length && source[position] == character) {
                position++
                return true
            }
            return false
        }

        private fun skipSpaces() {
            while (position < source.length && source[position].isWhitespace()) position++
        }
    }
}

private enum class KeyKind { DIGIT, OPERATOR, ACTION, CONFIRM }

private data class KeypadKey(
    val label: String,
    val kind: KeyKind,
    val description: String,
    val span: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    onEvaluate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(visible) {
        if (!visible) {
            delay(180)
            onDismiss()
        }
    }
    LaunchedEffect(value) {
        if (showError) showError = false
    }

    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun tryEvaluate(): Double? {
        val result = AmountExpressionEvaluator.evaluate(value) ?: return null
        return if (result >= 0) result else null
    }

    fun commitAndClose() {
        tap()
        if (value.isBlank()) {
            visible = false
            return
        }
        if (tryEvaluate() != null) {
            onEvaluate()
            visible = false
        } else {
            showError = true
        }
    }

    fun softDismiss() {
        tap()
        // 点遮罩/收起：能算就算，算不出也直接关，避免卡在脏输入上
        if (value.isNotBlank() && tryEvaluate() != null) {
            onEvaluate()
        }
        visible = false
    }

    Dialog(
        onDismissRequest = { softDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { softDismiss() }
                    )
            )
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(tween(180)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    AmountKeypadHeader(
                        expression = value,
                        onDismiss = { softDismiss() }
                    )
                    if (showError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "金额无效，请检查表达式",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    AmountKeypadContent(
                        value = value,
                        onValueChange = {
                            tap()
                            onValueChange(it)
                        },
                        onConfirm = { commitAndClose() },
                        modifier = modifier
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun AmountKeypadHeader(
    expression: String,
    onDismiss: () -> Unit
) {
    val preview = AmountExpressionEvaluator.evaluate(expression)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expression.ifBlank { "输入金额" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (preview != null && preview >= 0) {
                    "¥${AmountExpressionEvaluator.formatDisplay(preview)}"
                } else {
                    "—"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (preview != null && preview >= 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "收起",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AmountKeypadContent(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf(KeypadKey("7", KeyKind.DIGIT, "7"), KeypadKey("8", KeyKind.DIGIT, "8"), KeypadKey("9", KeyKind.DIGIT, "9"), KeypadKey("÷", KeyKind.OPERATOR, "除")),
        listOf(KeypadKey("4", KeyKind.DIGIT, "4"), KeypadKey("5", KeyKind.DIGIT, "5"), KeypadKey("6", KeyKind.DIGIT, "6"), KeypadKey("×", KeyKind.OPERATOR, "乘")),
        listOf(KeypadKey("1", KeyKind.DIGIT, "1"), KeypadKey("2", KeyKind.DIGIT, "2"), KeypadKey("3", KeyKind.DIGIT, "3"), KeypadKey("−", KeyKind.OPERATOR, "减")),
        listOf(KeypadKey("0", KeyKind.DIGIT, "0"), KeypadKey(".", KeyKind.DIGIT, "小数点"), KeypadKey("(", KeyKind.OPERATOR, "左括号"), KeypadKey(")", KeyKind.OPERATOR, "右括号")),
        listOf(
            KeypadKey("AC", KeyKind.ACTION, "清除", span = 1),
            KeypadKey("⌫", KeyKind.ACTION, "退格", span = 1),
            KeypadKey("+", KeyKind.OPERATOR, "加", span = 1),
            KeypadKey("完成", KeyKind.CONFIRM, "完成", span = 1)
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        value = value,
                        onValueChange = onValueChange,
                        onConfirm = onConfirm,
                        modifier = Modifier.weight(key.span.toFloat())
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: KeypadKey,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (key.kind) {
        KeyKind.DIGIT -> MaterialTheme.colorScheme.surfaceVariant
        KeyKind.OPERATOR -> MaterialTheme.colorScheme.primaryContainer
        KeyKind.ACTION -> MaterialTheme.colorScheme.surfaceVariant
        KeyKind.CONFIRM -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when (key.kind) {
        KeyKind.DIGIT -> MaterialTheme.colorScheme.onSurface
        KeyKind.OPERATOR -> MaterialTheme.colorScheme.onPrimaryContainer
        KeyKind.ACTION -> MaterialTheme.colorScheme.onSurfaceVariant
        KeyKind.CONFIRM -> MaterialTheme.colorScheme.onPrimary
    }

    Button(
        onClick = {
            when (key.kind) {
                KeyKind.CONFIRM -> onConfirm()
                KeyKind.ACTION -> when (key.label) {
                    "AC" -> onValueChange("")
                    "⌫" -> onValueChange(value.dropLast(1))
                }
                KeyKind.OPERATOR, KeyKind.DIGIT ->
                    onValueChange(AmountExpressionEvaluator.appendToken(value, key.label))
            }
        },
        modifier = modifier
            .height(52.dp)
            .semantics { contentDescription = key.description },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        elevation = null
    ) {
        Text(
            text = key.label,
            fontSize = when (key.kind) {
                KeyKind.DIGIT -> 20.sp
                KeyKind.OPERATOR -> 22.sp
                KeyKind.ACTION -> 14.sp
                KeyKind.CONFIRM -> 16.sp
            },
            fontWeight = when (key.kind) {
                KeyKind.DIGIT, KeyKind.CONFIRM -> FontWeight.SemiBold
                KeyKind.OPERATOR -> FontWeight.Bold
                KeyKind.ACTION -> FontWeight.Medium
            },
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
