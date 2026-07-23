package com.lifesaver.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.LifesaverApp
import com.lifesaver.data.IfThenPlan
import com.lifesaver.data.RedirectApp
import com.lifesaver.service.InstalledApps
import com.lifesaver.ui.components.AppIcon
import com.lifesaver.ui.components.AppPickerDialog
import com.lifesaver.ui.components.FlatButton
import com.lifesaver.ui.components.LifesaverCard
import com.lifesaver.ui.theme.TextCaption
import com.lifesaver.ui.theme.TextSecondary
import com.lifesaver.ui.theme.clickableNoRipple

/**
 * Editor for the mission pool (the activities the pause offers) + redirect apps. Missions are a
 * flat, add-as-many list — the pause quotes ONE at random each time, so more missions = more
 * variety. Stored as if-then plans (context "mission"). Used by onboarding and the plans screen.
 */
@Composable
fun PlanEditor(
    plans: List<IfThenPlan>,
    redirects: List<RedirectApp>,
    onPlansChange: (List<IfThenPlan>) -> Unit,
    onRedirectsChange: (List<RedirectApp>) -> Unit,
) {
    val missions: SnapshotStateList<String> = remember {
        plans.map { it.text }.ifEmpty { listOf("") }.toMutableStateList()
    }

    fun emit() {
        val list = missions.map { it.trim() }.filter { it.isNotBlank() }
            .map { IfThenPlan(context = "mission", label = "Mission", text = it) }
        onPlansChange(list)
    }

    Column {
        Text("Your missions", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Short things to do instead of scrolling. Add as many as you like — the pause hands you one at random each time.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))

        missions.forEachIndexed { i, value ->
            LifesaverCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { missions[i] = it; emit() },
                        placeholder = { Text(HINTS[i % HINTS.size]) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = TextCaption,
                        modifier = Modifier.size(28.dp).clickableNoRipple(
                            onClick = { missions.removeAt(i); if (missions.isEmpty()) missions.add(""); emit() },
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        FlatButton("Add mission", onClick = { missions.add("") })

        Spacer(Modifier.height(16.dp))
        RedirectSection(redirects, onRedirectsChange)
    }
}

private val HINTS = listOf(
    "Sketch for 5 minutes",
    "Text someone you miss",
    "Step outside / sunlight",
    "Put on one song and just listen",
    "Read a page of your book",
    "10 push-ups",
)

@Composable
private fun RedirectSection(
    redirects: List<RedirectApp>,
    onChange: (List<RedirectApp>) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val apps by produceState(initialValue = emptyList<InstalledApps.Entry>()) {
        value = LifesaverApp.instance.container.installedApps.launchable()
    }
    var selected by remember { mutableStateOf(redirects.map { it.appId }.toSet()) }

    Text("Redirect apps", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "One tap from the pause screen jumps to one of these instead of scrolling.",
        style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
    )
    Spacer(Modifier.height(12.dp))
    LifesaverCard {
        if (redirects.isEmpty()) {
            Text("None chosen yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                redirects.forEach { r ->
                    val entry = apps.firstOrNull { it.packageName == r.appId }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIcon(entry?.icon)
                        Text(r.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FlatButton("Choose apps", onClick = { pickerOpen = true })
        }
    }

    if (pickerOpen) {
        AppPickerDialog(
            apps = apps,
            selected = selected,
            maxSelect = 3,
            onToggle = { pkg -> selected = if (pkg in selected) selected - pkg else selected + pkg },
            onDismiss = { pickerOpen = false },
            onConfirm = {
                pickerOpen = false
                onChange(apps.filter { it.packageName in selected }.map { RedirectApp(it.packageName, it.label) })
            },
        )
    }
}
