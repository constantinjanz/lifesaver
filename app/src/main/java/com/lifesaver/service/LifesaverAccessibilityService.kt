package com.lifesaver.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.lifesaver.detection.DetectionConfig

/**
 * The interception engine. M1: registers and tracks connection state + last foreground package
 * (so the permission is real and the debug screen has something to show). Foreground accounting
 * (M2), intervention/block dispatch (M3), and Reels/Shorts surface detection (M5) are layered on
 * in later milestones. Detection is driven entirely by [DetectionConfig] (§3.4).
 */
class LifesaverAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == applicationContext.packageName) return
        lastForegroundPackage = pkg
        lastForegroundIsTarget = DetectionConfig.isTarget(pkg)
        // M2/M3/M5 enforcement is attached here.
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        isConnected = false
        return super.onUnbind(intent)
    }

    companion object {
        /** Live connection state — a stronger signal than the settings-string check (§9.2 seed). */
        @Volatile
        var isConnected: Boolean = false
            private set

        @Volatile
        var lastForegroundPackage: String? = null
            private set

        @Volatile
        var lastForegroundIsTarget: Boolean = false
            private set
    }
}
