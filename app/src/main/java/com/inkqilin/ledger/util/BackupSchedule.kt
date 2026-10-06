package com.inkqilin.ledger.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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
    /**
     * 仅序列化计划本身。自动备份口令由 ThemeManager 写入 Android Keystore，
     * 这里保留空字段以兼容旧版的五段格式；绝不把运行时口令写回字符串。
     */
    fun encode(): String = listOf(
        frequency.name,
        weekday.toString(),
        dayOfMonth.toString(),
        if (deletePreviousAuto) "1" else "0",
        ""
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
     *
     * [lastRun] 只应记录上一次成功完成的备份。对于周/月计划，如果 Worker
     * 错过了目标日期，只要当前日期已经越过下一次计划点，就会补做一份最新快照。
     * @param lastRun 上次成功执行时间戳（0=从未）
     * @param zoneId 用于日期边界判断的时区，默认使用系统时区
     */
    fun shouldRun(
        schedule: BackupSchedule,
        now: Long,
        lastRun: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (schedule.frequency == BackupFrequency.OFF) return false
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val lastDate = lastRun.takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }

        return when (schedule.frequency) {
            BackupFrequency.OFF -> false
            BackupFrequency.ON_APP_OPEN,
            BackupFrequency.DAILY -> lastDate == null || today.isAfter(lastDate)

            BackupFrequency.WEEKLY -> {
                val next = if (lastDate == null) {
                    weeklyDateOnOrAfter(today, schedule.weekday)
                        .takeIf { it == today }
                } else {
                    weeklyDateOnOrAfter(lastDate.plusDays(1), schedule.weekday)
                }
                next != null && !today.isBefore(next)
            }

            BackupFrequency.MONTHLY -> {
                val next = if (lastDate == null) {
                    monthlyDateOnOrAfter(today, schedule.dayOfMonth)
                        .takeIf { it == today }
                } else {
                    monthlyDateOnOrAfter(lastDate.plusDays(1), schedule.dayOfMonth)
                }
                next != null && !today.isBefore(next)
            }
        }
    }

    /** 返回从 [start] 开始（含当天）的下一个目标星期。 */
    fun weeklyDateOnOrAfter(start: LocalDate, weekday: Int): LocalDate {
        val target = weekday.coerceIn(1, 7)
        var date = start
        while (calendarWeekday(date.dayOfWeek) != target) {
            date = date.plusDays(1)
        }
        return date
    }

    /** 返回从 [start] 开始（含当天）的下一个目标月日，31 日按月末执行。 */
    fun monthlyDateOnOrAfter(start: LocalDate, dayOfMonth: Int): LocalDate {
        val targetDay = dayOfMonth.coerceIn(1, 31)
        var month = YearMonth.from(start)
        while (true) {
            val candidate = month.atDay(minOf(targetDay, month.lengthOfMonth()))
            if (!candidate.isBefore(start)) return candidate
            month = month.plusMonths(1)
        }
    }

    /** Calendar.DAY_OF_WEEK 兼容映射：1=周日 … 7=周六。 */
    private fun calendarWeekday(dayOfWeek: DayOfWeek): Int =
        (dayOfWeek.value % 7) + 1
}
