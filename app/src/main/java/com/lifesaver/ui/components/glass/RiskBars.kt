package com.lifesaver.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.BarDefault
import com.lifesaver.ui.theme.TextCaption

/**
 * Slim rounded bars (§5), e.g. risk-hours. No axes, no gridlines. Peaks glow accent; a caption
 * sits below ("peak risk · 22:00"). [values] are normalized-ish; they're scaled to the max.
 */
@Composable
fun RiskBars(
    values: List<Float>,
    modifier: Modifier = Modifier,
    caption: String? = null,
    peakIndices: Set<Int> = emptySet(),
    barHeight: androidx.compose.ui.unit.Dp = 64.dp,
) {
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(barHeight),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { i, v ->
                val frac = (v / max).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(frac.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (i in peakIndices) Accent else BarDefault),
                )
            }
        }
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = TextCaption,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
