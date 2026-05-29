package com.inkqilin.ledger.service

import android.util.Log

object NotificationParser {

    data class ParsedNotification(
        val amount: Double,
        val isIncome: Boolean,
        val category: String,
        val merchant: String,
        val rawSource: String
    )

    // 核心金额正则：匹配 ¥ / ￥ 符号后的金额
    private val AMOUNT_REGEX = Regex("""[¥￥]\s*(\d+(?:\.\d{1,2})?)""")

    // 收入关键词（高置信度，命中几乎必定是真实收款）
    private val INCOME_KEYWORDS = setOf(
        "到账", "收款", "收到", "转入", "向你付款", "向你转账",
        "成功收款", "收钱码", "扫码向你付款", "向你支付",
        "已到账", "收款成功", "转账收款", "收红包"
    )

    // 支出关键词（中等置信度，需要排除广告误判）
    private val EXPENSE_KEYWORDS = setOf(
        "支付", "付款", "消费", "扣款", "支出", "成功支付",
        "代办", "购买", "缴费", "充值", "已支付", "已付款",
        "成功消费", "已扣款", "转账支出"
    )

    // 广告/营销关键词：如果文本包含这些，极大概率不是真实交易
    private val AD_KEYWORDS = setOf(
        "优惠", "立减", "立享", "领券", "优惠券", "满减", "折扣",
        "限时", "秒杀", "抢购", "特价", "促销", "返现",
        "到手价", "立即抢", "立即抢购", "限时抢", "爆款",
        "津贴", "凑单", "仅需", "新人专享", "会员专享"
    )

    // 强交易信号：如果文本包含这些，大概率是真实交易（即使有广告词也信任）
    private val STRONG_TRADE_SIGNALS = setOf(
        "成功支付", "付款成功", "支付成功", "交易成功",
        "到账通知", "收款通知", "收银台",
        "账单", "消费提醒", "交易提醒",
        "扣款通知", "余额变动", "银行通知",
        "动账通知", "账户变动", "资金变动"
    )

    fun parse(pkg: String, title: String, text: String): ParsedNotification? {
        // 统一处理：合并 title + text，替换换行为空格
        val rawText = "$title\n$text".replace("\n", " ").replace("\r", " ")
        Log.d("NotificationParser", "Parsing: pkg=$pkg, rawText=$rawText")

        val result = when (pkg) {
            "com.eg.android.AlipayGphone", "com.android.shell" -> parseAlipay(rawText)
            "com.tencent.mm" -> parseWeChat(rawText)
            "com.unionpay" -> parseUnionPay(rawText)
            else -> parseGeneric(rawText)
        }

        // 过滤0元账单
        if (result != null && result.amount <= 0.0) {
            Log.d("NotificationParser", "Filtered zero-amount transaction: ${result.amount}")
            return null
        }

        return result
    }

    // ──────────────────────────────────────────────
    //  支付宝解析（适配扫码、到账、碰一下、转账等）
    // ──────────────────────────────────────────────
    private fun parseAlipay(text: String): ParsedNotification? {
        Log.d("NotificationParser", "Alipay fullText: $text")

        // 第 1 层：碰一下/碰一碰 专用匹配（强制支出）
        parseTapToPay(text)?.let { return it }

        // 第 2 层：付款成功专用匹配（标题为"付款成功"的情况）
        parsePaymentSuccess(text)?.let { return it }

        // 第 3 层：收入匹配
        parseIncome(text)?.let { return it }

        // 第 4 层：支出匹配
        parseExpense(text)?.let { return it }

        // 第 5 层：仅包含金额 + 收/支关键词的兜底
        parseFallback(text)?.let { return it }

        Log.d("NotificationParser", "Alipay: no match found")
        return null
    }

    // ──────────────────────────────────────────────
    //  微信支付解析
    // ──────────────────────────────────────────────
    private fun parseWeChat(text: String): ParsedNotification? {
        Log.d("NotificationParser", "WeChat fullText: $text")

        // 第 1 层：收入匹配
        parseIncome(text)?.let { return it }

        // 第 2 层：支出匹配
        parseExpense(text)?.let { return it }

        // 第 3 层：兜底
        parseFallback(text)?.let { return it }

        Log.d("NotificationParser", "WeChat: no match found")
        return null
    }

