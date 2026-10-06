package com.inkqilin.ledger.util

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

sealed class AutoBackupRunResult {
    data object Skipped : AutoBackupRunResult()
    data object Blocked : AutoBackupRunResult()
    data class Success(val name: String, val size: Long) : AutoBackupRunResult()
    data class RetryableFailure(val message: String) : AutoBackupRunResult()
    data class PermanentFailure(val message: String) : AutoBackupRunResult()
}

/**
 * 自动备份执行器：本地 / 云端计划独立。
 * - 打开 APP 时：前台回到可用状态后检查
 * - 每天 / 每周 / 每月：各自的 WorkManager 周期任务检查
 */
object AutoBackupRunner {
    private const val TAG = "AutoBackup"
    const val LOCAL_WORK_NAME = "auto_backup_local_periodic"
    const val CLOUD_WORK_NAME = "auto_backup_cloud_periodic"
    private const val LEGACY_WORK_NAME = "auto_backup_periodic"
    private const val WORK_INTERVAL_HOURS = 12L
    private val localRunMutex = Mutex()
    private val cloudRunMutex = Mutex()

    private suspend fun shouldRunNow(schedule: BackupSchedule, last: Long): Boolean =
        BackupScheduleRules.shouldRun(schedule, System.currentTimeMillis(), last)

    /** 执行本地自动备份；[onlyFrequency] 非空时仅当计划匹配该频次。 */
    suspend fun runLocalIfDue(
        context: Context,
        onlyFrequency: BackupFrequency? = null
    ): AutoBackupRunResult = localRunMutex.withLock {
        val tm = ThemeManager(context)
        if (!tm.privacyAccepted.first()) return@withLock AutoBackupRunResult.Blocked
        val schedule = tm.localBackupSchedule.first()
        if (schedule.frequency == BackupFrequency.OFF) return@withLock AutoBackupRunResult.Skipped
        if (onlyFrequency != null && schedule.frequency != onlyFrequency) {
            return@withLock AutoBackupRunResult.Skipped
        }
        val last = tm.localBackupLastRun.first()
        if (!shouldRunNow(schedule, last)) return@withLock AutoBackupRunResult.Skipped

        val attemptAt = System.currentTimeMillis()
        tm.markBackupAttempt(AutoBackupTarget.LOCAL, attemptAt)
        val password = schedule.autoPassword.takeIf { it.isNotBlank() }?.toCharArray()
        try {
            val file = CloudBackupManager.createLocalBackup(context, password, auto = true)
            if (!file.exists() || file.length() <= 0L) {
                error("本地自动备份文件为空")
            }
            val cleanupWarning = if (schedule.deletePreviousAuto) {
                CloudBackupManager.deletePreviousAutoLocalBackups(context, keepFile = file)
            } else {
                null
            }
            val successAt = System.currentTimeMillis()
            tm.markLocalBackupRun(successAt)
            tm.markBackupSuccess(
                target = AutoBackupTarget.LOCAL,
                fileName = file.name,
                size = file.length(),
                cleanupWarning = cleanupWarning,
                timeMs = successAt
            )
            Log.d(TAG, "Local auto backup ok: ${file.name}")
            AutoBackupRunResult.Success(file.name, file.length())
        } catch (t: Throwable) {
            val message = "本地自动备份失败：${t.message ?: t.javaClass.simpleName}"
            recordFailure(context, tm, AutoBackupTarget.LOCAL, message, t)
        } finally {
            password?.fill('\u0000')
        }
    }

    /** 执行云端自动备份；[onlyFrequency] 非空时仅当计划匹配该频次。 */
    suspend fun runCloudIfDue(
        context: Context,
        onlyFrequency: BackupFrequency? = null
    ): AutoBackupRunResult = cloudRunMutex.withLock {
        val tm = ThemeManager(context)
        if (!tm.privacyAccepted.first()) return@withLock AutoBackupRunResult.Blocked
        val schedule = tm.cloudBackupSchedule.first()
        if (schedule.frequency == BackupFrequency.OFF) return@withLock AutoBackupRunResult.Skipped
        if (onlyFrequency != null && schedule.frequency != onlyFrequency) {
            return@withLock AutoBackupRunResult.Skipped
        }
        val last = tm.cloudBackupLastRun.first()
        if (!shouldRunNow(schedule, last)) return@withLock AutoBackupRunResult.Skipped

        val config = tm.cosConfig.first()
        if (!config.isConfigured) {
            val message = "云端自动备份需要先配置腾讯云 COS"
            tm.markBackupFailure(AutoBackupTarget.CLOUD, message)
            Log.w(TAG, message)
            return@withLock AutoBackupRunResult.Blocked
        }

        val attemptAt = System.currentTimeMillis()
        tm.markBackupAttempt(AutoBackupTarget.CLOUD, attemptAt)
        val password = schedule.autoPassword.takeIf { it.isNotBlank() }?.toCharArray()
        try {
            val uploaded = CloudBackupManager.uploadBackup(context, config, password, auto = true)
            val cleanupWarning = if (schedule.deletePreviousAuto) {
                CloudBackupManager.deletePreviousAutoCloudBackups(config, keepKey = uploaded.key)
            } else {
                null
            }
            val successAt = System.currentTimeMillis()
            tm.markCloudBackupRun(successAt)
            tm.markBackupSuccess(
                target = AutoBackupTarget.CLOUD,
                fileName = uploaded.key.substringAfterLast('/'),
                size = uploaded.size,
                cleanupWarning = cleanupWarning,
                timeMs = successAt
            )
            Log.d(TAG, "Cloud auto backup ok: ${uploaded.key}")
            AutoBackupRunResult.Success(uploaded.key.substringAfterLast('/'), uploaded.size)
        } catch (t: Throwable) {
            val message = "云端自动备份失败：${t.message ?: t.javaClass.simpleName}"
            recordFailure(context, tm, AutoBackupTarget.CLOUD, message, t)
        } finally {
            password?.fill('\u0000')
        }
    }

