package com.lifesaver.domain

/** Time-of-day buckets for missions, so the pause suggests something you can actually do now. */
object MissionTime {
    const val MORNING = "morning"
    const val DAYTIME = "daytime"
    const val EVENING = "evening"
    const val NIGHT = "night"

    /** (contextKey, display label). Order matters for the editor. */
    val BUCKETS = listOf(
        MORNING to "Morning",
        DAYTIME to "Daytime",
        EVENING to "Evening",
        NIGHT to "Night",
    )

    fun bucketForHour(hour: Int): String = when (hour) {
        in 5..10 -> MORNING
        in 11..16 -> DAYTIME
        in 17..21 -> EVENING
        else -> NIGHT
    }

    fun label(context: String): String = BUCKETS.firstOrNull { it.first == context }?.second ?: context
}
