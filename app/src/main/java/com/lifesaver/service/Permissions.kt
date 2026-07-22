package com.lifesaver.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils

/**
 * Central helper for the four permissions the app guides the user through in onboarding (§3.1):
 * Usage Access, Accessibility Service, Display over other apps, and battery-optimization exclusion
 * (critical on Samsung, §5). Each exposes an `isGranted` check and an Intent that deep-links to the
 * right settings page, so the onboarding UI can show a live checkmark.
 */
object Permissions {

    enum class Kind { USAGE_ACCESS, ACCESSIBILITY, OVERLAY, BATTERY }

    fun isGranted(context: Context, kind: Kind): Boolean = when (kind) {
        Kind.USAGE_ACCESS -> hasUsageAccess(context)
        Kind.ACCESSIBILITY -> isAccessibilityEnabled(context)
        Kind.OVERLAY -> Settings.canDrawOverlays(context)
        Kind.BATTERY -> isIgnoringBatteryOptimizations(context)
    }

    fun allGranted(context: Context): Boolean = Kind.entries.all { isGranted(context, it) }

    fun settingsIntent(context: Context, kind: Kind): Intent = when (kind) {
        Kind.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        Kind.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        Kind.OVERLAY -> Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        Kind.BATTERY -> batteryIntent(context)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${LifesaverAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    @Suppress("BatteryLife")
    private fun batteryIntent(context: Context): Intent {
        // Direct request dialog when possible; falls back to the settings list.
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
    }
}
