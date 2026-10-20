package com.recentr.app.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.navigation.NavHostController
import com.recentr.core.designsystem.component.RecentrTab
import com.recentr.core.designsystem.component.RecentrTabBar
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.focus.FocusTab
import com.recentr.feature.home.HomeTab
import com.recentr.feature.insights.InsightsOverviewTab
import com.recentr.feature.insights.navigateToAppInsights
import com.recentr.feature.insights.navigateToStreak
import com.recentr.feature.profile.navigateToProfile
import com.recentr.feature.rules.RulesTab
import com.recentr.feature.rules.navigateToStartSession

/**
 * Main shell: the floating tab bar is drawn over the tab content, which receives the space it
 * occupies as [PaddingValues] (bottom) so scrolling content can clear it.
 */
@Composable
fun MainScreen(
    navController: NavHostController,
    initialTab: RecentrTab,
) {
    var selected by rememberSaveable { mutableStateOf(initialTab) }
    // Home's usage card: the Insights tab opens on the tab of that period.
    var insightsPeriod by rememberSaveable { mutableStateOf<UsagePeriod?>(null) }
    BackHandler(enabled = selected != RecentrTab.Home) { selected = RecentrTab.Home }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Top gap 6 + pill 68 + bottom gap (max(nav inset, 20dp)) — see RecentrTabBar.
    val tabBarPadding = PaddingValues(bottom = 74.dp + max(navBottom, 20.dp))

    // Keeps each tab's saveable state (scroll positions, pager page…) while another tab is shown.
    val tabStateHolder = rememberSaveableStateHolder()

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = selected, label = "tabs") { tab ->
            tabStateHolder.SaveableStateProvider(tab.name) {
                when (tab) {
                    RecentrTab.Home -> HomeTab(
                        navController = navController,
                        contentPadding = tabBarPadding,
                        onStartBlocking = { navController.navigateToStartSession() },
                        onOpenRules = { selected = RecentrTab.Rules },
                        onOpenInsights = {
                            insightsPeriod = it
                            selected = RecentrTab.Insights
                        },
                        onOpenStreak = { navController.navigateToStreak() },
                        onOpenProfile = { navController.navigateToProfile() },
                        onOpenApp = { packageName, period -> navController.navigateToAppInsights(packageName, period) },
                    )
                    RecentrTab.Rules -> RulesTab(navController = navController, contentPadding = tabBarPadding)
                    RecentrTab.Timer -> FocusTab(navController = navController, contentPadding = tabBarPadding)
                    RecentrTab.Insights -> InsightsOverviewTab(
                        navController = navController,
                        contentPadding = tabBarPadding,
                        openPeriod = insightsPeriod,
                        onPeriodOpened = { insightsPeriod = null },
                    )
                }
            }
        }
        RecentrTabBar(
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
