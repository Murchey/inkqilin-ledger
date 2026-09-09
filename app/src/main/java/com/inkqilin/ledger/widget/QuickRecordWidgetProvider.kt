package com.inkqilin.ledger.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.ColorStateList
import android.widget.RemoteViews
import com.inkqilin.ledger.R
import com.inkqilin.ledger.data.AppDatabase
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.util.DEFAULT_PRIMARY_COLOR_HEX
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.flow.first

/** 快捷记账小部件：高频分类按钮 → 直达"记一笔"并预填分类 */
class QuickRecordWidgetProvider : BaseLedgerWidgetProvider() {

    override suspend fun render(
        context: Context,
        theme: ThemeManager,
        manager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val db = AppDatabase.getDatabase(context)
        val cats = db.categoryDao().getCategoriesByTypeSync(TransactionType.EXPENSE)
        val quick = theme.widgetQuickCategories.first()
        // 最近使用的支出分类优先（近 90 天），不足时依次用设置勾选 / 全部分类补位
        val now = System.currentTimeMillis()
        val recent = db.transactionDao().getRecentExpenseCategoriesSync(now - 90L * 86_400_000L, 8)
        val recentCats = recent.map { it.category }.mapNotNull { n -> cats.firstOrNull { it.name == n } }
        val quickCats = quick.mapNotNull { q -> cats.firstOrNull { it.name == q } }
        val recentNames = recentCats.map { it.name }.toSet()
        val quickNames = quickCats.map { it.name }.toSet()
        val others = cats.filter { it.name !in recentNames && it.name !in quickNames }
        val slots = (recentCats + quickCats + others).distinctBy { it.name }.take(6)

        val dark = WidgetUtils.isDark(context)
        val textColor = WidgetUtils.textColor(dark)
        val subColor = WidgetUtils.subColor(dark)

        val (w, _) = widgetSize(manager, appWidgetId)
        val big = w >= 200
        val rv = RemoteViews(
            context.packageName,
            if (big) R.layout.widget_quick_record_big else R.layout.widget_quick_record_small
        )

        rv.setInt(
            R.id.quick_root, "setBackgroundResource",
            if (dark) R.drawable.widget_card_night else R.drawable.widget_card
        )

        val containers = if (big) {
            intArrayOf(R.id.q1, R.id.q2, R.id.q3, R.id.q4, R.id.q5, R.id.q6, R.id.q7, R.id.q8)
        } else {
            intArrayOf(R.id.q1, R.id.q2, R.id.q3, R.id.q4)
        }
        val icons = if (big) {
            intArrayOf(R.id.q1_icon, R.id.q2_icon, R.id.q3_icon, R.id.q4_icon, R.id.q5_icon, R.id.q6_icon, R.id.q7_icon, R.id.q8_icon)
        } else {
            intArrayOf(R.id.q1_icon, R.id.q2_icon, R.id.q3_icon, R.id.q4_icon)
        }
        val labels = if (big) {
            intArrayOf(R.id.q1_label, R.id.q2_label, R.id.q3_label, R.id.q4_label, R.id.q5_label, R.id.q6_label, R.id.q7_label, R.id.q8_label)
        } else {
            intArrayOf(R.id.q1_label, R.id.q2_label, R.id.q3_label, R.id.q4_label)
        }

        for (i in containers.indices) {
            val container = containers[i]
            val isLast = i == containers.size - 1
            // 底色交给布局的 tile drawable，不再用主题色 tint
            when {
                i < slots.size -> {
                    val cat = slots[i]
                    rv.setTextViewText(icons[i], cat.icon)
                    rv.setTextViewText(labels[i], cat.name)
                    rv.setTextColor(icons[i], textColor)
                    rv.setTextColor(labels[i], textColor)
                    rv.setOnClickPendingIntent(
                        container,
                        navPendingIntent(context, WidgetIntents.addWithCategory(cat.name))
                    )
                }
                // big 布局末位固定「设置」，其余补位「随意记」
                big && isLast -> {
                    rv.setTextViewText(icons[i], "⚙")
                    rv.setTextViewText(labels[i], "设置")
                    rv.setTextColor(icons[i], subColor)
                    rv.setTextColor(labels[i], subColor)
                    rv.setOnClickPendingIntent(container, navPendingIntent(context, WidgetIntents.TARGET_SETTINGS))
                }
                else -> {
                    rv.setTextViewText(icons[i], "＋")
                    rv.setTextViewText(labels[i], "随意记")
                    rv.setTextColor(icons[i], textColor)
                    rv.setTextColor(labels[i], textColor)
                    rv.setOnClickPendingIntent(container, navPendingIntent(context, WidgetIntents.TARGET_ADD))
                }
            }
        }

        manager.updateAppWidget(appWidgetId, rv)
    }
}