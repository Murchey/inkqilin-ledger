package com.inkqilin.ledger.util

/** 自动备份频次 */
enum class BackupFrequency(val label: String) {
    OFF("关闭"),
    ON_APP_OPEN("打开 APP 时"),
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月")
}

/**
 * 自动备份计划：本地与云端各自一份。
 * @param weekday 周备份：1=周日 … 7=周六（Calendar.DAY_OF_WEEK）
 * @param dayOfMonth 月备份：1–31，超过当月天数则取月末
 * @param deletePreviousAuto 自动备份时删除上一次自动备份（不影响手动备份）
 * @param autoPassword 自动备份加密密钥；空则明文自动备份
 */
data class BackupSchedule(
    val frequency: BackupFrequency = BackupFrequency.OFF,
    val weekday: Int = 2,
    val dayOfMonth: Int = 1,
    val deletePreviousAuto: Boolean = true,
    val autoPassword: String = ""
) {
    fun encode(): String = listOf(
        frequency.name,
        weekday.toString(),
        dayOfMonth.toString(),
        if (deletePreviousAuto) "1" else "0",
        autoPassword.replace('|', '/')
    ).joinToString("|")

    companion object {
        fun decode(raw: String?): BackupSchedule {
            if (raw.isNullOrBlank()) return BackupSchedule()
            val parts = raw.split('|')
            val freq = BackupFrequency.entries.firstOrNull { it.name == parts.getOrNull(0) }
                ?: BackupFrequency.OFF
            return BackupSchedule(
                frequency = freq,
                weekday = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 7) ?: 2,
                dayOfMonth = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                deletePreviousAuto = parts.getOrNull(3) != "0",
                autoPassword = parts.getOrNull(4) ?: ""
            )
        }
    }
}

object BackupScheduleRules {
    /**
     * 当前时刻是否应执行该计划。
     * @param lastRun 上次执行时间戳（0=从未）；同一天内 ON_APP_OPEN/DAILY 不重复。
     */
    fun shouldRun(schedule: BackupSchedule, now: Long, lastRun: Long): Boolean {
        if (schedule.frequency == BackupFrequency.OFF) return false
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val last = java.util.Calendar.getInstance().apply { timeInMillis = lastRun }
        val sameDay = lastRun > 0L &&
            cal.get(java.util.Calendar.YEAR) == last.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == last.get(java.util.Calendar.DAY_OF_YEAR)

        return when (schedule.frequency) {
            BackupFrequency.OFF -> false
            BackupFrequency.ON_APP_OPEN -> !sameDay
            BackupFrequency.DAILY -> !sameDay
            BackupFrequency.WEEKLY ->
                cal.get(java.util.Calendar.DAY_OF_WEEK) == schedule.weekday && !sameDay
            BackupFrequency.MONTHLY -> {
                val dim = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val lastDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                dim == minOf(schedule.dayOfMonth, lastDay) && !sameDay
            }
        }
    }
}
