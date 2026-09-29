package com.inkqilin.ledger.ui.screens

internal enum class ExportTimeRange(val label: String) {
    ALL("全部"),
    THIS_YEAR("本年"),
    CUSTOM("自定义")
}

internal enum class ExportFormat(val label: String) {
    EXCEL("Excel"),
    CSV("CSV")
}

/** 导出过程中的进度展示 */
internal sealed class ExportProgressState {
    data class Running(val fraction: Float, val message: String) : ExportProgressState()
}
