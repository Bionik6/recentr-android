package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.AppOpenCount
import com.recentr.core.model.AppUsage
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** The `UsageEvents.Event` types the aggregation reads. */
internal enum class UsageEventType {
    ActivityResumed,
    ActivityPaused,
    ActivityStopped,
    ScreenInteractive,
    ScreenNonInteractive,
    KeyguardShown,
    KeyguardHidden,
    NotificationInterruption,
    DeviceShutdown,
}

/** A `UsageEvents.Event` reduced to what the aggregation needs (fakeable in unit tests). */
internal data class UsageEvent(
    val timeMillis: Long,
    val type: UsageEventType,
    val packageName: String = "",
    /** Activity class of the activity events: one app can have several activities on screen. */
    val className: String = "",
)

/** Half-open interval `[start, end)` in epoch millis. */
internal data class TimeSpan(val start: Long, val end: Long) {
    val millis: Long get() = end - start
}

/** One stretch of [packageName] in the foreground. */
internal data class AppSession(val packageName: String, val start: Long, val end: Long) {
    val millis: Long get() = end - start
}

/** What the events of one day (or of the elapsed part of today) boil down to. */
internal data class DayUsage(
    val date: LocalDate,
    val start: Long,
    val end: Long,
    /** False when the system returned no event at all (before the retained history). */
    val hasData: Boolean,
    /** Foreground sessions per app, clipped to the day. */
    val sessions: List<AppSession>,
    /** Merged stretches with the screen on (or an app in the foreground). */
    val awake: List<TimeSpan>,
    val pickups: Int,
    val notifications: Int,
    /** When each of the [pickups] happened. */
    val pickupTimes: List<Long> = emptyList(),
    /** Package opened first after each pickup (pickups that opened no app are left out). */
    val firstAppsAfterPickup: List<String> = emptyList(),
) {
    /** Screen time: union of the app sessions (split screen / PiP is not counted twice). */
    val screenMillis: Long = mergeSpans(sessions.map { TimeSpan(it.start, it.end) }).sumOf { it.millis }
}

/** How the Quality page splits screen time. */
internal enum class QualityGroup { Focus, Social, Fun }

internal val AppCategory.qualityGroup: QualityGroup
    get() = when (this) {
        AppCategory.Social -> QualityGroup.Social
        AppCategory.Entertainment, AppCategory.Music, AppCategory.Games -> QualityGroup.Fun
        AppCategory.Productivity, AppCategory.Other -> QualityGroup.Focus
    }

/** Merges overlapping or touching spans, sorted by start. */
internal fun mergeSpans(spans: List<TimeSpan>): List<TimeSpan> {
    if (spans.isEmpty()) return emptyList()
    val sorted = spans.filter { it.end > it.start }.sortedBy { it.start }
    val merged = ArrayList<TimeSpan>(sorted.size)
    for (span in sorted) {
        val last = merged.lastOrNull()
        if (last != null && span.start <= last.end) {
            if (span.end > last.end) merged[merged.size - 1] = TimeSpan(last.start, span.end)
        } else {
            merged += span
        }
    }
    return merged
}

/** Rounded minutes of a duration in millis. */
internal fun Long.roundedMinutes(): Int = ((this + 30_000) / 60_000).toInt()

/**
 * Pure screen-time aggregation over [UsageEvent]s: per-day reduction, then the [ScreenTimeSummary]
 * of a period from its days. Unit-tested with fake events; the repository only feeds it.
 */
internal object ScreenTimeAggregation {

