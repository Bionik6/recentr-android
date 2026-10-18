package com.recentr.feature.insights.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.AchievementsRepository
import com.recentr.core.data.AchievementsSnapshot
import com.recentr.core.data.RecentrData
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.model.Achievement
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Achievements screens (Figma 105–110): one evaluation of the badges, streak and level. */
internal class AchievementsViewModel(
    private val achievements: AchievementsRepository,
    screenTime: ScreenTimeRepository,
) : ViewModel() {

    private val _snapshot = MutableStateFlow<AchievementsSnapshot?>(null)
    /** Null until the first evaluation. */
    val snapshot: StateFlow<AchievementsSnapshot?> = _snapshot.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
        viewModelScope.launch { screenTime.source.drop(1).collect { refresh() } }
    }

    /** Re-evaluates the badges (back on the screen: a goal or a rule may have changed). */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { _snapshot.value = achievements.load() }
    }

    /** The "New badge" screen is shown for the newest of [badges]: none of them is new any more. */
    fun markCelebrated(badges: List<Achievement>) {
        _snapshot.update { it?.copy(uncelebrated = emptyList()) }
        viewModelScope.launch { achievements.markCelebrated(badges) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AchievementsViewModel(RecentrData.achievements, RecentrData.screenTime) }
        }
    }
}
