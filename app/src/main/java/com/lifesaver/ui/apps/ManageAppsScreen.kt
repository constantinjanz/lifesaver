package com.lifesaver.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.domain.TimeSaved
import com.lifesaver.service.InstalledApps
import com.lifesaver.ui.components.AppIcon
import com.lifesaver.ui.components.LifesaverCard
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.components.glass.GlassScreen
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.TextCaption
import com.lifesaver.ui.theme.TextSecondary

/** Installed apps + their average daily foreground time. */
data class AppCatalog(
    val installed: List<InstalledApps.Entry>,
    val avgDailyMs: Map<String, Long>,
)

/** Packages that are usually "necessary" — kept out of the high-usage suggestions so we don't nudge
 *  the user to limit messengers, phone, maps, etc. They can still be added from the full list. */
private val NECESSARY = setOf(
    "com.whatsapp", "com.whatsapp.w4b",
    "org.thoughtcrime.securesms", // Signal
    "org.telegram.messenger",
    "com.facebook.orca", // Messenger
    "com.google.android.gm", // Gmail
    "com.google.android.apps.maps",
    "com.google.android.calendar", "com.samsung.android.calendar",
    "com.google.android.apps.messaging", "com.samsung.android.messaging",
    "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
    "com.android.settings",
)

/**
 * Manage which apps Lifesaver budgets (user feedback #4/#5). Shows the apps already tracked, a few
 * high-usage suggestions (excluding messengers & co.), and the full searchable installed list.
 * Custom apps get budget + breathe + lock-hours; Reels/Shorts detection stays Instagram/YouTube-only.
 */
@Composable
fun ManageAppsScreen(
    enabledApps: Set<String>,
    loadCatalog: suspend () -> AppCatalog,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onBack: () -> Unit,
) {
    val catalog by produceState<AppCatalog?>(initialValue = null) { value = loadCatalog() }
    var query by remember { mutableStateOf("") }

    GlassScreen(title = "Apps", onBack = onBack, seed = 3) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Pick which apps get a budget, breathe pause and lock hours. Reels/Shorts blocking stays specific to Instagram & YouTube.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            )

            val cat = catalog
            val labelOf: (String) -> String = { pkg ->
                DetectionConfig.targetFor(pkg)?.label ?: cat?.installed?.firstOrNull { it.packageName == pkg }?.label ?: pkg
            }
            val iconOf: (String) -> android.graphics.drawable.Drawable? = { pkg ->
                cat?.installed?.firstOrNull { it.packageName == pkg }?.icon
            }

            // --- Tracked apps ---
            Category("TRACKED")
            LifesaverCard {
                if (enabledApps.isEmpty()) {
                    Text("No apps tracked yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else {
                    // Targets first, then custom.
                    val ordered = DetectionConfig.targets.map { it.appId }.filter { it in enabledApps } +
                        enabledApps.filter { it !in DetectionConfig.targetPackages }
                    ordered.forEach { pkg ->
                        AppRow(
                            label = labelOf(pkg),
                            icon = iconOf(pkg),
                            subtitle = cat?.avgDailyMs?.get(pkg)?.let { "${TimeSaved.formatHm(it)} / day" },
                            trailing = { GlassPill("Remove", onClick = { onRemoveApp(pkg) }) },
                        )
                    }
                }
            }

            // --- Suggestions ---
            if (cat != null) {
                val suggestions = cat.avgDailyMs.entries.asSequence()
                    .filter { it.key !in enabledApps }
                    .filter { it.key !in NECESSARY }
                    .filter { it.key != "com.lifesaver" }
                    .filter { e -> cat.installed.any { it.packageName == e.key } } // launchable only
                    .filter { it.value >= 10 * 60_000L } // ≥ 10 min/day
                    .take(6).toList()
                if (suggestions.isNotEmpty()) {
                    Category("SUGGESTIONS · HIGH SCREEN TIME")
                    LifesaverCard {
                        suggestions.forEach { (pkg, ms) ->
                            AppRow(
                                label = labelOf(pkg),
                                icon = iconOf(pkg),
                                subtitle = "${TimeSaved.formatHm(ms)} / day",
                                trailing = { GlassPill("Add", onClick = { onAddApp(pkg) }, primary = true) },
                            )
                        }
                    }
                }
            }

            // --- Full list ---
            Category("ALL APPS")
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search apps") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            if (cat == null) {
                Text("Loading…", style = MaterialTheme.typography.bodySmall, color = TextCaption)
            } else {
                val q = query.trim().lowercase()
                val list = cat.installed
                    .filter { it.packageName !in enabledApps }
                    .filter { q.isEmpty() || it.label.lowercase().contains(q) }
                    .take(if (q.isEmpty()) 40 else 60)
                LifesaverCard {
                    if (list.isEmpty()) {
                        Text("Nothing matches.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    } else {
                        list.forEach { entry ->
                            AppRow(
                                label = entry.label,
                                icon = entry.icon,
                                subtitle = cat.avgDailyMs[entry.packageName]?.takeIf { it > 0 }?.let { "${TimeSaved.formatHm(it)} / day" },
                                trailing = { GlassPill("Add", onClick = { onAddApp(entry.packageName) }) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Category(title: String) {
    Text(title, style = MaterialTheme.typography.bodySmall, color = Accent, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun AppRow(
    label: String,
    icon: android.graphics.drawable.Drawable?,
    subtitle: String?,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(icon, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        trailing()
    }
}
