package com.recentr.feature.profile.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.FocusRepository
import com.recentr.core.data.RecentrData
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.data.UserPreferencesRepository
import com.recentr.core.model.AvatarColor
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class ProfileUiState(
    val name: String = "",
    val initial: String = "",
    val email: String = "",
    val avatar: AvatarColor = AvatarColor.Sunset,
    val memberDays: Int = 1,
    /** Onboarding answer ("Before · 6h · daily average"). */
    val beforeMinutes: Int = 6 * 60,
    /** This week's daily average, or null while screen time syncs. */
    val nowMinutes: Int? = null,
    /** Days behind [nowMinutes] ("last 5 days"). */
    val nowDays: Int = 1,
    val streakDays: Int = 0,
    val urgesResisted: Int = 0,
) {
    /** Remaining waking-life time won back at this pace, in months (6h → 2h10 a day ≈ 10 years). */
    val monthsWonBack: Int?
        get() {
            val now = nowMinutes ?: return null
            val savedPerDay = (beforeMinutes - now).coerceAtLeast(0)
            return (savedPerDay / MINUTES_PER_DAY * REMAINING_YEARS * 12).roundToInt()
        }

    private companion object {
        const val MINUTES_PER_DAY = 24f * 60f
        /** Same horizon as the onboarding "years on your phone" screen. */
        const val REMAINING_YEARS = 64f
    }
}

internal class ProfileViewModel(
    private val preferences: UserPreferencesRepository,
    screenTime: ScreenTimeRepository,
    focus: FocusRepository,
    private val installedAtMillis: () -> Long,
) : ViewModel() {

    private val weekSummary = screenTime.summary(UsagePeriod.ThisWeek)
        .filterIsInstance<ScreenTimeState.Ready>()
        .map<ScreenTimeState.Ready, ScreenTimeSummary?> { it.summary }
        .onStart { emit(null) }

    val state: StateFlow<ProfileUiState> = combine(
        preferences.profile,
        weekSummary,
        focus.history,
    ) { profile, summary, history ->
        val joinedAt = profile.joinedAtMillis ?: installedAtMillis()
        ProfileUiState(
            name = profile.name,
            initial = profile.initial,
            email = profile.email,
            avatar = profile.avatarColor,
            memberDays = ((System.currentTimeMillis() - joinedAt) / DAY_MILLIS + 1).toInt().coerceAtLeast(1),
            beforeMinutes = profile.reportedDailyMinutes,
            nowMinutes = summary?.dailyAverageMinutes,
            nowDays = summary?.days?.coerceAtLeast(1) ?: 1,
            streakDays = summary?.streakDays ?: 0,
            urgesResisted = history.sumOf { it.urgesResisted },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun save(name: String, email: String, avatar: AvatarColor) {
        viewModelScope.launch {
            preferences.update { it.copy(name = name.trim(), email = email.trim(), avatarColor = avatar) }
        }
    }

    companion object {
        private const val DAY_MILLIS = 24 * 3_600_000L

        val Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    preferences = RecentrData.userPreferences,
                    screenTime = RecentrData.screenTime,
                    focus = RecentrData.focus,
                    installedAtMillis = { RecentrData.installedAtMillis },
                )
            }
        }
    }
}

/** Next avatar colour when the edit badge is tapped. */
internal fun AvatarColor.next(): AvatarColor = AvatarColor.entries[(ordinal + 1) % AvatarColor.entries.size]
