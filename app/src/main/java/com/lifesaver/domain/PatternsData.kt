package com.lifesaver.domain

/** Derived usage patterns for the Patterns screen (§9.1). heatmap[weekday 0=Mon][hour 0-23] = minutes. */
data class PatternsData(
    val heatmap: List<List<Int>>,
    val reelsSharePct: Int,
    val postMidnightMinPerDay: Int,
    val firstTouchDays: Int,
    val riskWindows: List<String>,
    val daysCovered: Int,
) {
    companion object {
        val EMPTY = PatternsData(List(7) { List(24) { 0 } }, 0, 0, 0, emptyList(), 0)
    }
}

/** Weekly report aggregate (§9.5). Pure compute over existing tables. */
data class ReportData(
    val weekKey: String,
    val perApp: List<AppWeek>,
    val savedMs: Long,
    val substitutionPct: Int,
    val unlocks: List<UnlockLine>,
    val trackingGapMs: Long,
    val lifeMinutesByArea: Map<String, Int>,
    val focusText: String?,
    val checkin: CheckinValues?,
    val intentions: List<Pair<String, Int>> = emptyList(),
) {
    data class AppWeek(val label: String, val actualMs: Long, val baselineMs: Long)
    data class UnlockLine(val reason: String, val dayKey: String)
    data class CheckinValues(val control: Int, val satisfaction: Int, val impulse: Int)

    companion object {
        val EMPTY = ReportData("", emptyList(), 0, 0, emptyList(), 0, emptyMap(), null, null)
    }
}
