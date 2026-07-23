package com.lifesaver.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.domain.BudgetEngine
import com.lifesaver.domain.TimeSaved
import com.lifesaver.ui.DashboardState
import com.lifesaver.ui.components.glass.DockItem
import com.lifesaver.ui.components.glass.FloatingDock
import com.lifesaver.ui.components.glass.GlassBackground
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassTile
import com.lifesaver.ui.components.glass.RingGauge
import com.lifesaver.ui.components.glass.RiskBars
import com.lifesaver.ui.components.glass.StatusPill
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.Danger
import com.lifesaver.ui.theme.Success
import com.lifesaver.ui.theme.TextCaption
import com.lifesaver.ui.theme.TextPrimary
import com.lifesaver.ui.theme.TextSecondary
import com.lifesaver.ui.theme.clickableNoRipple

/** The cockpit as a swipe page (background + dock live in HomeScreen). */
@Composable
fun CockpitPage(
    state: DashboardState,
    onEditPlans: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenReport: () -> Unit,
    onFixPermissions: () -> Unit,
) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = topInset + 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TopRow(state, onOpenSettings, onOpenReport, onOpenDebug, onEditPlans)

        if (state.permissions.values.any { !it } && state.permissions.isNotEmpty()) {
            ActionCard("Setup incomplete", "Some permissions are missing — tap to finish.", onFixPermissions)
        }
        if (state.pendingChanges.isNotEmpty()) {
            ActionCard(
                "Pending changes",
                "${state.pendingChanges.size} change${if (state.pendingChanges.size == 1) "" else "s"} waiting on the 24h rule.",
                onOpenSettings,
            )
        }

        HeroRing(state)
        BudgetTiles(state)
        StreakAndSaved(state)
        SubstitutionCard(state)
    }
}

@Composable
private fun TopRow(state: DashboardState, onSettings: () -> Unit, onReport: () -> Unit, onDebug: () -> Unit, onPlans: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val connected = state.serviceConnected
        StatusPill(
            text = if (connected) "All systems on" else "Service off",
            dotColor = if (connected) Success else Danger,
        )
        Box {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = TextPrimary,
                modifier = Modifier.clickableNoRipple(onClick = { menuOpen = true }).padding(8.dp),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("If-then plans") }, onClick = { menuOpen = false; onPlans() })
                DropdownMenuItem(text = { Text("Weekly report") }, onClick = { menuOpen = false; onReport() })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; onSettings() })
                DropdownMenuItem(text = { Text("Debug") }, onClick = { menuOpen = false; onDebug() })
            }
        }
    }
}

@Composable
private fun HeroRing(state: DashboardState) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        RingGauge(progress = state.todayReclaimFraction, diameter = 190.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(TimeSaved.formatHm(state.todaySavedMs), style = MaterialTheme.typography.displaySmall)
                Text("reclaimed today", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun BudgetTiles(state: DashboardState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DetectionConfig.targets.forEach { target ->
            val usage = state.todayUsageMsByApp[target.appId] ?: 0L
            val budget = state.settings.budgetMs(target.appId)
            val remainingMin = BudgetEngine.remainingMinutes(budget, usage, 0)
            val fraction = BudgetEngine.remainingFraction(budget, usage, 0)
            GlassTile(modifier = Modifier.weight(1f)) {
                Text(target.label, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Box(contentAlignment = Alignment.Center) {
                    RingGauge(
                        progress = fraction,
                        diameter = 84.dp,
                        stroke = 6.dp,
                        color = if (fraction < 0.25f) Danger else Accent,
                    ) {
                        Text("$remainingMin", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("min left", style = MaterialTheme.typography.bodySmall, color = TextCaption)
            }
        }
    }
}

@Composable
private fun StreakAndSaved(state: DashboardState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassTile(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Accent, modifier = Modifier.height(22.dp))
                Text("${state.streaks.current}", style = MaterialTheme.typography.titleLarge)
            }
            Text("day streak", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(state.settings.bankedFreezes) {
                    Icon(Icons.Filled.AcUnit, contentDescription = "freeze", tint = com.lifesaver.ui.theme.BlobBlue, modifier = Modifier.height(12.dp))
                }
                Text("longest ${state.streaks.longest}", style = MaterialTheme.typography.bodySmall, color = TextCaption)
            }
        }
        GlassTile(modifier = Modifier.weight(1f)) {
            Text(TimeSaved.formatHm(state.weeklySavedMs), style = MaterialTheme.typography.titleLarge)
            Text("saved this week", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (state.weeklySavedMs > 0) {
                Text(TimeSaved.tangible(state.weeklySavedMs).lowercase(), style = MaterialTheme.typography.bodySmall, color = TextCaption)
            }
        }
    }
}

@Composable
private fun SubstitutionCard(state: DashboardState) {
    GlassPanel {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("${state.substitutionPercent}%", style = MaterialTheme.typography.titleLarge)
                Text("of interventions redirected", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(
                "${state.remainingUnlocks} unlocks left",
                style = MaterialTheme.typography.bodySmall,
                color = TextCaption,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun ActionCard(title: String, body: String, onClick: () -> Unit) {
    GlassPanel(modifier = Modifier.clickableNoRipple(onClick = onClick)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Accent)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
