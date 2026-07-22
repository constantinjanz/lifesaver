package com.lifesaver.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifesaver.LifesaverApp
import com.lifesaver.data.DailyUsage
import com.lifesaver.data.IfThenPlan
import com.lifesaver.data.RedirectApp
import com.lifesaver.data.Settings
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.domain.StreakCalculator
import com.lifesaver.domain.SubstitutionRate
import com.lifesaver.service.Permissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which enforcement phase the app is in, for the dashboard banner (§3.1). */
sealed interface Phase {
    data object NotOnboarded : Phase
    data class Baseline(val day: Int, val total: Int) : Phase
    data object Active : Phase
}

data class DashboardState(
    val hydrated: Boolean = false,
    val settings: Settings = Settings(),
    val permissions: Map<Permissions.Kind, Boolean> = emptyMap(),
    val phase: Phase = Phase.NotOnboarded,
    val todayUsageMsByApp: Map<String, Long> = emptyMap(),
    val todayRoomUsage: List<DailyUsage> = emptyList(),
    val streaks: StreakCalculator.Streaks = StreakCalculator.Streaks(0, 0),
    val totalInterventions: Int = 0,
    val substitutionPercent: Int = 0,
)

class LifesaverViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as LifesaverApp).container
    private val db = container.database

    private val permissionsFlow = MutableStateFlow<Map<Permissions.Kind, Boolean>>(emptyMap())
    private val usageStatsFlow = MutableStateFlow<Map<String, Long>>(emptyMap())

    val state: StateFlow<DashboardState> = combine(
        container.settings.settings,
        db.statusDao().all(),
        db.interventionDao().totalInterventions(),
        db.interventionDao().substitutionCount(),
        combine(permissionsFlow, usageStatsFlow) { perms, usage -> perms to usage },
    ) { settings, statuses, total, subs, (perms, usage) ->
        DashboardState(
            hydrated = true,
            settings = settings,
            permissions = perms,
            phase = phaseOf(settings),
            todayUsageMsByApp = usage,
            streaks = StreakCalculator.compute(statuses),
            totalInterventions = total,
            substitutionPercent = SubstitutionRate.percent(subs, total),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private fun phaseOf(s: Settings): Phase {
        if (!s.onboardingComplete) return Phase.NotOnboarded
        if (s.baselineStartEpochDay < 0) return Phase.Baseline(1, BASELINE_DAYS)
        val today = java.time.LocalDate.now().toEpochDay()
        val elapsed = (today - s.baselineStartEpochDay).toInt()
        return if (elapsed < BASELINE_DAYS) Phase.Baseline(elapsed + 1, BASELINE_DAYS) else Phase.Active
    }

    /** Called by the UI on resume — permission grants change outside the app. */
    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        permissionsFlow.value = Permissions.Kind.entries.associateWith { Permissions.isGranted(ctx, it) }
    }

    /** Reads today's usage from UsageStatsManager (needs Usage Access). */
    fun refreshUsageStats() {
        viewModelScope.launch {
            val reader = container.usageReader
            usageStatsFlow.value = reader.todayByApp(DetectionConfig.targetPackages)
        }
    }

    fun completeOnboarding(plans: List<IfThenPlan>, redirects: List<RedirectApp>, budgets: Map<String, Int>) {
        viewModelScope.launch {
            val repo = container.settings
            repo.setIfThenPlans(plans)
            repo.setRedirectApps(redirects)
            budgets.forEach { (appId, min) -> repo.setBudgetMin(appId, min) }
            // Start the 2-day baseline observation now (§3.1).
            repo.setBaselineStartEpochDay(java.time.LocalDate.now().toEpochDay())
            repo.setOnboardingComplete(true)
        }
    }

    companion object {
        const val BASELINE_DAYS = 2

        fun labelFor(appId: String): String =
            DetectionConfig.targetFor(appId)?.label ?: appId
    }
}
