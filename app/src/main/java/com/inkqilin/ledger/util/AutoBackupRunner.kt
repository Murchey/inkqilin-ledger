package com.inkqilin.ledger.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 自动备份执行器：本地 / 云端计划独立。
 * - 打开 APP 时：[runAppOpenBackups]
 * - 每天 / 每周 / 每月：[AutoBackupWorker] 周期检查
 */
object AutoBackupRunner {
    private const val TAG = "AutoBackup"

    private suspend fun shouldRunNow(schedule: BackupSchedule, last: Long): Boolean =
        BackupScheduleRules.shouldRun(schedule, System.currentTimeMillis(), last)

    /** 执行本地自动备份；[onlyFrequency] 非空时仅当计划匹配该频次 */
    suspend fun runLocalIfDue(context: Context, onlyFrequency: BackupFrequency? = null) {
        val tm = ThemeManager(context)
        val schedule = tm.localBackupSchedule.first()
        if (schedule.frequency == BackupFrequency.OFF) return
        if (onlyFrequency != null && schedule.frequency != onlyFrequency) return
        val last = tm.localBackupLastRun.first()
        if (!shouldRunNow(schedule, last)) return
        try {
            if (schedule.deletePreviousAuto) {
                CloudBackupManager.deletePreviousAutoLocalBackups(context)
            }
            val pw = schedule.autoPassword.takeIf { it.isNotBlank() }?.toCharArray()
            val file = CloudBackupManager.createLocalBackup(context, pw, auto = true)
            tm.markLocalBackupRun()
            Log.d(TAG, "Local auto backup ok: ${file.name}")
        } catch (t: Throwable) {
            Log.w(TAG, "Local auto backup failed", t)
            runCatching {
                tm.setAutoBackupError(
                    "本地自动备份失败：${t.message ?: t.javaClass.simpleName}"
                )
            }
        }
    }

    /** 执行云端自动备份；[onlyFrequency] 非空时仅当计划匹配该频次 */
    suspend fun runCloudIfDue(context: Context, onlyFrequency: BackupFrequency? = null) {
        val tm = ThemeManager(context)
        val schedule = tm.cloudBackupSchedule.first()
        if (schedule.frequency == BackupFrequency.OFF) return
        if (onlyFrequency != null && schedule.frequency != onlyFrequency) return
        val last = tm.cloudBackupLastRun.first()
        if (!shouldRunNow(schedule, last)) return
        val config = tm.cosConfig.first()
        if (!config.isConfigured) {
            Log.w(TAG, "Cloud auto backup skipped: COS not configured")
            runCatching {
                tm.setAutoBackupError("云端自动备份失败：未配置腾讯云 COS，请到数据备份中配置")
            }
            return
        }
        try {
            if (schedule.deletePreviousAuto) {
                CloudBackupManager.deletePreviousAutoCloudBackups(config)
            }
            val pw = schedule.autoPassword.takeIf { it.isNotBlank() }?.toCharArray()
            CloudBackupManager.uploadBackup(context, config, pw, auto = true)
            tm.markCloudBackupRun()
            Log.d(TAG, "Cloud auto backup ok")
        } catch (t: Throwable) {
            Log.w(TAG, "Cloud auto backup failed", t)
            runCatching {
                tm.setAutoBackupError(
                    "云端自动备份失败：${t.message ?: t.javaClass.simpleName}"
                )
            }
        }
    }

    /** 打开 APP 时触发 */
    suspend fun runAppOpenBackups(context: Context) {
        runLocalIfDue(context, BackupFrequency.ON_APP_OPEN)
        runCloudIfDue(context, BackupFrequency.ON_APP_OPEN)
    }

    /** 定时任务：每日 / 每周 / 每月 */
    suspend fun runDueScheduledBackups(context: Context) {
        listOf(BackupFrequency.DAILY, BackupFrequency.WEEKLY, BackupFrequency.MONTHLY).forEach { freq ->
            runLocalIfDue(context, freq)
            runCloudIfDue(context, freq)
        }
    }

    fun scheduleAutoBackupWorker(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "auto_backup_periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<AutoBackupWorker>(12, TimeUnit.HOURS).build()
        )
    }
}

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            AutoBackupRunner.runDueScheduledBackups(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            Log.w("AutoBackup", "worker failed", t)
            Result.retry()
        }
    }
}
