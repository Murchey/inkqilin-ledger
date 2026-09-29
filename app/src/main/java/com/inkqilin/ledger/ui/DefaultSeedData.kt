package com.inkqilin.ledger.ui

import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.KeywordCategory
import com.inkqilin.ledger.data.TransactionType

/** 首次启动种子数据（分类/币种/关键词），与业务表初始化解耦。 */
internal object DefaultSeedData {
    val categories = listOf(
        Category(name = "餐饮", icon = "🍜", type = TransactionType.EXPENSE),
        Category(name = "交通", icon = "🚗", type = TransactionType.EXPENSE),
        Category(name = "购物", icon = "🛒", type = TransactionType.EXPENSE),
        Category(name = "娱乐", icon = "🎮", type = TransactionType.EXPENSE),
        Category(name = "居住", icon = "🏠", type = TransactionType.EXPENSE),
        Category(name = "其他", icon = "📦", type = TransactionType.EXPENSE),
        Category(name = "工资", icon = "💰", type = TransactionType.INCOME),
        Category(name = "奖金", icon = "🎁", type = TransactionType.INCOME),
        Category(name = "理财", icon = "📈", type = TransactionType.INCOME),
        Category(name = "其他", icon = "📦", type = TransactionType.INCOME),
    )

    val currencies = listOf(
        CurrencyAsset(code = "CNY", symbol = "¥", name = "人民币", cardColor = "#43A047", isDefault = true),
        CurrencyAsset(code = "USD", symbol = "$", name = "美元", cardColor = "#1565C0"),
    )

    val keywordCategories = listOf(
        KeywordCategory(keyword = "外卖", categoryName = "餐饮"),
        KeywordCategory(keyword = "饿了么", categoryName = "餐饮"),
        KeywordCategory(keyword = "美团", categoryName = "餐饮"),
        KeywordCategory(keyword = "餐厅", categoryName = "餐饮"),
        KeywordCategory(keyword = "咖啡", categoryName = "餐饮"),
        KeywordCategory(keyword = "奶茶", categoryName = "餐饮"),
        KeywordCategory(keyword = "肯德基", categoryName = "餐饮"),
        KeywordCategory(keyword = "麦当劳", categoryName = "餐饮"),
        KeywordCategory(keyword = "星巴克", categoryName = "餐饮"),
        KeywordCategory(keyword = "瑞幸", categoryName = "餐饮"),
        KeywordCategory(keyword = "食堂", categoryName = "餐饮"),
        KeywordCategory(keyword = "公交", categoryName = "交通"),
        KeywordCategory(keyword = "地铁", categoryName = "交通"),
        KeywordCategory(keyword = "滴滴", categoryName = "交通"),
        KeywordCategory(keyword = "出租车", categoryName = "交通"),
        KeywordCategory(keyword = "加油", categoryName = "交通"),
        KeywordCategory(keyword = "高铁", categoryName = "交通"),
        KeywordCategory(keyword = "机票", categoryName = "交通"),
        KeywordCategory(keyword = "12306", categoryName = "交通"),
        KeywordCategory(keyword = "哈啰", categoryName = "交通"),
        KeywordCategory(keyword = "单车", categoryName = "交通"),
        KeywordCategory(keyword = "超市", categoryName = "购物"),
        KeywordCategory(keyword = "淘宝", categoryName = "购物"),
        KeywordCategory(keyword = "京东", categoryName = "购物"),
        KeywordCategory(keyword = "拼多多", categoryName = "购物"),
        KeywordCategory(keyword = "便利店", categoryName = "购物"),
        KeywordCategory(keyword = "天猫", categoryName = "购物"),
        KeywordCategory(keyword = "盒马", categoryName = "购物"),
        KeywordCategory(keyword = "唯品会", categoryName = "购物"),
        KeywordCategory(keyword = "沃尔玛", categoryName = "购物"),
        KeywordCategory(keyword = "电影", categoryName = "娱乐"),
        KeywordCategory(keyword = "游戏", categoryName = "娱乐"),
        KeywordCategory(keyword = "KTV", categoryName = "娱乐"),
        KeywordCategory(keyword = "网易云", categoryName = "娱乐"),
        KeywordCategory(keyword = "腾讯视频", categoryName = "娱乐"),
        KeywordCategory(keyword = "爱奇艺", categoryName = "娱乐"),
        KeywordCategory(keyword = "B站", categoryName = "娱乐"),
        KeywordCategory(keyword = "房租", categoryName = "居住"),
        KeywordCategory(keyword = "水电", categoryName = "居住"),
        KeywordCategory(keyword = "物业", categoryName = "居住"),
        KeywordCategory(keyword = "煤气", categoryName = "居住"),
        KeywordCategory(keyword = "燃气", categoryName = "居住"),
        KeywordCategory(keyword = "供暖", categoryName = "居住"),
        KeywordCategory(keyword = "薪水", categoryName = "工资"),
        KeywordCategory(keyword = "转账", categoryName = "工资"),
        KeywordCategory(keyword = "分红", categoryName = "工资"),
        KeywordCategory(keyword = "基金", categoryName = "理财"),
        KeywordCategory(keyword = "股票", categoryName = "理财"),
        KeywordCategory(keyword = "收益", categoryName = "理财"),
        KeywordCategory(keyword = "利息", categoryName = "理财"),
        KeywordCategory(keyword = "余额宝", categoryName = "理财"),
        KeywordCategory(keyword = "零钱通", categoryName = "理财"),
    )
}
