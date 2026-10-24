package com.recentr.feature.onboarding

import android.provider.Telephony
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.data.UserPreferencesRepository
import com.recentr.core.model.StarterRoutine
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single ViewModel of the onboarding flow, scoped to the onboarding navigation graph so every
 * screen shares the same answers. Answers are written to the stored [profile]
 * ([UserPreferencesRepository], through [updateProfile]) as the user confirms each step ([save]) so
 * they survive process death; the stored profile pre-fills the flow when it is replayed.
 * Completing it sets up the starter routines in [rules], blocking the social apps among the
 * [installedApps] but the default SMS app ([smsApp]), each called by [routineName].
 */
class OnboardingViewModel(
    private val profile: Flow<UserProfile>,
    private val updateProfile: suspend (transform: (UserProfile) -> UserProfile) -> Unit,
    private val rules: RulesRepository,
    private val installedApps: suspend () -> List<UsageApp>,
    private val routineName: (StarterRoutine) -> String,
    /** The default SMS app's package, if there's one. */
    private val smsApp: () -> String? = { null },
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
            val stored = profile.first()
            if (!touched) _uiState.value = stored.toUiState()
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

    fun onGoalChange(minutes: Int) = edit {
        if (minutes == it.dailyGoalMinutes) it else it.copy(dailyGoalMinutes = minutes.coerceIn(MIN_GOAL_MINUTES, MAX_GOAL_MINUTES), goalEdited = true)
    }

    fun onNotificationsResult(enabled: Boolean) {
        edit { it.copy(notificationsEnabled = enabled) }
        save()
    }

    /** Persists the current answers. Called whenever a step is confirmed. */
    fun save() {
        val state = _uiState.value
        viewModelScope.launch { updateProfile { it.withAnswers(state) } }
    }

    /**
     * Persists everything, marks onboarding as done, sets up the starter routines, then calls
     * [onDone] (navigates to Home).
     */
    fun complete(onDone: () -> Unit) {
        if (completing) return
        completing = true
        val state = _uiState.value
        viewModelScope.launch {
            updateProfile {
                it.withAnswers(state).copy(
                    onboardingCompleted = true,
                    joinedAtMillis = it.joinedAtMillis ?: System.currentTimeMillis(),
                )
            }
            setUpStarterRoutines()
            onDone()
        }
    }

    /**
     * Working Time, Night Pause and Weekend Zen, blocking the social apps installed but the SMS
     * app. Only for someone without rules: back from logging out, the rules kept stay as they are.
     */
    private suspend fun setUpStarterRoutines() {
        if (rules.rules.value.isNotEmpty()) return
        val apps = StarterRoutine.apps(installedApps(), smsApp())
        StarterRoutine.entries.forEach { routine ->
            rules.create(routine.rule(rules.newId(), routineName(routine), apps))
        }
    }

    private inline fun edit(transform: (OnboardingUiState) -> OnboardingUiState) {
        touched = true
        _uiState.update(transform)
    }

    companion object {
        const val MAX_NAME_LENGTH = 30

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                OnboardingViewModel(
                    profile = RecentrData.userPreferences.profile,
                    updateProfile = RecentrData.userPreferences::update,
                    rules = RecentrData.rules,
                    installedApps = RecentrData.installedApps::load,
                    routineName = { application.getString(it.title) },
                    smsApp = { Telephony.Sms.getDefaultSmsPackage(application) },
                )
            }
        }
    }
}

/** The starter routine's name, as Rules lists it. */
@get:StringRes
private val StarterRoutine.title: Int
    get() = when (this) {
        StarterRoutine.WorkingTime -> R.string.onb_routine_working_time
        StarterRoutine.NightPause -> R.string.onb_routine_night_pause
        StarterRoutine.WeekendZen -> R.string.onb_routine_weekend_zen
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
