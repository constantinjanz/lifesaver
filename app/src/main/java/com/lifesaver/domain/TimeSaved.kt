package com.lifesaver.domain

import kotlin.math.roundToInt

/**
 * Time saved = baseline usage minus actual usage, translated into tangibles (PRD §3.7).
 * Baseline comes from the observation phase, self-refining over the first 14 days then fixed
 * (see BaselineModel). Tangible conversions: book pages ~1.5 min, workouts 45 min, study 90 min.
 */
object TimeSaved {

    const val PAGE_MIN = 1.5
    const val WORKOUT_MIN = 45
    const val STUDY_MIN = 90

    /** Saved millis = max(0, baseline - actual). Never negative (§5 no negative budgets). */
    fun savedMs(baselineMs: Long, actualMs: Long): Long = (baselineMs - actualMs).coerceAtLeast(0)

    fun minutes(ms: Long): Int = (ms / 60_000L).toInt()

    /** "6H 20M" style, uppercase for the display card. */
    fun formatHm(ms: Long): String {
        val totalMin = minutes(ms)
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 && m > 0 -> "${h}H ${m}M"
            h > 0 -> "${h}H"
            else -> "${m}M"
        }
    }

    /** The single most motivating tangible for a given saved duration (PRD example: workouts). */
    fun tangible(ms: Long): String {
        val min = minutes(ms)
        if (min >= WORKOUT_MIN) {
            val n = (min.toDouble() / WORKOUT_MIN).roundToInt().coerceAtLeast(1)
            return "≈ $n ${if (n == 1) "WORKOUT" else "WORKOUTS"}"
        }
        val pages = (min / PAGE_MIN).roundToInt().coerceAtLeast(0)
        return "≈ $pages BOOK ${if (pages == 1) "PAGE" else "PAGES"}"
    }
}
