package com.lifesaver.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lifesaver.data.IfThenPlan
import com.lifesaver.data.RedirectApp
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.domain.BaselineInsight
import com.lifesaver.domain.TimeSaved
import com.lifesaver.service.Permissions
import com.lifesaver.ui.components.FlatButton
import com.lifesaver.ui.components.LifesaverCard
import com.lifesaver.ui.components.RaisedButton
import com.lifesaver.ui.plans.PlanEditor
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.SurfaceRaised
import com.lifesaver.ui.theme.TextSecondary
import com.lifesaver.ui.theme.Warning
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 7
private const val PERMISSIONS_PAGE = 6

@Composable
fun OnboardingScreen(
    permissions: Map<Permissions.Kind, Boolean>,
    initialBudgets: Map<String, Int>,
    initialPlans: List<IfThenPlan>,
    initialRedirects: List<RedirectApp>,
    loadInsight: suspend () -> BaselineInsight,
    onGrant: (Permissions.Kind) -> Unit,
    onFinish: (plans: List<IfThenPlan>, redirects: List<RedirectApp>, budgets: Map<String, Int>) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val budgets = remember {
        mutableStateMapOf<String, Int>().apply {
            DetectionConfig.targets.forEach { put(it.appId, initialBudgets[it.appId] ?: 30) }
        }
    }
    var budgetsTouched by remember { mutableStateOf(false) }
    var plans by remember { mutableStateOf(initialPlans) }
    var redirects by remember { mutableStateOf(initialRedirects) }
    var insight by remember { mutableStateOf<BaselineInsight?>(null) }

    val usageGranted = permissions[Permissions.Kind.USAGE_ACCESS] == true

    // As soon as Usage Access is granted, read the phone's history and suggest budgets from it.
    LaunchedEffect(usageGranted) {
        if (usageGranted && insight == null) {
            val result = loadInsight()
            insight = result
            if (!budgetsTouched) {
                result.perApp.forEach { (app, b) ->
                    val suggestion = ((b.overallAvgMs / 60_000L) / 2).toInt().coerceIn(10, 120)
                    if (suggestion in 10..120 && b.overallAvgMs > 0) budgets[app] = suggestion
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < PERMISSIONS_PAGE) {
                FlatButton("Skip", onClick = { scope.launch { pager.animateScrollToPage(PERMISSIONS_PAGE) } })
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> ConceptPage(
                    "Not a blocker.\nA redirection machine.",
                    "Lifesaver steps in the instant you open Instagram or YouTube — and points you at what you actually wanted to do.",
                )
                1 -> ConceptPage(
                    "Friction beats blocks.",
                    "A short pause before the feed cuts reflexive opens dramatically. Hard blocks just make you uninstall. So we make the impulse expensive, not impossible.",
                )
                2 -> UsageAccessPage(usageGranted, onGrant = { onGrant(Permissions.Kind.USAGE_ACCESS) })
                3 -> InsightPage(usageGranted, insight, onGrant = { onGrant(Permissions.Kind.USAGE_ACCESS) })
                4 -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
                    PlanEditor(
                        plans = plans,
                        redirects = redirects,
                        onPlansChange = { plans = it },
                        onRedirectsChange = { redirects = it },
                    )
                }
                5 -> BudgetPage(budgets, insight) { budgetsTouched = true }
                6 -> PermissionsPage(permissions, onGrant)
            }
        }

        PageDots(pager.currentPage)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < PERMISSIONS_PAGE) {
                RaisedButton("Next", onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } })
            } else {
                RaisedButton("Done", onClick = { onFinish(plans, redirects, budgets.toMap()) })
            }
        }
    }
}

@Composable
private fun ConceptPage(title: String, body: String) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 48.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun UsageAccessPage(granted: Boolean, onGrant: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 40.dp)) {
        Text("First, the honest part.", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Grant Usage Access and Lifesaver reads the screen-time your phone already tracked — weeks, even months back. No waiting through an observation period, and your real numbers become the baseline you're measured against.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        LifesaverCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Usage Access", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (granted) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Granted", tint = Accent)
                } else {
                    FlatButton("Grant", onClick = onGrant)
                }
            }
        }
    }
}

