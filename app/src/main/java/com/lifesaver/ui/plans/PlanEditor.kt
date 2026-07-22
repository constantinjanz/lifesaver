package com.lifesaver.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import com.lifesaver.LifesaverApp
import com.lifesaver.data.IfThenPlan
import com.lifesaver.data.RedirectApp
import com.lifesaver.domain.PlanMatcher
import com.lifesaver.service.InstalledApps
import com.lifesaver.ui.components.AppIcon
import com.lifesaver.ui.components.AppPickerDialog
import com.lifesaver.ui.components.FlatButton
import com.lifesaver.ui.components.LifesaverCard
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.TextSecondary

data class PlanContext(val key: String, val label: String, val hint: String)

val PLAN_CONTEXTS = listOf(
    PlanContext(PlanMatcher.EVENING, "Evening (after 21:00)", "…then I will read a chapter of my book."),
    PlanContext(PlanMatcher.WAITING, "Waiting / bored (daytime)", "…then I will text a friend or step outside."),
    PlanContext(PlanMatcher.JUST_WOKE, "Just woke up", "…then I will make coffee and plan the day first."),
)

/**
 * Shared editor for if-then plans (min 2 contexts, §3.1) and redirect apps (1–3, §3.1). Used by
 * both onboarding and the standalone plans screen. Emits changes up so the caller decides where to
 * persist (onboarding batches; the plans screen writes immediately).
 */
@Composable
fun PlanEditor(
    plans: List<IfThenPlan>,
    redirects: List<RedirectApp>,
    onPlansChange: (List<IfThenPlan>) -> Unit,
    onRedirectsChange: (List<RedirectApp>) -> Unit,
) {
    val texts = remember {
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
            PLAN_CONTEXTS.forEach { ctx ->
                put(ctx.key, plans.firstOrNull { it.context == ctx.key }?.text ?: "")
            }
        }
    }

    fun emitPlans() {
        val list = PLAN_CONTEXTS.mapNotNull { ctx ->
            val t = texts[ctx.key]?.trim().orEmpty()
            if (t.isBlank()) null else IfThenPlan(ctx.key, ctx.label, t)
        }
        onPlansChange(list)
    }

    Column {
        Text("If-then plans", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Write at least two. Lifesaver quotes the matching one back at you when you reach for the feed.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        PLAN_CONTEXTS.forEach { ctx ->
            LifesaverCard {
                Text(ctx.label.uppercase(), style = MaterialTheme.typography.bodySmall, color = Accent)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = texts[ctx.key] ?: "",
                    onValueChange = { texts[ctx.key] = it; emitPlans() },
                    placeholder = { Text(ctx.hint) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        RedirectSection(redirects, onRedirectsChange)
    }
}

@Composable
private fun RedirectSection(
    redirects: List<RedirectApp>,
    onChange: (List<RedirectApp>) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val apps by produceState(initialValue = emptyList<InstalledApps.Entry>()) {
        value = LifesaverApp.instance.container.installedApps.launchable()
    }
    var selected by remember { mutableStateOf(redirects.map { it.appId }.toSet()) }

    Text("Redirect apps", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "One tap from the pause screen jumps to one of these instead of scrolling.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(12.dp))
    LifesaverCard {
        if (redirects.isEmpty()) {
            Text("None chosen yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                redirects.forEach { r ->
                    val entry = apps.firstOrNull { it.packageName == r.appId }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIcon(entry?.icon)
                        Text(r.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FlatButton("Choose apps", onClick = { pickerOpen = true })
        }
    }

    if (pickerOpen) {
        AppPickerDialog(
            apps = apps,
            selected = selected,
            maxSelect = 3,
            onToggle = { pkg ->
                selected = if (pkg in selected) selected - pkg else selected + pkg
            },
            onDismiss = { pickerOpen = false },
            onConfirm = {
                pickerOpen = false
                val chosen = apps.filter { it.packageName in selected }
                    .map { RedirectApp(it.packageName, it.label) }
                onChange(chosen)
            },
        )
    }
}
