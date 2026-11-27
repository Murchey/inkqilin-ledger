package com.inkqilin.ledger.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCaptureDeduplicatorTest {
    @Test
    fun repeatedDeliveryHasStableIdentityButNewPostDoesNotCollide() {
        val first = notificationEventKey("alipay", "same-key", 1_000, 10.0, false, "商户", "支付宝")
        val redelivery = notificationEventKey("alipay", "same-key", 1_000, 10.0, false, "商户", "支付宝")
        val newPayment = notificationEventKey("alipay", "same-key", 2_000, 10.0, false, "商户", "支付宝")
        assertEquals(first, redelivery)
        assertNotEquals(first, newPayment)
    }

    @Test
    fun sameNotificationIsSuppressedOnlyAfterSuccessfulSave() {
        val deduplicator = NotificationCaptureDeduplicator(windowMs = 5_000)
        assertTrue(deduplicator.tryBegin("pkg|key|hash", 1_000))
        assertFalse(deduplicator.tryBegin("pkg|key|hash", 1_001))
        deduplicator.finish("pkg|key|hash", 1_002, succeeded = false)
        assertTrue(deduplicator.tryBegin("pkg|key|hash", 1_003))
        deduplicator.finish("pkg|key|hash", 1_004, succeeded = true)
        assertFalse(deduplicator.tryBegin("pkg|key|hash", 1_005))
        assertTrue(deduplicator.tryBegin("pkg|key|hash", 6_004))
    }

    @Test
    fun differentNotificationIdentitiesDoNotSuppressSameAmountTransactions() {
        val deduplicator = NotificationCaptureDeduplicator()
        assertTrue(deduplicator.tryBegin("alipay|notification-A|merchant-one", 10))
        deduplicator.finish("alipay|notification-A|merchant-one", 11, true)
        assertTrue(deduplicator.tryBegin("alipay|notification-B|merchant-two", 12))
    }
}
