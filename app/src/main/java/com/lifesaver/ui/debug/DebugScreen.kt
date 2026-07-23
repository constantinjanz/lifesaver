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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.service.LifesaverAccessibilityService
import com.lifesaver.service.Permissions
import kotlinx.coroutines.delay

/** Utilitarian, and LIVE: it re-reads the service's detector state every second so you can watch
 *  view IDs appear while scrolling Instagram/YouTube. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(permissions: Map<Permissions.Kind, Boolean>, onBack: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }

    com.lifesaver.ui.components.glass.GlassScreen(title = "Debug", onBack = onBack, seed = 6) { padding ->
        // Reference tick so the whole list re-reads the (plain) static values each second.
        val ids = tick.let { LifesaverAccessibilityService.lastSeenViewIds }
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
            Text("LIVE (updates each second)", style = MaterialTheme.typography.bodySmall, color = com.lifesaver.ui.theme.Accent)
            Line("accessibility.connected", LifesaverAccessibilityService.isConnected.toString())
            Line("last.foreground.pkg", LifesaverAccessibilityService.lastForegroundPackage ?: "—")
            Line("last.foreground.isTarget", LifesaverAccessibilityService.lastForegroundIsTarget.toString())
            Line("surface.isFast(2x)", LifesaverAccessibilityService.lastSurfaceFast.toString())
            Line("last.scan", LifesaverAccessibilityService.lastScanSummary)
            Line("seen.id.count", ids.size.toString())
            permissions.forEach { (k, v) -> Line("perm.${k.name.lowercase()}", v.toString()) }
            Text(
                "seen view ids (last target window):",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            ids.forEach { Line("  ", it) }
        }
    }
}

@Composable
private fun Line(key: String, value: String) {
    Text("$key = $value", style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
}
