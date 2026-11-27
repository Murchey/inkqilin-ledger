package com.inkqilin.ledger.service.parser


/** 云闪付通知解析。 */
object UnionPayParser {
    fun Parse(text: String): ParsedNotification? {

        if (NotificationParseSupport.ShouldSkip(text)) {
            return null
        }

        val incomeKeywords = NotificationKeywords.UNION_PAY_INCOME_KEYWORDS
        val expenseKeywords = NotificationKeywords.UNION_PAY_EXPENSE_KEYWORDS
        val strongSignals = NotificationKeywords.UNION_PAY_STRONG_SIGNALS

        if (NotificationParseSupport.IsAdvertisement(text) &&
            !strongSignals.any { text.contains(it) }
        ) {
            return null
        }

        val match = NotificationKeywords.AMOUNT_REGEX.find(text)
        if (match == null) {
            val altMatch = Regex("""(\d+(?:\.\d{1,2})?)\s*元""").find(text)
            if (altMatch != null) {
                val amount = altMatch.groupValues[1].toDoubleOrNull() ?: return null
                return ParseUnionPayWithAmount(text, amount, incomeKeywords, expenseKeywords, strongSignals)
            }
            return null
        }

        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        return ParseUnionPayWithAmount(text, amount, incomeKeywords, expenseKeywords, strongSignals)
    }

    fun ParseUnionPayWithAmount(
        text: String,
        amount: Double,
        incomeKeywords: Set<String>,
        expenseKeywords: Set<String>,
        strongSignals: Set<String>,
    ): ParsedNotification? {
        val hasIncomeKeyword = incomeKeywords.any { text.contains(it) }
        val hasExpenseKeyword = expenseKeywords.any { text.contains(it) }
        val hasStrongSignal = strongSignals.any { text.contains(it) }

        val isIncome = when {
            hasIncomeKeyword && !hasExpenseKeyword -> true
            hasExpenseKeyword && !hasIncomeKeyword -> false
            hasStrongSignal -> false
            else -> false
        }

        return ParsedNotification(
            amount = amount,
            isIncome = isIncome,
            category = NotificationParseSupport.InferCategory(text),
            merchant = NotificationParseSupport.ExtractMerchantFallback(text),
            rawSource = "云闪付",
        )
    }
}
