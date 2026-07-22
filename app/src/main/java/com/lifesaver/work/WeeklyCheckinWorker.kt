package com.lifesaver.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifesaver.LifesaverApp
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Posts the Sunday ~20:00 weekly check-in nudge (PRD §3.7, §9.4). Reschedules itself for the next
 * Sunday. The full weekly report (§9.5) is v1.1; this is the v1 check-in reminder.
 */
class WeeklyCheckinWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        LifesaverApp.instance.container.notifications.showWeeklyCheckin()
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "weekly_checkin"
        private const val HOUR = 20

        fun schedule(context: Context) {
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            var next = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .withHour(HOUR).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) {
                next = next.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                    .withHour(HOUR).withMinute(0).withSecond(0).withNano(0)
            }
            val delay = java.time.Duration.between(now, next).toMillis().coerceAtLeast(60_000L)
            val request = OneTimeWorkRequestBuilder<WeeklyCheckinWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
