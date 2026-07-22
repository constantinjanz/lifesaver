package com.lifesaver.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lifesaver.service.Permissions
import com.lifesaver.ui.checkin.CheckinScreen
import com.lifesaver.ui.components.PlaceholderScreen
import com.lifesaver.ui.dashboard.DashboardScreen
import com.lifesaver.ui.debug.DebugScreen
import com.lifesaver.ui.onboarding.OnboardingScreen
import com.lifesaver.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val PLANS = "plans"
    const val CHECKIN = "checkin"
    const val DEBUG = "debug"
    const val PATTERNS = "patterns"
    const val LIFE = "life"
    const val REPORT = "report"
}

@Composable
fun LifesaverNavHost(
    vm: LifesaverViewModel,
    state: DashboardState,
    onGrantPermission: (Permissions.Kind) -> Unit,
) {
    if (!state.hydrated) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val nav = rememberNavController()
    val start = remember { if (state.settings.onboardingComplete) Routes.DASHBOARD else Routes.ONBOARDING }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                permissions = state.permissions,
                initialBudgets = state.settings.budgetMinByApp,
                initialPlans = state.settings.ifThenPlans,
                initialRedirects = state.settings.redirectApps,
                loadInsight = vm::computeInsight,
                onGrant = onGrantPermission,
                onFinish = { plans, redirects, budgets ->
                    vm.completeOnboarding(plans, redirects, budgets)
                    nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                state = state,
                onEditPlans = { nav.navigate(Routes.PLANS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenDebug = { nav.navigate(Routes.DEBUG) },
                onOpenReport = { nav.navigate(Routes.REPORT) },
                onFixPermissions = { nav.navigate(Routes.ONBOARDING) },
                onSelectTab = { i ->
                    when (i) {
                        1 -> nav.navigate(Routes.PATTERNS)
                        2 -> nav.navigate(Routes.LIFE)
                    }
                },
            )
        }
        composable(Routes.PATTERNS) {
            com.lifesaver.ui.patterns.PatternsScreen(loadPatterns = vm::loadPatterns, onBack = { nav.popBackStack() })
        }
        composable(Routes.LIFE) {
            com.lifesaver.ui.life.LifeScreen(
                logsFlow = vm.lifeLogsThisWeek(),
                focusArea = state.settings.weeklyFocusArea,
                focusTarget = state.settings.weeklyFocusTarget,
                onLog = vm::logLife,
                onSetFocus = vm::setWeeklyFocus,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.REPORT) {
            com.lifesaver.ui.report.ReportScreen(
                loadReport = vm::loadReport,
                onSubmitCheckin = vm::submitCheckin,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = state,
                onChangeBudget = vm::changeBudget,
                onChangeStrictness = vm::changeStrictness,
                onCancelPending = vm::cancelPendingChange,
                onOpenCheckin = { nav.navigate(Routes.CHECKIN) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.PLANS) {
            com.lifesaver.ui.plans.PlansScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CHECKIN) {
            CheckinScreen(onSubmit = vm::submitCheckin, onBack = { nav.popBackStack() })
        }
        composable(Routes.DEBUG) {
            DebugScreen(permissions = state.permissions, onBack = { nav.popBackStack() })
        }
    }
}
