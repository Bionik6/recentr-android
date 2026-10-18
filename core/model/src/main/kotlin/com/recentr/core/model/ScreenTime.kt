package com.recentr.core.model

import kotlinx.serialization.Serializable

/** Category of an app, for the "Categories" grouping and the Quality split. */
enum class AppCategory { Social, Entertainment, Music, Games, Productivity, Other }

/**
 * An installed app, identified by its [packageName]: in the usage lists one of the catalogue apps
 * ([catalogue] set, Figma icon) or any other app (label from the launcher, icon loaded from the
 * package); in rules and focus sessions, an app picked from the device's launcher apps.
 */
@Serializable
data class UsageApp(
    val packageName: String,
    val label: String,
    val category: AppCategory,
    val catalogue: AppId? = null,
) {
    companion object {
        /** The catalogue [app], published under [packageName] (its first known package by default). */
        fun of(app: AppId, packageName: String = app.packageNames.first()): UsageApp =
            UsageApp(packageName, app.displayName, app.catalogueCategory, app)

        private val AppId.catalogueCategory: AppCategory
            get() = when (this) {
                AppId.Instagram, AppId.X, AppId.TikTok, AppId.Facebook -> AppCategory.Social
                AppId.YouTube -> AppCategory.Entertainment
                AppId.Spotify -> AppCategory.Music
                AppId.Roblox -> AppCategory.Games
                AppId.Gmail -> AppCategory.Productivity
            }
    }
}

/** Usage of one app over the selected period. Owned by the home flow. */
data class AppUsage(
    val app: UsageApp,
    val minutes: Int,
) {
    constructor(app: AppId, minutes: Int) : this(UsageApp.of(app), minutes)
}

enum class UsagePeriod { Today, Yesterday, ThisWeek, ThisMonth }

/** Aggregated screen-time numbers shown on Home. */
data class ScreenTimeSummary(
    val period: UsagePeriod,
    val totalMinutes: Int,
    /** Minutes saved versus the user's previous habit ("Decrease ▾ 6h48"). */
    val decreaseMinutes: Int,
    val pickups: Int,
    val notifications: Int,
    /** 24 values: minutes used during each hour (today / yesterday). */
    val hourlyMinutes: List<Int>,
    /** 7 values Monday → Sunday: minutes per day (week / month views). */
    val dailyMinutes: List<Int>,
    /** Most used apps first. */
    val mostUsed: List<AppUsage>,
    /** Longest stretch without the phone today, in minutes. */
    val longestOfflineMinutes: Int,
    /** Share of the day per category, in percent (Quality page). */
    val focusPercent: Int,
    val socialPercent: Int,
    val funPercent: Int,
    val streakDays: Int,
    /** Hours (0–23) mostly spent in focus apps — highlighted on the Quality page chart. */
    val focusHours: Set<Int> = emptySet(),
    /** Number of days covered by [totalMinutes] (1 for a single day, 7 for a week…). */
    val days: Int = 1,
    /** Total of the previous comparable period ("38% less than last week"), if known. */
    val previousTotalMinutes: Int? = null,
    /**
     * Indices of [dailyMinutes] backed by data: days still to come this week (or weekdays not yet
     * reached this month) are left out of the best day and the average line.
     */
    val measuredDays: Set<Int> = ALL_DAYS,
) {
    /** Average minutes per day over the period (equals [totalMinutes] for single days). */
    val dailyAverageMinutes: Int
        get() = if (days <= 1) totalMinutes else Math.round(totalMinutes.toFloat() / days)

    /** True for the multi-day periods (week / month), which show the per-day chart. */
    val isMultiDay: Boolean
        get() = period == UsagePeriod.ThisWeek || period == UsagePeriod.ThisMonth

    private companion object {
        val ALL_DAYS = (0..6).toSet()
    }
}
