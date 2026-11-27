package com.inkqilin.ledger.service.parser

/** Pure Kotlin snapshot of notification display content. */
data class NotificationContent(
    val title: String,
    val text: String = "",
    val bigText: String = "",
    val infoText: String = "",
    val subText: String = "",
    val textLines: List<String> = emptyList(),
    val latestMessage: String = "",
) {
    /** Select a single candidate before invoking the parser, preventing duplicate transactions. */
    fun parseDetailed(parser: (String, String) -> NotificationParseResult): NotificationParseResult {
        val longBody = bigText.trim()
        val shortBody = text.trim()
        val (body, source) = when {
            longBody.isNotEmpty() && shortBody.isNotEmpty() && isPlainSummary(longBody) -> shortBody to "text"
            longBody.isNotEmpty() -> longBody to "bigText"
            shortBody.isNotEmpty() -> shortBody to "text"
            latestMessage.isNotBlank() -> latestMessage.trim() to "latestMessage"
            else -> textLines.lastOrNull()?.trim().orEmpty() to "textLines"
        }
        val metadata = listOf(infoText.trim(), subText.trim())
            .filter { it.isNotEmpty() && it != title.trim() && !body.contains(it) }
            .distinct()
        val combinedBody = (listOf(body) + metadata).filter { it.isNotEmpty() }.joinToString("\n")
        if (combinedBody.isEmpty() && title.isBlank()) return NotificationParseResult(null, "empty", "empty")
        val candidateSource = if (body.isEmpty()) "metadata" else source
        return parser(title, combinedBody).copy(source = candidateSource)
    }

    private fun isPlainSummary(body: String): Boolean =
        !transactionOrStatus.containsMatchIn(body) && !amountOrMarketing.containsMatchIn(body)

    private companion object {
        val transactionOrStatus = Regex("到账|收款|付款|支付|消费|扣款|支出|转账|转入|缴费|充值|碰一碰|碰一下|交易|退款|退回|撤销|失败|取消|关闭|待付|未付|验证码|校验码|动态码")
        val amountOrMarketing = Regex("[0-9¥￥元]|优惠|立减|红包|返现|折扣|促销|福利|领券|优惠券|奖励|活动|推荐|抽奖|广告")
    }
}

data class NotificationParseResult(val parsed: ParsedNotification?, val reason: String, val source: String)
