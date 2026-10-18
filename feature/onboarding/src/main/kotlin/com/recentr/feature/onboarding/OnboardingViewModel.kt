package com.recentr.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.UserPreferencesRepository
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single ViewModel of the onboarding flow, scoped to the onboarding navigation graph so every
 * screen shares the same answers. Answers are written to [UserPreferencesRepository] as the user
 * confirms each step ([save]) so they survive process death; the stored profile pre-fills the
 * flow when it is replayed.
 */
class OnboardingViewModel(
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Header progress of the last step shown, so the next header animates from it (UI only). */
    internal var lastShownProgress: Float? = null

    /** Guards [complete] against double taps (it navigates away once). */
    private var completing = false

    /** Set as soon as the user answers something, so the async pre-fill never overwrites it. */
    private var touched = false

    init {
        viewModelScope.launch {
            val profile = preferences.profile.first()
            if (!touched) _uiState.value = profile.toUiState()
        }
    }

    fun onNameChange(name: String) = edit { it.copy(name = name.take(MAX_NAME_LENGTH)) }

    fun onTogglePainPoint(point: PainPoint) = edit { state ->
        val current = state.painPoints
        val next = when {
            point in current -> current - point
            point == PainPoint.Fine -> setOf(PainPoint.Fine)
            else -> current - PainPoint.Fine + point
        }
        state.copy(painPoints = next)
    }

    fun onSelectUsage(bucket: UsageBucket) = edit { state ->
        state.copy(
            usageBucket = bucket,
            dailyGoalMinutes = if (state.goalEdited) state.dailyGoalMinutes else bucket.suggestedGoalMinutes,
        )
    }

    fun onDecreaseGoal() = edit {
        it.copy(dailyGoalMinutes = (it.dailyGoalMinutes - GOAL_STEP_MINUTES).coerceAtLeast(MIN_GOAL_MINUTES), goalEdited = true)
    }

    fun onIncreaseGoal() = edit {
        it.copy(dailyGoalMinutes = (it.dailyGoalMinutes + GOAL_STEP_MINUTES).coerceAtMost(MAX_GOAL_MINUTES), goalEdited = true)
    }

    fun onNotificationsResult(enabled: Boolean) {
        edit { it.copy(notificationsEnabled = enabled) }
        save()
    }

    /** Persists the current answers. Called whenever a step is confirmed. */
    fun save() {
        val state = _uiState.value
        viewModelScope.launch { preferences.update { it.withAnswers(state) } }
    }

    /** Persists everything, marks onboarding as done, then calls [onDone] (navigates to Home). */
    fun complete(onDone: () -> Unit) {
        if (completing) return
        completing = true
        val state = _uiState.value
        viewModelScope.launch {
            preferences.update { it.withAnswers(state).copy(onboardingCompleted = true) }
            onDone()
        }
    }

    private inline fun edit(transform: (OnboardingUiState) -> OnboardingUiState) {
        touched = true
        _uiState.update(transform)
    }

    companion object {
        const val MAX_NAME_LENGTH = 30

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { OnboardingViewModel(RecentrData.userPreferences) }
        }
    }
}

private fun UserProfile.toUiState(): OnboardingUiState {
    val bucket = UsageBucket.fromId(reportedUsageBucket)
    return OnboardingUiState(
        name = name,
        painPoints = painPoints.mapNotNull(PainPoint::fromId).toSet(),
        usageBucket = bucket,
        // The stored goal is kept until the usage question is answered again.
        dailyGoalMinutes = dailyGoalMinutes,
        notificationsEnabled = notificationsEnabled,
    )
}

private fun UserProfile.withAnswers(state: OnboardingUiState) = copy(
    name = state.trimmedName,
    painPoints = state.painPoints.map(PainPoint::id).toSet(),
    reportedUsageBucket = state.usageBucket?.id,
    dailyGoalMinutes = state.dailyGoalMinutes,
    notificationsEnabled = state.notificationsEnabled,
)
