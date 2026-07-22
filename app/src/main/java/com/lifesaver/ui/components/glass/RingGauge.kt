package com.lifesaver.ui.components.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.GlassTrack

/**
 * The signature cockpit instrument (§5): circular progress ring, 8dp rounded-cap stroke, glass
 * track, accent fill, centered value + label. Progress springs in on entry.
 */
@Composable
fun RingGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    stroke: Dp = 8.dp,
    color: Color = Accent,
    track: Color = GlassTrack,
    animateOnEntry: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    var target by remember { mutableStateOf(if (animateOnEntry) 0f else progress) }
    androidx.compose.runtime.LaunchedEffect(progress) { target = progress }
    val animated by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
        label = "ring",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val sw = stroke.toPx()
            val inset = sw / 2f
            val arc = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arc, style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            if (animated > 0f) {
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                    topLeft = topLeft, size = arc, style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
