package com.lifesaver.domain

import com.lifesaver.data.IfThenPlan

/**
 * Picks the if-then plan whose context matches the current time of day (PRD §3.2 — the pause
 * quotes the user's own plan back at them). Contexts follow the onboarding suggestions:
 * "just_woke" (early morning), "evening" (after 21:00), "waiting" (the daytime default).
 */
object PlanMatcher {

    const val JUST_WOKE = "just_woke"
    const val EVENING = "evening"
    const val WAITING = "waiting"

    fun contextForHour(hour: Int): String = when {
        hour in 4..8 -> JUST_WOKE
        hour >= 21 || hour < 4 -> EVENING
        else -> WAITING
    }

    /** The best-matching plan text, or null if the user has no plans yet. */
    fun match(plans: List<IfThenPlan>, hour: Int): IfThenPlan? {
        if (plans.isEmpty()) return null
        val ctx = contextForHour(hour)
        return plans.firstOrNull { it.context == ctx }
            ?: plans.firstOrNull { it.context == WAITING }
            ?: plans.first()
    }
}
