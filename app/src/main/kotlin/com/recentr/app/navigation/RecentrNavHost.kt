package com.recentr.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.recentr.app.main.MainScreen
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.catalog.DesignSystemCatalog
import com.recentr.core.designsystem.component.RecentrTab
import com.recentr.feature.focus.focusDestinations
import com.recentr.feature.home.homeDestinations
import com.recentr.feature.onboarding.OnboardingGraphRoute
import com.recentr.feature.onboarding.onboardingGraph
import com.recentr.feature.profile.profileDestinations
import com.recentr.feature.rules.navigateToStartSession
import com.recentr.feature.rules.rulesDestinations
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object MainRoute

@Serializable data object DesignSystemRoute

/**
 * Root navigation: onboarding graph → main tabs. Feature modules contribute their own full-screen
 * destinations (rule editors, settings, focus timer…) at this level so they are drawn above the
 * tab bar. [debugTools] adds the QA shortcuts (replay onboarding, design system) to Settings.
 */
@Composable
fun RecentrNavHost(showOnboarding: Boolean, initialTab: RecentrTab = RecentrTab.Home, debugTools: Boolean = false) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) OnboardingGraphRoute else MainRoute,
        enterTransition = { slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 6 } + fadeOut(tween(200)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 6 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200)) },
    ) {
        onboardingGraph(
            navController = navController,
            onFinished = {
                navController.navigate(MainRoute) {
                    popUpTo(OnboardingGraphRoute) { inclusive = true }
                }
            },
            onStartFirstSession = {
                navController.navigate(MainRoute) {
                    popUpTo(OnboardingGraphRoute) { inclusive = true }
                }
                navController.navigateToStartSession()
            },
        )
        composable<MainRoute> {
            MainScreen(navController = navController, initialTab = initialTab)
        }
        composable<DesignSystemRoute> { DesignSystemCatalog() }
        homeDestinations(navController)
        rulesDestinations(navController)
        focusDestinations(navController)
        profileDestinations(
            navController = navController,
            onSignedOut = { navController.restartOnboarding() },
            onReplayOnboarding = if (debugTools) {
                {
                    scope.launch {
                        RecentrData.userPreferences.update { it.copy(onboardingCompleted = false) }
                        navController.restartOnboarding()
                    }
                }
            } else {
                null
            },
            onOpenDesignSystem = if (debugTools) {
                { navController.navigate(DesignSystemRoute) }
            } else {
                null
            },
        )
    }
}

/** Clears the whole back stack and starts the onboarding again. */
private fun NavHostController.restartOnboarding() {
    navigate(OnboardingGraphRoute) {
        popUpTo(graph.id) { inclusive = true }
    }
}