    /**
     * Reduces the [events] of `[start, end)` (one local day, or today until now) to a [DayUsage].
     *
     * State at [start] is inferred from the first events: an activity first seen pausing was on
     * screen since [start], a first "screen off" means the screen was on since [start]. Sessions
     * still open at [end] are closed there. Packages in [ignoredPackages] (launcher, system UI)
     * are not screen time.
     */
    fun day(
        date: LocalDate,
        start: Long,
        end: Long,
        events: List<UsageEvent>,
        ignoredPackages: Set<String> = emptySet(),
        hasData: Boolean = events.isNotEmpty(),
    ): DayUsage {
        val window = events.filter { it.timeMillis in start until end }.sortedBy { it.timeMillis }

        val firstScreen = window.firstOrNull { it.type.isScreenOn != null }
        val screenOnAtStart = firstScreen?.type?.isScreenOn == false
        // Without any screen event the day either had none (no data) or the screen never toggled.
        val appsMayBeOnAtStart = screenOnAtStart || firstScreen == null

        val resumed = HashMap<String, MutableSet<String>>()
        val openSince = HashMap<String, Long>()
        if (appsMayBeOnAtStart) {
            val firstActivityEvent = LinkedHashMap<Pair<String, String>, UsageEventType>()
            window.filter { it.type.isActivity }
                .forEach { firstActivityEvent.putIfAbsent(it.packageName to it.className, it.type) }
            firstActivityEvent.forEach { (activity, type) ->
                val (pkg, cls) = activity
                if (type == UsageEventType.ActivityPaused && pkg !in ignoredPackages) {
                    resumed.getOrPut(pkg) { HashSet() } += cls
                    openSince.putIfAbsent(pkg, start)
                }
            }
        }

        val sessions = ArrayList<AppSession>()
        fun close(pkg: String, at: Long) {
            resumed.remove(pkg)
            val since = openSince.remove(pkg) ?: return
            if (at > since) sessions += AppSession(pkg, since, at)
        }
        fun closeAll(at: Long) = openSince.keys.toList().forEach { close(it, at) }

        val awake = ArrayList<TimeSpan>()
        var screenOnSince: Long? = if (screenOnAtStart) start else null
        var keyguardSeen = false
        // Pickup candidates of both kinds (unlocks / screen-ons) and the app each one led to.
        val unlocks = PickupLog()
        val screenOns = PickupLog()
        var notifications = 0

        for (event in window) {
            val pkg = event.packageName
            when (event.type) {
                UsageEventType.ActivityResumed -> if (pkg !in ignoredPackages) {
                    resumed.getOrPut(pkg) { HashSet() } += event.className
                    openSince.putIfAbsent(pkg, event.timeMillis)
                    unlocks.opened(pkg)
                    screenOns.opened(pkg)
                }
                UsageEventType.ActivityPaused, UsageEventType.ActivityStopped -> {
                    val activities = resumed[pkg]
                    if (activities != null && activities.remove(event.className) && activities.isEmpty()) {
                        close(pkg, event.timeMillis)
                    }
                }
                UsageEventType.ScreenInteractive -> {
                    screenOns.pickedUp(event.timeMillis)
                    if (screenOnSince == null) screenOnSince = event.timeMillis
                }
                UsageEventType.ScreenNonInteractive, UsageEventType.DeviceShutdown -> {
                    screenOnSince?.let { awake += TimeSpan(it, event.timeMillis) }
                    screenOnSince = null
                    // Nothing is on screen once it is off, whatever pause events follow.
                    closeAll(event.timeMillis)
                    unlocks.screenOff()
                    screenOns.screenOff()
                }
                UsageEventType.KeyguardShown -> keyguardSeen = true
                UsageEventType.KeyguardHidden -> {
                    keyguardSeen = true
                    unlocks.pickedUp(event.timeMillis)
                }
                UsageEventType.NotificationInterruption -> notifications++
            }
        }
        closeAll(end)
        screenOnSince?.let { awake += TimeSpan(it, end) }

        // With a lock screen, a pickup is an unlock; without one, every screen-on.
        val pickups = if (keyguardSeen) unlocks else screenOns
        return DayUsage(
            date = date,
            start = start,
            end = end,
            hasData = hasData,
            sessions = sessions.sortedBy { it.start },
            awake = mergeSpans(awake + sessions.map { TimeSpan(it.start, it.end) }),
            pickups = pickups.times.size,
            notifications = notifications,
            pickupTimes = pickups.times,
            firstAppsAfterPickup = pickups.firstApps,
        )
    }

    /**
     * Minutes per package of the [days]' sessions from [since] on (a session that started before is
     * cut there), rounded per package like Most used: a limit's use since its Start.
     */
    fun appMinutes(days: List<DayUsage>, since: Long): Map<String, Int> {
        val millis = HashMap<String, Long>()
        days.forEach { day ->
            day.sessions.forEach { session ->
                val counted = session.end - maxOf(session.start, since)
                if (counted > 0) millis.merge(session.packageName, counted, Long::plus)
            }
        }
        return millis.mapValues { it.value.roundedMinutes() }.filterValues { it > 0 }
    }

