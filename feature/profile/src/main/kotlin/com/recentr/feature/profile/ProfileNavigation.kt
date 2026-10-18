package com.recentr.feature.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.recentr.core.data.RecentrData
import com.recentr.feature.profile.overview.EditProfileSheet
import com.recentr.feature.profile.overview.ProfileScreen
import com.recentr.feature.profile.overview.ProfileViewModel
import com.recentr.feature.profile.plus.PlusScreen
import com.recentr.feature.profile.plus.SubscriptionScreen
import com.recentr.feature.profile.recap.WeeklyRecapDestination
import com.recentr.feature.profile.settings.AppearanceScreen
import com.recentr.feature.profile.settings.HelpScreen
import com.recentr.feature.profile.settings.LanguageScreen
import com.recentr.feature.profile.settings.NotificationsScreen
import com.recentr.feature.profile.settings.SettingsActions
import com.recentr.feature.profile.settings.SettingsScreen
import com.recentr.feature.profile.settings.SettingsViewModel
import kotlinx.serialization.Serializable

@Serializable internal data object SettingsRoute

@Serializable internal data object SettingsNotificationsRoute

@Serializable internal data object SettingsAppearanceRoute

@Serializable internal data object SettingsLanguageRoute

@Serializable internal data object SettingsHelpRoute

@Serializable internal data object WeeklyRecapRoute

@Serializable internal data object PlusRoute

@Serializable internal data object SubscriptionRoute

/**
 * Profile tab content (Figma 61 · Overview + 62 · Edit sheet). Drawn under the floating tab bar:
 * [contentPadding] (bottom) is kept clear for it.
 */
@Composable
fun ProfileTab(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    ProfileScreen(
        state = state,
        contentPadding = contentPadding,
        onEdit = { editing = true },
        onOpenSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
        onOpenRecap = { navController.navigate(WeeklyRecapRoute) { launchSingleTop = true } },
        modifier = modifier,
    )
    if (editing) {
        EditProfileSheet(
            name = state.name,
            email = state.email,
            avatar = state.avatar,
            onSave = viewModel::save,
            onDismissRequest = { editing = false },
        )
    }
}

/**
 * Full-screen Profile & Settings destinations (Figma 63–72), drawn above the tab bar.
 *
 * @param onSignedOut after "Log out" or "Delete account": the app goes back to the onboarding.
 * @param onReplayOnboarding / [onOpenDesignSystem] debug-only QA shortcuts listed in Settings.
 */
fun NavGraphBuilder.profileDestinations(
    navController: NavHostController,
    onSignedOut: () -> Unit,
    onReplayOnboarding: (() -> Unit)? = null,
    onOpenDesignSystem: (() -> Unit)? = null,
) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        val blockingOn by RecentrData.blockingEnabled.collectAsStateWithLifecycle()
        SettingsScreen(
            state = state,
            blockingOn = blockingOn,
            actions = SettingsActions(
                onBack = back,
                onOpenNotifications = { navController.navigate(SettingsNotificationsRoute) { launchSingleTop = true } },
                onOpenAppearance = { navController.navigate(SettingsAppearanceRoute) { launchSingleTop = true } },
                onOpenLanguage = { navController.navigate(SettingsLanguageRoute) { launchSingleTop = true } },
                onOpenHelp = { navController.navigate(SettingsHelpRoute) { launchSingleTop = true } },
                onOpenPlus = {
                    val route: Any = if (state.settings.plusPlan != null) SubscriptionRoute else PlusRoute
                    navController.navigate(route) { launchSingleTop = true }
                },
                onUpdate = viewModel::update,
                onLogOut = { viewModel.logOut(onSignedOut) },
                onDeleteAccount = { viewModel.deleteAccount(onSignedOut) },
                onReplayOnboarding = onReplayOnboarding,
                onOpenDesignSystem = onOpenDesignSystem,
            ),
        )
    }
    composable<SettingsNotificationsRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        NotificationsScreen(settings = state.settings, onBack = back, onUpdate = viewModel::update)
    }
    composable<SettingsAppearanceRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        AppearanceScreen(settings = state.settings, onBack = back, onUpdate = viewModel::update)
    }
    composable<SettingsLanguageRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        LanguageScreen(
            selected = state.settings.language,
            onBack = back,
            onSelect = { language -> viewModel.update { it.copy(language = language) } },
        )
    }
    composable<SettingsHelpRoute> { HelpScreen(onBack = back) }
    composable<WeeklyRecapRoute> { WeeklyRecapDestination(onClose = back) }
    composable<PlusRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        PlusScreen(
            onClose = back,
            onStartTrial = { plan ->
                viewModel.startPlus(plan)
                navController.navigate(SubscriptionRoute) { popUpTo(PlusRoute) { inclusive = true } }
            },
        )
    }
    composable<SubscriptionRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        SubscriptionScreen(
            settings = state.settings,
            onBack = back,
            onSwitchPlan = viewModel::switchPlan,
            onCancelPlan = {
                viewModel.cancelPlus()
                navController.popBackStack()
            },
        )
    }
}
