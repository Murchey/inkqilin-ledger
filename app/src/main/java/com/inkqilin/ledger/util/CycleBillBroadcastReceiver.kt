package com.inkqilin.ledger.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.TaskStackBuilder
import com.inkqilin.ledger.MainActivity

class CycleBillBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val cycleBillId = intent.extras?.getLong(NotificationHelper.EXTRA_CYCLE_BILL_ID) ?: return

        when (action) {
            "com.inkqilin.ledger.action.REMIND" -> {
                Log.d("CycleBillBR", "REMIND triggered for billId=$cycleBillId")
                val baseCode = cycleBillId.hashCode()
                val notifId = baseCode + 1
                
                // Cancel any existing notification first to prevent stacking
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.cancel(notifId)
                
                NotificationHelper.createNotificationChannel(context)
                val notification = NotificationHelper.buildCycleBillNotification(
                    context,
                    cycleBillId,
                    intent.getStringExtra(NotificationHelper.EXTRA_NAME) ?: "周期账单",
                    intent.getDoubleExtra(NotificationHelper.EXTRA_AMOUNT, 0.0),
                    intent.getStringExtra(NotificationHelper.EXTRA_TYPE) ?: "支出",
                    intent.getIntExtra(NotificationHelper.EXTRA_ADVANCE_MINUTES, 0)
                )
                manager.notify(notifId, notification)
                Log.d("CycleBillBR", "Notification shown with id=$notifId for billId=$cycleBillId")
            }
            "com.inkqilin.ledger.action.GENERATE" -> {
                // 打开 CycleBill detail page or generate transaction directly
                val resultIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "cycle_bill_$cycleBillId")
                }
                context.startActivity(resultIntent)
            }
        }
    }
}
