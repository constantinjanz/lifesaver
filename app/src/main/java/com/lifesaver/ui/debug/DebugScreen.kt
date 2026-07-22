package com.lifesaver.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.service.LifesaverAccessibilityService
import com.lifesaver.service.Permissions

/** Deliberately utilitarian (DESIGN.md §6 — exempt from the design system). Live view IDs land in M5. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(permissions: Map<Permissions.Kind, Boolean>, onBack: () -> Unit) {
    com.lifesaver.ui.components.glass.GlassScreen(title = "Debug", onBack = onBack, seed = 6) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text("Backdrop blur (A/B)", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Switch(
                    checked = com.lifesaver.ui.theme.GlassPrefs.blurEnabled,
                    onCheckedChange = { com.lifesaver.ui.theme.GlassPrefs.blurEnabled = it },
                )
            }
            Line("accessibility.connected", LifesaverAccessibilityService.isConnected.toString())
            Line("last.foreground.pkg", LifesaverAccessibilityService.lastForegroundPackage ?: "—")
            Line("last.foreground.isTarget", LifesaverAccessibilityService.lastForegroundIsTarget.toString())
            Line("surface.isFast(2x)", LifesaverAccessibilityService.lastSurfaceFast.toString())
            permissions.forEach { (k, v) -> Line("perm.${k.name.lowercase()}", v.toString()) }
            Text(
                "seen view ids (last target window):",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            LifesaverAccessibilityService.lastSeenViewIds.forEach { Line("  ", it) }
        }
    }
}

@Composable
private fun Line(key: String, value: String) {
    Text("$key = $value", style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
}
