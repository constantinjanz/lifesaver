package com.lifesaver.domain

import kotlin.math.roundToInt

/**
 * Substitution rate (PRD §3.7): the % of interventions that ended in a redirect or micro-action
 * instead of "Continue". A primary success metric. "blocked" screens are not interventions and
 * are excluded from the denominator by the DAO query.
 */
object SubstitutionRate {

    fun fraction(substitutions: Int, totalInterventions: Int): Float {
        if (totalInterventions <= 0) return 0f
        return (substitutions.toFloat() / totalInterventions).coerceIn(0f, 1f)
    }

    fun percent(substitutions: Int, totalInterventions: Int): Int =
        (fraction(substitutions, totalInterventions) * 100).roundToInt()
}
