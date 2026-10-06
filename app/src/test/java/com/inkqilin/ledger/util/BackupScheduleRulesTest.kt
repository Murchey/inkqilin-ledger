package com.inkqilin.ledger.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BackupScheduleRulesTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun epoch(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun dailyRunsAfterCrossingToNextDate() {
        val schedule = BackupSchedule(BackupFrequency.DAILY)
        assertFalse(BackupScheduleRules.shouldRun(schedule, epoch(LocalDate.of(2026, 1, 2)), epoch(LocalDate.of(2026, 1, 2)), zone))
        assertTrue(BackupScheduleRules.shouldRun(schedule, epoch(LocalDate.of(2026, 1, 3)), epoch(LocalDate.of(2026, 1, 2)), zone))
    }

    @Test
    fun weeklyMissedTargetIsBackfilledOnce() {
        val schedule = BackupSchedule(BackupFrequency.WEEKLY, weekday = 2) // Monday
        val last = epoch(LocalDate.of(2026, 1, 5))
        assertTrue(BackupScheduleRules.shouldRun(schedule, epoch(LocalDate.of(2026, 1, 14)), last, zone))
        assertFalse(BackupScheduleRules.shouldRun(schedule, epoch(LocalDate.of(2026, 1, 14)), epoch(LocalDate.of(2026, 1, 14)), zone))
    }

    @Test
    fun monthlyDay31UsesEndOfShortMonth() {
        val schedule = BackupSchedule(BackupFrequency.MONTHLY, dayOfMonth = 31)
        val last = epoch(LocalDate.of(2026, 1, 31))
        assertTrue(BackupScheduleRules.shouldRun(schedule, epoch(LocalDate.of(2026, 2, 28)), last, zone))
        assertFalse(BackupScheduleRules.shouldRun(schedule, epoch(LocalDate.of(2026, 2, 28)), epoch(LocalDate.of(2026, 2, 28)), zone))
    }

    @Test
    fun monthlyDay31UsesLeapDayAndThirtyDayMonthEnds() {
        val schedule = BackupSchedule(BackupFrequency.MONTHLY, dayOfMonth = 31)
        val january = epoch(LocalDate.of(2028, 1, 31))
        assertTrue(BackupScheduleRules.shouldRun(schedule, epoch(LocalDate.of(2028, 2, 29)), january, zone))
        val march = epoch(LocalDate.of(2028, 3, 31))
        assertTrue(BackupScheduleRules.shouldRun(schedule, epoch(LocalDate.of(2028, 4, 30)), march, zone))
    }

    @Test
    fun appOpenRunsOnlyOncePerNaturalDay() {
        val schedule = BackupSchedule(BackupFrequency.ON_APP_OPEN)
        val date = LocalDate.of(2026, 3, 8)
        val morning = epoch(date)
        assertFalse(BackupScheduleRules.shouldRun(schedule, morning + 60_000, morning, zone))
        assertTrue(BackupScheduleRules.shouldRun(schedule, epoch(date.plusDays(1)), morning, zone))
    }

    @Test
    fun calendarWeekdayMapsSundayToOneAndSaturdayToSeven() {
        val schedule = BackupSchedule(BackupFrequency.WEEKLY, weekday = 1)
        val sunday = LocalDate.of(2026, 3, 8)
        assertTrue(BackupScheduleRules.shouldRun(schedule, epoch(sunday), 0L, zone))
        assertFalse(BackupScheduleRules.shouldRun(schedule, epoch(sunday.plusDays(1)), 0L, zone))
    }

    @Test
    fun legacyScheduleCanBeReadButNewEncodingOmitsPassword() {
        val legacy = BackupSchedule.decode("DAILY|2|1|1|old-secret")
        assertTrue(legacy.autoPassword == "old-secret")
        val encoded = BackupSchedule(
            frequency = BackupFrequency.DAILY,
            autoPassword = "new-secret"
        ).encode()
        assertFalse(encoded.contains("new-secret"))
        assertTrue(BackupSchedule.decode(encoded).autoPassword.isEmpty())
    }
}
