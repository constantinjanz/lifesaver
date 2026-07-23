package com.lifesaver.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.data.LifeLog
import com.lifesaver.domain.PatternsData
import com.lifesaver.ui.DashboardState
import com.lifesaver.ui.components.glass.DockItem
import com.lifesaver.ui.components.glass.FloatingDock
import com.lifesaver.ui.components.glass.GlassBackground
import com.lifesaver.ui.dashboard.CockpitPage
import com.lifesaver.ui.life.LifePage
import com.lifesaver.ui.patterns.PatternsPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The three main views as one swipeable surface (DESIGN v2 §7 + user feedback): Cockpit / Patterns
 * / Life share one ambient background and a HorizontalPager, so left-right swipe is continuous.
 * The floating dock reflects the current page and taps animate to it — no "new window" feeling.
 */
@Composable
fun HomeScreen(
    state: DashboardState,
    onEditPlans: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenReport: () -> Unit,
    onFixPermissions: () -> Unit,
    loadPatterns: suspend () -> PatternsData,
    lifeLogsFlow: Flow<List<LifeLog>>,
    lifeFocusArea: String?,
    lifeFocusTarget: Int,
    onLogLife: (String, Int) -> Unit,
    onSetFocus: (String, Int) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val dockItems = listOf(
        DockItem(Icons.Filled.GridView, "Cockpit"),
        DockItem(Icons.Filled.BarChart, "Patterns"),
        DockItem(Icons.Filled.Spa, "Life"),
    )

    GlassBackground(seed = 0, drift = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> CockpitPage(state, onEditPlans, onOpenSettings, onOpenDebug, onOpenReport, onFixPermissions)
                    1 -> PatternsPage(loadPatterns)
                    else -> LifePage(lifeLogsFlow, lifeFocusArea, lifeFocusTarget, onLogLife, onSetFocus)
                }
            }
            FloatingDock(
                items = dockItems,
                selected = pager.currentPage,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            )
        }
    }
}
