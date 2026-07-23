package com.lifesaver.service

import com.lifesaver.data.DailyUsage
import com.lifesaver.data.LifesaverDatabase
import com.lifesaver.data.UsageSession
import com.lifesaver.domain.BudgetEngine
import com.lifesaver.domain.DayKeys
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Accrues foreground time to Room (PRD §3.3). The accessibility service is the primary signal;
 * UsageStatsReconciler corrects drift daily. Reels/Shorts seconds are tracked separately so the
 * 2x multiplier (§3.4) is applied by BudgetEngine. All writes are serialized by a mutex because
 * accrual is read-modify-write on daily_usage.
 */
class ForegroundAccountant(private val db: LifesaverDatabase) {

    private val lock = Mutex()

    data class Accrued(val effectiveMs: Long, val remainingMs: Long, val exhausted: Boolean, val reelsMs: Long)

    /** Add a slice of foreground time (of which [deltaFastMs] was on the 2x surface). */
    suspend fun accrue(
        appId: String,
        dayKey: String,
        deltaMs: Long,
        deltaFastMs: Long,
        budgetMs: Long,
    ): Accrued = lock.withLock {
        if (deltaMs <= 0) {
            val cur = db.usageDao().get(dayKey, appId)
            val eff = cur?.let { BudgetEngine.effectiveBurnMs(it) } ?: 0
            return@withLock Accrued(eff, (budgetMs - eff).coerceAtLeast(0), budgetMs in 1..eff, cur?.reelsShortsMs ?: 0)
        }
        val fast = deltaFastMs.coerceIn(0, deltaMs)
        val cur = db.usageDao().get(dayKey, appId)
            ?: DailyUsage(dayKey = dayKey, appId = appId, budgetMs = budgetMs)
        val updated = cur.copy(
            foregroundMs = cur.foregroundMs + deltaMs,
            reelsShortsMs = cur.reelsShortsMs + fast,
            budgetMs = budgetMs,
        )
        val eff = BudgetEngine.effectiveBurnMs(updated)
        val exhausted = budgetMs in 1..eff
        db.usageDao().upsert(updated.copy(blocked = updated.blocked || exhausted))
        Accrued(eff, (budgetMs - eff).coerceAtLeast(0), exhausted, updated.reelsShortsMs)
    }

    suspend fun reelsToday(appId: String, dayKey: String): Long =
        db.usageDao().get(dayKey, appId)?.reelsShortsMs ?: 0

    /** Persist a completed session (append-only, §9.1-ready). */
    suspend fun recordSession(appId: String, startMs: Long, endMs: Long, fastMs: Long) {
        val duration = (endMs - startMs).coerceAtLeast(0)
        if (duration <= 0) return
        db.usageDao().insertSession(
            UsageSession(
                appId = appId,
                dayKey = DayKeys.dayKey(startMs),
                startTs = startMs,
                endTs = endMs,
                durationMs = duration,
                reelsShortsMs = fastMs.coerceIn(0, duration),
            ),
        )
    }

    suspend fun effectiveToday(appId: String, dayKey: String): Long =
        db.usageDao().get(dayKey, appId)?.let { BudgetEngine.effectiveBurnMs(it) } ?: 0
}
