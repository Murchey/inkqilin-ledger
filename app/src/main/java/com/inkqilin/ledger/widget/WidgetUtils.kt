package com.inkqilin.ledger.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 小部件通用工具：颜色 / 深浅色 / 金额格式化 / 周期进度 */
object WidgetUtils {

    const val DEFAULT_GREEN = 0xFF34C759.toInt()
    const val DEFAULT_ORANGE = 0xFFFF9500.toInt()
    const val OVERDUE_RED = 0xFFFF3B30.toInt()

    const val LIGHT_BG = 0xFFFFFFFF.toInt()
    const val LIGHT_TEXT = 0xFF1D1D1F.toInt()
    const val LIGHT_SUB = 0xFF6E6E73.toInt()
    const val DARK_BG = 0xFF1C1E24.toInt()
    const val DARK_TEXT = 0xFFF2F2F2.toInt()
    const val DARK_SUB = 0xFFB0B0B8.toInt()

    fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun textColor(dark: Boolean): Int = if (dark) DARK_TEXT else LIGHT_TEXT
    fun subColor(dark: Boolean): Int = if (dark) DARK_SUB else LIGHT_SUB
    fun bgColor(dark: Boolean): Int = if (dark) DARK_BG else LIGHT_BG

    fun parseColor(hex: String?, fallback: Int): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(fallback)

    /** 给 ARGB 色附加透明度（0-255），用于半透明按钮底 */
    fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha shl 24)

    /** 压缩金额：≥1亿→亿、≥1万→万、否则千分位 */
    fun compactAmount(value: Double): String {
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 100_000_000 -> String.format(Locale.CHINA, "%.2f亿", value / 100_000_000)
            abs >= 10_000 -> String.format(Locale.CHINA, "%.2f万", value / 10_000)
            else -> String.format(Locale.CHINA, "%,.2f", value)
        }
    }

    const val MASKED_AMOUNT = "¥ •••"

    /** 显示金额/隐藏金额 */
    fun amount(show: Boolean, value: Double): String =
        if (show) "¥" + compactAmount(value) else MASKED_AMOUNT

    /** 周期账单：距下次触发的文案 */
    fun dueText(nextTriggerDate: Long, now: Long, overdue: Boolean): String {
        if (overdue) return "逾期"
        val days = ((nextTriggerDate - now) / 86_400_000L).toInt() + 1
        return when {
            days <= 0 -> "今天"
            days == 1 -> "明天"
            days <= 7 -> "剩${days}天"
            else -> SimpleDateFormat("MM-dd", Locale.CHINA).format(Date(nextTriggerDate)) + "到期"
        }
    }

    /** 周期进度 0..1 */
    fun cycleProgress(now: Long, start: Long, end: Long): Float {
        if (end <= start) return 0f
        return ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
    }
}