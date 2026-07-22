package com.lifesaver.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.theme.Base
import com.lifesaver.ui.theme.GlassBorder
import com.lifesaver.ui.theme.GlassFallback
import com.lifesaver.ui.theme.GlassLightEdge
import com.lifesaver.ui.theme.GlassTint
import com.lifesaver.ui.theme.LocalGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** The screen-level Haze source. Panels read this to blur the ambient background behind them. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Wraps a screen: ambient background (blur source) + content on top. Every screen uses this so
 * `GlassPanel`s have something luminous to refract (DESIGN v2 §1–§2).
 */
@Composable
fun GlassBackground(
    seed: Int,
    drift: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = remember { HazeState() }
    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(seed, drift, Modifier.matchParentSize().hazeSource(haze))
        CompositionLocalProvider(LocalHazeState provides haze) {
            content()
        }
    }
}

/**
 * The one glass material (§2): backdrop blur (or solid fallback), white tint, hairline border,
 * top inner light edge, rounded corners, floating shadow.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tint: Color = GlassTint,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier.glassSurface(shape, tint)) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** 16dp-radius small tile (§2). */
@Composable
fun GlassTile(
    modifier: Modifier = Modifier,
    tint: Color = GlassTint,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) = GlassPanel(modifier, cornerRadius = 16.dp, tint = tint, contentPadding = contentPadding, content = content)

/** The glass surface modifier chain, reusable by pills/tiles/sheets. */
@Composable
fun Modifier.glassSurface(shape: Shape, tint: Color = GlassTint): Modifier {
    val blur = LocalGlass.current.blurEnabled
    val haze = LocalHazeState.current
    val blurred = blur && haze != null
    return this
        .shadow(elevation = 16.dp, shape = shape, clip = false)
        .clip(shape)
        .then(
            if (blurred) {
                Modifier.hazeEffect(state = haze!!) {
                    blurRadius = 24.dp
                    backgroundColor = Base
                }
            } else {
                Modifier.background(GlassFallback)
            },
        )
        .background(tint)
        .border(1.dp, GlassBorder, shape)
        .topLightEdge()
}

/** The polished-glass top highlight: a 1dp inner line, brightest at center, fading at the corners. */
fun Modifier.topLightEdge(): Modifier = drawWithContent {
    drawContent()
    val inset = 18.dp.toPx()
    val y = 1.dp.toPx()
    if (size.width <= inset * 2) return@drawWithContent
    drawLine(
        brush = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.2f to GlassLightEdge,
            0.8f to GlassLightEdge,
            1.0f to Color.Transparent,
            startX = inset,
            endX = size.width - inset,
        ),
        start = Offset(inset, y),
        end = Offset(size.width - inset, y),
        strokeWidth = 1.dp.toPx(),
    )
}
