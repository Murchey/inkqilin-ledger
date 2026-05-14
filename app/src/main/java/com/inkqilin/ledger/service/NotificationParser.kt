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

    fun parse(pkg: String, title: String, text: String): ParsedNotification? {
        Log.d("NotificationParser", "Parsing: pkg=$pkg, title=$title, text=$text")
        return when (pkg) {
            "com.eg.android.AlipayGphone", "com.android.shell" -> parseAlipay(title, text)
            "com.tencent.mm" -> parseWeChat(title, text)
            else -> parseGeneric(title, text)
        }
    }

    private fun parseAlipay(title: String, text: String): ParsedNotification? {
        val fullText = "$title$text"
        Log.d("NotificationParser", "Alipay fullText: $fullText")
        
        // 收入: "到账", "收款", "收到", "转入"
        val incomePattern = Regex("""(?:到账|收款|收到|转入)\s*[¥￥]?\s*(\d+(?:\.\d{1,2})?)""")
        incomePattern.find(fullText)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            Log.d("NotificationParser", "Alipay matched income: $amount")
            return ParsedNotification(
                amount = amount,
                isIncome = true,
                category = inferCategory(fullText),
                merchant = extractMerchant(fullText),
                rawSource = "支付宝"
            )
        }

        // 支出: "消费", "支付", "付款", "扣款", "支出"
        val expensePattern = Regex("""(?:消费|支付|付款|扣款|支出)\s*[¥￥]?\s*(\d+(?:\.\d{1,2})?)""")
        expensePattern.find(fullText)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            Log.d("NotificationParser", "Alipay matched expense: $amount")
            return ParsedNotification(
                amount = amount,
                isIncome = false,
                category = inferCategory(fullText),
                merchant = extractMerchant(fullText),
                rawSource = "支付宝"
            )
        }
        return null
    }

    private fun parseWeChat(title: String, text: String): ParsedNotification? {
        val fullText = "$title $text"
        Log.d("NotificationParser", "WeChat fullText: $fullText")
        
        // 微信支付通常格式: "微信支付: 商家收款0.01元" 或 "微信支付: 付款10.00元"
        val incomePattern = Regex("""(?:收款|收到|到账)\s*[¥￥]?\s*(\d+(?:\.\d{1,2})?)""")
        incomePattern.find(fullText)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            Log.d("NotificationParser", "WeChat matched income: $amount")
            return ParsedNotification(
                amount = amount,
                isIncome = true,
                category = inferCategory(fullText),
                merchant = extractMerchant(fullText),
                rawSource = "微信支付"
            )
        }

        val expensePattern = Regex("""(?:付款|支付|消费|支出)\s*[¥￥]?\s*(\d+(?:\.\d{1,2})?)""")
        expensePattern.find(fullText)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            Log.d("NotificationParser", "WeChat matched expense: $amount")
            return ParsedNotification(
                amount = amount,
                isIncome = false,
                category = inferCategory(fullText),
                merchant = extractMerchant(fullText),
                rawSource = "微信支付"
            )
        }
        return null
    }

    private fun parseGeneric(title: String, text: String): ParsedNotification? {
        val fullText = "$title $text"
        Log.d("NotificationParser", "Generic fullText: $fullText")
        
        val amountPattern = Regex("""[¥￥]\s*(\d+(?:\.\d{1,2})?)""")
        amountPattern.find(fullText)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            Log.d("NotificationParser", "Generic matched amount: $amount")
            val isIncome = fullText.contains("到账") || fullText.contains("收款")
            return ParsedNotification(
                amount = amount,
                isIncome = isIncome,
                category = inferCategory(fullText),
                merchant = extractMerchant(fullText),
                rawSource = "系统通知"
            )
        }
        return null
    }

    private fun inferCategory(text: String): String {
        return when {
            text.containsAny("餐饮", "美食", "外卖", "饿了么", "美团", "餐厅", "咖啡", "奶茶", "肯德基", "麦当劳") -> "餐饮"
            text.containsAny("交通", "公交", "地铁", "滴滴", "出租车", "加油", "高铁", "机票", "12306", "哈啰") -> "交通"
            text.containsAny("超市", "购物", "商场", "淘宝", "京东", "拼多多", "便利店", "天猫", "盒马", "唯品会") -> "购物"
            text.containsAny("娱乐", "电影", "游戏", "KTV", "视频", "音乐", "网易云", "腾讯视频", "爱奇艺", "B站") -> "娱乐"
            text.containsAny("酒店", "房租", "水电", "物业", "煤气", "自来水", "国家电网") -> "居住"
            text.containsAny("工资", "奖金", "薪水", "转账", "分红") -> "工资"
            text.containsAny("理财", "基金", "股票", "收益", "利息", "余额宝") -> "理财"
            else -> "其他"
        }
    }

    private fun extractMerchant(text: String): String {
        // 简单提取，通常商户名在“向”和“支付”之间，或者在开头
        val merchantPattern = Regex("""(?:向|从|于)\s*(.*?)\s*(?:支付|收款|消费|付款)""")
        return merchantPattern.find(text)?.groupValues?.get(1)?.take(10) ?: "未知商户"
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}
