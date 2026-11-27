package com.inkqilin.ledger.service.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlipayParserTest {
    @Test
    fun parsesCurrencyFormats() {
        listOf(
            "付款成功，向便利店付款￥12.50" to 12.5,
            "支付宝 扣款通知，扣款12.50元" to 12.5,
            "支付宝 付款成功，支付人民币1,234.56" to 1234.56,
            "支付宝 已支付 RMB 1,234.56" to 1234.56,
            "支付宝 已支付￥1，234．56" to 1234.56,
            "支付宝 碰一碰付款￥8.00" to 8.0,
        ).forEach { (text, amount) ->
            val result = requireNotNull(AlipayParser.Parse(text)) { text }
            assertEquals(text, amount, result.amount, 0.001)
            assertFalse(text, result.isIncome)
            assertEquals("支付宝", result.rawSource)
        }
    }

    @Test
    fun parsesIncomeIncludingSomeonePayingMe() {
        listOf("支付宝 收款到账12.50元", "支付宝 张三向你付款成功￥20.00").forEach { text ->
            assertTrue(text, requireNotNull(AlipayParser.Parse(text)).isIncome)
        }
    }

    @Test
    fun selectsActualPaymentBeforeDiscountAndBalance() {
        listOf(
            "支付宝 优惠2元，已支付10元，余额100元",
            "支付宝 支付成功，优惠￥2.00，向便利店付款￥10.00",
            "支付宝 支付成功，付款金额20元，实付10元",
        ).forEach { text ->
            assertEquals(text, 10.0, requireNotNull(AlipayParser.Parse(text)) { text }.amount, 0.001)
        }
    }

    @Test
    fun detailedReasonsAreStableAndDoNotContainNotificationText() {
        val cases = listOf(
            "" to "empty",
            "支付失败￥20.00" to "excluded_status",
            "新通知" to "no_transaction_signal",
            "付款成功" to "no_amount",
            "付款成功，订单￥10.00，订单￥20.00" to "ambiguous_amount",
            "付款成功￥0.00" to "invalid_amount",
            "付款成功￥10.00" to "success",
        )
        cases.forEach { (text, reason) ->
            assertEquals(reason, AlipayParser.ParseDetailed(text).reason)
        }
    }

    @Test
    fun rejectsFailedPendingMarketingAndAmbiguousAmounts() {
        listOf(
            "支付宝 支付失败￥20.00", "支付宝 未支付20元", "支付宝 待付款￥20.00",
            "支付宝 付款成功验证码123456，支付￥20.00",
            "支付宝 支付享优惠，立减￥20.00", "支付宝 优惠券到账20元",
            "支付宝 支付已取消￥20.00", "支付宝 退款到账20元",
            "支付宝 付款成功￥0.00", "支付宝 付款成功￥1,23.45",
            "支付宝 付款成功，订单￥10.00，订单￥20.00",
            "支付宝 付款成功", "",
        ).forEach { text -> assertNull(text, AlipayParser.Parse(text)) }
    }
}
