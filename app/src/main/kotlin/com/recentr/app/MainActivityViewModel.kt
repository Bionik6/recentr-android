package com.recentr.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.SettingsRepository
import com.recentr.core.data.UserPreferencesRepository
import com.recentr.core.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

enum class StartState { Loading, Onboarding, Main }

/** Decides the start destination once the persisted profile has been read (keeps the splash meanwhile). */
class MainActivityViewModel(
    userPreferences: UserPreferencesRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val startState: StateFlow<StartState> = flow {
        val profile = userPreferences.profile.first()
        emit(if (profile.onboardingCompleted) StartState.Main else StartState.Onboarding)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartState.Loading)

    /**
     * Settings › Appearance, null until first read. Held here so a recreated activity has them on
     * its first frame: Reduce motion is read once by animations as they start.
     */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { MainActivityViewModel(RecentrData.userPreferences, RecentrData.settings) }
        }
    }
}