@Composable
private fun InsightPage(usageGranted: Boolean, insight: BaselineInsight?, onGrant: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
        Text("Here's your reality.", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        when {
            !usageGranted -> LifesaverCard {
                Text("Grant Usage Access on the previous step to see this.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                FlatButton("Grant now", onClick = onGrant)
            }
            insight == null -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            insight.daysObserved == 0 -> LifesaverCard {
                Text(
                    "Your phone didn't hand over any history yet — that's fine. Lifesaver will learn as you go.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> InsightBody(insight)
        }
    }
}

@Composable
private fun InsightBody(insight: BaselineInsight) {
    DetectionConfig.targets.forEach { target ->
        val b = insight.perApp[target.appId]
        val perDay = (b?.overallAvgMs ?: 0L)
        LifesaverCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(target.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(TimeSaved.formatHm(perDay) + "/day", style = MaterialTheme.typography.bodyLarge, color = Accent)
            }
            if (b != null && b.weekdayAvgMs != b.weekendAvgMs) {
                Text(
                    "Weekdays ${TimeSaved.formatHm(b.weekdayAvgMs)} · weekends ${TimeSaved.formatHm(b.weekendAvgMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    val together = insight.overallDailyMs()
    if (together > 0) {
        val weekly = together * 7
        Text(
            "Together that's ${TimeSaved.formatHm(together)} a day — ${TimeSaved.tangible(weekly).lowercase()} of your week.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
    }

    if (insight.riskHours.isNotEmpty()) {
        LifesaverCard {
            Text("YOUR DANGER ZONE", style = MaterialTheme.typography.bodySmall, color = Warning)
            Spacer(Modifier.height(4.dp))
            Text(insight.riskHours.sorted().joinToString(", ") { hourLabel(it) }, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(8.dp))
    }
    if (insight.morningFirstDays > 0) {
        Text(
            "You reached for it before 9am on ${insight.morningFirstDays} of the last ${insight.eventDays} days.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
    }
    if (insight.lateNightMsPerDay > 60_000) {
        Text(
            "About ${TimeSaved.formatHm(insight.lateNightMsPerDay)} of that lands after midnight.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
    }
    Text(
        "Based on the last ${insight.daysObserved} days your phone remembers.",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
    )
}

private fun hourLabel(hour: Int): String = "%02d:00".format(hour)

@Composable
private fun BudgetPage(
    budgets: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>,
    insight: BaselineInsight?,
    onTouched: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
        Text("Daily budgets", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Suggested at about half your usual. Reels and Shorts burn this twice as fast.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        DetectionConfig.targets.forEach { target ->
            val value = budgets[target.appId] ?: 30
            val avg = insight?.perApp?.get(target.appId)?.overallAvgMs ?: 0L
            LifesaverCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(target.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("$value MIN", style = MaterialTheme.typography.bodyLarge, color = Accent)
                }
                if (avg > 0) {
                    Text("Your average: ${TimeSaved.formatHm(avg)}/day", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onTouched(); budgets[target.appId] = it.toInt() },
                    valueRange = 10f..120f,
                    steps = (120 - 10) / 5 - 1,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PermissionsPage(permissions: Map<Permissions.Kind, Boolean>, onGrant: (Permissions.Kind) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
        Text("Last thing: let me act.", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Three permissions so Lifesaver can step in at the moment you open the app.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        remainingPermissionMeta.forEach { (kind, meta) ->
            PermissionRow(meta.first, meta.second, permissions[kind] == true) { onGrant(kind) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PermissionRow(title: String, why: String, granted: Boolean, onGrant: () -> Unit) {
    LifesaverCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(why, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.size(12.dp))
            if (granted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Granted", tint = Accent)
            } else {
                FlatButton("Grant", onClick = onGrant)
            }
        }
    }
}

@Composable
private fun PageDots(current: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(PAGE_COUNT) { i ->
            Box(
                modifier = Modifier.padding(4.dp).size(8.dp).clip(CircleShape)
                    .background(if (i == current) Accent else SurfaceRaised),
            )
        }
    }
}

private val remainingPermissionMeta: List<Pair<Permissions.Kind, Pair<String, String>>> = listOf(
    Permissions.Kind.ACCESSIBILITY to ("Accessibility Service" to "Notices the moment Instagram or YouTube opens, before the feed."),
    Permissions.Kind.OVERLAY to ("Display over apps" to "Shows the pause screen on top of the app you opened."),
    Permissions.Kind.BATTERY to ("Ignore battery limits" to "Keeps Lifesaver alive on Samsung's aggressive power saving."),
)
