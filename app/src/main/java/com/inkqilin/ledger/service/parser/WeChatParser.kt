package com.inkqilin.ledger.service.parser


/** 微信支付通知解析。 */
object WeChatParser {
    fun Parse(text: String): ParsedNotification? {

        if (NotificationParseSupport.ShouldSkip(text)) {
            return null
        }

        NotificationParseSupport.ParseIncome(text)?.let { return it }
        NotificationParseSupport.ParseExpense(text)?.let { return it }
        NotificationParseSupport.ParseFallback(text)?.let { return it }

        return null
    }
}
