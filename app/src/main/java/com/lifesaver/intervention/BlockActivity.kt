package com.lifesaver.intervention

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifesaver.LifesaverApp
import com.lifesaver.domain.BaselineModel
import com.lifesaver.domain.BudgetEngine
import com.lifesaver.domain.DayKeys
import com.lifesaver.domain.StreakCalculator
import com.lifesaver.domain.TimeSaved
import com.lifesaver.ui.components.AppIcon
import com.lifesaver.ui.components.glass.GlassBackground
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.components.glass.RingGauge
import com.lifesaver.ui.theme.Danger
import com.lifesaver.ui.theme.LifesaverTheme
import com.lifesaver.ui.theme.TextPrimary
import com.lifesaver.ui.theme.TextSecondary
import com.lifesaver.ui.theme.clickableNoRipple
import kotlinx.coroutines.launch

/**
 * Budget-exhausted block screen (PRD §3.3, DESIGN v2 §7). Same cockpit as the intervention but the
 * ring is static danger-red with a lock; reclaimed + streak reassure; redirect dock stays; the
 * emergency unlock is a small text pill opening a glass sheet for the typed reason.
 */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appId = intent.getStringExtra(EXTRA_APP_ID) ?: ""
        val label = intent.getStringExtra(EXTRA_APP_LABEL) ?: appId
        val scheduled = intent.getBooleanExtra(EXTRA_SCHEDULED, false)
        val untilMin = intent.getIntExtra(EXTRA_UNTIL_MIN, -1)
        val reels = intent.getBooleanExtra(EXTRA_REELS, false)
        val surfaceName = intent.getStringExtra(EXTRA_SURFACE_NAME) ?: "Reels"
        setContent {
            LifesaverTheme {
                GlassBackground(seed = 2, drift = false) {
                    BlockContent(
                        appId = appId,
                        label = label,
                        scheduled = scheduled,
                        reels = reels,
                        surfaceName = surfaceName,
                        untilText = if (untilMin >= 0) com.lifesaver.domain.ScheduleBlock.format(untilMin) else null,
                        onClose = ::goHome,
                        onRedirect = ::redirectTo,
                        onUnlock = { reason -> activateUnlock(appId, reason) },
                    )
                }
            }
        }
    }

    private fun redirectTo(packageName: String) {
        LifesaverApp.instance.container.installedApps.launchIntent(packageName)?.let { startActivity(it) }
        finish()
    }

    private fun activateUnlock(appId: String, reason: String) {
        val container = LifesaverApp.instance.container
        container.appScope.launch {
            val weekKey = DayKeys.weekKey(System.currentTimeMillis())
            if (container.database.unlockDao().usedThisWeek(weekKey) >= 2) return@launch
            val now = System.currentTimeMillis()
            val expires = now + 15 * 60_000L
            container.database.unlockDao().insert(
                com.lifesaver.data.EmergencyUnlock(
                    ts = now, weekKey = weekKey, dayKey = DayKeys.todayKey(),
                    reason = reason.ifBlank { "(no reason given)" }, expiresTs = expires,
                ),
            )
            container.settings.setPausedUntil(expires)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                container.installedApps.launchIntent(appId)?.let { startActivity(it) }
                finish()
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    override fun onBackPressed() = goHome()

    companion object {
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_APP_LABEL = "app_label"
        const val EXTRA_SCHEDULED = "scheduled"
        const val EXTRA_UNTIL_MIN = "until_min"
        const val EXTRA_REELS = "reels"
        const val EXTRA_SURFACE_NAME = "surface_name"
    }
}

private data class BlockStats(val savedToday: Long, val streak: Int)
private data class BlockRedirect(val packageName: String, val label: String, val icon: Drawable?)

@Composable
private fun BlockContent(
    appId: String,
    label: String,
    scheduled: Boolean,
    reels: Boolean,
    surfaceName: String,
    untilText: String?,
    onClose: () -> Unit,
    onRedirect: (String) -> Unit,
    onUnlock: (String) -> Unit,
) {
    val stats by produceState(BlockStats(0, 0), appId) { value = loadBlockStats(appId) }
    val redirects by produceState(emptyList<BlockRedirect>()) { value = loadBlockRedirects() }
    val remainingUnlocks by produceState(0) { value = loadRemainingUnlocks() }
    var showUnlockDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RingGauge(progress = 1f, diameter = 190.dp, color = Danger, animateOnEntry = false) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Danger)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            when {
                reels -> "$surfaceName is off"
                scheduled -> "$label is locked"
                else -> "$label is done for today"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        if (reels) {
            Text(
                "You can still use $label — just not $surfaceName. That's where the time really goes.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center,
            )
        } else if (scheduled) {
            Text(
                if (untilText != null) "You locked it until $untilText. No way around it — that was the point."
                else "You locked it for this window. No way around it — that was the point.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center,
            )
        } else {
            if (stats.savedToday > 0) {
                Text(
                    "You've reclaimed ${TimeSaved.formatHm(stats.savedToday)} today.",
                    style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center,
                )
            }
            Text(
                "${stats.streak} day streak. Resets at midnight.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center,
            )
        }

        if (redirects.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            GlassPanel {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    redirects.forEach { r ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickableNoRipple(onClick = { onRedirect(r.packageName) }),
                        ) {
                            AppIcon(r.icon, size = 44.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(r.label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        GlassPill("Close $label", onClick = onClose, primary = true)
        // Scheduled hard-block and reels-limit are self-imposed surface locks — not unlockable here.
        if (!scheduled && !reels && remainingUnlocks > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Use emergency unlock · $remainingUnlocks left",
                style = MaterialTheme.typography.bodySmall,
                color = Danger,
                modifier = Modifier.clickableNoRipple(onClick = { showUnlockDialog = true }).padding(8.dp),
            )
        }
    }

    if (showUnlockDialog) {
        UnlockSheet(
            onConfirm = { reason -> showUnlockDialog = false; onUnlock(reason) },
            onDismiss = { showUnlockDialog = false },
        )
    }
}

@Composable
private fun UnlockSheet(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassPanel {
            Text("Emergency unlock", style = MaterialTheme.typography.titleLarge)
            Text(
                "15 minutes, all restrictions lifted. This day won't count toward your streak.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = reason, onValueChange = { reason = it },
                placeholder = { Text("Why do you need it?") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPill("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                GlassPill("Unlock", onClick = { onConfirm(reason) }, primary = true, enabled = reason.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}

private suspend fun loadBlockStats(appId: String): BlockStats {
    val db = LifesaverApp.instance.container.database
    val dayKey = DayKeys.todayKey()
    val actual = db.usageDao().get(dayKey, appId)?.let { BudgetEngine.effectiveBurnMs(it) } ?: 0L
    val baseline = BaselineModel.baselineForDay(db.baselineDao().all(), appId, dayKey)
    val streak = StreakCalculator.compute(db.statusDao().recent(400)).current
    return BlockStats(TimeSaved.savedMs(baseline, actual), streak)
}

private suspend fun loadBlockRedirects(): List<BlockRedirect> =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val container = LifesaverApp.instance.container
        val chosen = container.settings.current().redirectApps
        if (chosen.isEmpty()) return@withContext emptyList()
        val all = container.installedApps.launchable()
        chosen.map { r -> BlockRedirect(r.appId, r.label, all.firstOrNull { it.packageName == r.appId }?.icon) }
    }

private suspend fun loadRemainingUnlocks(): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val container = LifesaverApp.instance.container
    val weekKey = DayKeys.weekKey(System.currentTimeMillis())
    (2 - container.database.unlockDao().usedThisWeek(weekKey)).coerceAtLeast(0)
}
