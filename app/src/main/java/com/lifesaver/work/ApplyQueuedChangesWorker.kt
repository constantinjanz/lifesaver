package com.lifesaver.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifesaver.LifesaverApp
import com.lifesaver.data.Strictness
import java.util.concurrent.TimeUnit

/**
 * Applies queued loosening changes once their 24h delay elapses (PRD §3.6). Tightening changes are
 * applied immediately by the caller and never reach the queue. Reschedules itself for the next
 * pending change so the queue drains without polling.
 *
 * settingKey encoding: "budget:<appId>" -> minutes; "strictness" -> Strictness name.
 */
class ApplyQueuedChangesWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = LifesaverApp.instance.container
        val db = container.database
        val repo = container.settings
        val now = System.currentTimeMillis()

        db.pendingChangeDao().due(now).forEach { change ->
            when {
                change.settingKey.startsWith("budget:") -> {
                    val appId = change.settingKey.removePrefix("budget:")
                    change.newValue.toIntOrNull()?.let { repo.setBudgetMin(appId, it) }
                }
                change.settingKey == "strictness" -> {
                    runCatching { Strictness.valueOf(change.newValue) }.getOrNull()?.let { repo.setStrictness(it) }
                }
            }
            db.pendingChangeDao().markApplied(change.id)
        }

        // Reschedule for the next still-pending change, if any.
        val next = db.pendingChangeDao().due(Long.MAX_VALUE).minByOrNull { it.effectiveTs }
        if (next != null) schedule(applicationContext, next.effectiveTs)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "apply_queued_changes"

        fun schedule(context: Context, atMs: Long) {
            val delay = (atMs - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ApplyQueuedChangesWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
