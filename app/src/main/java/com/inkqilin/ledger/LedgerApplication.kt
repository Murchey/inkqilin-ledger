package com.inkqilin.ledger

import android.app.Application
import com.inkqilin.ledger.worker.CycleBillWorker
import com.inkqilin.ledger.widget.WidgetUpdater
import com.inkqilin.ledger.util.AutoBackupRunner
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 全局 Application：
 * - 提供静态刷新入口 [refreshWidgets]，供 ViewModel / Worker / 设置页统一调用
 * - 统一下沉全局任务调度（周期账单每日扫描）
 */
class LedgerApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        // 自动备份口令迁移必须在任何计划执行前完成；调度刷新允许失败后由前台再次补偿。
        applicationScope.launch {
            runCatching { ThemeManager(this@LedgerApplication).migrateAutoBackupSecrets() }
            runCatching { AutoBackupRunner.refreshAutoBackupWorkers(this@LedgerApplication) }
        }
    }
}
