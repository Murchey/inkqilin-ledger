package com.inkqilin.ledger

import android.app.Application
import com.inkqilin.ledger.worker.CycleBillWorker
import com.inkqilin.ledger.widget.WidgetUpdater

/**
 * 全局 Application：
 * - 提供静态刷新入口 [refreshWidgets]，供 ViewModel / Worker / 设置页统一调用
 * - 统一下沉全局任务调度（周期账单每日扫描）
 */
class LedgerApplication : Application() {

    companion object {
        @Volatile
        lateinit var instance: LedgerApplication
            private set

        /** 刷新所有已挂载的桌面小部件（未挂载时零开销） */
        fun refreshWidgets() {
            if (!::instance.isInitialized) return
            runCatching { WidgetUpdater.refreshAll(instance) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 周期账单每日扫描（周期 Worker 幂等，重复调度无害）
        CycleBillWorker.scheduleDailyScan(this)
    }
}