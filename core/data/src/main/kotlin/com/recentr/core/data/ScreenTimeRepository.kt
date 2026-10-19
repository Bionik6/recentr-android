package com.recentr.core.data

import com.recentr.core.model.AppId
import com.recentr.core.model.AppOpenCount
import com.recentr.core.model.AppUsage
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** Loading state of the screen-time aggregation ("Syncing screen time…" on Home). */
sealed interface ScreenTimeState {
    data object Syncing : ScreenTimeState
    data class Ready(val summary: ScreenTimeSummary) : ScreenTimeState
}

/** Where the screen-time numbers come from. */
enum class ScreenTimeSource {
    /** No usage access: nothing to show yet (the Figma demo numbers in previews). */
    NoAccess,
    /** This device's usage events (usage access granted). */
    Device,
}

/** Source of screen-time numbers. Owned by the home flow. */
interface ScreenTimeRepository {
    /** Current source; a change (usage access granted or revoked) makes the last summary stale. */
    val source: StateFlow<ScreenTimeSource>

    /**
     * Emits [ScreenTimeState.Syncing] first, then the summary of [range] (a day, week or month,
     * up to today; [ScreenTimeSummary.range] is set). Collect it again to re-sync.
     */
    fun summary(range: UsageRange): Flow<ScreenTimeState>

    /** Home's [period], on today's calendar. */
    fun summary(period: UsagePeriod): Flow<ScreenTimeState> = summary(UsageRange.of(period, LocalDate.now()))

    /** Totals of each day from [from] to [to] (inclusive); days to come have no data. */
    suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats>

    /** [packageName] over [range], or null when the app is unknown. */
    suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail?

    suspend fun appDetail(packageName: String, period: UsagePeriod): AppUsageDetail? =
        appDetail(packageName, UsageRange.of(period, LocalDate.now()))

    /** Pickups over [range] and the apps opened first after them. */
    suspend fun pickups(range: UsageRange): PickupStats

    suspend fun pickups(period: UsagePeriod): PickupStats = pickups(UsageRange.of(period, LocalDate.now()))

    /** Minutes per package from [start] (epoch millis) until now: a daily limit's period ([periodStart]). */
    suspend fun appMinutesSince(start: Long): Map<String, Int>

    /**
     * The first day this device has screen time for, or null when it has none: Insights' date
     * selector goes no further back.
     */
    suspend fun firstDataDay(): LocalDate?
}

/**
 * Screen time before usage access is granted: every period is empty (no minutes, apps or pickups,
 * no day with data, no quality split), so the screens show their empty states instead of
 * made-up numbers.
 */
class EmptyScreenTimeRepository(private val zone: () -> ZoneId = ZoneId::systemDefault) : ScreenTimeRepository {

    override val source: StateFlow<ScreenTimeSource> = MutableStateFlow(ScreenTimeSource.NoAccess)

    override fun summary(range: UsageRange): Flow<ScreenTimeState> = flowOf(
        ScreenTimeState.Ready(
            ScreenTimeAggregation.summarize(
                period = range.period(LocalDate.now(zone())),
                days = emptyList(),
                zone = zone(),
                resolveApp = { null },
                baselineDailyMinutes = 0,
                streakDays = 0,
                previousTotalMinutes = null,
                range = range,
            ).copy(focusPercent = 0, socialPercent = 0, funPercent = 0),
        ),
    )

    override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
            .map { DayStats(it, hasData = false, minutes = 0, pickups = 0) }
            .toList()

    override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? = null

    override suspend fun pickups(range: UsageRange): PickupStats =
        ScreenTimeAggregation.pickupStats(range.period(LocalDate.now(zone())), emptyList(), zone(), resolveApp = { null })

    override suspend fun appMinutesSince(start: Long): Map<String, Int> = emptyMap()

    override suspend fun firstDataDay(): LocalDate? = null
}

/**
 * Demo data matching the Figma screens ("Home & Insight": 1h12 today, 6h48 decrease, 4 pickups,
 * 5 notifications, X 38m / Instagram 17m…, this week 9h40 with a 1h23 daily average…).
 */
