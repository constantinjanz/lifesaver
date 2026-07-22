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
import com.lifesaver.ui.components.PlaceholderScreen
import com.lifesaver.ui.dashboard.DashboardScreen
import com.lifesaver.ui.debug.DebugScreen
import com.lifesaver.ui.onboarding.OnboardingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val PLANS = "plans"
    const val DEBUG = "debug"
}

@Composable
fun LifesaverNavHost(
    state: DashboardState,
    onGrantPermission: (Permissions.Kind) -> Unit,
    onCompleteOnboarding: (List<com.lifesaver.data.IfThenPlan>, List<com.lifesaver.data.RedirectApp>, Map<String, Int>) -> Unit,
) {
    if (!state.hydrated) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val nav = rememberNavController()
    // Decide the start once, from the first hydrated state, to avoid a flash.
    val start = remember { if (state.settings.onboardingComplete) Routes.DASHBOARD else Routes.ONBOARDING }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                permissions = state.permissions,
                initialBudgets = state.settings.budgetMinByApp,
                initialPlans = state.settings.ifThenPlans,
                initialRedirects = state.settings.redirectApps,
                onGrant = onGrantPermission,
                onFinish = { plans, redirects, budgets ->
                    onCompleteOnboarding(plans, redirects, budgets)
                    nav.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                state = state,
                onEditPlans = { nav.navigate(Routes.PLANS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenDebug = { nav.navigate(Routes.DEBUG) },
                onFixPermissions = { nav.navigate(Routes.ONBOARDING) },
            )
        }
        composable(Routes.SETTINGS) {
            PlaceholderScreen("Settings", "Settings arrive with rewards & self-binding.", onBack = { nav.popBackStack() })
        }
        composable(Routes.PLANS) {
            com.lifesaver.ui.plans.PlansScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.DEBUG) {
            DebugScreen(permissions = state.permissions, onBack = { nav.popBackStack() })
        }
    }
}
