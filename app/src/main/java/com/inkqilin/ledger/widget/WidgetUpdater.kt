package com.inkqilin.ledger.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** 统一刷新入口：记账写库 / 周期账单变更 / 业务触发时调用 */
object WidgetUpdater {

    val PROVIDERS: List<Class<out AppWidgetProvider>> = listOf(
        OverviewWidgetProvider::class.java,
        QuickRecordWidgetProvider::class.java,
        CalculatorWidgetProvider::class.java
    )

    /** 刷新所有已挂载的小部件（未挂载时零开销） */
    fun refreshAll(context: Context) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            val mounted = PROVIDERS.any { p ->
                manager.getAppWidgetIds(ComponentName(context, p)).isNotEmpty()
            }
            if (!mounted) return
            context.sendBroadcast(
                Intent(context, WidgetClickReceiver::class.java).apply {
                    action = WidgetIntents.ACTION_WIDGET_REFRESH
                }
            )
        }
    }
}