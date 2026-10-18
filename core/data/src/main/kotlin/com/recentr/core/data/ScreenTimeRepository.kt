package com.recentr.core.data

import com.recentr.core.model.AppId
import com.recentr.core.model.AppUsage
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/** Loading state of the screen-time aggregation ("Syncing screen time…" on Home). */
sealed interface ScreenTimeState {
    data object Syncing : ScreenTimeState
    data class Ready(val summary: ScreenTimeSummary) : ScreenTimeState
}

/** Where the screen-time numbers come from. */
enum class ScreenTimeSource {
    /** Figma demo numbers (no usage access). */
    Demo,
    /** This device's usage events (usage access granted). */
    Device,
}

/** Source of screen-time numbers. Owned by the home flow. */
interface ScreenTimeRepository {
    /** Current source; a change (usage access granted or revoked) makes the last summary stale. */
    val source: StateFlow<ScreenTimeSource>

    /**
     * Emits [ScreenTimeState.Syncing] first, then the summary for [period]. Collect it again to
     * re-sync (the Home refresh button does).
     */
    fun summary(period: UsagePeriod): Flow<ScreenTimeState>
}

/**
 * Demo data matching the Figma screens ("Home & Insight": 1h12 today, 6h48 decrease, 4 pickups,
 * 5 notifications, X 38m / Instagram 17m…, this week 9h40 with a 1h23 daily average…).
 */
class DemoScreenTimeRepository(
    private val syncDelayMillis: Long = 1_200,
) : ScreenTimeRepository {

    override val source: StateFlow<ScreenTimeSource> = MutableStateFlow(ScreenTimeSource.Demo)

    override fun summary(period: UsagePeriod): Flow<ScreenTimeState> = flow {
        emit(ScreenTimeState.Syncing)
        delay(syncDelayMillis)
        emit(ScreenTimeState.Ready(snapshot(period)))
    }

    /** The demo numbers for [period], without the sync delay (previews, tests). */
    fun snapshot(period: UsagePeriod): ScreenTimeSummary = demo(period)

    private fun hourly(vararg values: Pair<Int, Int>): List<Int> =
        MutableList(24) { 0 }.apply { values.forEach { (hour, minutes) -> this[hour] = minutes } }

    private fun demo(period: UsagePeriod): ScreenTimeSummary = when (period) {
        UsagePeriod.Today -> ScreenTimeSummary(
            period = period,
            totalMinutes = 60 + 12,
            decreaseMinutes = 6 * 60 + 48,
            pickups = 4,
            notifications = 5,
            hourlyMinutes = hourly(7 to 8, 8 to 20, 9 to 12, 10 to 28, 11 to 60, 12 to 24, 13 to 44, 14 to 16),
            dailyMinutes = WEEK,
            mostUsed = listOf(
                AppUsage(AppId.X, 38),
                AppUsage(AppId.Instagram, 17),
                AppUsage(AppId.Gmail, 9),
                AppUsage(AppId.YouTube, 6),
                AppUsage(AppId.Roblox, 2),
            ),
            longestOfflineMinutes = 8 * 60,
            focusPercent = 72,
            socialPercent = 18,
            funPercent = 10,
            streakDays = 3,
            focusHours = setOf(8, 10, 11, 13),
            days = 1,
            previousTotalMinutes = 2 * 60 + 5,
        )
        UsagePeriod.Yesterday -> ScreenTimeSummary(
            period = period,
            totalMinutes = 2 * 60 + 5,
            decreaseMinutes = 5 * 60 + 55,
            pickups = 11,
            notifications = 23,
            hourlyMinutes = hourly(8 to 10, 9 to 6, 11 to 14, 12 to 22, 13 to 8, 17 to 12, 18 to 18, 19 to 24, 20 to 11),
            dailyMinutes = WEEK,
            mostUsed = listOf(
                AppUsage(AppId.X, 52),
                AppUsage(AppId.Instagram, 31),
                AppUsage(AppId.YouTube, 24),
                AppUsage(AppId.Gmail, 11),
                AppUsage(AppId.Roblox, 7),
            ),
            longestOfflineMinutes = 6 * 60 + 30,
            focusPercent = 54,
            socialPercent = 31,
            funPercent = 15,
            streakDays = 3,
            focusHours = setOf(9, 11, 17),
            days = 1,
        )
        UsagePeriod.ThisWeek -> ScreenTimeSummary(
            period = period,
            totalMinutes = 9 * 60 + 40,
            decreaseMinutes = 7 * 8 * 60 - (9 * 60 + 40),
            pickups = 31,
            notifications = 64,
            hourlyMinutes = hourly(),
            dailyMinutes = WEEK,
            mostUsed = listOf(
                AppUsage(AppId.X, 3 * 60 + 2),
                AppUsage(AppId.Instagram, 2 * 60 + 11),
                AppUsage(AppId.YouTube, 60 + 40),
                AppUsage(AppId.Roblox, 60 + 25),
                AppUsage(AppId.Gmail, 60 + 22),
            ),
            longestOfflineMinutes = 9 * 60 + 10,
            focusPercent = 68,
            socialPercent = 21,
            funPercent = 11,
            streakDays = 3,
            days = 7,
            previousTotalMinutes = 15 * 60 + 35,
        )
        UsagePeriod.ThisMonth -> ScreenTimeSummary(
            period = period,
            totalMinutes = 41 * 60 + 12,
            decreaseMinutes = 18 * 8 * 60 - (41 * 60 + 12),
            pickups = 128,
            notifications = 270,
            hourlyMinutes = hourly(),
            // Average per weekday over the month so far.
            dailyMinutes = listOf(152, 141, 148, 132, 139, 96, 151),
            mostUsed = listOf(
                AppUsage(AppId.X, 12 * 60 + 40),
                AppUsage(AppId.Instagram, 9 * 60 + 5),
                AppUsage(AppId.YouTube, 7 * 60 + 30),
                AppUsage(AppId.Gmail, 6 * 60 + 12),
                AppUsage(AppId.Roblox, 5 * 60 + 45),
            ),
            longestOfflineMinutes = 10 * 60 + 5,
            focusPercent = 63,
            socialPercent = 24,
            funPercent = 13,
            streakDays = 3,
            days = 18,
            previousTotalMinutes = 52 * 60 + 9,
        )
    }

    private companion object {
        /** Monday → Sunday, 9h40 in total (bars of the "This week" card). */
        val WEEK = listOf(116, 86, 98, 73, 92, 36, 79)
    }
}
