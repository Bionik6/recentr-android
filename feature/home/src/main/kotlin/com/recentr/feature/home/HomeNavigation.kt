package com.recentr.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.recentr.core.model.UsagePeriod

/**
 * Home tab content (Home & Insight flow, Figma 22–29). Drawn under the floating tab bar:
 * [contentPadding] (bottom) is kept clear for it; the "Start blocking" CTA sits right above.
 * The usage card opens Insights ([onOpenInsights], with the period shown), the streak tile the
 * streak ([onOpenStreak]) and a "Most used" app its own insights ([onOpenApp]).
 */
@Composable
fun HomeTab(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onStartBlocking: () -> Unit,
    onOpenRules: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenInsights: (UsagePeriod) -> Unit = {},
    onOpenStreak: () -> Unit = onOpenRules,
    onOpenApp: (packageName: String, period: UsagePeriod) -> Unit = { _, _ -> },
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        contentPadding = contentPadding,
        onSelectPeriod = viewModel::selectPeriod,
        onRefresh = viewModel::refresh,
        onSaveGoal = viewModel::saveDailyGoal,
        onToggleWellSpent = viewModel::toggleWellSpent,
        onSelectGrouping = viewModel::selectGrouping,
        onStartBlocking = onStartBlocking,
        onOpenRules = onOpenRules,
        onOpenInsights = { onOpenInsights(state.period) },
        onOpenStreak = onOpenStreak,
        onOpenApp = { onOpenApp(it, state.period) },
        modifier = modifier,
    )
}

/**
 * Full-screen destinations pushed from Home (above the tab bar). The Home & Insight flow only
 * uses sheets over the tab, so there are none for now.
 */
@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
fun NavGraphBuilder.homeDestinations(navController: NavHostController) = Unit
