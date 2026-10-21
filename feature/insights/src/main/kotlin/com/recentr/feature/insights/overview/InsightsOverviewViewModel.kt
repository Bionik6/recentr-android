package com.recentr.feature.insights.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeSource
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.model.AppCategory
import com.recentr.core.model.DayStats
import com.recentr.core.model.RangeUnit
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import com.recentr.core.model.UserProfile
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
internal data class OverviewUiState(
    /** The day, week or month shown: the Day / Week / Month tab and the date selector. */
    val range: UsageRange = UsageRange.of(RangeUnit.Day, LocalDate.now()),
    /** The first day with screen time on the device: the selector stops there (null: no data). */
    val firstDay: LocalDate? = null,
    /** Numbers of the Day / Week tab ([ScreenTimeSummary.range] tells which); null while the first sync runs. */
    val summary: ScreenTimeSummary? = null,
    val loading: Boolean = true,
    /** Whether the numbers shown were read with usage access (device numbers) or without (empty). */
    val usageAccess: Boolean = true,
    val goalMinutes: Int = UserProfile().dailyGoalMinutes,
    /** Every day of the month shown (Month tab); null while loading. */
    val monthDays: List<DayStats>? = null,
    val today: LocalDate = LocalDate.now(),
) {
    val tab: RangeUnit get() = range.unit

    val month: YearMonth get() = range.month

    /** How the Day / Week numbers are laid out (one day's hours, or a week's days). */
    val period: UsagePeriod get() = range.period(today)

    val canGoBack: Boolean get() = range.hasPrevious(firstDay)

    val canGoForward: Boolean get() = range.hasNext(today)

    /** No usage access: the tab asks for it ("Connect") in place of empty numbers. */
    val needsUsageAccess: Boolean get() = !loading && !usageAccess

    /** Day with the least screen time so far (today only when it is the only day). */
    val bestDay: DayStats?
        get() = monthDays?.filter { it.hasData && !it.date.isAfter(today) }
            ?.let { days -> days.filter { it.date != today }.ifEmpty { days } }
            ?.minByOrNull { it.minutes }

    val toughestDay: DayStats?
        get() = monthDays?.filter { it.hasData && !it.date.isAfter(today) }?.maxByOrNull { it.minutes }
}

/**
 * Insights overview: the summary of the day or week shown (with the repository's Syncing state),
 * or the days of the month shown for the calendar. The date selector walks back to the first
 * day the device has screen time for, and forward to today. Without usage access the numbers are
 * empty, and [OverviewUiState.needsUsageAccess] asks for it instead.
 */
internal class InsightsOverviewViewModel(
    initialRange: UsageRange,
    private val screenTime: ScreenTimeRepository,
    /** The user's daily goal (the goal line). */
    goalMinutes: Flow<Int>,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val local = MutableStateFlow(OverviewUiState(range = initialRange, today = today()))
    private var loadJob: Job? = null

    val state: StateFlow<OverviewUiState> = combine(local, goalMinutes) { state, goal ->
        state.copy(goalMinutes = goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    init {
        load()
        viewModelScope.launch {
            readFirstDay()
            // Usage access granted (or revoked) in Settings: device ↔ empty numbers.
            screenTime.source.drop(1).collect {
                readFirstDay()
                load()
            }
        }
    }

    /** Day / Week / Month: the range of that unit around the day looked at. */
    fun selectTab(unit: RangeUnit) {
        if (unit == local.value.range.unit) return
        show(local.value.range.switchedTo(unit, today()))
    }

    /** The day, week or month before, down to the first day with data. */
    fun previous() {
        val current = local.value
        if (current.canGoBack) show(current.range.previous())
    }

    /** The day, week or month after, up to the current one. */
    fun next() {
        val current = local.value.copy(today = today())
        if (current.canGoForward) show(current.range.next())
    }

    /** Home's usage card: its period (today, this week…). */
    fun show(period: UsagePeriod) = show(UsageRange.of(period, today()))

    /** Re-reads the numbers (back from another screen, the day may have changed). */
    fun refresh() = load()

    private fun show(range: UsageRange) {
        if (range == local.value.range) return
        local.update {
            it.copy(
                range = range,
                summary = null,
                monthDays = if (range.unit == RangeUnit.Month && range.month == it.month) it.monthDays else null,
            )
        }
        load()
    }

    private suspend fun readFirstDay() {
        val first = screenTime.firstDataDay()
        local.update { it.copy(firstDay = first) }
    }

    private fun load() {
        loadJob?.cancel()
        val range = local.value.range
        val date = today()
        loadJob = viewModelScope.launch {
            if (range.unit == RangeUnit.Month) {
                local.update { it.copy(loading = it.monthDays == null, today = date) }
                val days = screenTime.dailyStats(range.start, range.end)
                local.update { it.copy(monthDays = days, loading = false, usageAccess = hasUsageAccess()) }
            } else {
                screenTime.summary(range).collect { result ->
                    when (result) {
                        ScreenTimeState.Syncing -> local.update { it.copy(loading = true, today = date) }
                        is ScreenTimeState.Ready -> local.update {
                            it.copy(summary = result.summary, loading = false, usageAccess = hasUsageAccess())
                        }
                    }
                }
            }
        }
    }

    private fun hasUsageAccess(): Boolean = screenTime.source.value == ScreenTimeSource.Device

    companion object {
        fun factory(range: UsageRange) = viewModelFactory {
            initializer {
                InsightsOverviewViewModel(range, RecentrData.screenTime, RecentrData.userPreferences.profile.map { it.dailyGoalMinutes })
            }
        }
    }
}

/** A segment of "By category": a category, or the rest when [category] is null. */
internal data class CategoryShare(val category: AppCategory?, val minutes: Int, val percent: Int)

/**
 * The three biggest categories of [summary] and the rest. Apps that are not listed (no launcher
 * entry) count as Other; percentages sum to 100.
 */
internal fun categoryShares(summary: ScreenTimeSummary): List<CategoryShare> {
    val byCategory = summary.mostUsed.groupBy { it.app.category }.mapValues { (_, apps) -> apps.sumOf { it.minutes } }.toMutableMap()
    val unlisted = summary.totalMinutes - byCategory.values.sum()
    if (unlisted > 0) byCategory[AppCategory.Other] = (byCategory[AppCategory.Other] ?: 0) + unlisted
    val sorted = byCategory.entries.filter { it.value > 0 }.sortedByDescending { it.value }
    val top = sorted.take(3).map { it.key to it.value }
    val rest = sorted.drop(3).sumOf { it.value }
    val parts: List<Pair<AppCategory?, Int>> = top + if (rest > 0) listOf(null to rest) else emptyList()
    val total = parts.sumOf { it.second }
    if (total <= 0) return emptyList()
    val exact = parts.map { it.second * 100.0 / total }
    val percents = exact.map { it.toInt() }.toMutableList()
    var left = 100 - percents.sum()
    exact.withIndex().sortedByDescending { it.value - it.value.toInt() }.forEach { (index, _) ->
        if (left > 0) {
            percents[index]++
            left--
        }
    }
    return parts.mapIndexed { index, (category, minutes) -> CategoryShare(category, minutes, percents[index]) }
}
