package com.lifesaver.domain

import com.lifesaver.data.Strictness

/**
 * The 24h self-binding rule (PRD §3.6). Loosening a rule (higher budget, gentler friction,
 * fewer target apps, weaker strictness) takes effect only 24h after confirmation. Tightening
 * takes effect immediately. This object only CLASSIFIES a change and computes the effective
 * time; the caller persists a pending_changes row for loosen, or applies immediately for tighten.
 */
object SelfBinding {

    const val DELAY_MS = 24L * 60 * 60 * 1000

    enum class Direction { LOOSEN, TIGHTEN, NEUTRAL }

    data class Decision(val direction: Direction, val effectiveTs: Long) {
        val isImmediate: Boolean get() = direction != Direction.LOOSEN
    }

    fun budgetChange(oldMin: Int, newMin: Int, nowMs: Long): Decision =
        decide(if (newMin > oldMin) Direction.LOOSEN else if (newMin < oldMin) Direction.TIGHTEN else Direction.NEUTRAL, nowMs)

    /** More strict = tighten; less strict = loosen. */
    fun strictnessChange(old: Strictness, new: Strictness, nowMs: Long): Decision {
        val dir = when {
            new.ordinal > old.ordinal -> Direction.TIGHTEN
            new.ordinal < old.ordinal -> Direction.LOOSEN
            else -> Direction.NEUTRAL
        }
        return decide(dir, nowMs)
    }

    /** Removing a target app or disabling detection is a loosening. Adding one is tightening. */
    fun targetAppsChange(oldApps: Set<String>, newApps: Set<String>, nowMs: Long): Decision {
        val removed = oldApps.any { it !in newApps }
        val added = newApps.any { it !in oldApps }
        val dir = when {
            removed && !added -> Direction.LOOSEN
            added && !removed -> Direction.TIGHTEN
            removed && added -> Direction.LOOSEN // net effect of dropping protection wins
            else -> Direction.NEUTRAL
        }
        return decide(dir, nowMs)
    }

    private fun decide(dir: Direction, nowMs: Long): Decision =
        Decision(dir, if (dir == Direction.LOOSEN) nowMs + DELAY_MS else nowMs)
}
