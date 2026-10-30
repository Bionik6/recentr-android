package com.recentr.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.recentr.app.NotificationLink
import com.recentr.app.leavesRestoredOnboarding
import com.recentr.app.main.MainScreen
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.catalog.DesignSystemCatalog
import com.recentr.core.designsystem.component.RecentrTab
import com.recentr.feature.focus.focusDestinations
import com.recentr.feature.home.homeDestinations
import com.recentr.feature.insights.insightsDestinations
import com.recentr.feature.onboarding.OnboardingGraphRoute
import com.recentr.feature.onboarding.onboardingGraph
import com.recentr.feature.profile.profileDestinations
import com.recentr.feature.rules.ChooseAppsPicker
import com.recentr.feature.rules.FocusAppsList
import com.recentr.feature.rules.navigateToAlwaysAllowed
import com.recentr.feature.rules.navigateToNewRule
import com.recentr.feature.rules.navigateToRule
import com.recentr.feature.rules.navigateToRuleEditor
import com.recentr.feature.rules.navigateToStartSession
import com.recentr.feature.rules.ruleShown
import com.recentr.feature.rules.rulesDestinations
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object MainRoute

@Serializable data object DesignSystemRoute

/** Going back, whether from the back button or a back swipe. */
private val PopEnter = slideInHorizontally(tween(300)) { -it / 6 } + fadeIn(tween(300))
private val PopExit = slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200))

/**
 * Root navigation: onboarding graph → main tabs. Feature modules contribute their own full-screen
 * destinations (rule editors, settings, focus timer…) at this level so they are drawn above the
 * tab bar. [debugTools] adds the QA shortcuts (replay onboarding, design system) to Settings.
 * [ruleToOpen] is a tapped notification's rule (NotificationLink): its page opens over the Rules
 * tab, then [onRuleOpened].
 */
@Composable
fun RecentrNavHost(
    showOnboarding: Boolean,
    initialTab: RecentrTab = RecentrTab.Home,
    debugTools: Boolean = false,
    ruleToOpen: String? = null,
    onRuleOpened: () -> Unit = {},
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) OnboardingGraphRoute else MainRoute,
        enterTransition = { slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 6 } + fadeOut(tween(200)) },
        popEnterTransition = { PopEnter },
        popExitTransition = { PopExit },
        // A back swipe plays the back button's transition, following the finger, instead of
        // Navigation's default predictive one (the screen shrinking, then vanishing).
        predictivePopEnterTransition = { PopEnter },
        predictivePopExitTransition = { PopExit },
    ) {
        onboardingGraph(
            navController = navController,
            chooseApps = { initial, onDone, onDismiss -> ChooseAppsPicker(initial, onDone, onDismiss) },
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
            MainScreen(navController = navController, selected = tab, onSelect = { tab = it })
        }
        composable<DesignSystemRoute> { DesignSystemCatalog() }
        homeDestinations(navController)
        rulesDestinations(navController)
        focusDestinations(
            navController,
            appsOnHold = { apps, onDismiss -> FocusAppsList(apps, onDismiss) },
        )
        insightsDestinations(
            navController = navController,
            onCreateRule = { type, apps -> navController.navigateToNewRule(type, apps) },
            onEditRule = { navController.navigateToRuleEditor(it) },
        )
        profileDestinations(
            navController = navController,
            onSignedOut = { navController.restartOnboarding() },
            onOpenAlwaysAllowed = { navController.navigateToAlwaysAllowed() },
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
    // A screen brought back on the onboarding (left there by a QA shortcut, or an older screen of
    // the task) when the stored profile finished it since: straight to Home.
    LaunchedEffect(navController) {
        if (leavesRestoredOnboarding(showOnboarding, navController.isOnOnboarding())) {
            navController.navigate(MainRoute) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }
    // A tapped rule notification: the rule's page over Rules, in place of any screen over the tabs.
    LaunchedEffect(ruleToOpen) {
        val ruleId = ruleToOpen ?: return@LaunchedEffect
        if (NotificationLink.opensRulePage(navController.isOnOnboarding(), navController.ruleShown(), ruleId)) {
            tab = RecentrTab.Rules
            navController.navigateToRule(ruleId) { popUpTo<MainRoute>() }
        }
        onRuleOpened()
    }
}

private fun NavHostController.isOnOnboarding(): Boolean =
    currentDestination?.hierarchy?.any { it.hasRoute(OnboardingGraphRoute::class) } == true

/** Clears the whole back stack and starts the onboarding again. */
private fun NavHostController.restartOnboarding() {
    navigate(OnboardingGraphRoute) {
        popUpTo(graph.id) { inclusive = true }
    }
}
