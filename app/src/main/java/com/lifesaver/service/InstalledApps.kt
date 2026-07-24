package com.lifesaver.service

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/**
 * Enumerates launchable installed apps for the redirect picker (PRD §3.1) and builds launch
 * intents for redirects (§2). Uses the QUERY_ALL_PACKAGES visibility granted for this personal
 * sideloaded build (§4).
 */
class InstalledApps(private val context: Context) {

    data class Entry(val packageName: String, val label: String, val icon: Drawable?)

    fun launchable(): List<Entry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved.asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .mapNotNull { pkg ->
                runCatching {
                    val info = pm.getApplicationInfo(pkg, 0)
                    Entry(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info))
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    /** App icon for any installed package (custom apps aren't in the launchable list cache). */
    fun iconFor(packageName: String): Drawable? = runCatching {
        val pm = context.packageManager
        pm.getApplicationIcon(pm.getApplicationInfo(packageName, 0))
    }.getOrNull()

    fun launchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