    // ──────────────────────────────────────────────
    //  云闪付解析（适配消费、转账、收款等通知）
    // ──────────────────────────────────────────────
    private fun parseUnionPay(text: String): ParsedNotification? {
        Log.d("NotificationParser", "UnionPay fullText: $text")

        // 云闪付收入关键词
        val unionPayIncomeKeywords = setOf(
            "收款", "到账", "转入", "收到转账", "收钱"
        )

        // 云闪付支出关键词
        val unionPayExpenseKeywords = setOf(
            "消费", "支出", "扣款", "付款", "支付", "转出", "已扣",
            "成功消费", "成功支付", "成功付款", "交易成功"
        )

        // 强交易信号
        val unionPayStrongSignals = setOf(
            "消费成功", "支付成功", "付款成功", "交易成功",
            "扣款通知", "消费提醒", "交易提醒", "银行通知",
            "动账通知", "账户变动"
        )

        // 广告检测
        if (isAdvertisement(text) && !unionPayStrongSignals.any { text.contains(it) }) {
            Log.d("NotificationParser", "UnionPay: ad content detected")
            return null
        }

        val match = AMOUNT_REGEX.find(text)
        if (match == null) {
            // 尝试匹配不带符号的金额格式：数字.数字 元
            val altMatch = Regex("""(\d+(?:\.\d{1,2})?)\s*元""").find(text)
            if (altMatch != null) {
                val amount = altMatch.groupValues[1].toDoubleOrNull() ?: return null
                return parseUnionPayWithAmount(text, amount, unionPayIncomeKeywords, unionPayExpenseKeywords, unionPayStrongSignals)
            }
            Log.d("NotificationParser", "UnionPay: no amount found")
            return null
        }

        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        return parseUnionPayWithAmount(text, amount, unionPayIncomeKeywords, unionPayExpenseKeywords, unionPayStrongSignals)
    }

    private fun parseUnionPayWithAmount(
        text: String,
        amount: Double,
        incomeKeywords: Set<String>,
        expenseKeywords: Set<String>,
        strongSignals: Set<String>
    ): ParsedNotification? {
        val hasIncomeKeyword = incomeKeywords.any { text.contains(it) }
        val hasExpenseKeyword = expenseKeywords.any { text.contains(it) }
        val hasStrongSignal = strongSignals.any { text.contains(it) }

        // 判断收支类型
        val isIncome = when {
            hasIncomeKeyword && !hasExpenseKeyword -> true
            hasExpenseKeyword && !hasIncomeKeyword -> false
            hasStrongSignal -> false // 强交易信号默认为支出
            else -> false // 默认为支出
        }

        Log.d("NotificationParser", "UnionPay matched: amount=$amount, isIncome=$isIncome")
        return ParsedNotification(
            amount = amount,
            isIncome = isIncome,
            category = inferCategory(text),
            merchant = extractMerchantFallback(text),
            rawSource = "云闪付"
        )
    }

    // ──────────────────────────────────────────────
    //  通用解析（其他 App / 系统通知）
    // ──────────────────────────────────────────────
    private fun parseGeneric(text: String): ParsedNotification? {
        Log.d("NotificationParser", "Generic fullText: $text")

        parseIncome(text)?.let {
            return it.copy(rawSource = "系统通知")
        }
        parseExpense(text)?.let {
            return it.copy(rawSource = "系统通知")
        }
        parseFallback(text)?.let {
            return it.copy(rawSource = "系统通知")
        }
        return null
    }

    // ══════════════════════════════════════════════
    //  各层解析实现
    // ══════════════════════════════════════════════

    /**
     * 碰一下/碰一碰 强制识别为支出
     * 典型文本:
     *   "碰一下支付成功 ¥15.00"
     *   "碰一碰向 便利店 付款 ¥12.00"
     *   "碰一下 收款 ¥15.00" (虽然含"收款"，但碰一下只有支出)
     */
    private fun parseTapToPay(text: String): ParsedNotification? {
        if (!text.containsAny("碰一碰", "碰一下")) return null

        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        Log.d("NotificationParser", "Tap-to-pay matched: $amount")

        return ParsedNotification(
            amount = amount,
            isIncome = false,
            category = inferCategory(text),
            merchant = extractMerchantFallback(text),
            rawSource = "支付宝"
        )
    }

