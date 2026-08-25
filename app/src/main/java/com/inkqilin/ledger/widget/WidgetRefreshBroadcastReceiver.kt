package com.inkqilin.ledger.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 处理 开机 / 时间 / 时区 / 日期 变更：
 * 保证跨日跨月后"本月/近7日"统计区间重新计算，重启后补齐刷新。
 */
class WidgetRefreshBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdater.refreshAll(context.applicationContext)
    }
}