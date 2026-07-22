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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lifesaver.service.Permissions
import com.lifesaver.ui.components.FlatButton
import com.lifesaver.ui.components.LifesaverCard
import com.lifesaver.ui.components.RaisedButton
import com.lifesaver.ui.plans.PlanEditor
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.SurfaceRaised
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6
private const val PERMISSIONS_PAGE = 5

@Composable
fun OnboardingScreen(
    permissions: Map<Permissions.Kind, Boolean>,
    initialBudgets: Map<String, Int>,
    initialPlans: List<IfThenPlan>,
    initialRedirects: List<RedirectApp>,
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
    var plans by remember { mutableStateOf(initialPlans) }
    var redirects by remember { mutableStateOf(initialRedirects) }

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
                2 -> ConceptPage(
                    "Substitute the scroll.",
                    "The urge is real — boredom, a break, a reset. Lifesaver hands you your own if-then plan and a one-tap jump to something better.",
                )
                3 -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
                    PlanEditor(
                        plans = plans,
                        redirects = redirects,
                        onPlansChange = { plans = it },
                        onRedirectsChange = { redirects = it },
                    )
                }
                4 -> BudgetPage(budgets)
                5 -> PermissionsPage(permissions, onGrant)
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
private fun BudgetPage(budgets: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
        Text("Daily budgets", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "How long per day is fair? Reels and Shorts burn this twice as fast.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        DetectionConfig.targets.forEach { target ->
            val value = budgets[target.appId] ?: 30
            LifesaverCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(target.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("$value MIN", style = MaterialTheme.typography.bodyLarge, color = Accent)
                }
                Slider(
                    value = value.toFloat(),
                    onValueChange = { budgets[target.appId] = it.toInt() },
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
        Text("Grant access", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Four permissions, each with a clear job. Nothing leaves your phone.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        permissionMeta.forEach { (kind, meta) ->
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

private val permissionMeta: List<Pair<Permissions.Kind, Pair<String, String>>> = listOf(
    Permissions.Kind.USAGE_ACCESS to ("Usage Access" to "Measures your real screen time to set an honest baseline."),
    Permissions.Kind.ACCESSIBILITY to ("Accessibility Service" to "Notices the moment Instagram or YouTube opens, before the feed."),
    Permissions.Kind.OVERLAY to ("Display over apps" to "Shows the pause screen on top of the app you opened."),
    Permissions.Kind.BATTERY to ("Ignore battery limits" to "Keeps Lifesaver alive on Samsung's aggressive power saving."),
)
