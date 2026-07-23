package com.lifesaver.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.data.Strictness
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.ui.DashboardState
import com.lifesaver.ui.components.FlatButton
import com.lifesaver.ui.components.LifesaverCard
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.Warning
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: DashboardState,
    onChangeBudget: (String, Int) -> Unit,
    onChangeStrictness: (Strictness) -> Unit,
    onCancelPending: (Long) -> Unit,
    onSetBlockedWindow: (String, com.lifesaver.domain.BlockWindow?) -> Unit,
    onSetReelsLimit: (String, Int?) -> Unit,
    onSetGrayscale: (Boolean) -> Unit,
    onOpenCheckin: () -> Unit,
    onOpenBuddy: () -> Unit,
    onOpenFutureSelf: () -> Unit,
    onBack: () -> Unit,
) {
    com.lifesaver.ui.components.glass.GlassScreen(title = "Settings", onBack = onBack, seed = 3) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.pendingChanges.isNotEmpty()) {
                Category("PENDING (24H RULE)")
                state.pendingChanges.forEach { PendingRow(it.settingKey, it.newValue, it.effectiveTs) { onCancelPending(it.id) } }
            }

            Category("BUDGETS")
            DetectionConfig.targets.forEach { target ->
                BudgetSetting(
                    label = target.label,
                    current = state.settings.budgetMin(target.appId),
                    onCommit = { onChangeBudget(target.appId, it) },
                )
            }
            Text(
                "Loosening (a bigger budget) takes effect after 24h. Tightening is instant.",
                style = MaterialTheme.typography.bodySmall,
            )

            Category("REELS & SHORTS")
            Text(
                "Give the endless-scroll surface its own limit — or block it entirely and keep only posts, DMs and normal videos.",
                style = MaterialTheme.typography.bodySmall,
            )
            DetectionConfig.targets.forEach { target ->
                ReelsLimitSetting(
                    label = target.label,
                    surfaceName = target.fastSurfaceName,
                    limitMin = state.settings.reelsLimitMin(target.appId),
                    maxMin = state.settings.budgetMin(target.appId),
                    onChange = { onSetReelsLimit(target.appId, it) },
                )
            }
            GrayscaleSetting(enabled = state.settings.grayscaleOnReels, onChange = onSetGrayscale)

            Category("APP LOCKS")
            Text(
                "Pick hours an app is fully blocked — no budget, no emergency unlock, no way around it.",
                style = MaterialTheme.typography.bodySmall,
            )
            DetectionConfig.targets.forEach { target ->
                BlockWindowSetting(
                    label = target.label,
                    window = state.settings.blockedWindow(target.appId),
                    onChange = { onSetBlockedWindow(target.appId, it) },
                )
            }

            Category("FRICTION")
            StrictnessSetting(state.settings.strictness, onChangeStrictness)

            Category("BUDDY UNLOCK")
            LifesaverCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (state.settings.buddyPaired) "Paired with ${state.settings.buddyLabel}" else "Get more time only via a trusted buddy",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "A friend approves extra time on WhatsApp with their secret PIN.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    FlatButton(if (state.settings.buddyPaired) "Open" else "Set up", onClick = onOpenBuddy)
                }
            }

            Category("NUDGES")
            LifesaverCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Future-self note", style = MaterialTheme.typography.titleMedium)
                        Text("A voice message to yourself, played at the pause.", style = MaterialTheme.typography.bodySmall)
                    }
                    FlatButton("Record", onClick = onOpenFutureSelf)
                }
            }

            Category("WEEKLY")
            LifesaverCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Weekly check-in", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    FlatButton("Open", onClick = onOpenCheckin)
                }
            }
        }
    }
}

@Composable
private fun Category(title: String) {
    Text(title, style = MaterialTheme.typography.bodySmall, color = Accent, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun BudgetSetting(label: String, current: Int, onCommit: (Int) -> Unit) {
    var value by remember(current) { mutableStateOf(current.toFloat()) }
    LifesaverCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${value.toInt()} MIN", style = MaterialTheme.typography.bodyLarge, color = Accent)
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onCommit(value.toInt()) },
            valueRange = 10f..120f,
            steps = (120 - 10) / 5 - 1,
        )
    }
}

