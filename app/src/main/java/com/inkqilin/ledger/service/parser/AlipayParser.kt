package com.inkqilin.ledger.service.parser

/** 支付宝通知解析。金额优先取交易字段，避免把优惠和余额当作交易金额。 */
object AlipayParser {
    private const val NUMBER = "(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]{1,2})?"
    private const val CURRENCY = "(?:[¥￥]|人民币|RMB|CNY)"
    private val moneyPattern = Regex(
        "(?:$CURRENCY\\s*($NUMBER)|(?<![0-9.,])($NUMBER)\\s*元)(?![0-9.]|,[0-9])",
        RegexOption.IGNORE_CASE,
    )
    private val incomeSignals = listOf(
        "到账", "收款", "向你付款", "向你转账", "向你支付", "转入", "收钱码", "收红包",
    )
    private val expenseSignals = listOf(
        "支付", "付款", "消费", "扣款", "支出", "碰一碰", "碰一下", "缴费", "充值",
    )
    private val confirmedSignals = listOf(
        "付款成功", "支付成功", "成功付款", "成功支付", "已付款", "已支付",
        "扣款成功", "已扣款", "消费成功", "交易成功", "收款成功", "成功收款", "已到账",
    )
    private val excludedStates = listOf(
        "支付失败", "付款失败", "扣款失败", "交易失败", "收款失败", "转账失败", "到账失败",
        "未支付", "未付款", "待支付", "待付款", "等待付款", "等待支付", "请支付", "请付款",
        "支付已取消", "付款已取消", "交易关闭", "交易取消", "退款", "退回", "撤销",
        "验证码", "校验码", "动态码",
    )
    private val actualAmountLabel = Regex("(?:实际支付|实际付款|实付)[\\s：:为共计]*$")
    private val amountLabel = Regex(
        "(?:实际支付|实际付款|实付|付款金额|支付金额|消费金额|扣款金额|到账金额|收款金额|" +
            "成功支付|成功付款|成功收款|已支付|已付款|已到账|已扣款|" +
            "支付|付款|消费|扣款|到账|收款|转入)(?:成功)?[\\s：:为共计]*$",
    )
    private val otherAmountLabel = Regex("(?:优惠|立减|减免|折扣|红包|余额|原价|节省|返现)[\\s：:为共计]*$")

    fun Parse(text: String): ParsedNotification? {
        val normalized = text.replace(Regex("(?<=[0-9])，(?=[0-9]{3}(?:[^0-9]|$))"), ",").replace('．', '.').replace('\u00a0', ' ')
        if (excludedStates.any { normalized.contains(it) }) return null

        val isIncome = incomeSignals.any { normalized.contains(it) }
        if (!isIncome && expenseSignals.none { normalized.contains(it) }) return null
        val confirmed = confirmedSignals.any { normalized.contains(it) }
        if (!confirmed && NotificationParseSupport.ShouldSkip(normalized)) return null

        val amounts = moneyPattern.findAll(normalized).toList()
        // 每个金额只检查紧邻的字段名，不跨过另一个金额去匹配交易词。
        val actualAmount = amounts.firstOrNull { match ->
            actualAmountLabel.containsMatchIn(normalized.substring(0, match.range.first).takeLast(16))
        }
        val transactionAmount = amounts.firstOrNull { match ->
            amountLabel.containsMatchIn(normalized.substring(0, match.range.first).takeLast(16))
        }
        val fallbackAmounts = amounts.filter { match ->
            !otherAmountLabel.containsMatchIn(normalized.substring(0, match.range.first).takeLast(16))
        }
        // 多个无明确字段的金额无法可靠区分，等待更明确的通知。
        val selected = actualAmount ?: transactionAmount ?: fallbackAmounts.singleOrNull() ?: return null
        val amount = selected.groupValues.drop(1).first { it.isNotEmpty() }
            .replace(",", "").toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return null

        return ParsedNotification(
            amount = amount,
            isIncome = isIncome,
            category = NotificationParseSupport.InferCategory(normalized),
            merchant = NotificationParseSupport.ExtractMerchantFallback(normalized),
            rawSource = "支付宝",
        )
    }
}
