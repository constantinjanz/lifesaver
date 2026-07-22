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
    val todaySavedMs: Long = 0,
    val todayBaselineMs: Long = 0,
    val serviceConnected: Boolean = false,
) {
    val remainingUnlocks: Int get() = (MAX_UNLOCKS_PER_WEEK - unlocksUsedThisWeek).coerceAtLeast(0)
    /** Fraction of today's baseline already reclaimed — the cockpit hero ring (§7). */
    val todayReclaimFraction: Float
        get() = if (todayBaselineMs <= 0) 0f else (todaySavedMs.toFloat() / todayBaselineMs).coerceIn(0f, 1f)
}

data class SavedSnapshot(val weeklyMs: Long = 0, val todayMs: Long = 0, val todayBaselineMs: Long = 0)

private data class Aux(
    val perms: Map<Permissions.Kind, Boolean>,
    val usage: Map<String, Long>,
    val pending: List<PendingChange>,
    val unlocksUsed: Int,
    val saved: SavedSnapshot,
)

const val MAX_UNLOCKS_PER_WEEK = 2
private const val UNLOCK_MINUTES = 15

class LifesaverViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as LifesaverApp).container
    private val db = container.database
    private val weekKey = DayKeys.weekKey(nowMs())

    private val permissionsFlow = MutableStateFlow<Map<Permissions.Kind, Boolean>>(emptyMap())
    private val usageStatsFlow = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val savedFlow = MutableStateFlow(SavedSnapshot())

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
            savedFlow,
        ) { perms, usage, pending, unlocksUsed, saved ->
            Aux(perms, usage, pending, unlocksUsed, saved)
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
            weeklySavedMs = aux.saved.weeklyMs,
            todaySavedMs = aux.saved.todayMs,
            todayBaselineMs = aux.saved.todayBaselineMs,
            serviceConnected = com.lifesaver.service.LifesaverAccessibilityService.isConnected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private fun phaseOf(s: Settings): Phase =
        if (s.onboardingComplete) Phase.Active else Phase.NotOnboarded

    /** Compute the baseline + insight picture from the phone's usage history (off the main thread). */
    suspend fun computeInsight(): com.lifesaver.domain.BaselineInsight =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            container.usageReader.computeBaseline()
        }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        permissionsFlow.value = Permissions.Kind.entries.associateWith { Permissions.isGranted(ctx, it) }
    }

    fun refreshUsageStats() {
        viewModelScope.launch {
            usageStatsFlow.value = container.usageReader.todayByApp(DetectionConfig.targetPackages)
            savedFlow.value = computeSaved()
        }
    }

    private suspend fun computeSaved(): SavedSnapshot {
        val settings = container.settings.current()
        val today = LocalDate.now()
        val todayKey = today.toString()
        val weekStart = LocalDate.parse(DayKeys.weekKeyOf(todayKey))
        val baselines = db.baselineDao().all()
        var weekly = 0L
        var todaySaved = 0L
        var todayBaseline = 0L
        var day = weekStart
        while (!day.isAfter(today)) {
            val dayKey = day.toString()
            settings.enabledApps.forEach { app ->
                val actual = db.usageDao().get(dayKey, app)?.let { BudgetEngine.effectiveBurnMs(it) } ?: 0L
                val base = BaselineModel.baselineForDay(baselines, app, dayKey)
                weekly += TimeSaved.savedMs(base, actual)
                if (dayKey == todayKey) {
                    todaySaved += TimeSaved.savedMs(base, actual)
                    todayBaseline += base
                }
            }
            day = day.plusDays(1)
        }
        return SavedSnapshot(weeklyMs = weekly, todayMs = todaySaved, todayBaselineMs = todayBaseline)
    }

    fun completeOnboarding(plans: List<IfThenPlan>, redirects: List<RedirectApp>, budgets: Map<String, Int>) {
        viewModelScope.launch {
            val repo = container.settings
            repo.setIfThenPlans(plans)
            repo.setRedirectApps(redirects)
            budgets.forEach { (appId, min) -> repo.setBudgetMin(appId, min) }
            // Seed the baseline from the phone's own history instead of a 2-day observation, then
            // enforce immediately. Best-effort: if Usage Access is missing, seeds resolve to 0.
            seedBaselineFromHistory()
            repo.setBaselineStartEpochDay(LocalDate.now().toEpochDay())
            repo.setOnboardingComplete(true)
            refreshUsageStats()
        }
    }

    private suspend fun seedBaselineFromHistory() {
        val insight = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            container.usageReader.computeBaseline()
        }
        insight.perApp.forEach { (app, b) ->
            db.baselineDao().upsert(BaselineModel.seeded(app, "weekday", b.weekdayAvgMs))
            db.baselineDao().upsert(BaselineModel.seeded(app, "weekend", b.weekendAvgMs))
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
        fun labelFor(appId: String): String = DetectionConfig.targetFor(appId)?.label ?: appId
    }
}