class DemoScreenTimeRepository(
    private val syncDelayMillis: Long = 1_200,
    /** The daily goal: the streak then follows the demo days, like the streak screen. */
    private val goalMinutes: Flow<Int>? = null,
) : ScreenTimeRepository {

    override val source: StateFlow<ScreenTimeSource> = MutableStateFlow(ScreenTimeSource.NoAccess)

    /** Any day, week or month reads like today's, this week's or this month's demo numbers. */
    override fun summary(range: UsageRange): Flow<ScreenTimeState> = flow {
        emit(ScreenTimeState.Syncing)
        delay(syncDelayMillis)
        val goal = goalMinutes?.first()
        val summary = snapshot(range.period(LocalDate.now())).copy(range = range)
        emit(ScreenTimeState.Ready(if (goal == null) summary else summary.copy(streakDays = streak(goal))))
    }

    /** The first of the demo days. */
    override suspend fun firstDataDay(): LocalDate? = LocalDate.now().minusDays(HISTORY_DAYS)

    /** Streak of the demo days against [goal] (savers included). */
    private fun streak(goal: Int, today: LocalDate = LocalDate.now()): Int {
        val days = dailySnapshot(today.minusDays(31), today, today)
        return Streaks.current(days.asReversed().asSequence().map { StreakDay(it.date, it.hasData, it.minutes) }, goal).days
    }

    /** The demo numbers for [period], without the sync delay (previews, tests). */
    fun snapshot(period: UsagePeriod): ScreenTimeSummary = demo(period)

    override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> {
        delay(syncDelayMillis / 2)
        return dailySnapshot(from, to)
    }

    /**
     * Demo days relative to [today]: today and yesterday match the Today / Yesterday numbers,
     * the rest of this week the Week card bars, older days a repeating pattern (six weeks of
     * history), with an evening-heavy hourly profile.
     */
    fun dailySnapshot(from: LocalDate, to: LocalDate, today: LocalDate = LocalDate.now()): List<DayStats> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { date ->
            val index = Math.floorMod(date.toEpochDay(), 1_000L).toInt()
            when {
                date.isAfter(today) || date.isBefore(today.minusDays(HISTORY_DAYS)) ->
                    DayStats(date, hasData = false, minutes = 0, pickups = 0)
                date == today -> demo(UsagePeriod.Today).let {
                    DayStats(date, true, it.totalMinutes, it.pickups, spread(it.totalMinutes, DAY_PROFILE), 7 * 60 + 2)
                }
                date == today.minusDays(1) -> demo(UsagePeriod.Yesterday).let {
                    DayStats(date, true, it.totalMinutes, it.pickups, spread(it.totalMinutes, DAY_PROFILE), 8 * 60 + 5)
                }
                else -> {
                    val minutes = if (!date.isBefore(monday)) WEEK[date.dayOfWeek.value - 1] else MONTH[index % MONTH.size]
                    DayStats(
                        date = date,
                        hasData = true,
                        minutes = minutes,
                        pickups = PICKUPS[index % PICKUPS.size],
                        hourlyMinutes = spread(minutes, DAY_PROFILE),
                        firstPickupMinute = 6 * 60 + 40 + (index % 5) * 11,
                    )
                }
            }
        }.toList()
    }

    override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? {
        delay(syncDelayMillis / 2)
        val period = range.period(LocalDate.now())
        val summary = demo(period)
        val usage = summary.mostUsed.firstOrNull { it.app.packageName == packageName }
        val app = usage?.app ?: AppId.fromPackageName(packageName)?.let { UsageApp.of(it, packageName) } ?: return null
        val minutes = usage?.minutes ?: 0
        return AppUsageDetail(
            app = app,
            period = period,
            minutes = minutes,
            opens = if (minutes == 0) 0 else minutes / (2 * summary.days) + 3 * summary.days,
            hourlyMinutes = spread(minutes, APP_PROFILE),
            longestSessionMinutes = (minutes / (3 * summary.days)).coerceAtLeast(if (minutes > 0) 3 else 0),
            firstOpenMinute = if (minutes == 0) null else 7 * 60 + 12,
            days = summary.days,
        )
    }

    override suspend fun pickups(range: UsageRange): PickupStats {
        delay(syncDelayMillis / 2)
        val period = range.period(LocalDate.now())
        val summary = demo(period)
        val firstOpens = listOf(AppId.Instagram to 14, AppId.X to 9, AppId.Gmail to 6).map { (app, share) ->
            AppOpenCount(UsageApp.of(app), (summary.pickups * share / 38).coerceAtLeast(1))
        }
        return PickupStats(
            period = period,
            pickups = summary.pickups,
            days = summary.days,
            firstPickupMinute = 7 * 60 + 2,
            hourlyPickups = spread(summary.pickups, PICKUP_PROFILE),
            firstOpens = firstOpens,
        )
    }

    /** Today's demo apps, whatever the start. */
    override suspend fun appMinutesSince(start: Long): Map<String, Int> =
        demo(UsagePeriod.Today).mostUsed.associate { it.app.packageName to it.minutes }

    /** Splits [total] over the 24 hours along [weights] (rounding leftovers go to the busiest hours). */
    private fun spread(total: Int, weights: List<Int>): List<Int> {
        val sum = weights.sum()
        val exact = weights.map { it * total.toDouble() / sum }
        val values = exact.map { it.toInt() }.toMutableList()
        var left = total - values.sum()
        exact.withIndex().sortedByDescending { it.value - it.value.toInt() }.forEach { (hour, _) ->
            if (left > 0) {
                values[hour]++
                left--
            }
        }
        return values
    }

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

        /** Older days (Insights calendar): 48m best day and 5h10 toughest day, as in the design. */
        val MONTH = listOf(150, 205, 112, 48, 190, 310, 95, 168, 130, 250, 72, 184, 140, 225, 88, 160, 118, 205, 60, 175, 138)

        val PICKUPS = listOf(38, 24, 31, 45, 19, 28, 52, 33, 27, 41, 22, 36)

        /** Days of demo history before today. */
        const val HISTORY_DAYS = 45L

        /** Screen time through a demo day: none at night, busiest in the evening. */
        val DAY_PROFILE = listOf(0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 3, 2, 4, 3, 2, 2, 3, 4, 5, 6, 7, 6, 3, 0)

        /** The design's "By hour" bars of an app (TikTok, mostly after 10 PM). */
        val APP_PROFILE = listOf(0, 0, 0, 4, 10, 6, 8, 14, 6, 4, 18, 30, 12, 8, 10, 20, 24, 16, 28, 40, 62, 80, 48, 10)

        /** The design's "When you pick up" heat (two hours per cell). */
        val PICKUP_PROFILE = listOf(0, 0, 0, 0, 0, 0, 2, 2, 4, 4, 1, 1, 2, 2, 1, 1, 4, 4, 6, 6, 6, 6, 2, 2)
    }
}
