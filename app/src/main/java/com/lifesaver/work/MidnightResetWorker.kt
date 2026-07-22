package com.lifesaver.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifesaver.LifesaverApp
import com.lifesaver.data.Baseline
import com.lifesaver.data.DailyStatus
import com.lifesaver.domain.BaselineModel
import com.lifesaver.domain.BudgetEngine
import com.lifesaver.domain.DayKeys
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Runs just after local midnight (PRD §2, §5). Finalizes the day that just ended: writes its
 * streak status (success/fail) and refines the self-adjusting baseline (§3.1, §3.7). Reschedules
 * itself for the next midnight so daily resets are anchored to local time, timezone changes
 * included. Budget "reset" is implicit: a new local dayKey means fresh daily_usage rows.
 */
class MidnightResetWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = LifesaverApp.instance.container
        val db = container.database
        val settings = container.settings.current()

        val endedDay = LocalDate.now().minusDays(1).toString()
        val group = DayKeys.weekdayGroup(endedDay)
        val enabled = settings.enabledApps

        // Effective usage per app for the ended day.
        val effByApp = enabled.associateWith { app ->
            db.usageDao().get(endedDay, app)?.let { BudgetEngine.effectiveBurnMs(it) } ?: 0L
        }

        // Refine baseline (caps itself at 14 sample days, §3.7).
        enabled.forEach { app ->
            val prior: Baseline? = db.baselineDao().get(app, group)
            val refined = BaselineModel.refine(prior, app, group, effByApp[app] ?: 0L)
            db.baselineDao().upsert(refined)
        }

        // Only score streaks once enforcement is live (baseline days are observation-only, §3.1).
        if (!isBaselineDay(settings.baselineStartEpochDay, endedDay)) {
            val withinBudget = enabled.all { app ->
                val usage = db.usageDao().get(endedDay, app)
                val budget = usage?.budgetMs ?: settings.budgetMs(app)
                val eff = usage?.let { BudgetEngine.effectiveBurnMs(it) } ?: 0L
                budget <= 0 || eff <= budget
            }
            val unlocksUsed = db.unlockDao().forWeek(DayKeys.weekKeyOf(endedDay))
                .count { it.dayKey == endedDay }
            val success = withinBudget && unlocksUsed == 0
            val reason = when {
                unlocksUsed > 0 -> "unlock_used"
                !withinBudget -> "over_budget"
                else -> "ok"
            }
            db.statusDao().upsert(DailyStatus(endedDay, success, reason, unlocksUsed))
        }

        scheduleNext(applicationContext)
        return Result.success()
    }

    private fun isBaselineDay(baselineStartEpochDay: Long, dayKey: String): Boolean {
        if (baselineStartEpochDay < 0) return true
        val day = LocalDate.parse(dayKey).toEpochDay()
        return (day - baselineStartEpochDay) < BASELINE_DAYS
    }

    companion object {
        const val WORK_NAME = "midnight_reset"
        private const val BASELINE_DAYS = 2

        /** Enqueue the next run for just after the next local midnight. */
        fun scheduleNext(context: Context) {
            val now = System.currentTimeMillis()
            val nextMidnight = DayKeys.nextMidnightMs(now) + 30_000L // small buffer past 00:00
            val delay = (nextMidnight - now).coerceAtLeast(60_000L)
            val request = OneTimeWorkRequestBuilder<MidnightResetWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
