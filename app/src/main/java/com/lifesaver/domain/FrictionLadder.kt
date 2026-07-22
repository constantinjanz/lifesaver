package com.lifesaver.domain

import com.lifesaver.data.Strictness

/**
 * Escalating friction pause per app per day (PRD §3.2): 1st open 5s, 2nd 15s, 3rd+ 30s.
 * Resets at local midnight (the caller passes the day's open index). Strictness scales the
 * whole ladder; STANDARD matches the PRD exactly.
 */
object FrictionLadder {

    /** @param openIndex 1-based count of this app's opens today (this open included). */
    fun pauseSeconds(openIndex: Int, strictness: Strictness = Strictness.STANDARD): Int {
        val base = when {
            openIndex <= 1 -> 5
            openIndex == 2 -> 15
            else -> 30
        }
        return when (strictness) {
            Strictness.GENTLE -> maxOf(3, (base * 0.6).toInt())
            Strictness.STANDARD -> base
            Strictness.STRICT -> (base * 1.5).toInt()
        }
    }
}
