package com.inkqilin.ledger.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import com.inkqilin.ledger.service.NotificationCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationCaptureState(
    val connected: Boolean = false,
    val lastEvent: String = "尚未收到通知",
    val lastEventTime: Long = 0L,
    val lastAlipayEvent: String = "尚未收到支付宝通知",
    val lastAlipayEventTime: Long = 0L,
)

/** 当前进程的监听状态，不保存通知正文。 */
object NotificationCaptureMonitor {
    private val mutableState = MutableStateFlow(NotificationCaptureState())
    val state = mutableState.asStateFlow()

    fun hasPermission(context: Context): Boolean {
        val component = ComponentName(context, NotificationCaptureService::class.java)
        return runCatching {
            val listeners = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ).orEmpty()
            listeners.split(':').any { ComponentName.unflattenFromString(it) == component }
        }.onFailure { Log.w("NotificationCapture", "Permission check failed", it) }.getOrDefault(false)
    }

    fun requestRebind(context: Context): Boolean {
        if (!hasPermission(context)) return false
        return runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, NotificationCaptureService::class.java)
            )
        }.onFailure { Log.w("NotificationCapture", "Listener rebind failed", it) }.isSuccess
    }

    fun setConnected(connected: Boolean) {
        mutableState.update { it.copy(connected = connected) }
    }

    fun recordEvent(packageName: String, stage: String) {
        val now = System.currentTimeMillis()
        mutableState.update {
            val current = it.copy(lastEvent = "$packageName · $stage", lastEventTime = now)
            if (packageName == "com.eg.android.AlipayGphone") {
                current.copy(lastAlipayEvent = stage, lastAlipayEventTime = now)
            } else current
        }
    }
}
