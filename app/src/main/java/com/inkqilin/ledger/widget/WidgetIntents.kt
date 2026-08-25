package com.inkqilin.ledger.widget

import android.net.Uri

/** 小部件相关 Intent 常量与导航目标构建 */
object WidgetIntents {
    const val ACTION_WIDGET_NAV = "com.inkqilin.ledger.widget.NAV"
    const val ACTION_WIDGET_REFRESH = "com.inkqilin.ledger.widget.REFRESH"
    const val EXTRA_NAV_TARGET = "nav_target"

    const val TARGET_HOME = "main"
    const val TARGET_SETTINGS = "settings"
    const val TARGET_ADD = "add_transaction"
    const val TARGET_CYCLE_LIST = "cycle_bill_list"

    /** 快捷记账 → 预填分类/类型的记一笔导航串 */
    fun addWithCategory(category: String, type: String = "EXPENSE"): String =
        "add_transaction?category=${Uri.encode(category)}&type=$type"

    /** 跳转周期账单编辑页（billId=0 时新建） */
    fun cycleEdit(billId: Long): String = "cycle_bill_edit/$billId"

    const val TARGET_CALCULATOR_HUB = "calculator_hub"

    /** 多功能计算器：直接定位到具体工具（dca / compound_interest / income_tax / savings_goal / installment / math_docs） */
    fun calculator(type: String): String = "calculator/$type"
}