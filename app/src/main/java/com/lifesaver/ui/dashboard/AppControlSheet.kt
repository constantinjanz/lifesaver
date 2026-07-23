package com.lifesaver.ui.dashboard

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
    settings: Settings,
    onChangeBudget: (String, Int) -> Unit,
    onSetReelsLimit: (String, Int?) -> Unit,
    onSetBlockedWindow: (String, BlockWindow?) -> Unit,
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

                // Reels/Shorts limit.
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

                // Lock hours.
                LockRow(appId, surfaceLabel = label, window = settings.blockedWindow(appId), onChange = { onSetBlockedWindow(appId, it) })

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GlassPill("Done", onClick = onDismiss, primary = true)
                }
            }
        }
    }
}

@Composable
private fun LockRow(appId: String, surfaceLabel: String, window: BlockWindow?, onChange: (BlockWindow?) -> Unit) {
    val context = LocalContext.current
    val enabled = window != null
    val start = window?.startMinute ?: 7 * 60
    val end = window?.endMinute ?: 10 * 60
    fun pick(initial: Int, onPicked: (Int) -> Unit) {
        android.app.TimePickerDialog(context, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, true).show()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Lock hours", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { on -> onChange(if (on) BlockWindow(start, end) else null) })
    }
    if (enabled) {
        Text("Fully blocked — no budget, no unlock — during:", style = MaterialTheme.typography.bodySmall, color = Warning)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPill("From ${ScheduleBlock.format(start)}", onClick = { pick(start) { onChange(BlockWindow(it, end)) } })
            GlassPill("To ${ScheduleBlock.format(end)}", onClick = { pick(end) { onChange(BlockWindow(start, it)) } })
        }
    }
}
