package com.recentr.core.data

import com.recentr.core.data.UsageEventType as T
import com.recentr.core.model.AppCategory
import com.recentr.core.model.AppId
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Day stats, app detail and pickups of the Insights flow. */
class InsightsAggregationTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val monday = LocalDate.of(2026, 10, 12)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0, second: Int = 0): Long =
        date.atStartOfDay(zone).plusHours(hour.toLong()).plusMinutes(minute.toLong()).plusSeconds(second.toLong())
            .toInstant().toEpochMilli()

    private fun dayStart(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun ev(time: Long, type: T, pkg: String = "") = UsageEvent(time, type, pkg, if (pkg.isEmpty()) "" else "$pkg.Main")

    private fun day(date: LocalDate, events: List<UsageEvent>) =
        ScreenTimeAggregation.day(date, dayStart(date), dayStart(date.plusDays(1)), events, setOf(LAUNCHER))

    /** Unlock at [hour]:[minute], the launcher then [apps] one after the other, screen off. */
    private fun pickup(date: LocalDate, hour: Int, minute: Int, vararg apps: Pair<String, Int>): List<UsageEvent> {
        var cursor = at(date, hour, minute)
        val events = mutableListOf(
            ev(cursor, T.ScreenInteractive),
            ev(cursor, T.KeyguardShown),
            ev(cursor + 1_000, T.KeyguardHidden),
            ev(cursor + 1_000, T.ActivityResumed, LAUNCHER),
            ev(cursor + 2_000, T.ActivityPaused, LAUNCHER),
        )
        cursor += 2_000
        for ((pkg, minutes) in apps) {
            events += ev(cursor, T.ActivityResumed, pkg)
            cursor += minutes * 60_000L
            events += ev(cursor, T.ActivityPaused, pkg)
        }
        events += ev(cursor, T.ScreenNonInteractive)
        return events
    }

    @Test
    fun `each pickup records its time and the first app it opened`() {
        val usage = day(
            monday,
            pickup(monday, 7, 2, INSTAGRAM to 5, X to 3) +
                pickup(monday, 12, 30, X to 10) +
                // Unlocked, then straight back to sleep: no first app.
                pickup(monday, 15, 0) +
                pickup(monday, 21, 45, INSTAGRAM to 20),
        )

        assertEquals(4, usage.pickups)
        assertEquals(at(monday, 7, 2, 1), usage.pickupTimes.first())
        assertEquals(listOf(INSTAGRAM, X, INSTAGRAM), usage.firstAppsAfterPickup)
    }

    @Test
    fun `day stats keep the hourly minutes and the first pickup`() {
        val stats = ScreenTimeAggregation.dayStats(day(monday, pickup(monday, 7, 2, INSTAGRAM to 30) + pickup(monday, 22, 0, X to 15)), zone)

        assertEquals(45, stats.minutes)
        assertEquals(2, stats.pickups)
        assertEquals(7 * 60 + 2, stats.firstPickupMinute)
        assertEquals(30, stats.hourlyMinutes[7])
        assertEquals(15, stats.hourlyMinutes[22])
    }

    @Test
    fun `app detail joins quick re-opens and finds the longest session`() {
        val tuesday = monday.plusDays(1)
        val events = listOf(
            ev(at(monday, 8, 0), T.ScreenInteractive),
            ev(at(monday, 8, 0), T.ActivityResumed, INSTAGRAM),
            ev(at(monday, 8, 10), T.ActivityPaused, INSTAGRAM),
            // Back 5 s later (app switcher): the same open.
            ev(at(monday, 8, 10, 5), T.ActivityResumed, INSTAGRAM),
            ev(at(monday, 8, 25, 5), T.ActivityPaused, INSTAGRAM),
            ev(at(monday, 8, 30), T.ActivityResumed, X),
            ev(at(monday, 8, 40), T.ActivityPaused, X),
            ev(at(monday, 23, 0), T.ActivityResumed, INSTAGRAM),
            ev(at(monday, 23, 5), T.ActivityPaused, INSTAGRAM),
            ev(at(monday, 23, 5), T.ScreenNonInteractive),
        )
        val days = listOf(day(monday, events), day(tuesday, pickup(tuesday, 9, 30, INSTAGRAM to 12)))

        val detail = ScreenTimeAggregation.appDetail(UsageApp.of(AppId.Instagram), UsagePeriod.ThisWeek, days, zone)

        assertEquals(30 + 12, detail.minutes)
        assertEquals(3, detail.opens)
        assertEquals(25, detail.longestSessionMinutes)
        assertEquals(25, detail.hourlyMinutes[8])
        assertEquals(5, detail.hourlyMinutes[23])
        assertEquals(12, detail.hourlyMinutes[9])
        // Median of 8:00 and 9:30 first opens (upper middle).
        assertEquals(9 * 60 + 30, detail.firstOpenMinute)
        assertEquals(2, detail.days)
    }

    @Test
    fun `an unused app has no first open`() {
        val detail = ScreenTimeAggregation.appDetail(
            UsageApp.of(AppId.TikTok),
            UsagePeriod.Today,
            listOf(day(monday, pickup(monday, 9, 0, X to 5))),
            zone,
        )

        assertEquals(0, detail.minutes)
        assertEquals(0, detail.opens)
        assertNull(detail.firstOpenMinute)
    }

    @Test
    fun `pickup stats count by hour and rank the first apps`() {
        val catalogue = mapOf(
            INSTAGRAM to UsageApp.of(AppId.Instagram),
            X to UsageApp.of(AppId.X),
            CHROME to UsageApp(CHROME, "Chrome", AppCategory.Other),
        )
        val usage = day(
            monday,
            pickup(monday, 7, 2, X to 1) +
                pickup(monday, 7, 40, INSTAGRAM to 1) +
                pickup(monday, 18, 0, INSTAGRAM to 1) +
                pickup(monday, 19, 0, HIDDEN to 1),
        )

        val stats = ScreenTimeAggregation.pickupStats(UsagePeriod.Today, listOf(usage), zone, catalogue::get)

        assertEquals(4, stats.pickups)
        assertEquals(4, stats.dailyAverage)
        assertEquals(7 * 60 + 2, stats.firstPickupMinute)
        assertEquals(2, stats.hourlyPickups[7])
        assertEquals(1, stats.hourlyPickups[18])
        // Unknown apps are not listed.
        assertEquals(listOf("Instagram" to 2, "X" to 1), stats.firstOpens.map { it.app.label to it.count })
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val X = "com.twitter.android"
        const val CHROME = "com.android.chrome"
        const val LAUNCHER = "com.google.android.apps.nexuslauncher"
        const val HIDDEN = "com.example.hidden"
    }
}