    /** Pickups of one kind and the first app opened after each, until the screen goes off. */
    private class PickupLog {
        val times = ArrayList<Long>()
        val firstApps = ArrayList<String>()
        private var waiting = false

        fun pickedUp(at: Long) {
            times += at
            waiting = true
        }

        fun opened(packageName: String) {
            if (!waiting) return
            firstApps += packageName
            waiting = false
        }

        fun screenOff() {
            waiting = false
        }
    }

    /** [day] reduced to its totals and hourly minutes (Insights calendar, goal, achievements). */
    fun dayStats(day: DayUsage, zone: ZoneId): DayStats = DayStats(
        date = day.date,
        hasData = day.hasData,
        minutes = day.screenMillis.roundedMinutes(),
        pickups = day.pickups,
        hourlyMinutes = hourlyMinutes(day, zone),
        firstPickupMinute = day.pickupTimes.minOrNull()?.let { minuteOfDay(it, zone) },
    )

    /**
     * [packageName] over [days] (the days of [period]): its foreground time, opens, hourly
     * minutes, longest session (sessions less than [MERGE_GAP_MILLIS] apart are one) and first
     * open of the day — the median one over several days.
     */
    fun appDetail(
        app: UsageApp,
        period: UsagePeriod,
        days: List<DayUsage>,
        zone: ZoneId,
    ): AppUsageDetail {
        val covered = days.filter { it.hasData }
        val perDay = covered.map { day -> day.sessions.filter { it.packageName == app.packageName } }
        val sessions = perDay.flatMap { joinSessions(it) }
        val spans = sessions.map { TimeSpan(it.start, it.end) }
        return AppUsageDetail(
            app = app,
            period = period,
            minutes = mergeSpans(spans).sumOf { it.millis }.roundedMinutes(),
            opens = sessions.size,
            hourlyMinutes = hourlyMillis(mergeSpans(spans), zone).map { it.roundedMinutes() },
            longestSessionMinutes = (sessions.maxOfOrNull { it.millis } ?: 0L).roundedMinutes(),
            firstOpenMinute = perDay.mapNotNull { day -> day.minOfOrNull { it.start }?.let { minuteOfDay(it, zone) } }.median(),
            days = covered.size,
        )
    }

    /** Pickups over [days] (the days of [period]), with the apps opened first after them. */
    fun pickupStats(
        period: UsagePeriod,
        days: List<DayUsage>,
        zone: ZoneId,
        resolveApp: (String) -> UsageApp?,
    ): PickupStats {
        val covered = days.filter { it.hasData }
        val hourly = IntArray(24)
        covered.forEach { day -> day.pickupTimes.forEach { hourly[Instant.ofEpochMilli(it).atZone(zone).hour]++ } }
        val firstOpens = covered.flatMap { it.firstAppsAfterPickup }
            .groupingBy { it }
            .eachCount()
            .mapNotNull { (pkg, count) -> resolveApp(pkg)?.let { AppOpenCount(it, count) } }
            .sortedWith(compareByDescending<AppOpenCount> { it.count }.thenBy { it.app.label })
        return PickupStats(
            period = period,
            pickups = covered.sumOf { it.pickups },
            days = covered.size,
            firstPickupMinute = covered.mapNotNull { day -> day.pickupTimes.minOrNull()?.let { minuteOfDay(it, zone) } }.median(),
            hourlyPickups = hourly.toList(),
            firstOpens = firstOpens,
        )
    }

    /** Sessions of one app joined when the gap between them is under [MERGE_GAP_MILLIS]. */
    private fun joinSessions(sessions: List<AppSession>): List<AppSession> {
        val joined = ArrayList<AppSession>()
        for (session in sessions.sortedBy { it.start }) {
            val last = joined.lastOrNull()
            if (last != null && session.start - last.end < MERGE_GAP_MILLIS) {
                joined[joined.lastIndex] = last.copy(end = maxOf(last.end, session.end))
            } else {
                joined += session
            }
        }
        return joined
    }

