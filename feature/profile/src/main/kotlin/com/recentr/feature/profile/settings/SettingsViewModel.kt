package com.recentr.feature.profile.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.SettingsRepository
import com.recentr.core.data.UserPreferencesRepository
import com.recentr.core.model.AppSettings
import com.recentr.core.model.PlusPlan
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val profile: UserProfile = UserProfile(),
    val loaded: Boolean = false,
)

/**
 * Shared by the Settings screens (66–72) and Recentr Plus (64–65): every change is written to
 * [SettingsRepository] / [UserPreferencesRepository] right away.
 */
internal class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(settingsRepository.settings, preferences.profile) { settings, profile ->
        SettingsUiState(settings = settings, profile = profile, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    /**
     * "Start 7-day free trial". There is no billing backend yet: the plan is kept on this device
     * and renews when the trial ends.
     */
    fun startPlus(plan: PlusPlan, now: Long = System.currentTimeMillis()) =
        update { it.copy(plusPlan = plan, plusRenewsAtMillis = now + TRIAL_MILLIS) }

    fun switchPlan(plan: PlusPlan) = update { it.copy(plusPlan = plan) }

    fun cancelPlus() = update { it.copy(plusPlan = null, plusRenewsAtMillis = null) }

    /** "Log out": no account backend, so the user goes back through the onboarding (rules stay). */
    fun logOut(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.update { it.copy(email = "", onboardingCompleted = false) }
            onDone()
        }
    }

    /** "Delete account": erases everything stored on this device. */
    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            RecentrData.eraseAll()
            onDone()
        }
    }

    companion object {
        private const val TRIAL_MILLIS = 7 * 24 * 3_600_000L

        val Factory = viewModelFactory {
            initializer { SettingsViewModel(RecentrData.settings, RecentrData.userPreferences) }
        }
    }
}
