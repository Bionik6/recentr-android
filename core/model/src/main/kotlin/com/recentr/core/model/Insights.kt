package com.recentr.core.model

import java.time.LocalDate

/** One local day of screen time (Insights calendar and daily goal, streak, achievements). */
data class DayStats(
    val date: LocalDate,
    /** False for days to come and for days before the system's retained usage history. */
    val hasData: Boolean,
    val minutes: Int,
    val pickups: Int,
    /** 24 values: minutes of screen time in each hour of the day. */
    val hourlyMinutes: List<Int> = List(24) { 0 },
    /** Minute of the day of the first pickup, or null when there was none. */
    val firstPickupMinute: Int? = null,
)

/** One app over a period (Insights · App detail). */
data class AppUsageDetail(
    val app: UsageApp,
    val period: UsagePeriod,
    val minutes: Int,
    /** Times the app came to the foreground. */
    val opens: Int,
    /** 24 values: minutes in each hour, summed over the days of the period. */
    val hourlyMinutes: List<Int>,
    val longestSessionMinutes: Int,
    /** First open of the day (minute of the day); over several days, the median first open. */
    val firstOpenMinute: Int?,
    /** Days of the period with data. */
    val days: Int,
)

/** An app and how many times it was opened first after a pickup. */
data class AppOpenCount(val app: UsageApp, val count: Int)

/** Pickups over a period (Insights · Pickups). */
data class PickupStats(
    val period: UsagePeriod,
    val pickups: Int,
    /** Days of the period with data (pickups a day = [pickups] / [days]). */
    val days: Int,
    /** First pickup of the day (minute of the day); over several days, the median. */
    val firstPickupMinute: Int?,
    /** 24 values: pickups in each hour, summed over the days of the period. */
    val hourlyPickups: List<Int>,
    /** Apps opened first after a pickup, most frequent first. */
    val firstOpens: List<AppOpenCount>,
) {
    /** Pickups per day over the period ([pickups] for a single day). */
    val dailyAverage: Int
        get() = if (days <= 1) pickups else Math.round(pickups.toFloat() / days)
}
