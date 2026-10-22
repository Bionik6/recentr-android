package com.recentr.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.recentr.core.data.BlockingAccess
import com.recentr.core.data.RecentrData
import com.recentr.core.data.UsageAccess
import com.recentr.core.designsystem.component.BlockingAccessSheet
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.home.ui.sheets.AlwaysAllowedInSettingsSheet

/**
 * Home tab content (Home & Insight flow, Figma 22–29). Drawn under the floating tab bar:
 * [contentPadding] (bottom) is kept clear for it; the "Start blocking" CTA sits right above.
 * While the app blocker is off, the "Blocking is off" card explains how to turn it on; without
 * usage access, "Connect" opens Settings › Usage access (back on Home, the device numbers load).
 * Pull to refresh reads the screen time and the app blocker's state again.
 * The usage card opens Insights ([onOpenInsights], with the period shown), the header's flame the
 * streak ([onOpenStreak]) and its avatar the profile ([onOpenProfile]), a "Most used" app its
 * own insights ([onOpenApp]). Once there's a rule, the Always allowed card sets up the apps never
 * blocked ([onSetUpAlwaysAllowed]); its × says where to find them in Settings, then goes.
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
    onOpenProfile: () -> Unit = {},
    onOpenApp: (packageName: String, period: UsagePeriod) -> Unit = { _, _ -> },
    onSetUpAlwaysAllowed: () -> Unit = {},
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blockingOn by RecentrData.blockingEnabled.collectAsStateWithLifecycle()
    var explainBlocking by rememberSaveable { mutableStateOf(false) }
    var closingAlwaysAllowed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    HomeScreen(
        state = state,
        contentPadding = contentPadding,
        onSaveGoal = viewModel::saveDailyGoal,
        onSelectGrouping = viewModel::selectGrouping,
        onStartBlocking = onStartBlocking,
        onOpenRules = onOpenRules,
        onOpenInsights = { onOpenInsights(state.period) },
        onOpenStreak = onOpenStreak,
        onOpenProfile = onOpenProfile,
        onOpenApp = { onOpenApp(it, state.period) },
        blockingOn = blockingOn,
        onTurnOnBlocking = { explainBlocking = true },
        onRefresh = {
            // App blocking may have been turned on or off meanwhile, too.
            RecentrData.recheckBlockingAccess()
            viewModel.refresh()
        },
        onConnectScreenTime = { UsageAccess.openSettings(context) },
        onSetUpAlwaysAllowed = onSetUpAlwaysAllowed,
        onCloseAlwaysAllowed = { closingAlwaysAllowed = true },
        modifier = modifier,
    )
    if (closingAlwaysAllowed) {
        // Where to find it again first; the card goes with the sheet.
        AlwaysAllowedInSettingsSheet(
            onDismissRequest = {
                closingAlwaysAllowed = false
                viewModel.dismissAlwaysAllowedCard()
            },
        )
    }
    if (explainBlocking) {
        BlockingAccessSheet(onOpenSettings = { BlockingAccess.openSettings(context) }, onDismissRequest = { explainBlocking = false })
    }
}

/**
 * Full-screen destinations pushed from Home (above the tab bar). The Home & Insight flow only
 * uses sheets over the tab, so there are none for now.
 */
@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
fun NavGraphBuilder.homeDestinations(navController: NavHostController) = Unit
