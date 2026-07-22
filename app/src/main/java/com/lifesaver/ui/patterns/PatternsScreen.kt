package com.lifesaver.ui.patterns

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.domain.PatternsData
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassScreen
import com.lifesaver.ui.components.glass.Heatmap
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.TextCaption
import com.lifesaver.ui.theme.TextSecondary

@Composable
fun PatternsScreen(loadPatterns: suspend () -> PatternsData, onBack: () -> Unit) {
    val data by produceState(PatternsData.EMPTY) { value = loadPatterns() }
    GlassScreen(title = "Patterns", onBack = onBack, seed = 1) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassPanel {
                Text("When you scroll", style = MaterialTheme.typography.bodyLarge)
                Text("Hour × weekday · last ${data.daysCovered} days", style = MaterialTheme.typography.bodySmall, color = TextCaption)
                Spacer(Modifier.height(12.dp))
                Heatmap(grid = data.heatmap)
            }

            if (data.riskWindows.isNotEmpty()) {
                GlassPanel {
                    Text("Your risk windows", style = MaterialTheme.typography.bodySmall, color = Accent)
                    Spacer(Modifier.height(6.dp))
                    Text(data.riskWindows.joinToString(" · "), style = MaterialTheme.typography.titleLarge)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatTile("Reels / Shorts", "${data.reelsSharePct}%", "of your time", Modifier.weight(1f))
                StatTile("After midnight", "${data.postMidnightMinPerDay}m", "per day", Modifier.weight(1f))
            }
            GlassPanel {
                Text("First-touch mornings", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("${data.firstTouchDays} days", style = MaterialTheme.typography.titleLarge)
                Text("you reached for it before 9am", style = MaterialTheme.typography.bodySmall, color = TextCaption)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    com.lifesaver.ui.components.glass.GlassTile(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = TextCaption)
    }
}
