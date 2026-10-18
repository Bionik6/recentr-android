package com.recentr.feature.focus

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.FocusPhase
import com.recentr.feature.focus.complete.FocusCompleteScreen
import com.recentr.feature.focus.history.FocusHistoryScreen
import com.recentr.feature.focus.session.AppOnHoldDialog
import com.recentr.feature.focus.session.FocusBreakContent
import com.recentr.feature.focus.session.FocusRunningContent
import com.recentr.feature.focus.session.FocusSessionActions
import com.recentr.feature.focus.setup.FocusSetupScreen
import com.recentr.feature.focus.setup.SoundsSheet
import com.recentr.feature.focus.sound.AmbientSoundPlayer
import com.recentr.feature.focus.ui.demoApp
import com.recentr.feature.focus.ui.formatClock
import kotlinx.serialization.Serializable

@Serializable internal data object FocusSetupRoute

@Serializable internal data object FocusSessionRoute

@Serializable internal data object FocusCompleteRoute

@Serializable internal data object FocusHistoryRoute

/** Opens the focus timer: the running session if there is one, the set-up otherwise. */
fun NavHostController.navigateToFocus() {
    val route: Any = if (RecentrData.focus.run.value != null) FocusSessionRoute else FocusSetupRoute
    navigate(route) { launchSingleTop = true }
}

/** Focus timer destinations (Figma "Focus timer" 91–98), drawn above the tab bar. */
fun NavGraphBuilder.focusDestinations(navController: NavHostController) {
    composable<FocusSetupRoute> {
        val viewModel: FocusSetupViewModel = viewModel(factory = FocusSetupViewModel.Factory)
        val config by viewModel.config.collectAsStateWithLifecycle()
        FocusSetupScreen(
            config = config,
            onUpdate = viewModel::update,
            onStart = {
                viewModel.start()
                navController.navigate(FocusSessionRoute) { launchSingleTop = true }
            },
            onOpenLog = { navController.navigate(FocusHistoryRoute) { launchSingleTop = true } },
        )
    }
    composable<FocusSessionRoute> { FocusSessionDestination(navController) }
    composable<FocusCompleteRoute> {
        val viewModel: FocusCompleteViewModel = viewModel(factory = FocusCompleteViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        FocusCompleteScreen(
            state = state,
            onNoteChange = viewModel::updateNote,
            onStartAnother = {
                viewModel.startAnotherRound()
                navController.navigate(FocusSessionRoute) { popUpTo(FocusCompleteRoute) { inclusive = true } }
            },
            onDone = { navController.popBackStack() },
        )
    }
    composable<FocusHistoryRoute> {
        val viewModel: FocusHistoryViewModel = viewModel(factory = FocusHistoryViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        FocusHistoryScreen(state = state, onBack = { navController.popBackStack() }, onClear = viewModel::clear)
    }
}

private enum class SessionOverlay { AppOnHold, Leave, Sounds }

/** Running ⇄ Break of one session, the focus sounds and the "on hold" dialogs. */
@Composable
private fun FocusSessionDestination(navController: NavHostController) {
    val viewModel: FocusSessionViewModel = viewModel(factory = FocusSessionViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val run = state.run
    var overlay by rememberSaveable { mutableStateOf<SessionOverlay?>(null) }

    // The session ended (time up or "End"): show what was done, or leave when nothing was logged.
    LaunchedEffect(run == null) {
        if (run == null) {
            if (viewModel.lastCompleted.value != null) {
                navController.navigate(FocusCompleteRoute) { popUpTo(FocusSessionRoute) { inclusive = true } }
            } else {
                navController.popBackStack()
            }
        }
    }

    // Focus sounds play while a round runs and the screen is in front.
    val player = remember { AmbientSoundPlayer() }
    var resumed by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        resumed = true
        onPauseOrDispose { resumed = false }
    }
    val playback = if (resumed && run != null && run.phase == FocusPhase.Focus && !run.isPaused) run.config.sound to run.config.volume else null
    LaunchedEffect(playback) {
        if (playback != null) player.play(playback.first, playback.second) else player.stop()
    }
    DisposableEffect(player) { onDispose { player.stop() } }

    if (run == null) {
        Box(Modifier.fillMaxSize().background(RecentrTheme.colors.bgPage))
        return
    }
    val actions = FocusSessionActions(
        onClose = { overlay = SessionOverlay.Leave },
        onMinimize = { navController.popBackStack() },
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onEnd = { viewModel.end() },
        onSkipBreak = viewModel::skipBreak,
        onBlockedApps = { overlay = SessionOverlay.AppOnHold },
        onChangeSound = { overlay = SessionOverlay.Sounds },
        onOpenLog = { navController.navigate(FocusHistoryRoute) { launchSingleTop = true } },
    )
    Crossfade(targetState = run.phase, label = "focusPhase") { phase ->
        when (phase) {
            FocusPhase.Focus -> FocusRunningContent(run = run, now = state.now, reduceMotion = state.reduceMotion, actions = actions)
            FocusPhase.Break -> FocusBreakContent(run = run, now = state.now, actions = actions)
        }
    }

    val focusedMinutes = (run.focusedMillis(state.now) / 60_000L).toInt()
    val endsAt = formatClock(state.now + run.remainingMillis(state.now))
    when (overlay) {
        SessionOverlay.AppOnHold, SessionOverlay.Leave -> {
            val app = if (overlay == SessionOverlay.AppOnHold) run.config.blockedApps.firstOrNull()?.demoApp else null
            AppOnHoldDialog(
                title = stringResource(R.string.hold_title, focusedMinutes),
                body = if (app != null) {
                    stringResource(R.string.hold_body, app.displayName, endsAt)
                } else {
                    stringResource(R.string.hold_leave_body, endsAt)
                },
                backText = stringResource(R.string.hold_back),
                endText = stringResource(R.string.hold_end),
                app = app,
                onBack = {
                    if (app != null) viewModel.resistUrge()
                    overlay = null
                },
                onEnd = {
                    overlay = null
                    viewModel.end()
                },
                onDismissRequest = { overlay = null },
            )
        }
        SessionOverlay.Sounds -> SoundsSheet(
            sound = run.config.sound,
            volume = run.config.volume,
            onSound = { sound -> viewModel.updateConfig { it.copy(sound = sound) } },
            onVolume = { volume -> viewModel.updateConfig { it.copy(volume = volume) } },
            onDismissRequest = { overlay = null },
        )
        null -> Unit
    }
}
