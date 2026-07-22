package com.lifesaver.domain

/** Per-app baseline derived from the phone's own usage history (weekday/weekend split). */
data class AppBaseline(
    val appId: String,
    val weekdayAvgMs: Long,
    val weekendAvgMs: Long,
    val overallAvgMs: Long,
)

/**
 * A personalized picture built from UsageStatsManager history at onboarding (replaces the 2-day
 * observation, PRD §3.1). The daily averages seed the baseline; the event-derived insights
 * (risk hours, late-night, morning-first) make onboarding a mirror rather than a form.
 */
data class BaselineInsight(
    val perApp: Map<String, AppBaseline>,
    /** Days of daily-history the system retained and we averaged over. */
    val daysObserved: Int,
    /** Top hours of day (0–23) by target-app time, from recent event history. */
    val riskHours: List<Int>,
    /** Average after-midnight (00:00–04:00) target-app minutes per day, recent. */
    val lateNightMsPerDay: Long,
    /** Of the recent days with data, how many had a target opened before 9am. */
    val morningFirstDays: Int,
    /** Days of event-level history available (typically ≤ ~7). */
    val eventDays: Int,
) {
    fun overallDailyMs(): Long = perApp.values.sumOf { it.overallAvgMs }
    fun heaviestApp(): String? = perApp.maxByOrNull { it.value.overallAvgMs }?.key

    companion object {
        val EMPTY = BaselineInsight(emptyMap(), 0, emptyList(), 0, 0, 0)
    }
}
