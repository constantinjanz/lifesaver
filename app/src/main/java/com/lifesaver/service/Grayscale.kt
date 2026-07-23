package com.lifesaver.service

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Toggles the system-wide monochrome "color correction" (grayscale). Killing the colour on
 * Reels/Shorts removes most of the dopamine pull without blocking. Needs WRITE_SECURE_SETTINGS,
 * which a normal app can't request — grant it once over adb (a personal sideloaded build):
 *
 *   adb shell pm grant com.lifesaver android.permission.WRITE_SECURE_SETTINGS
 */
object Grayscale {

    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER = "accessibility_display_daltonizer"
    private const val MONOCHROMACY = 0 // grayscale mode

    fun isAvailable(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, "android.permission.WRITE_SECURE_SETTINGS") ==
            PackageManager.PERMISSION_GRANTED

    fun setEnabled(context: Context, on: Boolean) {
        if (!isAvailable(context)) return
        runCatching {
            val cr = context.contentResolver
            if (on) Settings.Secure.putInt(cr, DALTONIZER, MONOCHROMACY)
            Settings.Secure.putInt(cr, DALTONIZER_ENABLED, if (on) 1 else 0)
        }
    }
}
