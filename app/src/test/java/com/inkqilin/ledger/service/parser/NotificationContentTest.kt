package com.inkqilin.ledger.service.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationContentTest {
    private fun parse(title: String, body: String) =
        NotificationParser.ParseDetailed("com.eg.android.AlipayGphone", title, body)

    @Test fun plainBigTextSummaryAllowsTextFallback() {
        val result = NotificationContent("支付宝", text = "付款成功，向便利店付款￥12.50", bigText = "你有一条新通知")
            .parseDetailed(::parse)
        assertNotNull(result.parsed)
        assertEquals(12.5, result.parsed!!.amount, 0.001)
        assertEquals("text", result.source)
    }

    @Test fun transactionBigTextWinsConflictingTextAndOnlyParsedOnce() {
        var calls = 0
        val content = NotificationContent("支付宝", text = "付款成功￥10.00", bigText = "付款成功￥20.00")
        val result = content.parseDetailed { title, body ->
            calls++
            parse(title, body)
        }
        assertEquals(1, calls)
        assertEquals(20.0, result.parsed!!.amount, 0.001)
        assertEquals("bigText", result.source)
    }

    @Test fun bigTextAmbiguityCannotFallThroughToShortAmount() {
        var calls = 0
        val content = NotificationContent("支付宝", text = "付款成功￥10.00", bigText = "付款成功订单￥20.00 订单￥30.00")
        val result = content.parseDetailed { title, body ->
            calls++
            parse(title, body)
        }
        assertEquals(1, calls)
        assertNull(result.parsed)
        assertEquals("bigText", result.source)
    }

    @Test fun marketingAndRefundSummariesCannotFallThrough() {
        listOf("支付享优惠，立减￥20.00", "退款到账￥12.50", "付款成功，优惠2元").forEach { summary ->
            val result = NotificationContent("支付宝", text = "付款成功￥12.50", bigText = summary)
                .parseDetailed(::parse)
            assertNull(result.parsed)
            assertEquals("bigText", result.source)
        }
    }

    @Test fun bodyPreventsHistoryFallback() {
        val result = NotificationContent("支付宝", text = "支付失败￥12.50", latestMessage = "付款成功￥12.50", textLines = listOf("付款成功￥12.50"))
            .parseDetailed(::parse)
        assertNull(result.parsed)
        assertEquals("text", result.source)
    }

    @Test fun paymentFailureCannotPassThroughFallback() {
        val result = NotificationContent("支付宝", text = "付款成功￥12.50", bigText = "支付失败￥12.50")
            .parseDetailed(::parse)
        assertNull(result.parsed)
        assertEquals("excluded_status", result.reason)
        assertEquals("bigText", result.source)
    }

    @Test fun latestMessageThenTextLinesAreOnlyUsedWhenBodyEmpty() {
        val latest = NotificationContent("支付宝", latestMessage = "付款成功￥3.00", textLines = listOf("付款成功￥2.00"))
            .parseDetailed(::parse)
        assertEquals(3.0, latest.parsed!!.amount, 0.001)
        assertEquals("latestMessage", latest.source)
        val lines = NotificationContent("支付宝", textLines = listOf("旧消息￥1.00", "付款成功￥2.00"))
            .parseDetailed(::parse)
        assertEquals(2.0, lines.parsed!!.amount, 0.001)
        assertEquals("textLines", lines.source)
    }

    @Test fun titleAndMetadataRemainAvailableWithoutBody() {
        val title = NotificationContent("付款成功￥10.00").parseDetailed(::parse)
        assertEquals(10.0, title.parsed!!.amount, 0.001)
        val info = NotificationContent("支付宝", infoText = "付款成功￥10.00").parseDetailed(::parse)
        assertEquals(10.0, info.parsed!!.amount, 0.001)
    }

    @Test fun labeledBalanceMetadataKeepsActualPayment() {
        val result = NotificationContent("支付宝", text = "实际支付￥10.00", infoText = "余额￥20.00").parseDetailed(::parse)
        assertEquals(10.0, result.parsed!!.amount, 0.001)
    }

    @Test fun metadataWithConflictingAmountsIsRejected() {
        val result = NotificationContent("付款成功", text = "订单￥10.00", infoText = "订单￥20.00").parseDetailed(::parse)
        assertNull(result.parsed)
        assertEquals("ambiguous_amount", result.reason)
    }

    @Test fun messagingStyleNeverMergesOldMessages() {
        var calls = 0
        var selectedBody = ""
        val result = NotificationContent("微信", textLines = listOf("张三付款￥100.00", "李四付款￥2.00"))
            .parseDetailed { _, body ->
                calls++
                selectedBody = body
                NotificationParseResult(null, "no_transaction_signal", "")
            }
        assertEquals(1, calls)
        assertEquals("李四付款￥2.00", selectedBody)
        assertEquals("textLines", result.source)
    }
}
