package com.recentr.core.data

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.recentr.core.model.AppId
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runInterruptible
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.TimeSource

/**
 * Screen time of this device, from `UsageStatsManager.queryEvents` (needs usage access, see
 * [UsageAccess]). Events are read one local day at a time and reduced by [ScreenTimeAggregation];
 * completed days are cached, so only today is re-read on a refresh.
 *
 * Apps: catalogue packages map to [AppId] (Figma icon); other apps keep their launcher label
 * ([describeApp]).
 * Package visibility comes from the `<queries>` of this module's manifest (launcher and home
 * intents), so only apps with a launcher entry get a label — the others still count in the total.
 *
 * @param profile the daily goal (streak) and the reported habit ("Decrease").
 */
class UsageStatsScreenTimeRepository(
    context: Context,
    private val profile: Flow<UserProfile>,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Keeps the Syncing skeleton up long enough not to flash when the days are cached. */
    private val minSyncMillis: Long = 600,
) : ScreenTimeRepository {

    private val appContext = context.applicationContext
    private val usageStats = appContext.getSystemService(UsageStatsManager::class.java)
    private val packageManager = appContext.packageManager

    override val source: StateFlow<ScreenTimeSource> = MutableStateFlow(ScreenTimeSource.Device)

    private val completedDays = ConcurrentHashMap<Pair<LocalDate, ZoneId>, DayUsage>()
    private val apps = ConcurrentHashMap<String, ResolvedApp>()
    private val ignoredPackages: Set<String> by lazy { homePackages() + SYSTEM_UI }

    override fun summary(period: UsagePeriod): Flow<ScreenTimeState> = flow {
        emit(ScreenTimeState.Syncing)
        val started = TimeSource.Monotonic.markNow()
        val user = profile.first()
        val summary = runInterruptible(Dispatchers.IO) { load(period, user) }
        val remaining = minSyncMillis - started.elapsedNow().inWholeMilliseconds
        if (remaining > 0) delay(remaining)
        emit(ScreenTimeState.Ready(summary))
    }

    private fun load(period: UsagePeriod, user: UserProfile): ScreenTimeSummary {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(clock()).atZone(zone)
        val today = now.toLocalDate()
        val todayUsage = day(today, zone, until = now)
        val days = periodDays(period, zone, now, todayUsage)

        val previousTotal: Int? = when (period) {
            UsagePeriod.Today ->
                completedDay(today.minusDays(1), zone).takeIf { it.hasData }?.screenMillis?.roundedMinutes()
            UsagePeriod.Yesterday -> null
            UsagePeriod.ThisWeek ->
                spanMinutes(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1), now.minusWeeks(1), zone)
            UsagePeriod.ThisMonth -> spanMinutes(today.withDayOfMonth(1).minusMonths(1), now.minusMonths(1), zone)
        }

        val streak = ScreenTimeAggregation.streakDays(
            today = todayUsage,
            previousDays = generateSequence(today.minusDays(1)) { it.minusDays(1) }.map { completedDay(it, zone) },
            goalMinutes = user.dailyGoalMinutes,
            maxDays = STREAK_MAX_DAYS,
            saversPerMonth = Streaks.SAVERS_PER_MONTH,
        )
        return ScreenTimeAggregation.summarize(
            period = period,
            days = days,
            zone = zone,
            resolveApp = ::resolveApp,
            baselineDailyMinutes = ReportedUsage.dailyMinutes(user.reportedUsageBucket),
            streakDays = streak,
            previousTotalMinutes = previousTotal,
        )
    }

    override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> = runInterruptible(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(clock()).atZone(zone)
        val today = now.toLocalDate()
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { date ->
                when {
                    date.isAfter(today) -> DayStats(date, hasData = false, minutes = 0, pickups = 0)
                    date == today -> ScreenTimeAggregation.dayStats(day(today, zone, until = now), zone)
                    else -> ScreenTimeAggregation.dayStats(completedDay(date, zone), zone)
                }
            }
            .toList()
    }

    override suspend fun appDetail(packageName: String, period: UsagePeriod): AppUsageDetail? = runInterruptible(Dispatchers.IO) {
        val app = resolveApp(packageName) ?: return@runInterruptible null
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(clock()).atZone(zone)
        ScreenTimeAggregation.appDetail(app, period, periodDays(period, zone, now), zone)
    }

    override suspend fun pickups(period: UsagePeriod): PickupStats = runInterruptible(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(clock()).atZone(zone)
        ScreenTimeAggregation.pickupStats(period, periodDays(period, zone, now), zone, ::resolveApp)
    }

    /** The days of [period] in order (today's partial day last for the current periods). */
    private fun periodDays(
        period: UsagePeriod,
        zone: ZoneId,
        now: ZonedDateTime,
        todayUsage: DayUsage = day(now.toLocalDate(), zone, until = now),
    ): List<DayUsage> {
        val today = now.toLocalDate()
        return when (period) {
            UsagePeriod.Today -> listOf(todayUsage)
            UsagePeriod.Yesterday -> listOf(completedDay(today.minusDays(1), zone))
            UsagePeriod.ThisWeek -> currentDays(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), todayUsage, zone)
            UsagePeriod.ThisMonth -> currentDays(today.withDayOfMonth(1), todayUsage, zone)
        }
    }

    /** Days from [first] to today (today's partial day last). */
    private fun currentDays(first: LocalDate, today: DayUsage, zone: ZoneId): List<DayUsage> =
        generateSequence(first) { it.plusDays(1) }
            .takeWhile { it.isBefore(today.date) }
            .map { completedDay(it, zone) }
            .toList() + today

    /** Screen time from [first] (start of day) to [end] — the previous comparable period. */
    private fun spanMinutes(first: LocalDate, end: ZonedDateTime, zone: ZoneId): Int? {
        val lastDate = end.toLocalDate()
        val days = generateSequence(first) { it.plusDays(1) }
            .takeWhile { it.isBefore(lastDate) }
            .map { completedDay(it, zone) }
            .toList() + day(lastDate, zone, until = end)
        return days.filter { it.hasData }.takeIf { it.isNotEmpty() }?.sumOf { it.screenMillis }?.roundedMinutes()
    }

    private fun completedDay(date: LocalDate, zone: ZoneId): DayUsage =
        completedDays.getOrPut(date to zone) { day(date, zone, until = null) }

    /** Reads and reduces the events of [date], up to [until] (today) or to its end. */
    private fun day(date: LocalDate, zone: ZoneId, until: ZonedDateTime?): DayUsage {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = (until ?: date.plusDays(1).atStartOfDay(zone)).toInstant().toEpochMilli()
        val (events, hasData) = readEvents(start, end)
        return ScreenTimeAggregation.day(date, start, end, events, ignoredPackages, hasData)
    }

    /** The relevant events of `[start, end)`, and whether the system returned any event at all. */
    private fun readEvents(start: Long, end: Long): Pair<List<UsageEvent>, Boolean> {
        if (end <= start) return emptyList<UsageEvent>() to false
        val usageEvents = usageStats?.queryEvents(start, end) ?: return emptyList<UsageEvent>() to false
        val events = ArrayList<UsageEvent>()
        val names = HashMap<String, String>()
        val event = UsageEvents.Event()
        var any = false
        while (usageEvents.hasNextEvent()) {
            if (!usageEvents.getNextEvent(event)) break
            any = true
            val type = event.eventType.toUsageEventType() ?: continue
            val pkg = event.packageName.orEmpty()
            val cls = event.className.orEmpty()
            events += UsageEvent(
                timeMillis = event.timeStamp,
                type = type,
                packageName = names.getOrPut(pkg) { pkg },
                className = names.getOrPut(cls) { cls },
            )
        }
        return events to any
    }

    private fun resolveApp(packageName: String): UsageApp? =
        apps.getOrPut(packageName) { ResolvedApp(lookUpApp(packageName)) }.app

    /** Not installed any more, or not visible to this app (no launcher entry): null. */
    private fun lookUpApp(packageName: String): UsageApp? = packageManager.describeApp(packageName)

    /** Launchers (time on the home screen is not screen time), without the Settings fallback home. */
    private fun homePackages(): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val candidates: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(home, PackageManager.ResolveInfoFlags.of(0))
        } else {
            packageManager.queryIntentActivities(home, 0)
        }
        // Settings' FallbackHome (boot-time placeholder) is registered with priority -1000.
        return candidates.filter { it.priority >= 0 }.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    private class ResolvedApp(val app: UsageApp?)

    internal companion object {
        const val SYSTEM_UI = "com.android.systemui"

        /** Streak look-back (each extra day is one `queryEvents` the first time). */
        const val STREAK_MAX_DAYS = 31

        /** `UsageEvents.Event.NOTIFICATION_INTERRUPTION` — hidden in the SDK, delivered since API 28. */
        const val NOTIFICATION_INTERRUPTION = 12

        @SuppressLint("InlinedApi") // Same values as MOVE_TO_FOREGROUND / BACKGROUND on API 28.
        fun Int.toUsageEventType(): UsageEventType? = when (this) {
            UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ActivityResumed
            UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.ActivityPaused
            UsageEvents.Event.ACTIVITY_STOPPED -> UsageEventType.ActivityStopped
            UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventType.ScreenInteractive
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.ScreenNonInteractive
            UsageEvents.Event.KEYGUARD_SHOWN -> UsageEventType.KeyguardShown
            UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEventType.KeyguardHidden
            UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEventType.DeviceShutdown
            NOTIFICATION_INTERRUPTION -> UsageEventType.NotificationInterruption
            else -> null
        }
    }
}

/**
 * Daily average of the onboarding answer "How much time do you spend on your phone?" — mirrors
 * `UsageBucket.averageMinutes` of the onboarding (5 – 7 hours when unanswered).
 */
internal object ReportedUsage {
    private val minutesByBucket = mapOf(
        "over_7h" to 8 * 60,
        "5_7h" to 6 * 60,
        "3_5h" to 4 * 60,
        "1_3h" to 2 * 60,
        "under_1h" to 45,
    )

    fun dailyMinutes(bucketId: String?): Int = minutesByBucket[bucketId] ?: 6 * 60
}
