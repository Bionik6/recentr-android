package com.recentr.feature.profile.recap

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import java.time.DayOfWeek
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Numbers of the weekly recap (this week versus last week). */
@Immutable
internal data class WeeklyRecap(
    /** Screen-time change versus last week, in percent (−38). */
    val changePercent: Int,
    /** Minutes less than last week (0 when more). */
    val savedMinutes: Int,
    /** Day with the least screen time so far this week. */
    val bestDay: DayOfWeek?,
    val streakDays: Int,
)

@Immutable
internal data class WeeklyRecapUiState(
    val name: String = "",
    val recap: WeeklyRecap? = null,
    val reduceMotion: Boolean = false,
)

internal class WeeklyRecapViewModel : ViewModel() {

    private val week = RecentrData.screenTime.summary(UsagePeriod.ThisWeek)
        .filterIsInstance<ScreenTimeState.Ready>()
        .map<ScreenTimeState.Ready, WeeklyRecap?> { it.summary.toRecap() }
        .onStart { emit(null) }

    val state: StateFlow<WeeklyRecapUiState> = combine(
        RecentrData.userPreferences.profile,
        RecentrData.settings.settings,
        week,
    ) { profile, settings, recap ->
        WeeklyRecapUiState(name = profile.name, recap = recap, reduceMotion = settings.reduceMotion)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyRecapUiState())

    companion object {
        val Factory = viewModelFactory { initializer { WeeklyRecapViewModel() } }
    }
}

private fun ScreenTimeSummary.toRecap(): WeeklyRecap {
    val previous = previousTotalMinutes?.takeIf { it > 0 }
    val change = if (previous == null) 0 else ((totalMinutes - previous) * 100f / previous).roundToInt()
    val best = measuredDays.filter { it in dailyMinutes.indices }.minByOrNull { dailyMinutes[it] }
    return WeeklyRecap(
        changePercent = change,
        savedMinutes = ((previous ?: totalMinutes) - totalMinutes).coerceAtLeast(0),
        bestDay = best?.let { DayOfWeek.of(it + 1) },
        streakDays = streakDays,
    )
}
