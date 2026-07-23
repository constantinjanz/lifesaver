package com.lifesaver.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifesaver.detection.DetectionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifesaver_settings")

/**
 * Persists all user settings (PRD §4: DataStore for settings, all local). Setters here write
 * the final value immediately. The 24h self-binding rule (§3.6) lives one layer up: loosening
 * changes are queued in pending_changes and only reach these setters when the worker applies them.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val BASELINE_START = longPreferencesKey("baseline_start_epoch_day")
        val BUDGETS = stringPreferencesKey("budgets_json")
        val ENABLED = stringSetPreferencesKey("enabled_apps")
        val STRICTNESS = stringPreferencesKey("strictness")
        val PLANS = stringPreferencesKey("if_then_plans_json")
        val REDIRECTS = stringPreferencesKey("redirect_apps_json")
        val PAUSED_UNTIL = longPreferencesKey("paused_until_ms")
        val FOCUS_AREA = stringPreferencesKey("weekly_focus_area")
        val FOCUS_TARGET = androidx.datastore.preferences.core.intPreferencesKey("weekly_focus_target")
        val FOCUS_WEEK = stringPreferencesKey("weekly_focus_week")
        val FREEZES = androidx.datastore.preferences.core.intPreferencesKey("banked_freezes")
        val DISMISSED = stringSetPreferencesKey("dismissed_patterns")
        val LAST_ALIVE = longPreferencesKey("last_alive_ms")
        val BLOCKED_WINDOWS = stringPreferencesKey("blocked_windows_json")
        val BUDDY_PAIRING = stringPreferencesKey("buddy_pairing_id")
        val BUDDY_LABEL = stringPreferencesKey("buddy_label")
        val REELS_LIMITS = stringPreferencesKey("reels_limits_json")
        val GRAYSCALE = booleanPreferencesKey("grayscale_on_reels")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            onboardingComplete = p[Keys.ONBOARDING] ?: false,
            baselineStartEpochDay = p[Keys.BASELINE_START] ?: -1,
            budgetMinByApp = Settings.budgetsFromJson(p[Keys.BUDGETS]).ifEmpty {
                mapOf(DetectionConfig.INSTAGRAM to 30, DetectionConfig.YOUTUBE to 30)
            },
            reelsLimitMinByApp = Settings.budgetsFromJson(p[Keys.REELS_LIMITS]),
            enabledApps = p[Keys.ENABLED] ?: DetectionConfig.targetPackages,
            strictness = runCatching { Strictness.valueOf(p[Keys.STRICTNESS] ?: "STANDARD") }
                .getOrDefault(Strictness.STANDARD),
            ifThenPlans = Settings.plansFromJson(p[Keys.PLANS]),
            redirectApps = Settings.redirectsFromJson(p[Keys.REDIRECTS]),
            pausedUntilMs = p[Keys.PAUSED_UNTIL] ?: 0,
            weeklyFocusArea = p[Keys.FOCUS_AREA],
            weeklyFocusTarget = p[Keys.FOCUS_TARGET] ?: 0,
            weeklyFocusWeekKey = p[Keys.FOCUS_WEEK] ?: "",
            bankedFreezes = p[Keys.FREEZES] ?: 0,
            dismissedPatterns = p[Keys.DISMISSED] ?: emptySet(),
            blockedWindows = Settings.windowsFromJson(p[Keys.BLOCKED_WINDOWS]),
            buddyPairingId = p[Keys.BUDDY_PAIRING],
            buddyLabel = p[Keys.BUDDY_LABEL],
            grayscaleOnReels = p[Keys.GRAYSCALE] ?: false,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setOnboardingComplete(value: Boolean) =
        edit { it[Keys.ONBOARDING] = value }

    suspend fun setBaselineStartEpochDay(epochDay: Long) =
        edit { it[Keys.BASELINE_START] = epochDay }

    suspend fun setBudgetMin(appId: String, minutes: Int) = edit { prefs ->
        val map = Settings.budgetsFromJson(prefs[Keys.BUDGETS]).toMutableMap()
        map[appId] = minutes.coerceIn(10, 120)
        prefs[Keys.BUDGETS] = Settings.budgetsToJson(map)
    }

    suspend fun setEnabledApps(apps: Set<String>) = edit { it[Keys.ENABLED] = apps }

    suspend fun setGrayscaleOnReels(on: Boolean) = edit { it[Keys.GRAYSCALE] = on }

    /** Set the fast-surface (Reels/Shorts) sub-limit in minutes (0 = fully blocked), or null to remove it. */
    suspend fun setReelsLimit(appId: String, minutes: Int?) = edit { prefs ->
        val map = Settings.budgetsFromJson(prefs[Keys.REELS_LIMITS]).toMutableMap()
        if (minutes == null) map.remove(appId) else map[appId] = minutes.coerceIn(0, 240)
        prefs[Keys.REELS_LIMITS] = Settings.budgetsToJson(map)
    }

    suspend fun setStrictness(value: Strictness) = edit { it[Keys.STRICTNESS] = value.name }

    suspend fun setIfThenPlans(plans: List<IfThenPlan>) =
        edit { it[Keys.PLANS] = Settings.plansToJson(plans) }

    suspend fun setRedirectApps(apps: List<RedirectApp>) =
        edit { it[Keys.REDIRECTS] = Settings.redirectsToJson(apps) }

    suspend fun setPausedUntil(epochMs: Long) = edit { it[Keys.PAUSED_UNTIL] = epochMs }

    suspend fun setWeeklyFocus(area: String, target: Int, weekKey: String) = edit {
        it[Keys.FOCUS_AREA] = area
        it[Keys.FOCUS_TARGET] = target
        it[Keys.FOCUS_WEEK] = weekKey
    }

    suspend fun setBankedFreezes(count: Int) = edit { it[Keys.FREEZES] = count.coerceIn(0, 2) }

    suspend fun dismissPattern(id: String) = edit {
        it[Keys.DISMISSED] = (it[Keys.DISMISSED] ?: emptySet()) + id
    }

    suspend fun setLastAlive(epochMs: Long) = edit { it[Keys.LAST_ALIVE] = epochMs }

    suspend fun setBuddyPairing(pairingId: String?, label: String?) = edit {
        if (pairingId.isNullOrBlank()) { it.remove(Keys.BUDDY_PAIRING); it.remove(Keys.BUDDY_LABEL) }
        else { it[Keys.BUDDY_PAIRING] = pairingId; it[Keys.BUDDY_LABEL] = label ?: "your buddy" }
    }

    /** Set (or clear, if window is null) an app's daily hard-block window. */
    suspend fun setBlockedWindow(appId: String, window: com.lifesaver.domain.BlockWindow?) = edit { prefs ->
        val map = Settings.windowsFromJson(prefs[Keys.BLOCKED_WINDOWS]).toMutableMap()
        if (window == null || !window.enabled) map.remove(appId) else map[appId] = window
        prefs[Keys.BLOCKED_WINDOWS] = Settings.windowsToJson(map)
    }

    suspend fun lastAlive(): Long = context.dataStore.data.map { it[Keys.LAST_ALIVE] ?: 0L }.first()

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
