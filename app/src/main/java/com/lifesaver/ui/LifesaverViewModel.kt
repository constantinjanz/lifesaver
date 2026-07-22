package com.lifesaver.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifesaver.LifesaverApp
import com.lifesaver.data.Checkin
import com.lifesaver.data.DailyUsage
import com.lifesaver.data.EmergencyUnlock
import com.lifesaver.data.IfThenPlan
import com.lifesaver.data.PendingChange
import com.lifesaver.data.RedirectApp
import com.lifesaver.data.Settings
import com.lifesaver.data.Strictness
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.domain.BaselineModel
import com.lifesaver.domain.BudgetEngine
import com.lifesaver.domain.DayKeys
import com.lifesaver.domain.SelfBinding
import com.lifesaver.domain.StreakCalculator
import com.lifesaver.domain.SubstitutionRate
import com.lifesaver.domain.TimeSaved
import com.lifesaver.service.Permissions
import com.lifesaver.work.ApplyQueuedChangesWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    val pendingChanges: List<PendingChange> = emptyList(),
    val unlocksUsedThisWeek: Int = 0,
    val weeklySavedMs: Long = 0,
) {
    val remainingUnlocks: Int get() = (MAX_UNLOCKS_PER_WEEK - unlocksUsedThisWeek).coerceAtLeast(0)
}

private data class Aux(
    val perms: Map<Permissions.Kind, Boolean>,
    val usage: Map<String, Long>,
    val pending: List<PendingChange>,
    val unlocksUsed: Int,
    val weeklySaved: Long,
)

const val MAX_UNLOCKS_PER_WEEK = 2
private const val UNLOCK_MINUTES = 15

class LifesaverViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as LifesaverApp).container
    private val db = container.database
    private val weekKey = DayKeys.weekKey(nowMs())

    private val permissionsFlow = MutableStateFlow<Map<Permissions.Kind, Boolean>>(emptyMap())
    private val usageStatsFlow = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val weeklySavedFlow = MutableStateFlow(0L)

    val state: StateFlow<DashboardState> = combine(
        container.settings.settings,
        db.statusDao().all(),
        db.interventionDao().totalInterventions(),
        db.interventionDao().substitutionCount(),
        combine(
            permissionsFlow,
            usageStatsFlow,
            db.pendingChangeDao().pending(),
            db.unlockDao().usedThisWeekFlow(weekKey),
            weeklySavedFlow,
        ) { perms, usage, pending, unlocksUsed, weeklySaved ->
            Aux(perms, usage, pending, unlocksUsed, weeklySaved)
        },
    ) { settings, statuses, total, subs, aux ->
        DashboardState(
            hydrated = true,
            settings = settings,
            permissions = aux.perms,
            phase = phaseOf(settings),
            todayUsageMsByApp = aux.usage,
            streaks = StreakCalculator.compute(statuses),
            totalInterventions = total,
            substitutionPercent = SubstitutionRate.percent(subs, total),
            pendingChanges = aux.pending,
            unlocksUsedThisWeek = aux.unlocksUsed,
            weeklySavedMs = aux.weeklySaved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private fun phaseOf(s: Settings): Phase {
        if (!s.onboardingComplete) return Phase.NotOnboarded
        if (s.baselineStartEpochDay < 0) return Phase.Baseline(1, BASELINE_DAYS)
        val elapsed = (LocalDate.now().toEpochDay() - s.baselineStartEpochDay).toInt()
        return if (elapsed < BASELINE_DAYS) Phase.Baseline(elapsed + 1, BASELINE_DAYS) else Phase.Active
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        permissionsFlow.value = Permissions.Kind.entries.associateWith { Permissions.isGranted(ctx, it) }
    }

    fun refreshUsageStats() {
        viewModelScope.launch {
            usageStatsFlow.value = container.usageReader.todayByApp(DetectionConfig.targetPackages)
            weeklySavedFlow.value = computeWeeklySaved()
        }
    }

    private suspend fun computeWeeklySaved(): Long {
        val settings = container.settings.current()
        val today = LocalDate.now()
        val weekStart = LocalDate.parse(DayKeys.weekKeyOf(today.toString()))
        val baselines = db.baselineDao().all()
        var saved = 0L
        var day = weekStart
        while (!day.isAfter(today)) {
            val dayKey = day.toString()
            settings.enabledApps.forEach { app ->
                val actual = db.usageDao().get(dayKey, app)?.let { BudgetEngine.effectiveBurnMs(it) } ?: 0L
                val base = BaselineModel.baselineForDay(baselines, app, dayKey)
                saved += TimeSaved.savedMs(base, actual)
            }
            day = day.plusDays(1)
        }
        return saved
    }

    fun completeOnboarding(plans: List<IfThenPlan>, redirects: List<RedirectApp>, budgets: Map<String, Int>) {
        viewModelScope.launch {
            val repo = container.settings
            repo.setIfThenPlans(plans)
            repo.setRedirectApps(redirects)
            budgets.forEach { (appId, min) -> repo.setBudgetMin(appId, min) }
            repo.setBaselineStartEpochDay(LocalDate.now().toEpochDay())
            repo.setOnboardingComplete(true)
        }
    }

    // --- M6 actions ---

    /** Budget change: tightening applies now; loosening queues for 24h (§3.6). */
    fun changeBudget(appId: String, newMin: Int) {
        viewModelScope.launch {
            val old = container.settings.current().budgetMin(appId)
            if (old == newMin) return@launch
            val decision = SelfBinding.budgetChange(old, newMin, nowMs())
            if (decision.isImmediate) {
                container.settings.setBudgetMin(appId, newMin)
            } else {
                enqueue("budget:$appId", newMin.toString(), decision.effectiveTs)
            }
        }
    }

    fun changeStrictness(new: Strictness) {
        viewModelScope.launch {
            val old = container.settings.current().strictness
            if (old == new) return@launch
            val decision = SelfBinding.strictnessChange(old, new, nowMs())
            if (decision.isImmediate) {
                container.settings.setStrictness(new)
            } else {
                enqueue("strictness", new.name, decision.effectiveTs)
            }
        }
    }

    private suspend fun enqueue(key: String, value: String, effectiveTs: Long) {
        db.pendingChangeDao().insert(
            PendingChange(
                settingKey = key,
                newValue = value,
                requestedTs = nowMs(),
                effectiveTs = effectiveTs,
                direction = "loosen",
            ),
        )
        ApplyQueuedChangesWorker.schedule(getApplication(), effectiveTs)
    }

    fun cancelPendingChange(id: Long) {
        viewModelScope.launch { db.pendingChangeDao().cancel(id) }
    }

    /** Emergency unlock (§3.5): 15 min lift, typed reason, day excluded from streak. */
    fun activateEmergencyUnlock(reason: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val used = db.unlockDao().usedThisWeek(weekKey)
            if (used >= MAX_UNLOCKS_PER_WEEK) return@launch
            val now = nowMs()
            val expires = now + UNLOCK_MINUTES * 60_000L
            db.unlockDao().insert(
                EmergencyUnlock(
                    ts = now,
                    weekKey = weekKey,
                    dayKey = DayKeys.todayKey(),
                    reason = reason.ifBlank { "(no reason given)" },
                    expiresTs = expires,
                ),
            )
            container.settings.setPausedUntil(expires)
            onDone()
        }
    }

    fun submitCheckin(control: Int, satisfaction: Int, impulse: Int) {
        viewModelScope.launch {
            db.checkinDao().upsert(Checkin(weekKey, nowMs(), control, satisfaction, impulse))
        }
    }

    private fun nowMs() = System.currentTimeMillis()

    companion object {
        const val BASELINE_DAYS = 2
        fun labelFor(appId: String): String = DetectionConfig.targetFor(appId)?.label ?: appId
    }
}
