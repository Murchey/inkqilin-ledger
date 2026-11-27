package com.inkqilin.ledger.service

internal fun notificationEventKey(
    packageName: String,
    notificationKey: String,
    postTime: Long,
    amount: Double,
    isIncome: Boolean,
    merchant: String,
    source: String,
): String = listOf(
    packageName, notificationKey, postTime.toString(), amount.toString(),
    isIncome.toString(), merchant, source,
).joinToString("\u0000")

/** 仅在数据库写入成功后登记；失败事务可由同一通知重试。 */
internal class NotificationCaptureDeduplicator(
    private val windowMs: Long = 30_000L,
    private val maxEntries: Int = 512,
) {
    private val processedAt = LinkedHashMap<String, Long>()
    private val inFlight = mutableSetOf<String>()

    @Synchronized
    fun tryBegin(key: String, now: Long): Boolean {
        prune(now)
        if (key in inFlight) return false
        val previous = processedAt[key]
        if (previous != null && now >= previous && now - previous < windowMs) return false
        inFlight += key
        return true
    }

    @Synchronized
    fun finish(key: String, now: Long, succeeded: Boolean) {
        inFlight -= key
        if (succeeded) {
            processedAt[key] = now
            while (processedAt.size > maxEntries) {
                processedAt.remove(processedAt.entries.first().key)
            }
        }
    }

    private fun prune(now: Long) {
        val iterator = processedAt.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now < entry.value || now - entry.value >= windowMs) iterator.remove()
        }
    }
}
