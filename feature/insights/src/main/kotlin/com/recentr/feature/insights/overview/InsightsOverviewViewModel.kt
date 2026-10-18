package com.recentr.feature.insights.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.data.UserPreferencesRepository
import com.recentr.core.model.AppCategory
import com.recentr.core.model.DayStats
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UserProfile
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tabs of the Insights overview (Figma 99 · Day / Week / Month, Month = Figma 102). */
internal enum class InsightsTab { Day, Week, Month }

@Immutable
internal data class OverviewUiState(
    val tab: InsightsTab = InsightsTab.Week,
    /** The day shown by the Day tab (Home's Today or Yesterday). */
    val dayPeriod: UsagePeriod = UsagePeriod.Today,
    /** Numbers of the Day / Week tab; null while the first sync runs. */
    val summary: ScreenTimeSummary? = null,
    val loading: Boolean = true,
    val goalMinutes: Int = UserProfile().dailyGoalMinutes,
    val month: YearMonth = YearMonth.now(),
    /** Every day of [month] (Month tab); null while loading. */
    val monthDays: List<DayStats>? = null,
    val today: LocalDate = LocalDate.now(),
) {
    /** Period of the Day / Week tab. */
    val period: UsagePeriod
        get() = when (tab) {
            InsightsTab.Day -> dayPeriod
            InsightsTab.Week -> UsagePeriod.ThisWeek
            InsightsTab.Month -> UsagePeriod.ThisMonth
        }

    /** Day with the least screen time so far (today only when it is the only day). */
    val bestDay: DayStats?
        get() = monthDays?.filter { it.hasData && !it.date.isAfter(today) }
            ?.let { days -> days.filter { it.date != today }.ifEmpty { days } }
            ?.minByOrNull { it.minutes }

    val toughestDay: DayStats?
        get() = monthDays?.filter { it.hasData && !it.date.isAfter(today) }?.maxByOrNull { it.minutes }
}

/**
 * Insights overview: the Day / Week summaries of the screen-time repository (with its Syncing
 * state) and the days of the current month for the calendar.
 */
internal class InsightsOverviewViewModel(
    initialTab: InsightsTab,
    dayPeriod: UsagePeriod,
    private val screenTime: ScreenTimeRepository,
    userPreferences: UserPreferencesRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val local = MutableStateFlow(OverviewUiState(tab = initialTab, dayPeriod = dayPeriod, today = today()))
    private var loadJob: Job? = null

    val state: StateFlow<OverviewUiState> = combine(local, userPreferences.profile.map { it.dailyGoalMinutes }) { state, goal ->
        state.copy(goalMinutes = goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    init {
        load()
        // Usage access granted (or revoked) in Settings: demo ↔ device numbers.
        viewModelScope.launch { screenTime.source.drop(1).collect { load() } }
    }

    fun selectTab(tab: InsightsTab) {
        if (tab == local.value.tab) return
        local.update { it.copy(tab = tab, summary = if (tab == InsightsTab.Month) it.summary else null) }
        load()
    }

    /** Re-reads the numbers (back from another screen, the day may have changed). */
    fun refresh() = load()

    private fun load() {
        loadJob?.cancel()
        val tab = local.value.tab
        val date = today()
        loadJob = viewModelScope.launch {
            if (tab == InsightsTab.Month) {
                val month = YearMonth.from(date)
                local.update { it.copy(loading = it.monthDays == null, month = month, today = date) }
                val days = screenTime.dailyStats(month.atDay(1), month.atEndOfMonth())
                local.update { it.copy(monthDays = days, loading = false) }
            } else {
                screenTime.summary(local.value.period).collect { result ->
                    when (result) {
                        ScreenTimeState.Syncing -> local.update { it.copy(loading = true, today = date) }
                        is ScreenTimeState.Ready -> local.update { it.copy(summary = result.summary, loading = false) }
                    }
                }
            }
        }
    }

    companion object {
        fun factory(tab: InsightsTab, dayPeriod: UsagePeriod) = viewModelFactory {
            initializer {
                InsightsOverviewViewModel(tab, dayPeriod, RecentrData.screenTime, RecentrData.userPreferences)
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
