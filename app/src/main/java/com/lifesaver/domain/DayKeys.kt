package com.lifesaver.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Local calendar keys for daily/weekly rollups. All resets happen at LOCAL midnight and
 * local Monday 00:00 (PRD §2, §3.5). Timezone changes are absorbed because keys are always
 * derived from the current system zone at read time — never stored as offsets.
 */
object DayKeys {

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun localDate(epochMs: Long, zone: ZoneId = zone()): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /** yyyy-MM-dd of the local day containing [epochMs]. */
    fun dayKey(epochMs: Long, zone: ZoneId = zone()): String = localDate(epochMs, zone).toString()

    fun todayKey(zone: ZoneId = zone()): String = LocalDate.now(zone).toString()

    /** weekday | weekend for baseline splitting (§3.1). */
    fun weekdayGroup(dayKey: String): String {
        val d = LocalDate.parse(dayKey).dayOfWeek
        return if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) "weekend" else "weekday"
    }

    /** The Monday date (yyyy-MM-dd) of the week containing [epochMs]; the weekly reset anchor. */
    fun weekKey(epochMs: Long, zone: ZoneId = zone()): String =
        localDate(epochMs, zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

    fun weekKeyOf(dayKey: String): String =
        LocalDate.parse(dayKey).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

    /** Epoch millis of the next local midnight strictly after [epochMs] — the daily reset time. */
    fun nextMidnightMs(epochMs: Long, zone: ZoneId = zone()): Long =
        localDate(epochMs, zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Whole days between two dayKeys (b - a). Negative if b precedes a. */
    fun daysBetween(aDayKey: String, bDayKey: String): Long =
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(aDayKey), LocalDate.parse(bDayKey))
}
