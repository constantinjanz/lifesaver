package com.lifesaver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.lifesaver.MainActivity
import com.lifesaver.R

/**
 * Notification channels and builders. v1.1 rule (§9.4): the app speaks only at the scroll moment
 * and Sunday evening — so channels here are the silent low-priority usage bar (§3.3) and the
 * weekly report. No nagging channels.
 */
class Notifications(private val context: Context) {

    private val nm get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        val usage = NotificationChannel(
            CHANNEL_USAGE,
            "Budget status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Silent reminder of time left while a watched app is open."
            setShowBadge(false)
            enableVibration(false)
        }
        val weekly = NotificationChannel(
            CHANNEL_WEEKLY,
            "Weekly report",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Sunday evening summary." }
        nm.createNotificationChannels(listOf(usage, weekly))
    }

    /** Low-priority, silent, ongoing "Instagram: 12 min left today" (§3.3). */
    fun buildUsageNotification(appLabel: String, minutesLeft: Int): Notification {
        val open = android.app.PendingIntent.getActivity(
            context, 0,
            android.content.Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_USAGE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(appLabel)
            .setContentText("$minutesLeft min left today")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .build()
    }

    fun showUsage(id: Int, notification: Notification) = nm.notify(id, notification)
    fun cancelUsage(id: Int) = nm.cancel(id)

    companion object {
        const val CHANNEL_USAGE = "usage_status"
        const val CHANNEL_WEEKLY = "weekly_report"
        const val USAGE_NOTIFICATION_ID_BASE = 1000
    }
}
