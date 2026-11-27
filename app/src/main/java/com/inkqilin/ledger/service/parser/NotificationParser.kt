package com.inkqilin.ledger.service.parser

/** 通知解析门面，兼容原有入口并提供可诊断的原因。 */
object NotificationParser {
    fun Parse(pkg: String, title: String, text: String): ParsedNotification? =
        ParseDetailed(pkg, title, text).parsed

    fun ParseDetailed(pkg: String, content: NotificationContent): NotificationParseResult =
        content.parseDetailed { title, body -> ParseDetailed(pkg, title, body) }

    fun ParseDetailed(pkg: String, title: String, text: String): NotificationParseResult {
        val rawText = "$title $text".replace("\n", " ").replace("\r", " ").trim()
        if (rawText.isEmpty()) return NotificationParseResult(null, "empty", "text")
        if (pkg == "com.eg.android.AlipayGphone" || pkg == "com.android.shell") {
            val result = AlipayParser.ParseDetailed(rawText)
            return NotificationParseResult(result.parsed, result.reason, "text")
        }
        val parsed = when (pkg) {
            "com.tencent.mm" -> WeChatParser.Parse(rawText)
            "com.unionpay" -> UnionPayParser.Parse(rawText)
            else -> GenericPaymentParser.Parse(rawText)
        } ?: return NotificationParseResult(null, "no_transaction_signal", "text")
        if (!parsed.amount.isFinite() || parsed.amount <= 0.0) {
            return NotificationParseResult(null, "invalid_amount", "text")
        }
        return NotificationParseResult(parsed, "success", "text")
    }
}
