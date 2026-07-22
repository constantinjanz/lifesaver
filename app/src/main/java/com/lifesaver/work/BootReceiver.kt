package com.lifesaver.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Re-anchors scheduled work after a reboot (PRD §7). The AccessibilityService itself restarts on
 * boot if the user left it enabled; WorkManager persists enqueued jobs, but we reschedule the
 * self-chaining daily/weekly workers defensively and drain any now-due self-binding changes.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        MidnightResetWorker.scheduleNext(context)
        WeeklyCheckinWorker.schedule(context)
        // Apply anything whose 24h delay elapsed while powered off, and reschedule the rest.
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<ApplyQueuedChangesWorker>().build(),
        )
    }
}
