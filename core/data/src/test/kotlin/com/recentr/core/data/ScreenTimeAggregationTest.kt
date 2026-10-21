package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.AppId
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import com.recentr.core.data.UsageEventType as T

class ScreenTimeAggregationTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val monday = LocalDate.of(2026, 10, 12)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atStartOfDay(zone).plusHours(hour.toLong()).plusMinutes(minute.toLong()).toInstant().toEpochMilli()

    private fun dayStart(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun dayEnd(date: LocalDate) = dayStart(date.plusDays(1))

    private fun ev(time: Long, type: T, pkg: String = "", cls: String = "$pkg.Main") =
        UsageEvent(time, type, pkg, if (pkg.isEmpty()) "" else cls)

    /** Screen on, [pkg] resumed then paused, screen off. */
    private fun use(date: LocalDate, pkg: String, fromHour: Int, fromMinute: Int, toHour: Int, toMinute: Int) = listOf(
        ev(at(date, fromHour, fromMinute), T.ScreenInteractive),
        ev(at(date, fromHour, fromMinute), T.ActivityResumed, pkg),
        ev(at(date, toHour, toMinute), T.ActivityPaused, pkg),
        ev(at(date, toHour, toMinute), T.ScreenNonInteractive),
    )

    private fun day(date: LocalDate, events: List<UsageEvent>, end: Long = dayEnd(date), ignored: Set<String> = emptySet()) =
        ScreenTimeAggregation.day(date, dayStart(date), end, events, ignored)

    private val Long.minutes get() = roundedMinutes()

    // region Day reduction

    @Test
    fun `a session counts its foreground time in its hours`() {
        val usage = day(monday, use(monday, INSTAGRAM, 10, 50, 11, 20))

        assertEquals(listOf(AppSession(INSTAGRAM, at(monday, 10, 50), at(monday, 11, 20))), usage.sessions)
        assertEquals(30, usage.screenMillis.minutes)
        val hourly = ScreenTimeAggregation.hourlyMinutes(usage, zone)
        assertEquals(10, hourly[10])
        assertEquals(20, hourly[11])
        assertEquals(30, hourly.sum())
    }

    @Test
    fun `an app first seen pausing was on screen since midnight`() {
        val usage = day(
            monday,
            listOf(
                ev(at(monday, 0, 15), T.ActivityPaused, CHROME),
                ev(at(monday, 0, 15), T.ScreenNonInteractive),
            ),
        )

        assertEquals(15, usage.screenMillis.minutes)
        assertEquals(listOf(TimeSpan(dayStart(monday), at(monday, 0, 15))), usage.awake)
    }

    @Test
    fun `no session is inferred when the screen was off at midnight`() {
        val usage = day(
            monday,
            listOf(
                // Late pause of an activity left from the evening, then a regular use.
                ev(at(monday, 0, 5), T.ActivityPaused, CHROME),
                ev(at(monday, 7, 0), T.ScreenInteractive),
                ev(at(monday, 7, 0), T.ActivityResumed, CHROME),
                ev(at(monday, 7, 10), T.ActivityPaused, CHROME),
                ev(at(monday, 7, 10), T.ScreenNonInteractive),
            ),
        )

        assertEquals(10, usage.screenMillis.minutes)
    }

    @Test
    fun `the app still on screen counts until now`() {
        val now = at(monday, 9, 45)
        val usage = day(
            monday,
            listOf(ev(at(monday, 9, 0), T.ScreenInteractive), ev(at(monday, 9, 0), T.ActivityResumed, RECENTR)),
            end = now,
        )

        assertEquals(45, usage.screenMillis.minutes)
        assertEquals(listOf(TimeSpan(at(monday, 9, 0), now)), usage.awake)
    }

    @Test
    fun `screen off ends the session even if the pause comes later`() {
        val usage = day(
            monday,
            listOf(
                ev(at(monday, 8, 0), T.ScreenInteractive),
                ev(at(monday, 8, 0), T.ActivityResumed, YOUTUBE),
                ev(at(monday, 8, 20), T.ScreenNonInteractive),
                ev(at(monday, 8, 25), T.ActivityPaused, YOUTUBE),
            ),
        )

        assertEquals(20, usage.screenMillis.minutes)
    }

    @Test
    fun `the stop of the previous activity does not end the app's time`() {
        val usage = day(
            monday,
            listOf(
                ev(at(monday, 12, 0), T.ScreenInteractive),
                ev(at(monday, 12, 0), T.ActivityResumed, GMAIL, "Inbox"),
                ev(at(monday, 12, 5), T.ActivityPaused, GMAIL, "Inbox"),
                ev(at(monday, 12, 5), T.ActivityResumed, GMAIL, "Compose"),
                ev(at(monday, 12, 6), T.ActivityStopped, GMAIL, "Inbox"),
                ev(at(monday, 12, 30), T.ActivityPaused, GMAIL, "Compose"),
                ev(at(monday, 12, 30), T.ScreenNonInteractive),
            ),
        )

        assertEquals(30, usage.screenMillis.minutes)
        assertEquals(30, usage.sessions.sumOf { it.millis }.minutes)
    }

    @Test
    fun `launcher time is not screen time`() {
        val usage = day(
            monday,
            listOf(
                ev(at(monday, 18, 0), T.ScreenInteractive),
                ev(at(monday, 18, 0), T.ActivityResumed, LAUNCHER),
                ev(at(monday, 18, 10), T.ActivityPaused, LAUNCHER),
                ev(at(monday, 18, 10), T.ActivityResumed, INSTAGRAM),
                ev(at(monday, 18, 25), T.ActivityPaused, INSTAGRAM),
                ev(at(monday, 18, 25), T.ScreenNonInteractive),
            ),
            ignored = setOf(LAUNCHER),
        )

        assertEquals(15, usage.screenMillis.minutes)
        assertEquals(setOf(INSTAGRAM), usage.sessions.map { it.packageName }.toSet())
        // The phone was in hand on the home screen too.
        assertEquals(listOf(TimeSpan(at(monday, 18, 0), at(monday, 18, 25))), usage.awake)
    }

    @Test
    fun `overlapping apps count once in the total and fully per app`() {
        val usage = day(
            monday,
            listOf(
                ev(at(monday, 20, 0), T.ScreenInteractive),
                ev(at(monday, 20, 0), T.ActivityResumed, YOUTUBE),
                ev(at(monday, 20, 10), T.ActivityResumed, X),
                ev(at(monday, 20, 20), T.ActivityPaused, YOUTUBE),
                ev(at(monday, 20, 30), T.ActivityPaused, X),
                ev(at(monday, 20, 30), T.ScreenNonInteractive),
            ),
        )

        assertEquals(30, usage.screenMillis.minutes)
        assertEquals(40, usage.sessions.sumOf { it.millis }.minutes)
    }

    @Test
    fun `pickups are unlocks with a lock screen, screen-ons without`() {
        val locked = day(
            monday,
            listOf(
                ev(at(monday, 7, 0), T.ScreenInteractive),
                ev(at(monday, 7, 0), T.KeyguardShown),
                ev(at(monday, 7, 1), T.KeyguardHidden),
                ev(at(monday, 7, 5), T.ScreenNonInteractive),
                // A glance at the lock screen is no pickup.
                ev(at(monday, 9, 0), T.ScreenInteractive),
                ev(at(monday, 9, 0), T.KeyguardShown),
                ev(at(monday, 9, 1), T.ScreenNonInteractive),
                ev(at(monday, 10, 0), T.ScreenInteractive),
                ev(at(monday, 10, 0), T.KeyguardHidden),
                ev(at(monday, 10, 3), T.ScreenNonInteractive),
            ),
        )
        val unlocked = day(monday, use(monday, X, 7, 0, 7, 5) + use(monday, X, 9, 0, 9, 1))

        assertEquals(2, locked.pickups)
        assertEquals(2, unlocked.pickups)
    }

    @Test
    fun `notification interruptions are counted`() {
        val usage = day(
            monday,
            listOf(
                ev(at(monday, 8, 0), T.NotificationInterruption, GMAIL),
                ev(at(monday, 9, 0), T.NotificationInterruption, X),
                ev(at(monday, 23, 59), T.NotificationInterruption, X),
                // Outside the day.
                ev(dayEnd(monday) + 1, T.NotificationInterruption, X),
            ),
        )

        assertEquals(3, usage.notifications)
        assertTrue(usage.hasData)
    }

    @Test
    fun `a OnePlus day with its app lock, calls and launcher counts its screen time`() {
        // As ColorOS logs it (OnePlus 15, 21 October 2026): the app lock shows over WhatsApp before
        // each unlock, activities of one app hand over to each other, the launcher in between.
        fun t(hour: Int, minute: Int, second: Int) = at(monday, hour, minute) + second * 1_000L
        val events = listOf(
            ev(t(0, 9, 16), T.ScreenInteractive),
            ev(t(0, 9, 19), T.ActivityResumed, WHATSAPP, CONVERSATION),
            ev(t(0, 9, 19), T.ActivityPaused, WHATSAPP, CONVERSATION),
            ev(t(0, 9, 19), T.ActivityResumed, APP_LOCK),
            ev(t(0, 9, 20), T.KeyguardHidden),
            ev(t(0, 9, 20), T.ActivityStopped, WHATSAPP, CONVERSATION),
            ev(t(0, 9, 20), T.ActivityPaused, APP_LOCK),
            ev(t(0, 9, 20), T.ActivityResumed, WHATSAPP, CONVERSATION),
            ev(t(0, 9, 20), T.ActivityStopped, APP_LOCK),
            ev(t(0, 9, 21), T.ActivityPaused, WHATSAPP, CONVERSATION),
            ev(t(0, 9, 22), T.ActivityResumed, WHATSAPP, CALL),
            ev(t(0, 9, 22), T.ActivityStopped, WHATSAPP, CONVERSATION),
            ev(t(0, 10, 2), T.NotificationInterruption, WHATSAPP),
            ev(t(0, 28, 21), T.ActivityPaused, WHATSAPP, CALL),
            ev(t(0, 28, 21), T.ActivityResumed, WHATSAPP, CONVERSATION),
            ev(t(0, 28, 21), T.ActivityStopped, WHATSAPP, CALL),
            ev(t(0, 28, 22), T.ActivityPaused, WHATSAPP, CONVERSATION),
            ev(t(0, 28, 22), T.ActivityResumed, WHATSAPP, CHATS),
            ev(t(0, 28, 23), T.ActivityStopped, WHATSAPP, CONVERSATION),
            ev(t(0, 28, 23), T.ScreenNonInteractive),
            ev(t(0, 28, 23), T.KeyguardShown),
            ev(t(0, 28, 23), T.ActivityPaused, WHATSAPP, CHATS),
            ev(t(0, 28, 24), T.ActivityStopped, WHATSAPP, CHATS),
            ev(t(0, 30, 0), T.NotificationInterruption, WHATSAPP),
            ev(t(0, 31, 49), T.ScreenInteractive),
            ev(t(0, 31, 50), T.ActivityResumed, APP_LOCK),
            ev(t(0, 31, 50), T.KeyguardHidden),
            ev(t(0, 31, 50), T.ActivityPaused, APP_LOCK),
            ev(t(0, 31, 50), T.ActivityResumed, WHATSAPP, CHATS),
            ev(t(0, 31, 51), T.ActivityStopped, APP_LOCK),
            ev(t(0, 31, 51), T.ActivityPaused, WHATSAPP, CHATS),
            ev(t(0, 31, 51), T.ActivityResumed, WHATSAPP, CONVERSATION),
            ev(t(0, 31, 52), T.ActivityStopped, WHATSAPP, CHATS),
            ev(t(0, 31, 55), T.ActivityResumed, ONEPLUS_LAUNCHER),
            ev(t(0, 31, 56), T.ActivityPaused, WHATSAPP, CONVERSATION),
            ev(t(0, 31, 56), T.ActivityStopped, WHATSAPP, CONVERSATION),
            ev(t(0, 31, 56), T.ActivityPaused, ONEPLUS_LAUNCHER),
            ev(t(0, 31, 56), T.ActivityResumed, RECENTR),
            ev(t(0, 31, 56), T.ActivityStopped, ONEPLUS_LAUNCHER),
            ev(t(0, 33, 0), T.ScreenNonInteractive),
            ev(t(0, 33, 0), T.ActivityPaused, RECENTR),
            ev(t(0, 33, 0), T.KeyguardShown),
        )

        val usage = day(monday, events, ignored = setOf(ONEPLUS_LAUNCHER, UsageStatsScreenTimeRepository.SYSTEM_UI))
        val perApp = usage.sessions.groupBy { it.packageName }.mapValues { (_, s) -> s.sumOf { it.millis } }

        // The call: 19 minutes of WhatsApp, then a minute of Recentr.
        assertEquals(19, perApp.getValue(WHATSAPP).minutes)
        assertEquals(1, perApp.getValue(RECENTR).minutes)
        assertEquals(20, usage.screenMillis.minutes)
        assertEquals(20, ScreenTimeAggregation.dayStats(usage, zone).minutes)
        assertEquals(2, usage.pickups)
        assertEquals(2, usage.notifications)
        assertTrue(usage.hasData)
    }

    @Test
    fun `a day without events has no data`() {
        val usage = day(monday, emptyList())

        assertFalse(usage.hasData)
        assertEquals(0L, usage.screenMillis)
    }

    @Test
    fun `hours follow the local clock on daylight saving days`() {
        // 25 October 2026: clocks go back from 03:00 to 02:00 in Paris (25-hour day).
        val date = LocalDate.of(2026, 10, 25)
        val start = dayStart(date)
        val span = TimeSpan(start + 2 * HOUR, start + 4 * HOUR) // 02:00 CEST → 03:00 CET.
        val hours = ScreenTimeAggregation.hourlyMillis(listOf(span), zone)

        assertEquals(2 * HOUR, hours.sum())
        assertEquals(2 * HOUR, hours[2]) // Both 02:00 hours.
    }

    // endregion

    // region Offline stretch & streak

    @Test
    fun `longest offline is the biggest gap of the day, edges included`() {
        val usage = day(monday, use(monday, X, 7, 0, 7, 30) + use(monday, X, 9, 0, 9, 10) + use(monday, X, 22, 0, 22, 30))

        // 09:10 → 22:00.
        assertEquals(12 * 60 + 50, ScreenTimeAggregation.longestOfflineMillis(listOf(usage)).minutes)
    }

    @Test
    fun `a night spanning midnight counts once over a week`() {
        val tuesday = monday.plusDays(1)
        val days = listOf(
            day(monday, use(monday, X, 10, 0, 21, 0)),
            day(tuesday, use(tuesday, X, 9, 0, 23, 0)),
        )

        // Monday 21:00 → Tuesday 09:00, longer than Monday 00:00 → 10:00.
        assertEquals(12 * 60, ScreenTimeAggregation.longestOfflineMillis(days).minutes)
    }

    @Test
    fun `days that are not adjacent do not join their gaps`() {
        val wednesday = monday.plusDays(2)
        val days = listOf(
            day(monday, use(monday, X, 0, 0, 14, 0)),
            day(wednesday, use(wednesday, X, 6, 0, 23, 59)),
        )

        // Monday 14:00 → 24:00; Tuesday is missing, so not 14:00 → Wednesday 06:00.
        assertEquals(10 * 60, ScreenTimeAggregation.longestOfflineMillis(days).minutes)
    }

    @Test
    fun `streak counts today and the previous days within the goal`() {
        val today = day(monday, use(monday, X, 9, 0, 10, 0), end = at(monday, 12, 0))
        val previous = listOf(30, 50, 59, 200, 10).mapIndexed { index, minutes ->
            val date = monday.minusDays(index + 1L)
            day(date, use(date, X, 8, 0, 8 + minutes / 60, minutes % 60))
        }

        assertEquals(4, ScreenTimeAggregation.streakDays(today, previous.asSequence(), goalMinutes = 60))
        assertEquals(0, ScreenTimeAggregation.streakDays(today, previous.asSequence(), goalMinutes = 30))
        // The history stops at the first day without data.
        val noData = listOf(previous[0], day(monday.minusDays(2), emptyList()), previous[2])
        assertEquals(2, ScreenTimeAggregation.streakDays(today, noData.asSequence(), goalMinutes = 60))
    }

    // endregion

    // region Summary

    private val catalogue = mapOf(
        INSTAGRAM to UsageApp.of(AppId.Instagram),
        X to UsageApp.of(AppId.X),
        YOUTUBE to UsageApp.of(AppId.YouTube),
        GMAIL to UsageApp.of(AppId.Gmail),
        CHROME to UsageApp(CHROME, "Chrome", AppCategory.Other),
    )

    private fun summarize(period: UsagePeriod, days: List<DayUsage>, previous: Int? = null) = ScreenTimeAggregation.summarize(
        period = period,
        days = days,
        zone = zone,
        resolveApp = catalogue::get,
        baselineDailyMinutes = 6 * 60,
        streakDays = 2,
        previousTotalMinutes = previous,
    )

    @Test
    fun `today summary lists apps by usage and keeps hidden apps in the total`() {
        val now = at(monday, 15, 0)
        val events = use(monday, X, 8, 0, 8, 38) +
            use(monday, INSTAGRAM, 9, 0, 9, 17) +
            use(monday, GMAIL, 10, 0, 10, 9) +
            use(monday, HIDDEN, 11, 0, 11, 8) +
            listOf(ev(at(monday, 12, 0), T.NotificationInterruption, X))
        val summary = summarize(UsagePeriod.Today, listOf(day(monday, events, end = now)), previous = 125)

        assertEquals(38 + 17 + 9 + 8, summary.totalMinutes)
        assertEquals(listOf(X, INSTAGRAM, GMAIL), summary.mostUsed.map { it.app.packageName })
        assertEquals(listOf(38, 17, 9), summary.mostUsed.map { it.minutes })
        assertEquals(AppId.X, summary.mostUsed.first().app.catalogue)
        assertEquals(4, summary.pickups)
        assertEquals(1, summary.notifications)
        assertEquals(6 * 60 - 72, summary.decreaseMinutes)
        assertEquals(38, summary.hourlyMinutes[8])
        assertEquals(1, summary.days)
        assertEquals(125, summary.previousTotalMinutes)
        assertEquals(2, summary.streakDays)
        // 00:00 → 08:00, longer than 11:08 → 15:00 (now).
        assertEquals(8 * 60, summary.longestOfflineMinutes)
    }

    @Test
    fun `quality split and focus hours follow the app categories`() {
        val events = use(monday, X, 8, 0, 8, 30) + // Social.
            use(monday, YOUTUBE, 9, 0, 9, 20) + // Fun.
            use(monday, GMAIL, 10, 0, 10, 40) + // Focus.
            use(monday, HIDDEN, 11, 0, 11, 10) // Unknown → focus.
        val summary = summarize(UsagePeriod.Yesterday, listOf(day(monday, events)))

        assertEquals(50, summary.focusPercent)
        assertEquals(30, summary.socialPercent)
        assertEquals(20, summary.funPercent)
        assertEquals(setOf(10, 11), summary.focusHours)
        assertNull(summary.previousTotalMinutes)
    }

    @Test
    fun `a day without any usage is fully focused`() {
        val morning = ScreenTimeAggregation.day(monday, dayStart(monday), at(monday, 6, 0), emptyList(), hasData = true)
        val summary = summarize(UsagePeriod.Today, listOf(morning))

        assertEquals(0, summary.totalMinutes)
        assertEquals(100, summary.focusPercent)
        assertEquals(6 * 60, summary.longestOfflineMinutes)
        assertTrue(summary.mostUsed.isEmpty())
    }

    @Test
    fun `week summary has one bar per day so far and averages over them`() {
        val wednesday = monday.plusDays(2)
        val days = listOf(
            day(monday, use(monday, X, 9, 0, 10, 0)),
            day(monday.plusDays(1), use(monday.plusDays(1), X, 9, 0, 9, 30)),
            day(wednesday, use(wednesday, INSTAGRAM, 9, 0, 9, 45), end = at(wednesday, 12, 0)),
        )
        val summary = summarize(UsagePeriod.ThisWeek, days, previous = 200)

        assertEquals(listOf(60, 30, 45, 0, 0, 0, 0), summary.dailyMinutes)
        assertEquals(setOf(0, 1, 2), summary.measuredDays)
        assertEquals(135, summary.totalMinutes)
        assertEquals(3, summary.days)
        assertEquals(45, summary.dailyAverageMinutes)
        assertEquals(3 * 6 * 60 - 135, summary.decreaseMinutes)
        assertTrue(summary.hourlyMinutes.all { it == 0 })
        assertEquals(200, summary.previousTotalMinutes)
    }

    @Test
    fun `month summary averages each weekday and skips days without data`() {
        val first = LocalDate.of(2026, 10, 1) // Thursday.
        val days = (0L until 9L).map { offset ->
            val date = first.plusDays(offset)
            when (offset) {
                2L -> day(date, emptyList()) // Saturday 3rd: no data retained.
                0L -> day(date, use(date, X, 9, 0, 10, 0)) // Thursday 1st: 60.
                7L -> day(date, use(date, X, 9, 0, 9, 30)) // Thursday 8th: 30.
                else -> day(date, use(date, GMAIL, 9, 0, 9, 10))
            }
        }
        val summary = summarize(UsagePeriod.ThisMonth, days)

        assertEquals(8, summary.days)
        // Monday → Sunday; Thursday = (60 + 30) / 2, Saturday has no data.
        assertEquals(listOf(10, 10, 10, 45, 10, 0, 10), summary.dailyMinutes)
        assertEquals(setOf(0, 1, 2, 3, 4, 6), summary.measuredDays)
        assertEquals(60 + 30 + 6 * 10, summary.totalMinutes)
    }

    @Test
    fun `percentages always sum to 100`() {
        assertEquals(listOf(34, 33, 33), ScreenTimeAggregation.percentages(listOf(1, 1, 1)))
        assertEquals(listOf(67, 33, 0), ScreenTimeAggregation.percentages(listOf(2, 1, 0)))
        assertEquals(listOf(0, 0, 0), ScreenTimeAggregation.percentages(listOf(0, 0, 0)))
    }

    @Test
    fun `merging joins overlapping and touching spans`() {
        val merged = mergeSpans(listOf(TimeSpan(10, 20), TimeSpan(0, 5), TimeSpan(15, 30), TimeSpan(30, 40), TimeSpan(50, 50)))

        assertEquals(listOf(TimeSpan(0, 5), TimeSpan(10, 40)), merged)
    }

    // endregion

    // region Limit periods

    @Test
    fun `a limit's use counts its apps from the start of its period`() {
        val sunday = monday.minusDays(1)
        val days = listOf(
            day(sunday, use(sunday, YOUTUBE, 23, 0, 23, 40)),
            day(monday, use(monday, YOUTUBE, 3, 30, 4, 20) + use(monday, X, 4, 30, 4, 45), end = at(monday, 5)),
        )

        // Since 4:00 (a 4 AM reset): the session that started at 3:30 counts from 4:00 on.
        assertEquals(mapOf(YOUTUBE to 20, X to 15), ScreenTimeAggregation.appMinutes(days, since = at(monday, 4)))
        // Since Sunday 4:00, the evening and the night count together.
        assertEquals(mapOf(YOUTUBE to 90, X to 15), ScreenTimeAggregation.appMinutes(days, since = at(sunday, 4)))
        // Since midnight: Monday only.
        assertEquals(mapOf(YOUTUBE to 50, X to 15), ScreenTimeAggregation.appMinutes(days, since = at(monday, 0)))
        assertEquals(emptyMap<String, Int>(), ScreenTimeAggregation.appMinutes(days, since = at(monday, 5)))
    }

    // endregion

    private companion object {
        const val HOUR = 3_600_000L
        const val INSTAGRAM = "com.instagram.android"
        const val X = "com.twitter.android"
        const val YOUTUBE = "com.google.android.youtube"
        const val GMAIL = "com.google.android.gm"
        const val CHROME = "com.android.chrome"
        const val RECENTR = "com.recentr.app"
        const val LAUNCHER = "com.google.android.apps.nexuslauncher"
        const val HIDDEN = "com.example.hidden"
        const val WHATSAPP = "com.whatsapp"
        const val CONVERSATION = "com.whatsapp.Conversation"
        const val CALL = "com.whatsapp.calling.ui.VoipActivityV2"
        const val CHATS = "com.whatsapp.home.ui.HomeActivity"
        const val APP_LOCK = "com.oplus.safecenter"
        const val ONEPLUS_LAUNCHER = "com.android.launcher"
    }
}
