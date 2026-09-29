package com.inkqilin.ledger.service.parser

import android.util.Log

/** 通知解析共用规则：过滤、金额抽取、分类、商户。 */
object NotificationParseSupport {

    fun ShouldSkip(text: String): Boolean = IsNonTransaction(text) || IsAdvertisement(text)

    fun IsNonTransaction(text: String): Boolean {
        if (NotificationKeywords.STRONG_TRADE_SIGNALS.any { text.contains(it) }) return false
        return NotificationKeywords.NON_TRANSACTION_INDICATORS.any { text.contains(it) }
    }

    fun IsAdvertisement(text: String): Boolean {
        if (NotificationKeywords.STRONG_TRADE_SIGNALS.any { text.contains(it) }) return false
        return NotificationKeywords.AD_KEYWORDS.any { text.contains(it) }
    }

    fun InferCategory(text: String): String {
        return when {
            ContainsAny(text, "餐饮", "美食", "外卖", "饿了么", "美团", "餐厅", "咖啡", "奶茶", "肯德基", "麦当劳", "星巴克", "瑞幸", "茶", "食堂") -> "餐饮"
            ContainsAny(text, "交通", "公交", "地铁", "滴滴", "出租车", "加油", "高铁", "机票", "12306", "哈啰", "单车", "骑行", "油费") -> "交通"
            ContainsAny(text, "超市", "购物", "商场", "淘宝", "京东", "拼多多", "便利店", "天猫", "盒马", "唯品会", "小米有品", "沃尔玛") -> "购物"
            ContainsAny(text, "娱乐", "电影", "游戏", "KTV", "视频", "音乐", "网易云", "腾讯视频", "爱奇艺", "B站", "直播", "乐充") -> "娱乐"
            ContainsAny(text, "酒店", "房租", "水电", "物业", "煤气", "自来水", "国家电网", "缴纳", "燃气", "供暖") -> "居住"
            ContainsAny(text, "工资", "奖金", "薪水", "转账", "分红") -> "工资"
            ContainsAny(text, "理财", "基金", "股票", "收益", "利息", "余额宝", "零钱通") -> "理财"
            ContainsAny(text, "医疗", "医院", "药店", "体检", "挂号") -> "医疗"
            ContainsAny(text, "教育", "培训", "学费", "书", "课程", "考试") -> "教育"
            else -> "其他"
        }
    }

    fun ExtractMerchantFallback(text: String): String {
        val patterns = listOf(
            Regex("""(?:向|从|于|给)\s*(.*?)\s*(?:支付|收款|消费|付款|转账)"""),
            Regex("""(?:在|于)\s*(.*?)\s*(?:成功支付|消费|付款|花费)"""),
            Regex("""(?:扫码|扫)\s*(.*?)\s*(?:付|收款)"""),
            Regex("""(?:到|转账)\s*(.*?)\s*(?:的|的账户|到账)"""),
        )
        for (pattern in patterns) {
            pattern.find(text)?.let {
                val name = it.groupValues[1].trim()
                if (name.isNotBlank() && name.length <= 20) {
                    return name.take(10)
                }
            }
        }
        val firstMerchant = Regex("""^[^\d¥￥]{2,10}?""").find(text)
        return firstMerchant?.value?.trim()?.take(10) ?: "未知商户"
    }

    fun ParseIncome(text: String, rawSource: String = "支付宝"): ParsedNotification? {
        val hasIncomeKeyword = NotificationKeywords.INCOME_KEYWORDS.any { text.contains(it) }
        if (!hasIncomeKeyword) return null

        val match = NotificationKeywords.AMOUNT_REGEX.find(text) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null

        if (ContainsAny(text, "碰一碰", "碰一下")) return null

        Log.d("NotificationParser", "Income matched: $amount")
        return ParsedNotification(
            amount = amount,
            isIncome = true,
            category = InferCategory(text),
            merchant = ExtractMerchantFallback(text),
            rawSource = rawSource,
        )
    }

    fun ParseExpense(text: String, rawSource: String = "支付宝"): ParsedNotification? {
        val hasExpenseKeyword = NotificationKeywords.EXPENSE_KEYWORDS.any { text.contains(it) }
        if (!hasExpenseKeyword) return null

        val amount = listOf(
            Regex("""付款[¥￥](\d+(?:\.\d{1,2})?)"""),
            NotificationKeywords.AMOUNT_REGEX,
        ).firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf { it > 0 }
        } ?: return null

        if (IsAdvertisement(text)) {
            Log.d("NotificationParser", "Expense skipped: ad content detected")
            return null
        }

        Log.d("NotificationParser", "Expense matched: $amount")
        return ParsedNotification(
            amount = amount,
            isIncome = false,
            category = InferCategory(text),
            merchant = ExtractMerchantFallback(text),
            rawSource = rawSource,
        )
    }

    fun ParseFallback(text: String, rawSource: String = "支付宝"): ParsedNotification? {
        val match = NotificationKeywords.AMOUNT_REGEX.find(text) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null

        val hasTradeKeyword = NotificationKeywords.INCOME_KEYWORDS.any { text.contains(it) } ||
            NotificationKeywords.EXPENSE_KEYWORDS.any { text.contains(it) }
        if (!hasTradeKeyword) {
            Log.d("NotificationParser", "Fallback skipped: no trade keyword, amount=$amount")
            return null
        }

        val isIncome = NotificationKeywords.INCOME_KEYWORDS.any { text.contains(it) }
        Log.d("NotificationParser", "Fallback matched: $amount, isIncome=$isIncome")
        return ParsedNotification(
            amount = amount,
            isIncome = isIncome,
            category = InferCategory(text),
            merchant = ExtractMerchantFallback(text),
            rawSource = rawSource,
        )
    }

    fun ParseTapToPay(text: String): ParsedNotification? {
        if (!ContainsAny(text, "碰一碰", "碰一下")) return null

        val match = NotificationKeywords.AMOUNT_REGEX.find(text) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        Log.d("NotificationParser", "Tap-to-pay matched: $amount")

        return ParsedNotification(
            amount = amount,
            isIncome = false,
            category = InferCategory(text),
            merchant = ExtractMerchantFallback(text),
            rawSource = "支付宝",
        )
    }

    fun ParsePaymentSuccess(text: String): ParsedNotification? {
        if (!ContainsAny(text, 
                "付款成功", "支付成功", "成功付款", "成功支付", "已付款", "已支付",
            )
        ) return null

        val amountPatterns = listOf(
            Regex("""付款[¥￥](\d+(?:\.\d{1,2})?)"""),
            NotificationKeywords.AMOUNT_REGEX,
            Regex("""(\d+(?:\.\d{1,2})?)\s*元"""),
            Regex("""付款[^\d]*(\d+(?:\.\d{1,2})?)"""),
            Regex("""支付[^\d]*(\d+(?:\.\d{1,2})?)"""),
        )

        var amount: Double? = null
        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                amount = match.groupValues[1].toDoubleOrNull()
                if (amount != null && amount > 0) break
            }
        }

        if (amount == null || amount <= 0) {
            Log.d("NotificationParser", "PaymentSuccess: no valid amount found")
            return null
        }

        if (IsAdvertisement(text)) {
            Log.d("NotificationParser", "PaymentSuccess skipped: ad content detected")
            return null
        }

        Log.d("NotificationParser", "PaymentSuccess matched: $amount")
        return ParsedNotification(
            amount = amount,
            isIncome = false,
            category = InferCategory(text),
            merchant = ExtractMerchantFallback(text),
            rawSource = "支付宝",
        )
    }

    fun ContainsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}
