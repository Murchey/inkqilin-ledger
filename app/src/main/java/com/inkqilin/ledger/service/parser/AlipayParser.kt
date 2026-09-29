package com.inkqilin.ledger.service.parser

import android.util.Log

/** 支付宝通知解析。 */
object AlipayParser {
    fun Parse(text: String): ParsedNotification? {
        Log.d("NotificationParser", "Alipay fullText: $text")

        if (NotificationParseSupport.ShouldSkip(text)) {
            Log.d("NotificationParser", "Alipay: non-transaction/ad content filtered")
            return null
        }

        NotificationParseSupport.ParseTapToPay(text)?.let { return it }
        NotificationParseSupport.ParsePaymentSuccess(text)?.let { return it }
        NotificationParseSupport.ParseIncome(text)?.let { return it }
        NotificationParseSupport.ParseExpense(text)?.let { return it }
        NotificationParseSupport.ParseFallback(text)?.let { return it }

        Log.d("NotificationParser", "Alipay: no match found")
        return null
    }
}
