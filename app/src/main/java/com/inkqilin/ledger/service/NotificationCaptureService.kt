package com.inkqilin.ledger.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.inkqilin.ledger.data.AppDatabase
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NotificationCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val themeManager by lazy { ThemeManager(applicationContext) }
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }

    // 去重缓存：(金额+类别+收支类型) -> 时间戳
    private val lastProcessedMap = ConcurrentHashMap<String, Long>()
    private val DEDUPLICATION_WINDOW_MS = 5000L // 5秒内去重

    override fun onListenerConnected() {
        try {
            super.onListenerConnected()
            Log.d("NotificationCapture", "Service connected and listening")
        } catch (t: Throwable) {
            // 鸿蒙/卓易通上 NotificationListener 绑定可能失败，不得拖垮进程
            Log.w("NotificationCapture", "onListenerConnected failed", t)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 鸿蒙/卓易通上通知监听不可用；任何异常不得导致进程崩溃
        try {
            super.onNotificationPosted(sbn)

            serviceScope.launch {
                try {
                    // 检查功能是否开启
                    val isEnabled = themeManager.autoRecordEnabled.first()
                    if (!isEnabled) return@launch

                    // 鸿蒙兼容模式：自动记账不可用，直接降级
                    if (themeManager.harmonyCompatMode.first()) {
                        Log.d("NotificationCapture", "Harmony compat mode: auto-record disabled")
                        return@launch
                    }

                    val packageName = sbn.packageName
                    Log.d("NotificationCapture", "Captured from $packageName. Auto-record enabled: $isEnabled")

                    val notification = sbn.notification
                    val extras = notification.extras ?: return@launch

                    val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                    val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
                    val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

                    val combinedText = "$text $bigText $infoText $subText"
                    Log.d("NotificationCapture", "Full contents: Title=$title, CombinedText=$combinedText")

                    NotificationParser.parse(packageName, title, combinedText)?.let { parsed ->
                        Log.d("NotificationCapture", "Successfully parsed: $parsed")
                        processAndSave(parsed)
                    } ?: run {
                        Log.d("NotificationCapture", "Parser returned null for $packageName")
                    }
                } catch (t: Throwable) {
                    Log.w("NotificationCapture", "process notification failed", t)
                }
            }
        } catch (t: Throwable) {
            Log.w("NotificationCapture", "onNotificationPosted failed", t)
        }
    }

    private suspend fun processAndSave(parsed: NotificationParser.ParsedNotification) {
        val now = System.currentTimeMillis()
        val key = "${parsed.amount}_${parsed.category}_${parsed.isIncome}"

        // 去重逻辑
        val lastTime = lastProcessedMap[key]
        if (lastTime != null && now - lastTime < DEDUPLICATION_WINDOW_MS) {
            return
        }
        lastProcessedMap[key] = now

        val transaction = Transaction(
            amount = parsed.amount,
            category = parsed.category,
            note = "[自动] ${parsed.rawSource} · ${parsed.merchant}",
            date = now,
            type = if (parsed.isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
            currency = "CNY"
        )

        try {
            database.transactionDao().insertTransaction(transaction)
        } catch (t: Throwable) {
            Log.w("NotificationCapture", "insert auto transaction failed", t)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理缓存，防止内存泄漏（虽然 ConcurrentHashMap 本身很小）
        lastProcessedMap.clear()
    }
}
