package com.inkqilin.ledger.service

import android.app.Notification
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.data.repository.LedgerRepositories
import com.inkqilin.ledger.service.parser.NotificationContent
import com.inkqilin.ledger.service.parser.NotificationParser
import com.inkqilin.ledger.service.parser.ParsedNotification
import com.inkqilin.ledger.util.NotificationCaptureMonitor
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class NotificationCaptureService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val themeManager by lazy { ThemeManager(applicationContext) }
    private val repos by lazy { LedgerRepositories.get(applicationContext) }
    private val deduplicator = NotificationCaptureDeduplicator()
    private val saveMutex = Mutex()

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationCaptureMonitor.setConnected(true)
        Log.i(TAG, "Listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationCaptureMonitor.setConnected(false)
        Log.w(TAG, "Listener disconnected")
        serviceScope.launch {
            if (themeManager.autoRecordEnabled.first() && !themeManager.harmonyCompatMode.first()) {
                NotificationCaptureMonitor.requestRebind(this@NotificationCaptureService)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val pkg = sbn.packageName
        // 在任何设置检查之前记录回调，区分没有投递与收到后被拦截。
        event(pkg, "received", "已收到通知")
        serviceScope.launch {
            try {
                if (!themeManager.autoRecordEnabled.first()) {
                    event(pkg, "disabled", "自动记账未开启")
                    return@launch
                }
                if (themeManager.harmonyCompatMode.first()) {
                    event(pkg, "compatibility_mode", "兼容模式暂停自动记账")
                    return@launch
                }
                val notification = sbn.notification
                if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
                    event(pkg, "group_summary", "已忽略分组汇总通知")
                    return@launch
                }
                val extras = notification.extras
                if (extras == null) {
                    event(pkg, "missing_extras", "通知没有文本字段")
                    return@launch
                }
                val messages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(
                    extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                )
                val content = NotificationContent(
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                    bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
                    infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString().orEmpty(),
                    subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty(),
                    textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                        ?.map { it.toString() }.orEmpty(),
                    latestMessage = messages.maxByOrNull { it.timestamp }?.text?.toString().orEmpty(),
                )
                // 只输出字段长度和模板信息，账单正文、金额与商户不写入日志。
                Log.d(TAG, "Fields pkg=$pkg channel=${notification.channelId} flags=${notification.flags} " +
                    "title=${content.title.length} text=${content.text.length} bigText=${content.bigText.length} " +
                    "lines=${content.textLines.size} messages=${messages.size}")
                val result = NotificationParser.ParseDetailed(pkg, content)
                val parsed = result.parsed
                if (parsed == null) {
                    event(pkg, "parse_rejected reason=${result.reason} source=${result.source}", "未匹配账单（${result.reason}）")
                    return@launch
                }
                event(pkg, "parsed source=${result.source}", "账单解析成功")
                processAndSave(sbn, parsed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                event(pkg, "processing_failed", "通知处理失败，可查看诊断日志")
                Log.w(TAG, "Notification processing failed pkg=$pkg", error)
            }
        }
    }

    private suspend fun processAndSave(sbn: StatusBarNotification, parsed: ParsedNotification) {
        // 同一通知身份与交易字段去重，独立通知和不同商户不再相互抑制。
        val key = notificationEventKey(
            packageName = sbn.packageName,
            notificationKey = sbn.key,
            postTime = sbn.postTime,
            amount = parsed.amount,
            isIncome = parsed.isIncome,
            merchant = parsed.merchant,
            source = parsed.rawSource,
        )
        val uuid = UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
        saveMutex.withLock {
            if (!deduplicator.tryBegin(key, SystemClock.elapsedRealtime())) {
                event(sbn.packageName, "duplicate", "已忽略重复通知")
                return@withLock
            }
            var saved = false
            try {
                val inserted = repos.transactions.insertNotificationIfAbsent(Transaction(
                    amount = parsed.amount,
                    category = parsed.category,
                    note = "[自动] ${parsed.rawSource} · ${parsed.merchant}",
                    date = sbn.postTime,
                    type = if (parsed.isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                    currency = "CNY",
                    uuid = uuid,
                ))
                saved = true
                if (inserted) {
                    event(sbn.packageName, "saved", "账单已保存")
                } else {
                    event(sbn.packageName, "already_saved", "该通知已记账")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                event(sbn.packageName, "save_failed", "账单保存失败，等待通知重试")
                Log.w(TAG, "Auto transaction insert failed pkg=${sbn.packageName}", error)
            } finally {
                deduplicator.finish(key, SystemClock.elapsedRealtime(), saved)
            }
        }
    }

    private fun event(pkg: String, stage: String, message: String) {
        Log.d(TAG, "pkg=$pkg stage=$stage")
        NotificationCaptureMonitor.recordEvent(pkg, message)
    }

    override fun onDestroy() {
        NotificationCaptureMonitor.setConnected(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "NotificationCapture"
    }
}
