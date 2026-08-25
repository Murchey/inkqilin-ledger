package com.inkqilin.ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.inkqilin.ledger.MainActivity
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 小部件基类：
 * - onUpdate 内用 goAsync + IO 协程安全读取 Room / DataStore
 * - 尺寸变化（resize）时自动重绘
 */
abstract class BaseLedgerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val result = goAsync()
        val appContext = context.applicationContext
        val manager = appWidgetManager
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderWidgets(appContext, manager, appWidgetIds)
            } finally {
                // 极端情况下 goAsync 可能返回 null，做空保护
                runCatching { result.finish() }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    /**
     * 业务侧（记账 / 设置变更 / 手动刷新）触发：
     * 不依赖系统 BroadcastReceiver 生命周期，可直接刷新已挂载实例。
     */
    fun refresh(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        val manager = appWidgetManager
        CoroutineScope(Dispatchers.IO).launch {
            renderWidgets(appContext, manager, appWidgetIds)
        }
    }

    private suspend fun renderWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val theme = ThemeManager(context)
        appWidgetIds.forEach { id ->
            runCatching { render(context, theme, appWidgetManager, id) }
        }
    }

    protected abstract suspend fun render(
        context: Context,
        theme: ThemeManager,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    )

    /** 获取小部件当前可用空间（dp 单位） */
    protected fun widgetSize(appWidgetManager: AppWidgetManager, id: Int): Pair<Int, Int> {
        val options = appWidgetManager.getAppWidgetOptions(id)
        val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        return w to h
    }

    /** 统一导航 PendingIntent（requestCode 用 target hash，避免溢出） */
    protected fun navPendingIntent(context: Context, target: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = WidgetIntents.ACTION_WIDGET_NAV
            putExtra(WidgetIntents.EXTRA_NAV_TARGET, target)
        }
        return PendingIntent.getActivity(
            context,
            target.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}