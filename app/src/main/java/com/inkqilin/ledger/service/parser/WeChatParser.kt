package com.inkqilin.ledger.service.parser

import android.util.Log

/** 微信支付通知解析。 */
object WeChatParser {
    fun Parse(text: String): ParsedNotification? {
        Log.d("NotificationParser", "WeChat fullText: $text")

        if (NotificationParseSupport.ShouldSkip(text)) {
            Log.d("NotificationParser", "WeChat: non-transaction/ad content filtered")
            return null
        }

        NotificationParseSupport.ParseIncome(text)?.let { return it }
        NotificationParseSupport.ParseExpense(text)?.let { return it }
        NotificationParseSupport.ParseFallback(text)?.let { return it }

        Log.d("NotificationParser", "WeChat: no match found")
        return null
    }
}
