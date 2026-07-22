package com.lifesaver.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.GlassTintStrong
import com.lifesaver.ui.theme.TextPrimary
import com.lifesaver.ui.theme.clickableNoRipple

data class DockItem(val icon: ImageVector, val label: String)

/** Detached glass tab bar (§5): icon-only items, active = white 100% + tiny dot, inactive white 45%. */
@Composable
fun FloatingDock(
    items: List<DockItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .glassSurface(RoundedCornerShape(percent = 50), GlassTintStrong)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val active = i == selected
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (active) TextPrimary else TextPrimary.copy(alpha = 0.45f),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .clickableNoRipple(onClick = { onSelect(i) })
                        .padding(11.dp),
                )
                if (active) {
                    Box(
                        modifier = Modifier
                            .padding(top = 34.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Accent),
                    )
                }
            }
        }
    }
}