    /** 应用进程回到前台时触发；每个目标按自然日去重。 */
    suspend fun runAppOpenBackups(context: Context) {
        runLocalIfDue(context, BackupFrequency.ON_APP_OPEN)
        runCloudIfDue(context, BackupFrequency.ON_APP_OPEN)
    }

    /** 定时任务：每日 / 每周 / 每月。 */
    suspend fun runDueScheduledBackups(context: Context, target: AutoBackupTarget): AutoBackupRunResult {
        val frequencies = listOf(
            BackupFrequency.DAILY,
            BackupFrequency.WEEKLY,
            BackupFrequency.MONTHLY
        )
        var result: AutoBackupRunResult = AutoBackupRunResult.Skipped
        frequencies.forEach { frequency ->
            val current = when (target) {
                AutoBackupTarget.LOCAL -> runLocalIfDue(context, frequency)
                AutoBackupTarget.CLOUD -> runCloudIfDue(context, frequency)
            }
            if (current is AutoBackupRunResult.RetryableFailure ||
                current is AutoBackupRunResult.PermanentFailure ||
                current is AutoBackupRunResult.Success
            ) {
                result = current
            }
        }
        return result
    }

    /** 根据两端计划状态维护独立的周期 Worker。 */
    suspend fun refreshAutoBackupWorkers(context: Context) {
        val tm = ThemeManager(context)
        val local = tm.localBackupSchedule.first()
        val cloud = tm.cloudBackupSchedule.first()
        val cos = tm.cosConfig.first()
        val workManager = WorkManager.getInstance(context)

        // 旧版本的共享任务不能继续运行，避免和新 Worker 重复执行。
        workManager.cancelUniqueWork(LEGACY_WORK_NAME)

        if (local.frequency == BackupFrequency.OFF) {
            workManager.cancelUniqueWork(LOCAL_WORK_NAME)
        } else {
            workManager.enqueueUniquePeriodicWork(
                LOCAL_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                localWorkRequest()
            )
        }

        if (cloud.frequency == BackupFrequency.OFF || !cos.isConfigured) {
            workManager.cancelUniqueWork(CLOUD_WORK_NAME)
        } else {
            workManager.enqueueUniquePeriodicWork(
                CLOUD_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                cloudWorkRequest()
            )
        }
    }

    private fun localWorkRequest() = PeriodicWorkRequestBuilder<LocalAutoBackupWorker>(
        WORK_INTERVAL_HOURS,
        TimeUnit.HOURS
    )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
        .build()

    private fun cloudWorkRequest() = PeriodicWorkRequestBuilder<CloudAutoBackupWorker>(
        WORK_INTERVAL_HOURS,
        TimeUnit.HOURS
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
        .build()

    private suspend fun recordFailure(
        context: Context,
        tm: ThemeManager,
        target: AutoBackupTarget,
        message: String,
        throwable: Throwable
    ): AutoBackupRunResult {
        tm.markBackupFailure(target, message)
        Log.w(TAG, message, throwable)
        // 通知只是辅助反馈；权限或系统通知服务异常不能改变备份结果分类。
        runCatching { NotificationHelper.showAutoBackupFailure(context, target, message) }
        if (isRetryable(throwable, message)) {
            return AutoBackupRunResult.RetryableFailure(message)
        }
        return AutoBackupRunResult.PermanentFailure(message)
    }

    private fun isRetryable(throwable: Throwable, message: String): Boolean {
        if (throwable is IOException || throwable is SocketTimeoutException) return true
        return message.contains("HTTP 5") ||
            message.contains("数据库正忙") ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("超时")
    }
}

class LocalAutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (
        AutoBackupRunner.runDueScheduledBackups(applicationContext, AutoBackupTarget.LOCAL)
    ) {
        is AutoBackupRunResult.RetryableFailure -> Result.retry()
        else -> Result.success()
    }
}

class CloudAutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (
        AutoBackupRunner.runDueScheduledBackups(applicationContext, AutoBackupTarget.CLOUD)
    ) {
        is AutoBackupRunResult.RetryableFailure -> Result.retry()
        else -> Result.success()
    }
}