    /**
     * 付款成功专用匹配
     * 典型文本:
     *   标题: "付款成功"
     *   内容: "￥3.00 付款成功"
     *   标题: "付款成功￥3.00"
     *   内容: "你已成功付款3.00元"
     */
    private fun parsePaymentSuccess(text: String): ParsedNotification? {
        if (!text.containsAny("付款成功", "支付成功", "成功付款", "成功支付", "已付款", "已支付")) return null

        // 尝试多种金额格式
        val amountPatterns = listOf(
            AMOUNT_REGEX, // ¥15.00 或 ￥15.00
            Regex("""(\d+(?:\.\d{1,2})?)\s*元"""), // 15.00元
            Regex("""付款[^\d]*(\d+(?:\.\d{1,2})?)"""), // 付款3.00
            Regex("""支付[^\d]*(\d+(?:\.\d{1,2})?)"""), // 支付3.00
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

        // 广告检测（付款成功通常不是广告）
        if (isAdvertisement(text)) {
            Log.d("NotificationParser", "PaymentSuccess skipped: ad content detected")
            return null
        }

        Log.d("NotificationParser", "PaymentSuccess matched: $amount")
        return ParsedNotification(
            amount = amount,
            isIncome = false,
            category = inferCategory(text),
            merchant = extractMerchantFallback(text),
            rawSource = "支付宝"
        )
    }

    /**
     * 收入匹配层
     */
    private fun parseIncome(text: String): ParsedNotification? {
        val hasIncomeKeyword = INCOME_KEYWORDS.any { text.contains(it) }
        if (!hasIncomeKeyword) return null

        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null

        if (text.containsAny("碰一碰", "碰一下")) return null

        Log.d("NotificationParser", "Income matched: $amount")
        return ParsedNotification(
            amount = amount,
            isIncome = true,
            category = inferCategory(text),
            merchant = extractMerchantFallback(text),
            rawSource = "支付宝"
        )
    }

    /**
     * 支出匹配层 — 加入广告检测
     */
    private fun parseExpense(text: String): ParsedNotification? {
        val hasExpenseKeyword = EXPENSE_KEYWORDS.any { text.contains(it) }
        if (!hasExpenseKeyword) return null

        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null

        // ⚠️ 广告检测：如果文本是广告性质，跳过
        if (isAdvertisement(text)) {
            Log.d("NotificationParser", "Expense skipped: ad content detected")
            return null
        }

        Log.d("NotificationParser", "Expense matched: $amount")
        return ParsedNotification(
            amount = amount,
            isIncome = false,
            category = inferCategory(text),
            merchant = extractMerchantFallback(text),
            rawSource = "支付宝"
        )
    }

    /**
     * 兜底逻辑 — 加入广告检测
     */
    private fun parseFallback(text: String): ParsedNotification? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null

        // ⚠️ 广告检测：兜底层必须严格拦截
        if (isAdvertisement(text)) {
            Log.d("NotificationParser", "Fallback skipped: ad content detected")
            return null
        }

        val isIncome = INCOME_KEYWORDS.any { text.contains(it) }
        Log.d("NotificationParser", "Fallback matched: $amount, isIncome=$isIncome")
        return ParsedNotification(
            amount = amount,
            isIncome = isIncome,
            category = inferCategory(text),
            merchant = extractMerchantFallback(text),
            rawSource = "支付宝"
        )
    }

    // ══════════════════════════════════════════════
    //  辅助方法
    // ══════════════════════════════════════════════

    private fun inferCategory(text: String): String {
        return when {
            text.containsAny("餐饮", "美食", "外卖", "饿了么", "美团", "餐厅", "咖啡", "奶茶", "肯德基", "麦当劳", "星巴克", "瑞幸", "茶", "食堂") -> "餐饮"
            text.containsAny("交通", "公交", "地铁", "滴滴", "出租车", "加油", "高铁", "机票", "12306", "哈啰", "单车", "骑行", "油费") -> "交通"
            text.containsAny("超市", "购物", "商场", "淘宝", "京东", "拼多多", "便利店", "天猫", "盒马", "唯品会", "小米有品", "沃尔玛") -> "购物"
            text.containsAny("娱乐", "电影", "游戏", "KTV", "视频", "音乐", "网易云", "腾讯视频", "爱奇艺", "B站", "直播", "乐充") -> "娱乐"
            text.containsAny("酒店", "房租", "水电", "物业", "煤气", "自来水", "国家电网", "缴纳", "燃气", "供暖") -> "居住"
            text.containsAny("工资", "奖金", "薪水", "转账", "分红") -> "工资"
            text.containsAny("理财", "基金", "股票", "收益", "利息", "余额宝", "零钱通") -> "理财"
            text.containsAny("医疗", "医院", "药店", "体检", "挂号") -> "医疗"
            text.containsAny("教育", "培训", "学费", "书", "课程", "考试") -> "教育"
            else -> "其他"
        }
    }

    /**
     * 提取商户名
     * 优先从"于 XXX 支付" / "向 XXX 支付" / "到 XXX 支付" 等模式中提取
     */
    private fun extractMerchantFallback(text: String): String {
        val patterns = listOf(
            Regex("""(?:向|从|于|给)\s*(.*?)\s*(?:支付|收款|消费|付款|转账)"""),
            Regex("""(?:在|于)\s*(.*?)\s*(?:成功支付|消费|付款|花费)"""),
            Regex("""(?:扫码|扫)\s*(.*?)\s*(?:付|收款)"""),
            Regex("""(?:到|转账)\s*(.*?)\s*(?:的|的账户|到账)""")
        )
        for (pattern in patterns) {
            pattern.find(text)?.let {
                val name = it.groupValues[1].trim()
                if (name.isNotBlank() && name.length <= 20) {
                    return name.take(10)
                }
            }
        }

        // 如果没有任何模式匹配，尝试从开头截取第一个中文词
        val firstMerchant = Regex("""^[^\d¥￥]{2,10}?""").find(text)
        return firstMerchant?.value?.trim()?.take(10) ?: "未知商户"
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }

    /**
     * 广告检测：判断文本是否是营销/促销通知
     *
     * 规则：
     * 1. 包含强交易信号（"支付成功"、"到账通知"等）→ 信任为真实交易
     * 2. 包含广告关键词 → 判定为广告
     */
    private fun isAdvertisement(text: String): Boolean {
        // 如果有强交易信号，直接信任（"成功支付"比"优惠"权重高）
        if (STRONG_TRADE_SIGNALS.any { text.contains(it) }) {
            return false
        }
        // 检查广告关键词
        return AD_KEYWORDS.any { text.contains(it) }
    }
}
