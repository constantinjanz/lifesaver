package com.lifesaver.service

import android.app.usage.UsageStatsManager
import android.content.Context
import com.lifesaver.domain.DayKeys
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
}
