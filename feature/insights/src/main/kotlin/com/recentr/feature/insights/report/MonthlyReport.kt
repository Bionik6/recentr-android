package com.recentr.feature.insights.report

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.StreakDay
import com.recentr.core.data.Streaks
import com.recentr.core.model.DayStats
import com.recentr.core.model.UserProfile
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Numbers of the monthly report (Figma 103) and of its share card (110). */
@Immutable
internal data class MonthlyReport(
    val month: YearMonth,
    /** Time under the habit reported in the onboarding, summed over the days that are over. */
    val wonBackMinutes: Int,
    val dailyAverageMinutes: Int,
    /** Daily average versus last month, in minutes (negative = less), if last month has data. */
    val dailyAverageChange: Int?,
    val pickupsPerDay: Int,
    val pickupsPerDayChange: Int?,
    /** Longest streak within the month. */
    val bestStreak: Int,
    /** [bestStreak] beats last month's. */
    val newBestStreak: Boolean,
)

/**
 * Builds the report of [today]'s month from the days of this month and the previous one. Today
 * is left out of the averages and of the time won back while other days exist (it is not over).
 */
internal suspend fun loadMonthlyReport(screenTime: ScreenTimeRepository, profile: UserProfile, today: LocalDate): MonthlyReport {
    val month = YearMonth.from(today)
    val previous = month.minusMonths(1)
    val days = screenTime.dailyStats(previous.atDay(1), today)
    val thisMonth = days.filter { YearMonth.from(it.date) == month }
    val lastMonth = days.filter { YearMonth.from(it.date) == previous }
    val measured = thisMonth.filter { it.hasData && it.date != today }.ifEmpty { thisMonth.filter { it.hasData } }
    val lastMeasured = lastMonth.filter { it.hasData }

    fun List<DayStats>.averageOf(value: (DayStats) -> Int): Int? = if (isEmpty()) null else (sumOf(value).toFloat() / size).roundToInt()
    fun List<DayStats>.bestStreak() = Streaks.best(map { StreakDay(it.date, it.hasData, it.minutes) }, profile.dailyGoalMinutes)

    val average = measured.averageOf { it.minutes } ?: 0
    val pickups = measured.averageOf { it.pickups } ?: 0
    val best = thisMonth.bestStreak()
    return MonthlyReport(
        month = month,
        wonBackMinutes = measured.sumOf { (profile.reportedDailyMinutes - it.minutes).coerceAtLeast(0) },
        dailyAverageMinutes = average,
        dailyAverageChange = lastMeasured.averageOf { it.minutes }?.let { average - it },
        pickupsPerDay = pickups,
        pickupsPerDayChange = lastMeasured.averageOf { it.pickups }?.let { pickups - it },
        bestStreak = best,
        newBestStreak = best > 0 && best > lastMonth.bestStreak(),
    )
}

@Immutable
internal data class MonthlyReportUiState(
    val report: MonthlyReport? = null,
    val reduceMotion: Boolean = false,
)

/** 103 · Monthly report (and the month share card). */
internal class MonthlyReportViewModel(
    screenTime: ScreenTimeRepository,
    today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val report = MutableStateFlow<MonthlyReport?>(null)

    val state: StateFlow<MonthlyReportUiState> = combine(report, RecentrData.settings.settings) { report, settings ->
        MonthlyReportUiState(report, settings.reduceMotion)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyReportUiState())

    init {
        viewModelScope.launch {
            report.value = loadMonthlyReport(screenTime, RecentrData.userPreferences.profile.first(), today())
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { MonthlyReportViewModel(RecentrData.screenTime) } }
    }
}
