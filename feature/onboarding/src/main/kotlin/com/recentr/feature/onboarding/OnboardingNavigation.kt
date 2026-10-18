package com.recentr.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.recentr.feature.onboarding.component.HeaderProgress
import com.recentr.feature.onboarding.screen.AllSetScreen
import com.recentr.feature.onboarding.screen.DailyGoalScreen
import com.recentr.feature.onboarding.screen.FrictionScreen
import com.recentr.feature.onboarding.screen.HonestNewsScreen
import com.recentr.feature.onboarding.screen.HowItWorksScreen
import com.recentr.feature.onboarding.screen.MathScreen
import com.recentr.feature.onboarding.screen.NameScreen
import com.recentr.feature.onboarding.screen.NotificationsRoute
import com.recentr.feature.onboarding.screen.PainPointsScreen
import com.recentr.feature.onboarding.screen.SaveProgressScreen
import com.recentr.feature.onboarding.screen.ScreenTimeRoute
import com.recentr.feature.onboarding.screen.SocialProofScreen
import com.recentr.feature.onboarding.screen.TimeSpentScreen
import com.recentr.feature.onboarding.screen.WayOutScreen
import com.recentr.feature.onboarding.screen.WelcomeScreen
import com.recentr.feature.onboarding.screen.YearsScreen
import com.recentr.feature.onboarding.screen.YourTurnScreen
import kotlinx.serialization.Serializable

/** Root route of the onboarding graph — public contract used by `:app`. */
@Serializable data object OnboardingGraphRoute

/**
 * The 17 onboarding steps, in Figma order (artboards 05 → 21). [progress] is the header bar
 * value of the design (null = the artboard has no header).
 */
@Serializable
internal sealed interface OnboardingStep {
    val progress: Float? get() = null

    @Serializable data object Welcome : OnboardingStep
    @Serializable data object Friction : OnboardingStep { override val progress get() = 0.1f }
    @Serializable data object SocialProof : OnboardingStep { override val progress get() = 0.2f }
    @Serializable data object YourTurn : OnboardingStep { override val progress get() = 0.35f }
    @Serializable data object Name : OnboardingStep { override val progress get() = 0.5f }
    @Serializable data object PainPoints : OnboardingStep { override val progress get() = 0.65f }
    @Serializable data object TimeSpent : OnboardingStep { override val progress get() = 0.8f }
    @Serializable data object DailyGoal : OnboardingStep { override val progress get() = 0.9f }
    @Serializable data object Math : OnboardingStep
    @Serializable data object HonestNews : OnboardingStep
    @Serializable data object Years : OnboardingStep
    @Serializable data object WayOut : OnboardingStep
    @Serializable data object HowItWorks : OnboardingStep { override val progress get() = 0.9f }
    @Serializable data object ScreenTime : OnboardingStep { override val progress get() = 0.9f }
    @Serializable data object Notifications : OnboardingStep { override val progress get() = 1f }
    @Serializable data object SaveProgress : OnboardingStep { override val progress get() = 1f }
    @Serializable data object AllSet : OnboardingStep
}

/**
 * Onboarding flow (17 screens). Every step is a nested type-safe destination sharing one
 * [OnboardingViewModel] scoped to this graph. Calls [onFinished] once onboarding is persisted as
 * completed (last screen, or "I already have an account" on Welcome).
 */
