package com.lifesaver.ui.life

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifesaver.data.LifeLog
import com.lifesaver.ui.LifesaverViewModel
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.components.glass.GlassScreen
import com.lifesaver.ui.components.glass.GlassTile
import com.lifesaver.ui.components.glass.RingGauge
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.TextCaption
import com.lifesaver.ui.theme.TextSecondary
import com.lifesaver.ui.theme.clickableNoRipple
import kotlinx.coroutines.flow.Flow

@Composable
fun LifePage(
    logsFlow: Flow<List<LifeLog>>,
    focusArea: String?,
    focusTarget: Int,
    onLog: (String, Int) -> Unit,
    onSetFocus: (String, Int) -> Unit,
) {
    val logs by logsFlow.collectAsStateWithLifecycle(initialValue = emptyList<LifeLog>())
    var minutes by remember { mutableIntStateOf(30) }
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(top = topInset + 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Life", style = MaterialTheme.typography.headlineSmall)
            // Weekly focus ring.
            if (focusArea != null && focusTarget > 0) {
                val done = logs.count { it.area == focusArea }
                GlassPanel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        RingGauge(progress = (done.toFloat() / focusTarget).coerceIn(0f, 1f), diameter = 72.dp, stroke = 6.dp) {
                            Text("$done/$focusTarget", style = MaterialTheme.typography.titleMedium)
                        }
                        Column {
                            Text("This week's focus", style = MaterialTheme.typography.bodySmall, color = Accent)
                            Text(LifesaverViewModel.areaLabel(focusArea), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            } else {
                FocusSetter(onSetFocus)
            }

            // Minutes stepper.
            GlassPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Log $minutes min", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill("−15", onClick = { minutes = (minutes - 15).coerceAtLeast(15) })
                        GlassPill("+15", onClick = { minutes = (minutes + 15).coerceAtMost(240) })
                    }
                }
            }

            // Four area tiles.
            LifesaverViewModel.LIFE_AREAS.chunked(2).forEach { rowAreas ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    rowAreas.forEach { area ->
                        val count = logs.count { it.area == area }
                        AreaTile(area, count, Modifier.weight(1f)) { onLog(area, minutes) }
                    }
                }
            }
        }
}

@Composable
private fun AreaTile(area: String, count: Int, modifier: Modifier, onLog: () -> Unit) {
    var pulse by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pulse) 1.06f else 1f, spring(), label = "pulse", finishedListener = { pulse = false })
    GlassTile(
        modifier = modifier.scale(scale).clickableNoRipple(onClick = { pulse = true; onLog() }),
    ) {
        Text(LifesaverViewModel.areaLabel(area), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("$count logged", style = MaterialTheme.typography.bodySmall, color = TextCaption)
    }
}

@Composable
private fun FocusSetter(onSetFocus: (String, Int) -> Unit) {
    var area by remember { mutableStateOf(LifesaverViewModel.LIFE_AREAS.first()) }
    var target by remember { mutableIntStateOf(3) }
    GlassPanel {
        Text("Pick one focus for this week", style = MaterialTheme.typography.bodyLarge)
        Text("Just one area, one concrete target. The rest stay loggable.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LifesaverViewModel.LIFE_AREAS.forEach { a ->
                GlassPill(LifesaverViewModel.areaLabel(a), onClick = { area = a }, primary = a == area)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Target: $target", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("−", onClick = { target = (target - 1).coerceAtLeast(1) })
                GlassPill("+", onClick = { target = (target + 1).coerceAtMost(14) })
            }
        }
        Spacer(Modifier.height(10.dp))
        GlassPill("Set focus", onClick = { onSetFocus(area, target) }, primary = true, modifier = Modifier.fillMaxWidth())
    }
}
