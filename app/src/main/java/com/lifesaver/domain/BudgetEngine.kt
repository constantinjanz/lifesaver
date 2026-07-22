package com.lifesaver.domain

import com.lifesaver.data.DailyUsage

/**
 * Budget accounting (PRD §3.3). Reels/Shorts time burns budget at 2x, the rest at 1x (§2, §3.4).
 * `reelsShortsMs` is the raw seconds spent on the 2x surface and is a SUBSET of `foregroundMs`,
 * so effective burn = (foreground - fast)*1 + fast*2 = foreground + fast.
 *
 * Pure functions only — no clock, no I/O — so this is fully unit-testable.
 */
object BudgetEngine {

    fun effectiveBurnMs(foregroundMs: Long, reelsShortsMs: Long): Long {
        val fast = reelsShortsMs.coerceIn(0, foregroundMs.coerceAtLeast(0))
        val normal = (foregroundMs - fast).coerceAtLeast(0)
        return normal + fast * 2
    }

    fun effectiveBurnMs(usage: DailyUsage): Long =
        effectiveBurnMs(usage.foregroundMs, usage.reelsShortsMs)

    fun remainingMs(budgetMs: Long, foregroundMs: Long, reelsShortsMs: Long): Long =
        (budgetMs - effectiveBurnMs(foregroundMs, reelsShortsMs)).coerceAtLeast(0)

    fun remainingMs(usage: DailyUsage): Long = remainingMs(usage.budgetMs, usage.foregroundMs, usage.reelsShortsMs)

    fun isExhausted(budgetMs: Long, foregroundMs: Long, reelsShortsMs: Long): Boolean =
        effectiveBurnMs(foregroundMs, reelsShortsMs) >= budgetMs && budgetMs > 0

    fun isExhausted(usage: DailyUsage): Boolean =
        isExhausted(usage.budgetMs, usage.foregroundMs, usage.reelsShortsMs)

    /** Whole minutes left, for the persistent notification and Continue button copy. */
    fun remainingMinutes(budgetMs: Long, foregroundMs: Long, reelsShortsMs: Long): Int =
        (remainingMs(budgetMs, foregroundMs, reelsShortsMs) / 60_000L).toInt()

    /** Fraction of budget REMAINING (1.0 full .. 0.0 empty) for the progress bar. */
    fun remainingFraction(budgetMs: Long, foregroundMs: Long, reelsShortsMs: Long): Float {
        if (budgetMs <= 0) return 0f
        return (remainingMs(budgetMs, foregroundMs, reelsShortsMs).toFloat() / budgetMs).coerceIn(0f, 1f)
    }
}
