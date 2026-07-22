package com.lifesaver.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.lifesaver.ui.theme.TextCaption

/** Hour×weekday heatmap (§7): rounded cells, white-opacity scale, accent for the hottest. */
@Composable
fun Heatmap(grid: List<List<Int>>, modifier: Modifier = Modifier) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val max = (grid.flatten().maxOrNull() ?: 0).coerceAtLeast(1)
    val threshold = max * 0.7f
    Column(modifier = modifier.fillMaxWidth()) {
        grid.forEachIndexed { wd, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(days.getOrElse(wd) { "" }, style = MaterialTheme.typography.bodySmall, color = TextCaption, modifier = Modifier.width(14.dp))
                row.forEach { v ->
                    val alpha = 0.06f + 0.85f * (v.toFloat() / max)
                    val color = if (v >= threshold && v > 0) Accent.copy(alpha = 0.9f) else androidx.compose.ui.graphics.Color.White.copy(alpha = alpha)
                    Box(
                        modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(2.dp)).background(color),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "23").forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextCaption)
            }
        }
    }
}
