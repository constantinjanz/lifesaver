package com.lifesaver.ui.components.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.GlassTintStrong
import com.lifesaver.ui.theme.TextPrimary
import com.lifesaver.ui.theme.clickableNoRipple
import dev.chrisbanes.haze.hazeEffect

/**
 * Glass pill button (§5). Primary = accent-tinted glass; secondary = neutral glass. Sentence case,
 * never all caps. Press springs to 0.97.
 */
@Composable
fun GlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(), label = "press")
    val shape = RoundedCornerShape(percent = 50)
    val tint = if (primary) Accent.copy(alpha = 0.22f) else GlassTintStrong
    val borderColor = if (primary) Accent.copy(alpha = 0.40f) else com.lifesaver.ui.theme.GlassBorder

    Row(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.35f)
            .glassSurfacePill(shape, tint, borderColor)
            .clickableNoRipple(enabled = enabled, interactionSource = interaction, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        CompositionLocalProvider(LocalContentColor provides TextPrimary) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

/** Pill variant of the glass surface with a custom border tint (accent for primary). */
@Composable
private fun Modifier.glassSurfacePill(
    shape: androidx.compose.ui.graphics.Shape,
    tint: Color,
    borderColor: Color,
): Modifier {
    val blur = com.lifesaver.ui.theme.LocalGlass.current.blurEnabled
    val haze = LocalHazeState.current
    return this
        .clip(shape)
        .then(
            if (blur && haze != null) {
                Modifier.hazeEffect(state = haze) {
                    blurRadius = 20.dp
                    backgroundColor = com.lifesaver.ui.theme.Base
                }
            } else {
                Modifier.background(com.lifesaver.ui.theme.GlassFallback)
            },
        )
        .background(tint)
        .border(1.dp, borderColor, shape)
        .topLightEdge()
}
