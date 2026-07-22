package com.lifesaver.domain

import com.lifesaver.data.DailyStatus

/**
 * Streaks (PRD §3.7): a day is a success if total target-app time stayed within budget AND no
 * emergency unlock was used (§3.5) AND no >10min tracking gap (§9.2 — enforced when the status
 * row is written). Streak = consecutive success days. We report current and longest.
 *
 * `statuses` may be in any order; only finalized past days should be passed (an in-progress
 * "today" is excluded by the caller so a not-yet-failed day doesn't inflate the count).
 */
object StreakCalculator {

    data class Streaks(val current: Int, val longest: Int)

    fun compute(statuses: List<DailyStatus>): Streaks {
        if (statuses.isEmpty()) return Streaks(0, 0)
        val byDay = statuses.sortedBy { it.dayKey }
        var longest = 0
        var run = 0
        for (s in byDay) {
            if (s.success) {
                run += 1
                longest = maxOf(longest, run)
            } else {
                run = 0
            }
        }
        // Current streak = trailing run of successes from the most recent day backward,
        // but only if consecutive by calendar day (a gap in dates breaks it).
        var current = 0
        var expectedDay: String? = null
        for (s in byDay.reversed()) {
            if (expectedDay != null && s.dayKey != expectedDay) break
            if (!s.success) break
            current += 1
            expectedDay = java.time.LocalDate.parse(s.dayKey).minusDays(1).toString()
        }
        return Streaks(current, longest)
    }
}
