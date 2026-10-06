package com.inkqilin.ledger.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object NotificationHelper {
    const val CHANNEL_ID = "cycle_bill_reminder"
    const val CHANNEL_NAME = "周期账单提醒"
    const val NOTIFICATION_ID_PREFIX = "cycle_"
    const val AUTO_BACKUP_CHANNEL_ID = "auto_backup_status"
    const val AUTO_BACKUP_CHANNEL_NAME = "自动备份状态"
    private const val AUTO_BACKUP_LOCAL_NOTIFICATION_ID = 987701
    private const val AUTO_BACKUP_CLOUD_NOTIFICATION_ID = 987702

    // Intent extras
    const val EXTRA_CYCLE_BILL_ID = "cycle_bill_id"
    const val EXTRA_ACTION = "action"
    const val ACTION_GENERATE = "generate"
    const val ACTION_REMIND = "remind"
    const val EXTRA_NAME = "name"
    const val EXTRA_AMOUNT = "amount"
    const val EXTRA_TYPE = "type"
    const val EXTRA_ADVANCE_MINUTES = "advance_minutes"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "周期账单到期提醒"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createAutoBackupNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AUTO_BACKUP_CHANNEL_ID,
                AUTO_BACKUP_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "自动备份失败和需要处理的状态"
                enableVibration(false)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** 自动备份失败提醒；通知权限被拒绝时由设置页状态继续承担提示职责。 */
    fun showAutoBackupFailure(context: Context, target: AutoBackupTarget, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createAutoBackupNotificationChannel(context)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { putExtra("open_backup_status", true) }
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                if (target == AutoBackupTarget.LOCAL) AUTO_BACKUP_LOCAL_NOTIFICATION_ID else AUTO_BACKUP_CLOUD_NOTIFICATION_ID,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val title = if (target == AutoBackupTarget.LOCAL) "本地自动备份失败" else "云端自动备份失败"
        val notification = NotificationCompat.Builder(context, AUTO_BACKUP_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message.take(180))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()
        val notificationId = if (target == AutoBackupTarget.LOCAL) {
            AUTO_BACKUP_LOCAL_NOTIFICATION_ID
        } else {
            AUTO_BACKUP_CLOUD_NOTIFICATION_ID
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, notification)
    }

    fun buildCycleBillNotification(
        context: Context,
        cycleBillId: Long,
        name: String,
        amount: Double,
        typeStr: String,
        advanceMinutes: Int
    ): Notification {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        // Use hash-based request codes to avoid integer overflow
        val baseCode = cycleBillId.hashCode()

        // 生成按钮的 PendingIntent
        val generateIntent = Intent(context, CycleBillBroadcastReceiver::class.java).apply {
            action = "com.inkqilin.ledger.action.GENERATE"
            putExtra(EXTRA_CYCLE_BILL_ID, cycleBillId)
            putExtra(EXTRA_ACTION, ACTION_GENERATE)
        }
        val generatePendingIntent = PendingIntent.getBroadcast(
            context,
            baseCode + 1,
            generateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("墨麒麟记账 - 周期账单（${name}）到期")
            .setContentText("${typeStr} ¥${String.format("%.2f", amount)}，提前${advanceMinutes}分钟提醒您。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(android.R.drawable.ic_menu_edit, "立即生成", generatePendingIntent)
            .setAutoCancel(false)
            .build()
    }

    fun scheduleCycleBillReminder(context: Context, cycleBillId: Long, name: String, amount: Double, typeStr: String, triggerAt: Long, advanceMinutes: Int) {
        cancelCycleBillReminder(context, cycleBillId)
        if (triggerAt <= System.currentTimeMillis()) return
        val baseCode = cycleBillId.hashCode()
        val intent = Intent(context, CycleBillBroadcastReceiver::class.java).apply {
            action = "com.inkqilin.ledger.action.REMIND"
            putExtra(EXTRA_CYCLE_BILL_ID, cycleBillId)
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_TYPE, typeStr)
            putExtra(EXTRA_ADVANCE_MINUTES, advanceMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, baseCode + 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelCycleBillReminder(context: Context, cycleBillId: Long) {
        val baseCode = cycleBillId.hashCode()
        val intent = Intent(context, CycleBillBroadcastReceiver::class.java).apply {
            action = "com.inkqilin.ledger.action.REMIND"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, baseCode + 3, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent?.let { alarmManager.cancel(it); it.cancel() }
    }

    fun showTestNotification(context: Context) {
        createNotificationChannel(context)
        val notification = buildCycleBillNotification(context, -1L, "测试周期账单", 0.01, "支出", 0)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(987654, notification)
    }

    /** 取消指定周期账单的所有通知 */
    fun cancelCycleBillNotification(context: Context, cycleBillId: Long) {
        val baseCode = cycleBillId.hashCode()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(baseCode + 1)
        cancelCycleBillReminder(context, cycleBillId)
    }
}