    private fun minuteOfDay(millis: Long, zone: ZoneId): Int {
        val time = Instant.ofEpochMilli(millis).atZone(zone)
        return time.hour * 60 + time.minute
    }

    private fun List<Int>.median(): Int? = if (isEmpty()) null else sorted()[size / 2]

    /** Two sessions of the same app closer than this are one open (app switcher, rotation…). */
    private const val MERGE_GAP_MILLIS = 15_000L

    /** Millis of [spans] falling in each local hour (0–23) of the day. */
    fun hourlyMillis(spans: List<TimeSpan>, zone: ZoneId): LongArray {
        val hours = LongArray(24)
        for (span in spans) {
            var cursor = span.start
            while (cursor < span.end) {
                val local = Instant.ofEpochMilli(cursor).atZone(zone)
                val nextHour = local.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
                val until = minOf(nextHour, span.end)
                hours[local.hour] += until - cursor
                cursor = until
            }
        }
        return hours
    }

    /** Minutes of screen time in each hour of [day] (the Today card bars). */
    fun hourlyMinutes(day: DayUsage, zone: ZoneId): List<Int> =
        hourlyMillis(mergeSpans(day.sessions.map { TimeSpan(it.start, it.end) }), zone).map { it.roundedMinutes() }

    /** Hours of [day] mostly spent in focus apps (more than half of that hour's app time). */
    fun focusHours(day: DayUsage, zone: ZoneId, categoryOf: (String) -> AppCategory): Set<Int> {
        val total = LongArray(24)
        val focus = LongArray(24)
        day.sessions.groupBy { it.packageName }.forEach { (pkg, sessions) ->
            val hours = hourlyMillis(sessions.map { TimeSpan(it.start, it.end) }, zone)
            val isFocus = categoryOf(pkg).qualityGroup == QualityGroup.Focus
            for (h in 0 until 24) {
                total[h] += hours[h]
                if (isFocus) focus[h] += hours[h]
            }
        }
        return (0 until 24).filter { h -> total[h] >= 60_000 && focus[h] * 2 > total[h] }.toSet()
    }

    /**
     * Longest stretch without the screen on across consecutive [days] (a night spanning midnight
     * counts once in a week). A gap between two non-adjacent days resets the stretch.
     */
    fun longestOfflineMillis(days: List<DayUsage>): Long {
        var best = 0L
        var carry = 0L
        var previousEnd: Long? = null
        for (day in days) {
            if (previousEnd != null && day.start != previousEnd) {
                best = maxOf(best, carry)
                carry = 0
            }
            if (day.awake.isEmpty()) {
                carry += day.end - day.start
                best = maxOf(best, carry)
            } else {
                best = maxOf(best, carry + (day.awake.first().start - day.start))
                day.awake.zipWithNext { a, b -> best = maxOf(best, b.start - a.end) }
                carry = day.end - day.awake.last().end
            }
            previousEnd = day.end
        }
        return maxOf(best, carry)
    }

    /**
     * Consecutive days within [goalMinutes], counting today (when still within the goal) and then
     * [previousDays] (newest first, evaluated lazily) until one is over the goal or has no data.
     * With [saversPerMonth], days over the goal use the month's streak savers first ([Streaks]).
     */
    fun streakDays(
        today: DayUsage,
        previousDays: Sequence<DayUsage>,
        goalMinutes: Int,
        maxDays: Int = 31,
        saversPerMonth: Int = 0,
    ): Int = Streaks.current(
        newestFirst = (sequenceOf(today) + previousDays).map { StreakDay(it.date, it.hasData, it.screenMillis.roundedMinutes()) },
        goalMinutes = goalMinutes,
        saversPerMonth = saversPerMonth,
        maxDays = maxDays,
    ).days

    /** Integer percentages of [values] summing to 100 (largest remainder). */
    fun percentages(values: List<Long>): List<Int> {
        val total = values.sum()
        if (total <= 0) return values.map { 0 }
        val exact = values.map { it * 100.0 / total }
        val floors = exact.map { it.toInt() }.toMutableList()
        var left = 100 - floors.sum()
        exact.withIndex()
            .sortedByDescending { it.value - it.value.toInt() }
            .forEach { (index, _) ->
                if (left > 0) {
                    floors[index]++
                    left--
                }
            }
        return floors
    }

