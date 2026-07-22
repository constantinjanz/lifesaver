package com.lifesaver.intervention

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.lifesaver.ui.components.CountdownRing
import com.lifesaver.ui.components.RaisedButton
import com.lifesaver.ui.theme.Background
import com.lifesaver.ui.theme.Danger
import com.lifesaver.ui.theme.LifesaverTheme
import com.lifesaver.ui.theme.TextSecondary

/**
 * Budget-exhausted block screen (PRD §3.3). Same visual language as the intervention screen but a
 * static Deep Orange ring with a lock. Shows time saved today + streak. Redirect buttons (M4) and
 * the emergency unlock (M6) are layered on later. Block ends at midnight (the service simply stops
 * launching this once the new day's budget is available).
 */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appId = intent.getStringExtra(EXTRA_APP_ID) ?: ""
        val label = intent.getStringExtra(EXTRA_APP_LABEL) ?: appId
        setContent {
            LifesaverTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    BlockContent(appId = appId, label = label, onClose = ::goHome, onRedirect = ::redirectTo)
                }
            }
        }
    }

    private fun redirectTo(packageName: String) {
        LifesaverApp.instance.container.installedApps.launchIntent(packageName)?.let { startActivity(it) }
        finish()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    override fun onBackPressed() {
        goHome()
    }

    companion object {
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_APP_LABEL = "app_label"
    }
}

private data class BlockStats(val savedToday: Long, val streak: Int)
private data class BlockRedirect(val packageName: String, val label: String, val icon: android.graphics.drawable.Drawable?)

@Composable
private fun BlockContent(appId: String, label: String, onClose: () -> Unit, onRedirect: (String) -> Unit) {
    val stats by produceState(BlockStats(0, 0), appId) {
        value = loadBlockStats(appId)
    }
    val redirects by produceState(emptyList<BlockRedirect>()) {
        value = loadBlockRedirects()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CountdownRing(progress = 1f, color = Danger) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Danger)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "${label.uppercase()} IS DONE FOR TODAY",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        if (stats.savedToday > 0) {
            Text(
                "You've reclaimed ${TimeSaved.formatHm(stats.savedToday)} today.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            "${stats.streak} day streak. It resets at midnight.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (redirects.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                redirects.forEach { r ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onRedirect(r.packageName) },
                    ) {
                        AppIcon(r.icon)
                        Text(r.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        RaisedButton("Close $label", onClick = onClose)
    }
}

private suspend fun loadBlockRedirects(): List<BlockRedirect> =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val container = LifesaverApp.instance.container
        val chosen = container.settings.current().redirectApps
        if (chosen.isEmpty()) return@withContext emptyList()
        val all = container.installedApps.launchable()
        chosen.map { r -> BlockRedirect(r.appId, r.label, all.firstOrNull { it.packageName == r.appId }?.icon) }
    }

private suspend fun loadBlockStats(appId: String): BlockStats {
    val db = LifesaverApp.instance.container.database
    val dayKey = DayKeys.todayKey()
    val actual = db.usageDao().get(dayKey, appId)?.let { BudgetEngine.effectiveBurnMs(it) } ?: 0L
    val baseline = BaselineModel.baselineForDay(db.baselineDao().all(), appId, dayKey)
    val streak = StreakCalculator.compute(db.statusDao().recent(400)).current
    return BlockStats(TimeSaved.savedMs(baseline, actual), streak)
}
