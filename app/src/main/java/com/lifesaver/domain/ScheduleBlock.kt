package com.lifesaver.domain

/** A daily hard-block window for one app, in minutes since local midnight. Wraps midnight when
 *  startMinute > endMinute (e.g. 22:00–06:00). start == end means "disabled". */
data class BlockWindow(val startMinute: Int, val endMinute: Int) {
    val enabled: Boolean get() = startMinute != endMinute
}

/**
 * Scheduled hard-block (user feature): an app can be made fully inaccessible during a daily time
 * window regardless of budget or emergency unlocks. Pure so it's unit-tested.
 */
object ScheduleBlock {

    fun isBlocked(window: BlockWindow?, nowMinuteOfDay: Int): Boolean {
        if (window == null || !window.enabled) return false
        val m = ((nowMinuteOfDay % 1440) + 1440) % 1440
        return if (window.startMinute < window.endMinute) {
            m >= window.startMinute && m < window.endMinute
        } else {
            // Wraps past midnight.
            m >= window.startMinute || m < window.endMinute
        }
    }

    fun format(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
}
