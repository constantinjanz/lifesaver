package com.lifesaver.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lifesaver.data.Settings
import com.lifesaver.domain.BlockWindow
import com.lifesaver.domain.ScheduleBlock
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.TextSecondary
import com.lifesaver.ui.theme.Warning

/** Per-app controls, opened by tapping an app tile on the cockpit (keeps Settings lean). */
@Composable
fun AppControlSheet(
    appId: String,
    label: String,
    surfaceName: String,
    isTarget: Boolean,
    settings: Settings,
    onChangeBudget: (String, Int) -> Unit,
    onSetReelsLimit: (String, Int?) -> Unit,
    onSetBlockedWindows: (String, List<BlockWindow>) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(label, style = MaterialTheme.typography.titleLarge)
                Text("Tune this app right here.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(16.dp))

                // Daily budget.
                var budget by remember { mutableStateOf(settings.budgetMin(appId).toFloat()) }
                Text("Daily budget · ${budget.toInt()} min", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = budget, onValueChange = { budget = it },
                    onValueChangeFinished = { onChangeBudget(appId, budget.toInt()) },
                    valueRange = 10f..120f, steps = (120 - 10) / 5 - 1,
                )
                Text("Loosening applies after 24h; tightening is instant.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(16.dp))

                // Reels/Shorts limit — only for the known targets (custom apps have no fast surface).
                if (isTarget) {
                    val reelsLimit = settings.reelsLimitMin(appId)
                    var reelsOn by remember { mutableStateOf(reelsLimit != null) }
                    var reelsValue by remember { mutableStateOf((reelsLimit ?: minOf(10, budget.toInt())).toFloat()) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Limit $surfaceName", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(checked = reelsOn, onCheckedChange = {
                            reelsOn = it
                            onSetReelsLimit(appId, if (it) reelsValue.toInt() else null)
                        })
                    }
                    if (reelsOn) {
                        Slider(
                            value = reelsValue, onValueChange = { reelsValue = it },
                            onValueChangeFinished = { onSetReelsLimit(appId, reelsValue.toInt()) },
                            valueRange = 0f..budget, steps = 0,
                        )
                        Text(
                            if (reelsValue.toInt() == 0) "$surfaceName fully blocked" else "Max ${reelsValue.toInt()} min on $surfaceName",
                            style = MaterialTheme.typography.bodySmall, color = Accent,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Lock hours (multiple windows allowed, e.g. morning + evening).
                LockHours(windows = settings.windowsFor(appId), onChange = { onSetBlockedWindows(appId, it) })

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GlassPill("Done", onClick = onDismiss, primary = true)
                }
            }
        }
    }
}

@Composable
private fun LockHours(windows: List<BlockWindow>, onChange: (List<BlockWindow>) -> Unit) {
    val context = LocalContext.current
    fun pick(initial: Int, onPicked: (Int) -> Unit) {
        android.app.TimePickerDialog(context, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, true).show()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Lock hours", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = windows.isNotEmpty(),
            onCheckedChange = { on -> onChange(if (on) listOf(BlockWindow(7 * 60, 10 * 60)) else emptyList()) },
        )
    }
    if (windows.isNotEmpty()) {
        Text("Fully blocked — no budget, no unlock — during:", style = MaterialTheme.typography.bodySmall, color = Warning)
        Spacer(Modifier.height(8.dp))
        windows.forEachIndexed { i, w ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("From ${ScheduleBlock.format(w.startMinute)}", onClick = {
                    pick(w.startMinute) { m -> onChange(windows.replaceAt(i, BlockWindow(m, w.endMinute))) }
                })
                GlassPill("To ${ScheduleBlock.format(w.endMinute)}", onClick = {
                    pick(w.endMinute) { m -> onChange(windows.replaceAt(i, BlockWindow(w.startMinute, m))) }
                })
                Text("Remove", style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    modifier = Modifier.clickable { onChange(windows.filterIndexed { j, _ -> j != i }) })
            }
            Spacer(Modifier.height(8.dp))
        }
        GlassPill("+ Add window", onClick = { onChange(windows + BlockWindow(21 * 60, 23 * 60)) })
    }
}

private fun List<BlockWindow>.replaceAt(index: Int, w: BlockWindow): List<BlockWindow> =
    mapIndexed { i, old -> if (i == index) w else old }
