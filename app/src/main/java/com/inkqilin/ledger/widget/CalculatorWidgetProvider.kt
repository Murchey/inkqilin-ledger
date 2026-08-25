package com.inkqilin.ledger.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.ColorStateList
import android.widget.RemoteViews
import com.inkqilin.ledger.R
import com.inkqilin.ledger.util.DEFAULT_PRIMARY_COLOR_HEX
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.flow.first

/** 多功能计算器入口小部件：一键直达各计算工具 */
class CalculatorWidgetProvider : BaseLedgerWidgetProvider() {

    private data class CalcEntry(val name: String, val icon: String, val type: String)

    private val entries = listOf(
        CalcEntry("定投", "📈", "dca"),
        CalcEntry("复利", "🔄", "compound_interest"),
        CalcEntry("个税", "🧾", "income_tax"),
        CalcEntry("储蓄", "🎯", "savings_goal"),
        CalcEntry("分期", "💳", "installment"),
        CalcEntry("原理", "📖", "math_docs")
    )

    override suspend fun render(
        context: Context,
        theme: ThemeManager,
        manager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val dark = WidgetUtils.isDark(context)
        val textColor = WidgetUtils.textColor(dark)
        val primary = WidgetUtils.parseColor(
            theme.customPrimaryColor.first() ?: DEFAULT_PRIMARY_COLOR_HEX,
            WidgetUtils.DEFAULT_GREEN
        )

        val (w, h) = widgetSize(manager, appWidgetId)
        val big = w >= 200 && h >= 200
        val rv = RemoteViews(
            context.packageName,
            if (big) R.layout.widget_calculator_big else R.layout.widget_calculator_small
        )

        rv.setInt(R.id.calc_root, "setBackgroundResource", R.drawable.widget_card)
        rv.setColorStateList(
            R.id.calc_root, "setBackgroundTintList",
            ColorStateList.valueOf(WidgetUtils.bgColor(dark))
        )
        rv.setTextViewText(R.id.calc_title, if (big) "多功能计算器" else "多功能计算")
        rv.setTextColor(R.id.calc_title, primary)

        val list = if (big) entries else entries.take(4)
        val containers = if (big) {
            intArrayOf(R.id.calc1, R.id.calc2, R.id.calc3, R.id.calc4, R.id.calc5, R.id.calc6)
        } else {
            intArrayOf(R.id.calc1, R.id.calc2, R.id.calc3, R.id.calc4)
        }
        val icons = if (big) {
            intArrayOf(
                R.id.calc1_icon, R.id.calc2_icon, R.id.calc3_icon,
                R.id.calc4_icon, R.id.calc5_icon, R.id.calc6_icon
            )
        } else {
            intArrayOf(R.id.calc1_icon, R.id.calc2_icon, R.id.calc3_icon, R.id.calc4_icon)
        }
        val labels = if (big) {
            intArrayOf(
                R.id.calc1_label, R.id.calc2_label, R.id.calc3_label,
                R.id.calc4_label, R.id.calc5_label, R.id.calc6_label
            )
        } else {
            intArrayOf(R.id.calc1_label, R.id.calc2_label, R.id.calc3_label, R.id.calc4_label)
        }
        val tint = WidgetUtils.withAlpha(primary, 0x24)

        for (i in list.indices) {
            val e = list[i]
            rv.setTextViewText(icons[i], e.icon)
            rv.setTextViewText(labels[i], e.name)
            rv.setTextColor(icons[i], textColor)
            rv.setTextColor(labels[i], textColor)
            rv.setColorStateList(containers[i], "setBackgroundTintList", ColorStateList.valueOf(tint))
            rv.setOnClickPendingIntent(containers[i], navPendingIntent(context, WidgetIntents.calculator(e.type)))
        }

        // 整卡点击 → 计算器主页
        rv.setOnClickPendingIntent(R.id.calc_root, navPendingIntent(context, WidgetIntents.TARGET_CALCULATOR_HUB))
        manager.updateAppWidget(appWidgetId, rv)
    }
}