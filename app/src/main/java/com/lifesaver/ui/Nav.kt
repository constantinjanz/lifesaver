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
    const val BUDDY = "buddy"
    const val FUTURE_SELF = "future_self"
    const val MANAGE_APPS = "manage_apps"
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
            val lifeFlow = remember { vm.lifeLogsThisWeek() }
            com.lifesaver.ui.home.HomeScreen(
                state = state,
                onEditPlans = { nav.navigate(Routes.PLANS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenDebug = { nav.navigate(Routes.DEBUG) },
                onOpenReport = { nav.navigate(Routes.REPORT) },
                onFixPermissions = { nav.navigate(Routes.ONBOARDING) },
                onChangeBudget = vm::changeBudget,
                onSetReelsLimit = vm::setReelsLimit,
                onSetBlockedWindows = vm::setBlockedWindows,
                loadPatterns = vm::loadPatterns,
                lifeLogsFlow = lifeFlow,
                lifeFocusArea = state.settings.weeklyFocusArea,
                lifeFocusTarget = state.settings.weeklyFocusTarget,
                onLogLife = vm::logLife,
                onSetFocus = vm::setWeeklyFocus,
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
                onChangeStrictness = vm::changeStrictness,
                onCancelPending = vm::cancelPendingChange,
                onSetGrayscale = vm::setGrayscaleOnReels,
                onOpenCheckin = { nav.navigate(Routes.CHECKIN) },
                onOpenBuddy = { nav.navigate(Routes.BUDDY) },
                onOpenFutureSelf = { nav.navigate(Routes.FUTURE_SELF) },
                onSetBreatheReminder = vm::setBreatheReminder,
                onManageApps = { nav.navigate(Routes.MANAGE_APPS) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.MANAGE_APPS) {
            com.lifesaver.ui.apps.ManageAppsScreen(
                enabledApps = state.settings.enabledApps,
                loadCatalog = vm::loadAppCatalog,
                onAddApp = vm::addEnabledApp,
                onRemoveApp = vm::removeEnabledApp,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.PLANS) {
            com.lifesaver.ui.plans.PlansScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CHECKIN) {
            CheckinScreen(onSubmit = vm::submitCheckin, onBack = { nav.popBackStack() })
        }
        composable(Routes.BUDDY) {
            com.lifesaver.ui.buddy.BuddyScreen(
                paired = state.settings.buddyPaired,
                buddyLabel = state.settings.buddyLabel,
                onPair = vm::buddyPair,
                onRequest = vm::buddyRequest,
                onPoll = vm::buddyStatus,
                onApproved = vm::buddyApprove,
                onUnpair = vm::buddyUnpair,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.FUTURE_SELF) {
            com.lifesaver.ui.futureself.FutureSelfScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.DEBUG) {
            DebugScreen(permissions = state.permissions, onBack = { nav.popBackStack() })
        }
    }
}
