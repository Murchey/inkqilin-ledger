package com.inkqilin.ledger.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.RemoteViews
import com.inkqilin.ledger.R
import com.inkqilin.ledger.data.AppDatabase
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.util.DEFAULT_PRIMARY_COLOR_HEX
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.flow.first
import java.util.Calendar

/** 记账概览小部件：本月收支结余 / 预算进度 / 近7日Top分类 / 周期账单待办 */
class OverviewWidgetProvider : BaseLedgerWidgetProvider() {

    override suspend fun render(
        context: Context,
        theme: ThemeManager,
        manager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()

        val (monthStart, monthEnd) = currentMonthRange()
        val income = db.transactionDao().getIncomeSumByRangeSync(monthStart, monthEnd)
        val expense = db.transactionDao().getExpenseSumByRangeSync(monthStart, monthEnd)
        val budget = theme.monthlyBudget.first()
        val showAmount = theme.widgetShowAmount.first()
        val top = db.transactionDao().getTopExpenseCategoriesSync(now - 7L * 86_400_000L, now, 3)
        val catIcons = db.categoryDao().getCategoriesByTypeSync(TransactionType.EXPENSE)
            .associate { it.name to it.icon }
        val bills = db.cycleBillDao().getWidgetBillsSync(50)
        val overdueCount = bills.count { it.nextTriggerDate < now }
        val dueSoonCount = bills.count { it.nextTriggerDate >= now && it.nextTriggerDate <= now + 7L * 86_400_000L }

        val dark = WidgetUtils.isDark(context)
        val textColor = WidgetUtils.textColor(dark)
        val subColor = WidgetUtils.subColor(dark)
        val incomeColor = WidgetUtils.parseColor(theme.incomeColor.first(), WidgetUtils.DEFAULT_GREEN)
        val expenseColor = WidgetUtils.parseColor(theme.expenseColor.first(), WidgetUtils.DEFAULT_ORANGE)
        val primary = WidgetUtils.parseColor(
            theme.customPrimaryColor.first() ?: DEFAULT_PRIMARY_COLOR_HEX,
            WidgetUtils.DEFAULT_GREEN
        )

        val (w, h) = widgetSize(manager, appWidgetId)
        val big = w >= 200 && h >= 200
        val rv = RemoteViews(
            context.packageName,
            if (big) R.layout.widget_overview_big else R.layout.widget_overview_small
        )

        // 公共部分
        rv.setInt(R.id.overview_root, "setBackgroundResource", R.drawable.widget_card)
        rv.setColorStateList(
            R.id.overview_root, "setBackgroundTintList",
            ColorStateList.valueOf(WidgetUtils.bgColor(dark))
        )
        rv.setTextViewText(R.id.ov_title, "墨麒麟记账")
        rv.setTextColor(R.id.ov_title, textColor)
        rv.setTextViewText(
            R.id.ov_month,
            if (big) "$nowYearMonth" else "${Calendar.getInstance().get(Calendar.MONTH) + 1}月"
        )
        rv.setTextColor(R.id.ov_month, subColor)
        rv.setTextViewText(R.id.ov_income, "收 " + WidgetUtils.amount(showAmount, income))
        rv.setTextColor(R.id.ov_income, incomeColor)
        rv.setTextViewText(R.id.ov_expense, "支 " + WidgetUtils.amount(showAmount, expense))
        rv.setTextColor(R.id.ov_expense, expenseColor)
        rv.setTextViewText(R.id.ov_balance, "余 " + WidgetUtils.amount(showAmount, income - expense))
        rv.setTextColor(R.id.ov_balance, textColor)

        rv.setTextViewText(R.id.ov_add_btn, "记一笔")
        rv.setTextColor(R.id.ov_add_btn, 0xFFFFFFFF.toInt())
        rv.setColorStateList(
            R.id.ov_add_btn, "setBackgroundTintList",
            ColorStateList.valueOf(primary)
        )
        rv.setOnClickPendingIntent(R.id.ov_add_btn, navPendingIntent(context, WidgetIntents.TARGET_ADD))

        val pending = overdueCount + dueSoonCount
        rv.setTextViewText(R.id.ov_cycle_btn, "🔁 周期账单" + if (pending > 0) " · $pending" else "")
        rv.setTextColor(R.id.ov_cycle_btn, primary)
        rv.setOnClickPendingIntent(R.id.ov_cycle_btn, navPendingIntent(context, WidgetIntents.TARGET_CYCLE_LIST))

        // 卡片整体点击 → 首页
        rv.setOnClickPendingIntent(R.id.overview_root, navPendingIntent(context, WidgetIntents.TARGET_HOME))

        if (big) {
            // 预算
            val pct = budgetPct(expense, budget)
            rv.setTextViewText(R.id.ov_budget_pct, if (budget <= 0) "未设预算" else "$pct%")
            rv.setTextColor(R.id.ov_budget_pct, textColor)
            rv.setProgressBar(R.id.ov_budget_progress, 100, pct, false)
            rv.setColorStateList(
                R.id.ov_budget_progress, "setProgressTintList",
                ColorStateList.valueOf(if (pct >= 100) WidgetUtils.OVERDUE_RED else primary)
            )
            rv.setInt(R.id.ov_divider, "setBackgroundColor", subColor)

            // 近 7 日 Top3
            rv.setTextViewText(R.id.ov_top_label, "近 7 日支出")
            rv.setTextColor(R.id.ov_top_label, subColor)
            val iconIds = intArrayOf(R.id.ov_top1_icon, R.id.ov_top2_icon, R.id.ov_top3_icon)
            val nameIds = intArrayOf(R.id.ov_top1_name, R.id.ov_top2_name, R.id.ov_top3_name)
            val amountIds = intArrayOf(R.id.ov_top1_amount, R.id.ov_top2_amount, R.id.ov_top3_amount)
            for (i in 0 until 3) {
                if (i < top.size) {
                    val row = top[i]
                    rv.setTextViewText(iconIds[i], catIcons[row.category] ?: "•")
                    rv.setTextViewText(nameIds[i], row.category)
                    rv.setTextColor(nameIds[i], textColor)
                    rv.setTextViewText(amountIds[i], WidgetUtils.amount(showAmount, row.total))
                    rv.setTextColor(amountIds[i], subColor)
                } else {
                    rv.setViewVisibility(iconIds[i], View.GONE)
                    rv.setViewVisibility(nameIds[i], View.GONE)
                    rv.setViewVisibility(amountIds[i], View.GONE)
                }
            }
        }

        manager.updateAppWidget(appWidgetId, rv)
    }

    private val nowYearMonth: String
        get() {
            val c = Calendar.getInstance()
            return "${c.get(Calendar.YEAR)}年${c.get(Calendar.MONTH) + 1}月"
        }

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { set(Calendar.MILLISECOND, 0) }
        val start = Calendar.getInstance().apply {
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis - 1
        return start to end
    }

    private fun budgetPct(expense: Double, budget: Double): Int {
        if (budget <= 0) return 0
        return ((expense / budget) * 100).toInt().coerceIn(0, 100)
    }
}