fun NavGraphBuilder.onboardingGraph(
    navController: NavHostController,
    onFinished: () -> Unit,
    /** "Start my first session" on the last screen; defaults to just finishing. */
    onStartFirstSession: () -> Unit = onFinished,
) {
    navigation<OnboardingGraphRoute>(startDestination = OnboardingStep.Welcome) {
        step<OnboardingStep.Welcome>(navController) { flow ->
            WelcomeScreen(
                onGetStarted = flow.next(OnboardingStep.Friction),
                // No account backend yet: an existing user simply skips the questions.
                onHaveAccount = { flow.viewModel.complete(onFinished) },
            )
        }
        step<OnboardingStep.Friction>(navController) { flow ->
            FrictionScreen(flow.progress, onBack = flow.back, onContinue = flow.next(OnboardingStep.SocialProof))
        }
        step<OnboardingStep.SocialProof>(navController) { flow ->
            SocialProofScreen(flow.progress, onBack = flow.back, onContinue = flow.next(OnboardingStep.YourTurn))
        }
        step<OnboardingStep.YourTurn>(navController) { flow ->
            YourTurnScreen(flow.progress, onBack = flow.back, onContinue = flow.next(OnboardingStep.Name))
        }
        step<OnboardingStep.Name>(navController) { flow ->
            val state = flow.state
            NameScreen(
                progress = flow.progress,
                name = state.name,
                isValid = state.isNameValid,
                onNameChange = flow.viewModel::onNameChange,
                onBack = flow.back,
                onContinue = flow.saveAndNext(OnboardingStep.PainPoints),
            )
        }
        step<OnboardingStep.PainPoints>(navController) { flow ->
            PainPointsScreen(
                progress = flow.progress,
                selected = flow.state.painPoints,
                onToggle = flow.viewModel::onTogglePainPoint,
                onBack = flow.back,
                onContinue = flow.saveAndNext(OnboardingStep.TimeSpent),
            )
        }
        step<OnboardingStep.TimeSpent>(navController) { flow ->
            TimeSpentScreen(
                progress = flow.progress,
                selected = flow.state.usageBucket,
                onSelect = flow.viewModel::onSelectUsage,
                onBack = flow.back,
                onContinue = flow.saveAndNext(OnboardingStep.DailyGoal),
            )
        }
        step<OnboardingStep.DailyGoal>(navController) { flow ->
            DailyGoalScreen(
                progress = flow.progress,
                state = flow.state,
                onDecrease = flow.viewModel::onDecreaseGoal,
                onIncrease = flow.viewModel::onIncreaseGoal,
                onBack = flow.back,
                onContinue = flow.saveAndNext(OnboardingStep.Math),
            )
        }
        step<OnboardingStep.Math>(navController) { flow ->
            MathScreen(
                // The loader replaces itself so that back from the next screen returns to the goal.
                onFinished = flow.next(OnboardingStep.HonestNews, replaceCurrent = true),
            )
        }
        step<OnboardingStep.HonestNews>(navController) { flow ->
            HonestNewsScreen(onContinue = flow.next(OnboardingStep.Years))
        }
        step<OnboardingStep.Years>(navController) { flow ->
            val state = flow.state
            YearsScreen(
                years = state.yearsOnPhone,
                averageMinutes = state.averageMinutes,
                daysPerYear = state.daysPerYear,
                onContinue = flow.next(OnboardingStep.WayOut),
            )
        }
        step<OnboardingStep.WayOut>(navController) { flow ->
            WayOutScreen(winBackMinutes = flow.state.winBackMinutes, onContinue = flow.next(OnboardingStep.HowItWorks))
        }
        step<OnboardingStep.HowItWorks>(navController) { flow ->
            HowItWorksScreen(flow.progress, onBack = flow.back, onContinue = flow.next(OnboardingStep.ScreenTime))
        }
        step<OnboardingStep.ScreenTime>(navController) { flow ->
            val goNext = flow.nextFromCallback(OnboardingStep.Notifications)
            ScreenTimeRoute(flow.progress, onBack = flow.back, onContinue = { goNext() })
        }
        step<OnboardingStep.Notifications>(navController) { flow ->
            val goNext = flow.nextFromCallback(OnboardingStep.SaveProgress)
            NotificationsRoute(
                progress = flow.progress,
                dailyGoalMinutes = flow.state.dailyGoalMinutes,
                onBack = flow.back,
                onResult = { enabled ->
                    flow.viewModel.onNotificationsResult(enabled)
                    goNext()
                },
            )
        }
        step<OnboardingStep.SaveProgress>(navController) { flow ->
            val goNext = flow.next(OnboardingStep.AllSet)
            SaveProgressScreen(
                progress = flow.progress,
                onBack = flow.back,
                onContinueWithEmail = goNext,
                onContinueWithPhone = goNext,
            )
        }
        step<OnboardingStep.AllSet>(navController) { flow ->
            AllSetScreen(state = flow.state, onStart = { flow.viewModel.complete(onStartFirstSession) })
        }
    }
}

/** What each step destination gets: the shared ViewModel, its state and navigation helpers. */
internal class OnboardingFlowScope(
    val viewModel: OnboardingViewModel,
    val state: OnboardingUiState,
    val progress: HeaderProgress,
    val back: () -> Unit,
    private val navigate: (OnboardingStep, Boolean) -> Unit,
    private val guard: (() -> Unit) -> () -> Unit,
) {
    /** Navigates to [step] (ignored unless the current screen is resumed, so double taps are safe). */
    fun next(step: OnboardingStep, replaceCurrent: Boolean = false): () -> Unit = guard { navigate(step, replaceCurrent) }

    /**
     * Navigates to [step] without the resumed check — for callbacks that arrive while the activity
     * is paused (permission dialog result, return from Settings).
     */
    fun nextFromCallback(step: OnboardingStep): () -> Unit = { navigate(step, false) }

    /** Persists the answers, then navigates to [step]. */
    fun saveAndNext(step: OnboardingStep): () -> Unit = guard {
        viewModel.save()
        navigate(step, false)
    }
}

private inline fun <reified T : OnboardingStep> NavGraphBuilder.step(
    navController: NavHostController,
    crossinline content: @Composable (OnboardingFlowScope) -> Unit,
) {
    composable<T> { entry ->
        val viewModel = entry.onboardingViewModel(navController)
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val step: OnboardingStep = remember(entry) { entry.toRoute<T>() }
        val progress = remember(entry) {
            step.progress?.let { HeaderProgress(value = it, from = viewModel.lastShownProgress ?: 0f) }
        }
        LaunchedEffect(Unit) { step.progress?.let { viewModel.lastShownProgress = it } }
        val back = dropUnlessResumed { navController.popBackStack() }
        val scope = OnboardingFlowScope(
            viewModel = viewModel,
            state = state,
            progress = progress ?: HeaderProgress(0f),
            back = back,
            navigate = { target, replace ->
                navController.navigate(target) {
                    launchSingleTop = true
                    if (replace) popUpTo(entry.destination.id) { inclusive = true }
                }
            },
            guard = { action -> { if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) action() } },
        )
        content(scope)
    }
}

/** The onboarding ViewModel shared by every step: scoped to the [OnboardingGraphRoute] entry. */
@Composable
private fun NavBackStackEntry.onboardingViewModel(navController: NavHostController): OnboardingViewModel {
    val graphEntry = remember(this) { navController.getBackStackEntry<OnboardingGraphRoute>() }
    return viewModel(graphEntry, factory = OnboardingViewModel.Factory)
}
