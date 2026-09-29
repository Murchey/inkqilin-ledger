package com.inkqilin.ledger.service.parser

/** 通知解析结果。 */
data class ParsedNotification(
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    val merchant: String,
    val rawSource: String,
)
