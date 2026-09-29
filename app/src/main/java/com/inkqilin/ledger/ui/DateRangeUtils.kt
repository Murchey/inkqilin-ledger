package com.inkqilin.ledger.ui

import java.util.Calendar

/** 年/月时间范围工具。 */
internal object DateRangeUtils {
    fun GetYearRange(year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val end = cal.timeInMillis - 1
        return start to end
    }

    fun GetMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.MONTH, month + 1)
        val end = cal.timeInMillis - 1
        return start to end
    }
}