    /**
     * Builds the summary of [period] (or of [range], laid out as [period]) from its [days] (in
     * order; today's partial day last for the current periods). Days without data are left out of
     * the totals and averages.
     *
     * @param resolveApp label / category of a package; null hides it from the Most used list
     *   (its time still counts).
     * @param baselineDailyMinutes the user's previous habit, for "Decrease".
     */
    fun summarize(
        period: UsagePeriod,
        days: List<DayUsage>,
        zone: ZoneId,
        resolveApp: (String) -> UsageApp?,
        baselineDailyMinutes: Int,
        streakDays: Int,
        previousTotalMinutes: Int?,
        range: UsageRange? = null,
    ): ScreenTimeSummary {
        val covered = days.filter { it.hasData }
        val dayCount = covered.size.coerceAtLeast(1)
        val totalMinutes = covered.sumOf { it.screenMillis }.roundedMinutes()
        val singleDay = period == UsagePeriod.Today || period == UsagePeriod.Yesterday
        val day = days.lastOrNull()

        val appMillis = HashMap<String, Long>()
        covered.forEach { d -> d.sessions.forEach { appMillis.merge(it.packageName, it.millis, Long::plus) } }
        val apps = appMillis.keys.associateWith(resolveApp)
        val categoryOf = { pkg: String -> apps[pkg]?.category ?: AppCategory.Other }

        // A minute or more on screen, so no app reads "0m" or a rounded-up "1m".
        val mostUsed = appMillis.entries
            .filter { (_, millis) -> millis >= 60_000 }
            .mapNotNull { (pkg, millis) -> apps[pkg]?.let { AppUsage(it, millis.roundedMinutes()) } }
            .sortedWith(compareByDescending<AppUsage> { it.minutes }.thenBy { it.app.label })

        val groupMillis = QualityGroup.entries.map { group ->
            appMillis.entries.filter { categoryOf(it.key).qualityGroup == group }.sumOf { it.value }
        }
        val (focusShare, socialShare, funShare) = if (groupMillis.sum() > 0) percentages(groupMillis) else listOf(100, 0, 0)

        val weekdayMinutes = covered.groupBy { it.date.dayOfWeek.value - 1 }
            .mapValues { (_, list) -> list.map { it.screenMillis.roundedMinutes() } }
        val dailyMinutes = List(7) { index ->
            val values = weekdayMinutes[index].orEmpty()
            when {
                values.isEmpty() -> 0
                period == UsagePeriod.ThisMonth -> values.average().roundToInt()
                else -> values.sum()
            }
        }

        return ScreenTimeSummary(
            period = period,
            totalMinutes = totalMinutes,
            decreaseMinutes = (baselineDailyMinutes * dayCount - totalMinutes).coerceAtLeast(0),
            pickups = covered.sumOf { it.pickups },
            notifications = covered.sumOf { it.notifications },
            hourlyMinutes = if (singleDay && day != null) hourlyMinutes(day, zone) else List(24) { 0 },
            dailyMinutes = dailyMinutes,
            mostUsed = mostUsed,
            longestOfflineMinutes = longestOfflineMillis(covered).roundedMinutes(),
            focusPercent = focusShare,
            socialPercent = socialShare,
            funPercent = funShare,
            streakDays = streakDays,
            focusHours = if (singleDay && day != null) focusHours(day, zone, categoryOf) else emptySet(),
            days = dayCount,
            previousTotalMinutes = previousTotalMinutes,
            measuredDays = weekdayMinutes.keys.toSet(),
            range = range,
        )
    }

    /** true = screen on, false = screen off, null = not a screen event. */
    private val UsageEventType.isScreenOn: Boolean?
        get() = when (this) {
            UsageEventType.ScreenInteractive -> true
            UsageEventType.ScreenNonInteractive, UsageEventType.DeviceShutdown -> false
            else -> null
        }

    private val UsageEventType.isActivity: Boolean
        get() = this == UsageEventType.ActivityResumed ||
            this == UsageEventType.ActivityPaused ||
            this == UsageEventType.ActivityStopped
}
