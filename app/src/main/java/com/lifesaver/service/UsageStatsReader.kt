package com.lifesaver.service

import android.app.usage.UsageStatsManager
import android.content.Context
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.domain.AppBaseline
import com.lifesaver.domain.BaselineInsight
import com.lifesaver.domain.DayKeys
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads foreground time per target app from UsageStatsManager (PRD §5). Used for (a) the M1
 * dashboard's "today's usage", (b) baseline measurement, and (c) daily reconciliation of the
 * accessibility-based accounting (§3.3). Requires the Usage Access permission.
 */
class UsageStatsReader(private val context: Context) {

    private val usm: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** Total foreground millis today (since local midnight) for [appId]. 0 if unavailable. */
    fun foregroundMsToday(appId: String, zone: ZoneId = ZoneId.systemDefault()): Long {
        val startOfDay = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        return foregroundMsBetween(appId, startOfDay, System.currentTimeMillis())
    }

    /** Foreground millis for [appId] in [startMs, endMs], summed from usage events. */
    fun foregroundMsBetween(appId: String, startMs: Long, endMs: Long): Long {
        val events = usm.queryEvents(startMs, endMs)
        val event = android.app.usage.UsageEvents.Event()
        var total = 0L
        var resumedAt = -1L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != appId) continue
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> resumedAt = event.timeStamp
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (resumedAt >= 0) {
                        total += (event.timeStamp - resumedAt).coerceAtLeast(0)
                        resumedAt = -1
                    }
                }
            }
        }
        // Still foreground at window end.
        if (resumedAt >= 0) total += (endMs - resumedAt).coerceAtLeast(0)
        return total
    }

    /** Convenience map for the two target apps today. */
    fun todayByApp(appIds: Collection<String>): Map<String, Long> =
        appIds.associateWith { foregroundMsToday(it) }

    fun dayKeyNow(): String = DayKeys.todayKey()

    /**
     * Builds a baseline + insight picture from the system's own usage history. Daily totals (up to
     * [daysBack]) drive the weekday/weekend averages; recent event history (whatever the system
     * retained, ~a week) drives the hour-of-day risk windows. Call off the main thread.
     */
    fun computeBaseline(daysBack: Int = 120, zone: ZoneId = ZoneId.systemDefault()): BaselineInsight {
        val targets = DetectionConfig.targetPackages
        val now = System.currentTimeMillis()
        val start = now - daysBack.toLong() * 24 * 60 * 60 * 1000

        // --- Daily totals -> weekday/weekend averages ---
        val stats = runCatching {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
        }.getOrNull().orEmpty()

        val perAppPerDay = HashMap<String, HashMap<LocalDate, Long>>()
        val allDates = HashSet<LocalDate>()
        for (us in stats) {
            val date = Instant.ofEpochMilli(us.firstTimeStamp).atZone(zone).toLocalDate()
            allDates.add(date)
            val pkg = us.packageName
            if (pkg in targets && us.totalTimeInForeground > 0) {
                perAppPerDay.getOrPut(pkg) { HashMap() }.merge(date, us.totalTimeInForeground, Long::plus)
            }
        }
        val weekdayDates = allDates.count { !isWeekend(it) }.coerceAtLeast(1)
        val weekendDates = allDates.count { isWeekend(it) }.coerceAtLeast(1)
        val totalDays = allDates.size.coerceAtLeast(1)

        val perApp = targets.associateWith { app ->
            val byDay = perAppPerDay[app] ?: emptyMap()
            val weekdaySum = byDay.entries.filter { !isWeekend(it.key) }.sumOf { it.value }
            val weekendSum = byDay.entries.filter { isWeekend(it.key) }.sumOf { it.value }
            val overall = byDay.values.sum()
            AppBaseline(
                appId = app,
                weekdayAvgMs = weekdaySum / weekdayDates,
                weekendAvgMs = weekendSum / weekendDates,
                overallAvgMs = overall / totalDays,
            )
        }

        // --- Recent events -> hour histogram, late-night, morning-first ---
        val hourMs = LongArray(24)
        val lateNightByDate = HashMap<LocalDate, Long>()
        val morningDates = HashSet<LocalDate>()
        val eventDates = HashSet<LocalDate>()
        val events = usm.queryEvents(start, now)
        val ev = android.app.usage.UsageEvents.Event()
        val resumedAt = HashMap<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.packageName !in targets) continue
            when (ev.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> resumedAt[ev.packageName] = ev.timeStamp
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val began = resumedAt.remove(ev.packageName) ?: continue
                    val span = (ev.timeStamp - began).coerceAtLeast(0)
                    if (span <= 0) continue
                    val zdt = Instant.ofEpochMilli(began).atZone(zone)
                    val date = zdt.toLocalDate()
                    val hour = zdt.hour
                    hourMs[hour] += span
                    eventDates.add(date)
                    if (hour < 4) lateNightByDate.merge(date, span, Long::plus)
                    if (hour in 5..8) morningDates.add(date)
                }
            }
        }
        val riskHours = hourMs.indices.sortedByDescending { hourMs[it] }
            .filter { hourMs[it] > 0 }.take(2)
        val eventDayCount = eventDates.size.coerceAtLeast(1)
        val lateNightPerDay = lateNightByDate.values.sum() / eventDayCount

        return BaselineInsight(
            perApp = perApp,
            daysObserved = allDates.size,
            riskHours = riskHours,
            lateNightMsPerDay = lateNightPerDay,
            morningFirstDays = morningDates.size,
            eventDays = eventDates.size,
        )
    }

    private fun isWeekend(d: LocalDate): Boolean =
        d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
}