@Composable
private fun GrayscaleSetting(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val available = remember { com.lifesaver.service.Grayscale.isAvailable(context) }
    LifesaverCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Grayscale on Reels/Shorts", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Drains the colour out of the fast feed — far less pull, no hard block.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            androidx.compose.material3.Switch(
                checked = enabled && available,
                enabled = available,
                onCheckedChange = onChange,
            )
        }
        if (!available) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Needs one adb command (personal build). Connect the phone and run:",
                style = MaterialTheme.typography.bodySmall, color = Warning,
            )
            Text(
                "adb shell pm grant com.lifesaver android.permission.WRITE_SECURE_SETTINGS",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Accent,
            )
        }
    }
}

@Composable
private fun ReelsLimitSetting(
    label: String,
    surfaceName: String,
    limitMin: Int?,
    maxMin: Int,
    onChange: (Int?) -> Unit,
) {
    val enabled = limitMin != null
    LifesaverCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$label · $surfaceName", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !enabled -> "No separate limit"
                        limitMin == 0 -> "Blocked entirely"
                        else -> "Max $limitMin min/day"
                    },
                    style = MaterialTheme.typography.bodySmall, color = Accent,
                )
            }
            androidx.compose.material3.Switch(
                checked = enabled,
                // Default to a gentle limit when turning on; user drags to 0 to block completely.
                onCheckedChange = { on -> onChange(if (on) minOf(10, maxMin) else null) },
            )
        }
        if (enabled) {
            var value by remember(limitMin) { mutableStateOf((limitMin ?: 0).toFloat()) }
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { onChange(value.toInt()) },
                valueRange = 0f..maxMin.toFloat(),
                steps = (maxMin - 1).coerceAtLeast(0),
            )
            Text(
                if (value.toInt() == 0) "Drag = 0 → $surfaceName fully blocked" else "${value.toInt()} of $maxMin min may be $surfaceName",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BlockWindowSetting(
    label: String,
    window: com.lifesaver.domain.BlockWindow?,
    onChange: (com.lifesaver.domain.BlockWindow?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val enabled = window != null
    // Sensible default when turning it on (07:00–10:00).
    val start = window?.startMinute ?: 7 * 60
    val end = window?.endMinute ?: 10 * 60

    fun pick(initial: Int, onPicked: (Int) -> Unit) {
        android.app.TimePickerDialog(
            context, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, true,
        ).show()
    }

    LifesaverCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = enabled,
                onCheckedChange = { on -> onChange(if (on) com.lifesaver.domain.BlockWindow(start, end) else null) },
            )
        }
        if (enabled) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.lifesaver.ui.components.glass.GlassPill(
                    "From ${com.lifesaver.domain.ScheduleBlock.format(start)}",
                    onClick = { pick(start) { onChange(com.lifesaver.domain.BlockWindow(it, end)) } },
                )
                com.lifesaver.ui.components.glass.GlassPill(
                    "To ${com.lifesaver.domain.ScheduleBlock.format(end)}",
                    onClick = { pick(end) { onChange(com.lifesaver.domain.BlockWindow(start, it)) } },
                )
            }
            if (start == end) {
                Text(
                    "Start and end can't be the same.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Warning,
                )
            }
        }
    }
}

@Composable
private fun StrictnessSetting(current: Strictness, onChange: (Strictness) -> Unit) {
    LifesaverCard {
        Text("How hard should the pauses push?", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Strictness.entries.forEach { s ->
                FilterChip(
                    selected = s == current,
                    onClick = { onChange(s) },
                    label = { Text(s.name) },
                )
            }
        }
    }
}

@Composable
private fun PendingRow(key: String, value: String, effectiveTs: Long, onCancel: () -> Unit) {
    val when0 = remember(effectiveTs) {
        runCatching {
            Instant.ofEpochMilli(effectiveTs).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE HH:mm"))
        }.getOrDefault("soon")
    }
    LifesaverCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(humanKey(key, value), style = MaterialTheme.typography.titleMedium)
                Text("Active $when0", style = MaterialTheme.typography.bodySmall, color = Warning)
            }
            FlatButton("Cancel", onClick = onCancel)
        }
    }
}

private fun humanKey(key: String, value: String): String = when {
    key.startsWith("budget:") -> "${DetectionConfig.targetFor(key.removePrefix("budget:"))?.label ?: "Budget"} → $value min"
    key == "strictness" -> "Friction → $value"
    else -> "$key → $value"
}
