package com.inkqilin.ledger.service.parser

import android.util.Log

/**
 * 通知解析门面：按包名分发到各渠道解析器。
 * 原 `service.NotificationParser` 拆分而来，对外入口保持稳定。
 */
object NotificationParser {

    fun Parse(pkg: String, title: String, text: String): ParsedNotification? {
        val rawText = "$title\n$text".replace("\n", " ").replace("\r", " ")
        Log.d("NotificationParser", "Parsing: pkg=$pkg, rawText=$rawText")

        val result = when (pkg) {
            "com.eg.android.AlipayGphone", "com.android.shell" -> AlipayParser.Parse(rawText)
            "com.tencent.mm" -> WeChatParser.Parse(rawText)
            "com.unionpay" -> UnionPayParser.Parse(rawText)
            else -> GenericPaymentParser.Parse(rawText)
        }

        if (result != null && result.amount <= 0.0) {
            Log.d("NotificationParser", "Filtered zero-amount transaction: ${result.amount}")
            return null
        }

        return result
    }
}
