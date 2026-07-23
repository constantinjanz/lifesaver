package com.lifesaver.ui.report

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.domain.ReportData
import com.lifesaver.domain.TimeSaved
import com.lifesaver.ui.LifesaverViewModel
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.components.glass.GlassRow
import com.lifesaver.ui.components.glass.GlassRowDivider
import com.lifesaver.ui.components.glass.GlassScreen
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.Danger
import com.lifesaver.ui.theme.TextSecondary

@Composable
fun ReportScreen(
    loadReport: suspend () -> ReportData,
    onSubmitCheckin: (Int, Int, Int) -> Unit,
    onBack: () -> Unit,
) {
    val data by produceState(ReportData.EMPTY) { value = loadReport() }
    GlassScreen(title = "Weekly report", onBack = onBack, seed = 3) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassPanel {
                Text("Screen time vs your baseline", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                data.perApp.forEachIndexed { i, app ->
                    if (i > 0) GlassRowDivider()
                    GlassRow(app.label, "${TimeSaved.formatHm(app.actualMs)} / ${TimeSaved.formatHm(app.baselineMs)}")
                }
            }

            GlassPanel {
                Text(TimeSaved.formatHm(data.savedMs), style = MaterialTheme.typography.displaySmall, color = Accent)
                Text("reclaimed this week · ${TimeSaved.tangible(data.savedMs).lowercase()}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                GlassRow("Interventions redirected", "${data.substitutionPct}%")
            }

            GlassPanel {
                Text("Integrity", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                GlassRow(
                    "Tracking gaps",
                    if (data.trackingGapMs > 0) TimeSaved.formatHm(data.trackingGapMs) else "none",
                    valueColor = if (data.trackingGapMs > 0) Danger else Accent,
                )
                if (data.unlocks.isEmpty()) {
                    GlassRowDivider()
                    GlassRow("Emergency unlocks", "none used")
                } else {
                    data.unlocks.forEach { u ->
                        GlassRowDivider()
                        GlassRow("Unlock · ${u.dayKey}", u.reason, valueColor = TextSecondary)
                    }
                }
            }

            if (data.lifeMinutesByArea.isNotEmpty() || data.focusText != null) {
                GlassPanel {
                    Text("Life", style = MaterialTheme.typography.bodyLarge)
                    if (data.focusText != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(data.focusText!!, style = MaterialTheme.typography.titleLarge, color = Accent)
                    }
                    Spacer(Modifier.height(6.dp))
                    data.lifeMinutesByArea.forEach { (area, min) ->
                        GlassRow(LifesaverViewModel.areaLabel(area), TimeSaved.formatHm(min * 60_000L))
                    }
                }
            }

            if (data.intentions.isNotEmpty()) {
                GlassPanel {
                    Text("Why you opened them", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(6.dp))
                    val total = data.intentions.sumOf { it.second }.coerceAtLeast(1)
                    data.intentions.forEachIndexed { i, (intention, count) ->
                        if (i > 0) GlassRowDivider()
                        val pct = count * 100 / total
                        GlassRow(
                            intention,
                            "$pct%",
                            valueColor = if (intention.equals("Just browsing", true)) Danger else Accent,
                        )
                    }
                    val browsing = data.intentions.firstOrNull { it.first.equals("Just browsing", true) }?.second ?: 0
                    if (browsing > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${browsing * 100 / total}% of your opens had no real goal. That's the scroll talking.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                        )
                    }
                }
            }

            CheckinBlock(data.checkin, onSubmitCheckin)
        }
    }
}

@Composable
private fun CheckinBlock(existing: ReportData.CheckinValues?, onSubmit: (Int, Int, Int) -> Unit) {
    var control by remember { mutableFloatStateOf((existing?.control ?: 5).toFloat()) }
    var satisfaction by remember { mutableFloatStateOf((existing?.satisfaction ?: 5).toFloat()) }
    var impulse by remember { mutableFloatStateOf((existing?.impulse ?: 5).toFloat()) }
    GlassPanel {
        Text("This week's check-in", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        SliderRow("Sense of control", control) { control = it }
        SliderRow("Satisfaction with your time", satisfaction) { satisfaction = it }
        SliderRow("Scroll impulse", impulse) { impulse = it }
        Spacer(Modifier.height(8.dp))
        GlassPill("Save check-in", onClick = { onSubmit(control.toInt(), satisfaction.toInt(), impulse.toInt()) }, primary = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("${value.toInt()}", style = MaterialTheme.typography.bodyLarge, color = Accent)
    }
    Slider(value = value, onValueChange = onChange, valueRange = 1f..10f, steps = 8)
}
