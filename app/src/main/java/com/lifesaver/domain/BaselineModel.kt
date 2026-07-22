package com.lifesaver.domain

import com.lifesaver.data.Baseline

/**
 * Self-refining usage baseline (PRD §3.1, §3.7). The 2-day observation phase seeds it; it then
 * keeps refining silently over the first 14 sample days (per weekday/weekend group), after which
 * it is fixed and only editable via the 24h rule. "Time saved" measures against this baseline.
 */
object BaselineModel {

    const val MAX_SAMPLE_DAYS = 14

    /** Running average that stops moving once MAX_SAMPLE_DAYS samples are in. */
    fun refine(prior: Baseline?, appId: String, group: String, dayEffectiveMs: Long): Baseline {
        if (prior == null) {
            return Baseline(appId, group, avgMs = dayEffectiveMs, sampleDays = 1)
        }
        if (prior.sampleDays >= MAX_SAMPLE_DAYS) return prior // fixed
        val n = prior.sampleDays
        val newAvg = (prior.avgMs * n + dayEffectiveMs) / (n + 1)
        return prior.copy(avgMs = newAvg, sampleDays = n + 1)
    }

    /** Baseline millis to compare a given day against, picking the matching weekday/weekend group. */
    fun baselineForDay(baselines: List<Baseline>, appId: String, dayKey: String): Long {
        val group = DayKeys.weekdayGroup(dayKey)
        return baselines.firstOrNull { it.appId == appId && it.weekdayGroup == group }?.avgMs ?: 0L
    }
}
