package com.lifesaver.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.theme.Success
import com.lifesaver.ui.theme.TextPrimary

/** Cockpit status pill (§5, §7): a dot indicator + text, e.g. "All systems on" / "Tracking gap today". */
@Composable
fun StatusPill(text: String, dotColor: Color = Success, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.glassSurface(RoundedCornerShape(percent = 50), com.lifesaver.ui.theme.GlassTintStrong)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor),
        )
        Text(text, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}
