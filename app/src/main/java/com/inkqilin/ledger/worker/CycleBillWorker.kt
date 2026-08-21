package com.inkqilin.ledger.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ListenableWorker
import com.inkqilin.ledger.util.CycleBillBroadcastReceiver
import com.inkqilin.ledger.util.NotificationHelper
import java.util.concurrent.TimeUnit

class CycleBillWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CycleBillWorker"
        const val WORKER_TAG_SCAN = "cycle_scan_daily"

        fun scheduleDailyScan(applicationContext: Context) {
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                WORKER_TAG_SCAN,
                ExistingPeriodicWorkPolicy.REPLACE,
                PeriodicWorkRequestBuilder<CycleBillWorker>(
                    6, TimeUnit.HOURS  // every 6 hours as safety net
                ).build()
            )
            Log.d(TAG, "Scheduled daily scan worker")
        }

        fun cancelAlarm(applicationContext: Context, billId: Long) {
            val alarmMgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(applicationContext, CycleBillBroadcastReceiver::class.java).apply {
                putExtra("cycleBillId", billId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                REQUEST_CODE_CYCLE_BILL_ALARM + billId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmMgr.cancel(it)
                it.cancel()
                Log.d(TAG, "Cancelled alarm for bill id=$billId")
            }
        }

        const val REQUEST_CODE_CYCLE_BILL_ALARM = 1000
    }

    override suspend fun doWork(): ListenableWorker.Result {
        return try {
            Log.d(TAG, "CycleBillWorker running: periodic check")
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in CycleBillWorker", e)
            ListenableWorker.Result.retry()
        }
    }
}
