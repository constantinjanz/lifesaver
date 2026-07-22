package com.lifesaver.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.lifesaver.ui.Phase
import com.lifesaver.ui.components.BudgetBar
import com.lifesaver.ui.components.LifesaverCard
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.OnAccent
import com.lifesaver.ui.theme.TextSecondary
import com.lifesaver.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    onEditPlans: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDebug: () -> Unit,
    onFixPermissions: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lifesaver", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.lifesaver.ui.theme.AppBar,
                    titleContentColor = com.lifesaver.ui.theme.TextPrimary,
                ),
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Settings") }, onClick = {
                            menuOpen = false; onOpenSettings()
                        })
                        DropdownMenuItem(text = { Text("Debug") }, onClick = {
                            menuOpen = false; onOpenDebug()
                        })
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onEditPlans, containerColor = Accent, contentColor = OnAccent) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit plans")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.permissions.values.any { !it } && state.permissions.isNotEmpty()) {
                PermissionsWarning(onFixPermissions)
            }
            if (state.pendingChanges.isNotEmpty()) {
                PendingBanner(state.pendingChanges.size, onOpenSettings)
            }

            TodayCard(state)
            StreakCard(state)
            TimeSavedCard(state)
            SubstitutionCard(state)
        }
    }
}

@Composable
private fun PermissionsWarning(onFix: () -> Unit) {
    LifesaverCard(modifier = Modifier.clickableCard(onFix)) {
        Text("SETUP INCOMPLETE", style = MaterialTheme.typography.bodySmall, color = Warning)
        Spacer(Modifier.height(4.dp))
        Text("Some permissions are missing", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap to finish granting access so Lifesaver can step in.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TodayCard(state: DashboardState) {
    LifesaverCard {
        Text("TODAY", style = MaterialTheme.typography.bodySmall, color = Accent)
        Spacer(Modifier.height(12.dp))
        val enforcing = state.phase is Phase.Active
        DetectionConfig.targets.forEach { target ->
            BudgetRow(
                label = target.label,
                usageMs = state.todayUsageMsByApp[target.appId] ?: 0L,
                budgetMs = state.settings.budgetMs(target.appId),
                enforcing = enforcing,
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            "${state.remainingUnlocks} emergency unlocks left this week",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun TimeSavedCard(state: DashboardState) {
    LifesaverCard {
        Text(TimeSaved.formatHm(state.weeklySavedMs), style = MaterialTheme.typography.displaySmall)
        Text("SAVED THIS WEEK", style = MaterialTheme.typography.bodySmall, color = Accent)
        if (state.weeklySavedMs > 0) {
            Spacer(Modifier.height(4.dp))
            Text(TimeSaved.tangible(state.weeklySavedMs), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PendingBanner(count: Int, onOpen: () -> Unit) {
    LifesaverCard(modifier = Modifier.clickableCard(onOpen)) {
        Text("PENDING CHANGES", style = MaterialTheme.typography.bodySmall, color = Warning)
        Spacer(Modifier.height(4.dp))
        Text(
            "$count setting change${if (count == 1) "" else "s"} waiting on the 24h rule. Tap to review.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BudgetRow(label: String, usageMs: Long, budgetMs: Long, enforcing: Boolean) {
    val remainingMin = BudgetEngine.remainingMinutes(budgetMs, usageMs, 0)
    val fraction = BudgetEngine.remainingFraction(budgetMs, usageMs, 0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        val used = (usageMs / 60_000L).toInt()
        Text(
            if (enforcing) "$remainingMin MIN LEFT" else "$used MIN TODAY",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
    }
    Spacer(Modifier.height(6.dp))
    BudgetBar(fraction = if (enforcing) fraction else 1f)
}

@Composable
private fun StreakCard(state: DashboardState) {
    LifesaverCard {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${state.streaks.current}", style = MaterialTheme.typography.displaySmall)
            Text("DAY STREAK", style = MaterialTheme.typography.bodySmall, color = Accent)
        }
        Text(
            "Longest: ${state.streaks.longest}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SubstitutionCard(state: DashboardState) {
    LifesaverCard {
        Text("${state.substitutionPercent}%", style = MaterialTheme.typography.displaySmall)
        Text(
            "OF INTERVENTIONS REDIRECTED",
            style = MaterialTheme.typography.bodySmall,
            color = Accent,
        )
        Spacer(Modifier.height(8.dp))
        BudgetBar(fraction = state.substitutionPercent / 100f)
    }
}

// Make a card tappable (ripple included by Modifier.clickable's default indication).
private fun Modifier.clickableCard(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
