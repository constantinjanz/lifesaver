package com.lifesaver.data

import com.lifesaver.detection.DetectionConfig
import com.lifesaver.domain.BlockWindow
import org.json.JSONArray
import org.json.JSONObject

/** A user implementation intention (§3.1). context is a stable key; label is shown. */
data class IfThenPlan(
    val context: String, // evening | waiting | just_woke | custom:<id>
    val label: String, // e.g. "Evening (after 21:00)"
    val text: String, // "If I want to scroll ..., then I will ..."
) {
    fun toJson(): JSONObject = JSONObject()
        .put("context", context).put("label", label).put("text", text)

    companion object {
        fun fromJson(o: JSONObject) = IfThenPlan(
            o.optString("context"), o.optString("label"), o.optString("text"),
        )
    }
}

/** A chosen redirect target app (§3.1). */
data class RedirectApp(val appId: String, val label: String) {
    fun toJson(): JSONObject = JSONObject().put("appId", appId).put("label", label)
    companion object {
        fun fromJson(o: JSONObject) = RedirectApp(o.optString("appId"), o.optString("label"))
    }
}

enum class Strictness { GENTLE, STANDARD, STRICT }

/** Immutable snapshot of all user settings. */
data class Settings(
    val onboardingComplete: Boolean = false,
    /** Local epoch-day when the 2-day baseline observation began; -1 if not started. */
    val baselineStartEpochDay: Long = -1,
    val budgetMinByApp: Map<String, Int> = mapOf(
        DetectionConfig.INSTAGRAM to 30,
        DetectionConfig.YOUTUBE to 30,
    ),
    /** Optional per-app sub-budget for the 2x fast surface (Reels/Shorts). Key present = limited;
     *  value in minutes, 0 = fully blocked. Key absent = no separate limit. */
    val reelsLimitMinByApp: Map<String, Int> = emptyMap(),
    val enabledApps: Set<String> = DetectionConfig.targetPackages,
    val strictness: Strictness = Strictness.STANDARD,
    val ifThenPlans: List<IfThenPlan> = emptyList(),
    val redirectApps: List<RedirectApp> = emptyList(),
    /** Epoch millis until which all friction/blocks are lifted by an active emergency unlock (§3.5). */
    val pausedUntilMs: Long = 0,
    // v1.1 life layer (§9.4) + streak insurance (§9.6) + patterns (§9.1).
    val weeklyFocusArea: String? = null,
    val weeklyFocusTarget: Int = 0,
    val weeklyFocusWeekKey: String = "",
    val bankedFreezes: Int = 0,
    val dismissedPatterns: Set<String> = emptySet(),
    /** Per-app daily hard-block window (user feature: fully inaccessible during those hours). */
    val blockedWindows: Map<String, BlockWindow> = emptyMap(),
    // Buddy approval (Supabase-backed): the paired buddy grants extra time via WhatsApp + PIN.
    val buddyPairingId: String? = null,
    val buddyLabel: String? = null,
) {
    val buddyPaired: Boolean get() = !buddyPairingId.isNullOrBlank()
    fun budgetMin(appId: String): Int = budgetMinByApp[appId] ?: 30
    fun budgetMs(appId: String): Long = budgetMin(appId) * 60_000L
    fun reelsLimited(appId: String): Boolean = reelsLimitMinByApp.containsKey(appId)
    fun reelsLimitMin(appId: String): Int? = reelsLimitMinByApp[appId]
    fun reelsLimitMs(appId: String): Long = (reelsLimitMinByApp[appId] ?: Int.MAX_VALUE) * 60_000L
    fun reelsFullyBlocked(appId: String): Boolean = reelsLimitMinByApp[appId] == 0
    fun isPaused(nowMs: Long): Boolean = nowMs < pausedUntilMs
    fun blockedWindow(appId: String): BlockWindow? = blockedWindows[appId]

    companion object {
        fun plansToJson(plans: List<IfThenPlan>): String =
            JSONArray().apply { plans.forEach { put(it.toJson()) } }.toString()

        fun plansFromJson(s: String?): List<IfThenPlan> {
            if (s.isNullOrBlank()) return emptyList()
            val arr = runCatching { JSONArray(s) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { IfThenPlan.fromJson(it) }
            }
        }

        fun redirectsToJson(apps: List<RedirectApp>): String =
            JSONArray().apply { apps.forEach { put(it.toJson()) } }.toString()

        fun redirectsFromJson(s: String?): List<RedirectApp> {
            if (s.isNullOrBlank()) return emptyList()
            val arr = runCatching { JSONArray(s) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { RedirectApp.fromJson(it) }
            }
        }

        fun budgetsToJson(map: Map<String, Int>): String =
            JSONObject().apply { map.forEach { (k, v) -> put(k, v) } }.toString()

        fun budgetsFromJson(s: String?): Map<String, Int> {
            if (s.isNullOrBlank()) return emptyMap()
            val o = runCatching { JSONObject(s) }.getOrNull() ?: return emptyMap()
            return o.keys().asSequence().associateWith { o.optInt(it, 30) }
        }

        fun windowsToJson(map: Map<String, BlockWindow>): String =
            JSONObject().apply {
                map.forEach { (k, w) -> put(k, JSONObject().put("s", w.startMinute).put("e", w.endMinute)) }
            }.toString()

        fun windowsFromJson(s: String?): Map<String, BlockWindow> {
            if (s.isNullOrBlank()) return emptyMap()
            val o = runCatching { JSONObject(s) }.getOrNull() ?: return emptyMap()
            return o.keys().asSequence().mapNotNull { k ->
                val w = o.optJSONObject(k) ?: return@mapNotNull null
                k to BlockWindow(w.optInt("s"), w.optInt("e"))
            }.toMap()
        }
    }
}
