package com.inkqilin.ledger.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * 统一响应小部件刷新广播（由 WidgetUpdater 发出），
 * 并把业务侧触发转发给各 Provider 的 onUpdate。
 */
class WidgetClickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetIntents.ACTION_WIDGET_REFRESH) return
        val manager = AppWidgetManager.getInstance(context)
        val appContext = context.applicationContext
        WidgetUpdater.PROVIDERS.forEach { providerCls ->
            val ids = manager.getAppWidgetIds(ComponentName(context, providerCls))
            if (ids.isNotEmpty()) {
                runCatching {
                    val provider = providerCls.getDeclaredConstructor().newInstance()
                    provider.onUpdate(appContext, manager, ids)
                }
            }
        }
    }
}