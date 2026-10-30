package com.recentr.feature.focus

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.recentr.core.data.BlockingAccess
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.component.BlockingAccessSheet
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.FocusPhase
import com.recentr.core.model.UsageApp
import com.recentr.feature.focus.complete.FocusCompleteScreen
import com.recentr.feature.focus.history.FocusHistoryScreen
import com.recentr.feature.focus.session.AppOnHoldDialog
import com.recentr.feature.focus.session.FocusBreakContent
import com.recentr.feature.focus.session.FocusRunningContent
import com.recentr.feature.focus.session.FocusSessionActions
import com.recentr.feature.focus.setup.FocusSetupScreen
import com.recentr.feature.focus.setup.SoundsSheet
import com.recentr.feature.focus.sound.FocusSessionService
import com.recentr.feature.focus.ui.formatClock
import kotlinx.serialization.Serializable

@Serializable internal data object FocusSessionRoute

@Serializable internal data object FocusCompleteRoute

@Serializable internal data object FocusHistoryRoute

/**
 * Timer tab content (Figma "Focus timer" 91 · Set up): starting a session — or going back to the
 * running one — pushes the session screens above the tab bar. [contentPadding] (bottom) is kept
 * clear for the floating tab bar. The apps on hold are picked with the rules' "Choose apps"
 * ([chooseApps]), so both pick apps the same way.
 */
@Composable
fun FocusTab(
    navController: NavHostController,
    contentPadding: PaddingValues,
    chooseApps: @Composable (initial: List<UsageApp>, onDone: (List<UsageApp>) -> Unit, onDismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FocusSetupViewModel = viewModel(factory = FocusSetupViewModel.Factory)
    val config by viewModel.config.collectAsStateWithLifecycle()
    val run by viewModel.run.collectAsStateWithLifecycle()
    val blockingOn by RecentrData.blockingEnabled.collectAsStateWithLifecycle()
    var explainBlocking by rememberSaveable { mutableStateOf(false) }
    FocusSetupScreen(
        config = config,
        run = run,
        contentPadding = contentPadding,
        onUpdate = viewModel::update,
        onStart = {
            viewModel.start()
            navController.navigate(FocusSessionRoute) { launchSingleTop = true }
        },
        onResume = { navController.navigate(FocusSessionRoute) { launchSingleTop = true } },
        onOpenLog = { navController.navigate(FocusHistoryRoute) { launchSingleTop = true } },
        modifier = modifier,
        blockingOn = blockingOn,
        onTurnOnBlocking = { explainBlocking = true },
        chooseApps = chooseApps,
    )
    if (explainBlocking) {
        val context = LocalContext.current
        BlockingAccessSheet(onOpenSettings = { BlockingAccess.openSettings(context) }, onDismissRequest = { explainBlocking = false })
    }
}

/** Focus timer destinations (Figma "Focus timer" 92–98), drawn above the tab bar. */
fun NavGraphBuilder.focusDestinations(
    navController: NavHostController,
    /** The session's apps on hold, all of them, on the sheet a rule's page lists its own with. */
    appsOnHold: @Composable (apps: List<UsageApp>, onDismiss: () -> Unit) -> Unit,
) {
    composable<FocusSessionRoute> { FocusSessionDestination(navController, appsOnHold) }
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

private enum class SessionOverlay { Apps, Leave, Ending, Sounds }

/** Running ⇄ Break of one session, the focus sounds and the "on hold" dialogs. */
@Composable
private fun FocusSessionDestination(
    navController: NavHostController,
    appsOnHold: @Composable (apps: List<UsageApp>, onDismiss: () -> Unit) -> Unit,
) {
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

    // The session goes on, sounds and all, once this screen is gone or Recentr is in the background.
    val context = LocalContext.current
    LaunchedEffect(run != null) {
        if (run != null) FocusSessionService.start(context)
    }

    if (run == null) {
        Box(Modifier.fillMaxSize().background(RecentrTheme.colors.bgPage))
        return
    }
    val actions = FocusSessionActions(
        onClose = { overlay = SessionOverlay.Leave },
        onMinimize = { navController.popBackStack() },
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onEnd = { overlay = SessionOverlay.Ending },
        onRestart = viewModel::restartRound,
        onUndoRestart = viewModel::undoRestart,
        onSkipBreak = viewModel::skipBreak,
        onBlockedApps = { overlay = SessionOverlay.Apps },
        onChangeSound = { overlay = SessionOverlay.Sounds },
        onOpenLog = { navController.navigate(FocusHistoryRoute) { launchSingleTop = true } },
    )
    Crossfade(targetState = run.phase, label = "focusPhase") { phase ->
        when (phase) {
            FocusPhase.Focus -> FocusRunningContent(
                run = run,
                now = state.now,
                reduceMotion = state.reduceMotion,
                actions = actions,
                restartedRound = state.restartedRound,
            )
            FocusPhase.Break -> FocusBreakContent(run = run, now = state.now, actions = actions)
        }
    }

    val focusedMinutes = (run.focusedMillis(state.now) / 60_000L).toInt()
    val endsAt = formatClock(state.now + run.remainingMillis(state.now))
    when (overlay) {
        SessionOverlay.Apps -> appsOnHold(run.config.blockedApps) { overlay = null }
        SessionOverlay.Leave -> AppOnHoldDialog(
            title = stringResource(R.string.hold_title, focusedMinutes),
            body = stringResource(R.string.hold_leave_body, endsAt),
            backText = stringResource(R.string.hold_back),
            endText = stringResource(R.string.hold_end),
            app = null,
            onBack = { overlay = null },
            onEnd = {
                overlay = null
                viewModel.end()
            },
            onDismissRequest = { overlay = null },
            strict = run.isStrict,
        )
        // From End (Figma 92D): "Keep focusing" stays the main action, "End session" the other.
        SessionOverlay.Ending -> {
            val count = run.config.blockedApps.size
            val apps = if (count == 0) stringResource(R.string.hold_end_now) else pluralStringResource(R.plurals.hold_end_apps, count, count)
            AppOnHoldDialog(
                title = stringResource(R.string.hold_title, focusedMinutes),
                body = if (focusedMinutes < 1) apps else stringResource(R.string.hold_end_minutes, apps, focusedMinutes),
                backText = stringResource(R.string.hold_keep_focusing),
                endText = stringResource(R.string.focus_end_session),
                app = null,
                onBack = { overlay = null },
                onEnd = {
                    overlay = null
                    viewModel.end()
                },
                onDismissRequest = { overlay = null },
                strict = run.isStrict,
            )
        }
        // During a round the session plays the changes; on a break or a pause, the sheet previews them.
        SessionOverlay.Sounds -> SoundsSheet(
            mix = run.config.sounds,
            muted = run.config.mutedSounds,
            onChange = { mix, muted -> viewModel.updateConfig { it.copy(sounds = mix, mutedSounds = muted) } },
            onDismissRequest = { overlay = null },
            preview = run.phase != FocusPhase.Focus || run.isPaused,
        )
        null -> Unit
    }
}
