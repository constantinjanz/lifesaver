package com.lifesaver.ui.components.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import com.lifesaver.ui.theme.Base
import com.lifesaver.ui.theme.BlobBlue
import com.lifesaver.ui.theme.BlobOrange
import com.lifesaver.ui.theme.BlobViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * The light source behind the glass (DESIGN v2 §1): near-black base + 2–3 large, extremely soft
 * radial color blobs. Radial gradients are inherently formless, so no blur pass is needed here.
 * [seed] shifts blob positions so screens feel distinct; [drift] enables the slow 60s loop
 * (cockpit only).
 */
@Composable
fun AmbientBackground(seed: Int, drift: Boolean, modifier: Modifier = Modifier) {
    val blobs = remember(seed) { blobsFor(seed) }

    val phase: Float = if (drift) {
        val transition = rememberInfiniteTransition(label = "ambient")
        val p by transition.animateFloat(
            initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        p
    } else {
        0f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Base)
        blobs.forEachIndexed { i, b ->
            val wobble = if (drift) 0.04f else 0f
            val cx = (b.x + wobble * cos(phase + i)) * size.width
            val cy = (b.y + wobble * sin(phase + i * 1.3f)) * size.height
            val radius = b.radius * size.maxDimension
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(b.color.copy(alpha = b.alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
    }
}

private data class Blob(val x: Float, val y: Float, val radius: Float, val color: Color, val alpha: Float)

private fun blobsFor(seed: Int): List<Blob> {
    // A few distinct arrangements; screens pick by seed so no two feel identical.
    val sets = listOf(
        listOf(
            Blob(0.18f, 0.16f, 0.9f, BlobOrange, 0.12f),
            Blob(0.85f, 0.30f, 0.8f, BlobViolet, 0.10f),
            Blob(0.55f, 0.92f, 1.0f, BlobBlue, 0.08f),
        ),
        listOf(
            Blob(0.80f, 0.12f, 0.85f, BlobOrange, 0.12f),
            Blob(0.12f, 0.55f, 0.9f, BlobBlue, 0.08f),
            Blob(0.70f, 0.88f, 0.8f, BlobViolet, 0.10f),
        ),
        listOf(
            Blob(0.20f, 0.85f, 0.95f, BlobViolet, 0.10f),
            Blob(0.90f, 0.70f, 0.8f, BlobOrange, 0.12f),
            Blob(0.40f, 0.10f, 0.9f, BlobBlue, 0.08f),
        ),
    )
    return sets[((seed % sets.size) + sets.size) % sets.size]
}
