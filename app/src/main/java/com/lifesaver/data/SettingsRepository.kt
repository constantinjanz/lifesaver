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
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            onboardingComplete = p[Keys.ONBOARDING] ?: false,
            baselineStartEpochDay = p[Keys.BASELINE_START] ?: -1,
            budgetMinByApp = Settings.budgetsFromJson(p[Keys.BUDGETS]).ifEmpty {
                mapOf(DetectionConfig.INSTAGRAM to 30, DetectionConfig.YOUTUBE to 30)
            },
            enabledApps = p[Keys.ENABLED] ?: DetectionConfig.targetPackages,
            strictness = runCatching { Strictness.valueOf(p[Keys.STRICTNESS] ?: "STANDARD") }
                .getOrDefault(Strictness.STANDARD),
            ifThenPlans = Settings.plansFromJson(p[Keys.PLANS]),
            redirectApps = Settings.redirectsFromJson(p[Keys.REDIRECTS]),
            pausedUntilMs = p[Keys.PAUSED_UNTIL] ?: 0,
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

    suspend fun setStrictness(value: Strictness) = edit { it[Keys.STRICTNESS] = value.name }

    suspend fun setIfThenPlans(plans: List<IfThenPlan>) =
        edit { it[Keys.PLANS] = Settings.plansToJson(plans) }

    suspend fun setRedirectApps(apps: List<RedirectApp>) =
        edit { it[Keys.REDIRECTS] = Settings.redirectsToJson(apps) }

    suspend fun setPausedUntil(epochMs: Long) = edit { it[Keys.PAUSED_UNTIL] = epochMs }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
