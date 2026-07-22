package com.lifesaver.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Runtime glass config. [blurEnabled] drives backdrop blur vs the solid fallback (§2). */
data class GlassConfig(val blurEnabled: Boolean)

val LocalGlass = staticCompositionLocalOf { GlassConfig(blurEnabled = true) }

/** Global, debug-toggleable blur preference (Debug screen A/B, §8). Defaults to on when the OS
 *  can render backdrop blur (API 31+); off below that. */
object GlassPrefs {
    private val default = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var blurEnabled by mutableStateOf(default)
}

private val LifesaverColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = Accent,
    onSecondary = OnAccent,
    background = Base,
    onBackground = TextPrimary,
    surface = Base,
    onSurface = TextPrimary,
    surfaceVariant = Base,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = TextPrimary,
    outline = GlassBorder,
    outlineVariant = HairlineDivider,
)

@Composable
fun LifesaverTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge over the ambient background; bars transparent.
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    CompositionLocalProvider(LocalGlass provides GlassConfig(blurEnabled = GlassPrefs.blurEnabled)) {
        MaterialTheme(
            colorScheme = LifesaverColorScheme,
            typography = LifesaverTypography,
            shapes = LifesaverShapes,
            content = content,
        )
    }
}
