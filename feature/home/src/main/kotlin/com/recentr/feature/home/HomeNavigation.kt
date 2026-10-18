package com.recentr.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * Home tab content (Home & Insight flow, Figma 22–29). Drawn under the floating tab bar:
 * [contentPadding] (bottom) is kept clear for it; the "Start blocking" CTA sits right above.
 */
@Composable
fun HomeTab(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onStartBlocking: () -> Unit,
    onOpenRules: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
    )
}

/**
 * Full-screen destinations pushed from Home (above the tab bar). The Home & Insight flow only
 * uses sheets over the tab, so there are none for now.
 */
@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
fun NavGraphBuilder.homeDestinations(navController: NavHostController) = Unit
