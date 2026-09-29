package com.inkqilin.ledger.service.parser

import android.util.Log

/** 其他 App / 系统通知的通用解析。 */
object GenericPaymentParser {
    fun Parse(text: String): ParsedNotification? {
        Log.d("NotificationParser", "Generic fullText: $text")

        if (NotificationParseSupport.ShouldSkip(text)) {
            Log.d("NotificationParser", "Generic: non-transaction/ad content filtered")
            return null
        }

        NotificationParseSupport.ParseIncome(text, rawSource = "系统通知")?.let { return it }
        NotificationParseSupport.ParseExpense(text, rawSource = "系统通知")?.let { return it }
        NotificationParseSupport.ParseFallback(text, rawSource = "系统通知")?.let { return it }
        return null
    }
}
