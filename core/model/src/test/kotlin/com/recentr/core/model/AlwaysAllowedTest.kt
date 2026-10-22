package com.recentr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Always allowed (docs/RULES.md): removing an app applies now, allowing one waits for the rules it loosens. */
class AlwaysAllowedTest {

    private val zone = ZoneId.of("Europe/Paris")

    /** [day] Oct 2026 at [hour]:[minutes]; Monday 19 Oct by default. */
    private fun at(hour: Int, minutes: Int = 0, day: Int = 19): Long =
        LocalDate.of(2026, 10, day).atTime(hour, minutes).atZone(zone).toInstant().toEpochMilli()

    private val maps = UsageApp("com.google.android.apps.maps", "Maps", AppCategory.Other)
    private val uber = UsageApp("com.ubercab", "Uber", AppCategory.Other)
    private val strava = UsageApp("com.strava", "Strava", AppCategory.Other)
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)

    private val now = at(12)

    /** On since 9:00, holding Maps and TikTok. */
    private val lock = Rule("lock", RuleType.Lock, "Phone", apps = listOf(maps, tiktok), activeSince = at(9))

    /** Weekdays 9:00 → 17:00, holding Uber. */
    private val workHours = Rule("work", RuleType.Bedtime, "Work hours", apps = listOf(uber), schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()))

    /** Started at 11:50 for 30 minutes, holding Strava. */
    private val session = Rule("s", RuleType.Session, "Deep work", apps = listOf(strava), durationMinutes = 30, activeSince = at(11, 50), activeUntil = at(12, 20))

    /** Started at 8:00 with an hour, holding Uber. */
    private val limit = Rule("m", RuleType.Limit, "Rides", apps = listOf(uber), limitMinutes = 60, activeSince = at(8))

    /** A focus round until 12:25, then a break and a last round: over at 12:55. */
    private val run = FocusRun(
        config = FocusConfig(focusMinutes = 25, shortBreakMinutes = 5, rounds = 2, blockedApps = listOf(strava)),
        startedAt = at(12),
        round = 1,
        phase = FocusPhase.Focus,
        phaseEndsAt = at(12, 25),
        pausedRemainingMillis = null,
    )

    private fun AlwaysAllowed.choosing(vararg apps: UsageApp, rules: List<Rule> = emptyList(), focus: FocusRun? = null, at: Long = now) =
        choosing(apps.toList(), rules, focus, at, zone)

    @Test
    fun `with nothing engaged, a new list applies now`() {
        val allowed = AlwaysAllowed().choosing(maps, strava, rules = listOf(lock.copy(activeSince = null), session.copy(activeSince = null, activeUntil = null)))
        assertEquals(AlwaysAllowed(listOf(maps, strava)), allowed)
        assertTrue(allowed.allows(maps.packageName, now))
    }

    @Test
    fun `an app an engaged rule holds waits for the end of its engagement`() {
        // A lock and a limit let go at midnight.
        val lockWait = AlwaysAllowed().choosing(maps, rules = listOf(lock))
        assertEquals(AlwaysAllowed(apps = emptyList(), pending = AllowedChange(at(0, day = 20), listOf(maps))), lockWait)
        assertFalse(lockWait.allows(maps.packageName, now))
        assertEquals(at(0, day = 20), AlwaysAllowed().choosing(uber, rules = listOf(limit)).pending?.appliesAt)
        // A routine at the end of its window, a session when it ends.
        assertEquals(at(17), AlwaysAllowed().choosing(uber, rules = listOf(workHours)).pending?.appliesAt)
        assertEquals(at(12, 20), AlwaysAllowed().choosing(strava, rules = listOf(session)).pending?.appliesAt)
    }

    @Test
    fun `an app no engaged rule holds joins now`() {
        // Maps isn't Work hours' app; outside its window, Work hours lets Uber through anyway.
        assertEquals(AlwaysAllowed(listOf(maps)), AlwaysAllowed().choosing(maps, rules = listOf(workHours)))
        assertEquals(AlwaysAllowed(listOf(uber)), AlwaysAllowed().choosing(uber, rules = listOf(workHours), at = at(18)))
        // A paused routine isn't engaged.
        assertEquals(AlwaysAllowed(listOf(uber)), AlwaysAllowed().choosing(uber, rules = listOf(workHours.copy(pausedUntil = at(13)))))
    }

    @Test
    fun `only the apps an engaged rule holds wait, the others join now`() {
        // The lock holds Maps, not Uber.
        val allowed = AlwaysAllowed().choosing(maps, uber, rules = listOf(lock))
        assertEquals(AlwaysAllowed(listOf(uber), AllowedChange(at(0, day = 20), listOf(maps, uber))), allowed)
        assertTrue(allowed.allows(uber.packageName, now))
        assertFalse(allowed.allows(maps.packageName, now))
        assertEquals(listOf(maps), allowed.waiting)
    }

    @Test
    fun `the apps that wait join together, at the latest end`() {
        val allowed = AlwaysAllowed().choosing(strava, uber, rules = listOf(session, workHours, lock))
        assertEquals(AlwaysAllowed(emptyList(), AllowedChange(at(17), listOf(strava, uber))), allowed)
        // At 13:00 the session is over: Strava joins now. Maps, added, waits for the lock, so Uber
        // waits with it until midnight.
        val more = allowed.choosing(strava, uber, maps, rules = listOf(session, workHours, lock), at = at(13))
        assertEquals(AlwaysAllowed(listOf(strava), AllowedChange(at(0, day = 20), listOf(strava, uber, maps))), more)
        assertEquals(listOf(uber, maps), more.waiting)
    }

    @Test
    fun `removing an app applies now, even while others wait`() {
        val allowed = AlwaysAllowed(listOf(maps)).choosing(maps, uber, rules = listOf(workHours))
        assertEquals(AlwaysAllowed(listOf(maps), AllowedChange(at(17), listOf(maps, uber))), allowed)
        assertEquals(listOf(uber), allowed.waiting)
        // Through the picker: Maps goes now, Uber still waits.
        val picked = allowed.choosing(uber, rules = listOf(workHours))
        assertEquals(AlwaysAllowed(emptyList(), AllowedChange(at(17), listOf(uber))), picked)
        assertFalse(picked.allows(maps.packageName, now))
    }

    /** A rule page's Blocked card and Apps sheet: the rule's apps, but for those always allowed. */
    @Test
    fun `a rule blocks its apps but those always allowed`() {
        val picked = listOf(maps, tiktok, uber)
        assertEquals(picked, AlwaysAllowed().blocked(picked, now))
        assertEquals(listOf(tiktok), AlwaysAllowed(listOf(maps, uber, strava)).blocked(picked, now))
        assertEquals(emptyList<UsageApp>(), AlwaysAllowed(picked).blocked(picked, now))
    }

    /** An app waiting to join is still blocked, until it has joined. */
    @Test
    fun `an app waiting to join stays blocked until it joins`() {
        val waiting = AlwaysAllowed(pending = AllowedChange(at(17), listOf(uber)))
        assertEquals(listOf(maps, uber), waiting.blocked(listOf(maps, uber), now))
        assertEquals(listOf(maps), waiting.blocked(listOf(maps, uber), at(17)))
    }

    @Test
    fun `removing the app that waits drops the kept change`() {
        val allowed = AlwaysAllowed(listOf(maps)).choosing(maps, uber, rules = listOf(workHours))
        // Unpicked: nothing waits any more, and Maps stays.
        assertEquals(AlwaysAllowed(listOf(maps)), allowed.choosing(maps, rules = listOf(workHours)))
    }

    @Test
    fun `removing the app that waits longest lets the others join at their own end`() {
        // Strava waits for the session (12:20), Maps for the lock (midnight): together, at midnight.
        val allowed = AlwaysAllowed().choosing(strava, maps, rules = listOf(session, lock))
        assertEquals(AllowedChange(at(0, day = 20), listOf(strava, maps)), allowed.pending)
        // Maps unpicked: Strava joins when the session ends.
        assertEquals(AlwaysAllowed(emptyList(), AllowedChange(at(12, 20), listOf(strava))), allowed.choosing(strava, rules = listOf(session, lock)))
    }

    @Test
    fun `a strict focus session holds the apps it blocks, a relaxed one doesn't`() {
        assertEquals(at(12, 55), run.projectedEnd(now))
        val strict = run.copy(config = run.config.copy(strictMode = true))
        assertEquals(AllowedChange(at(12, 55), listOf(strava)), AlwaysAllowed().choosing(strava, focus = strict).pending)
        assertEquals(AlwaysAllowed(listOf(strava)), AlwaysAllowed().choosing(strava, focus = run))
        // It only holds its own apps.
        assertEquals(AlwaysAllowed(listOf(maps)), AlwaysAllowed().choosing(maps, focus = strict))
    }

    @Test
    fun `a focus session's end counts the breaks and rounds to come`() {
        val config = FocusConfig(focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15, rounds = 5)
        // Round 4's break is the long one: 4 rounds, 3 short breaks and a long one after the 4th.
        val first = run.copy(config = config)
        assertEquals(at(12, 25) + (4 * 25 + 3 * 5 + 15) * 60_000L, first.projectedEnd(now))
        // On the last break: one round left.
        val onBreak = first.copy(round = 4, phase = FocusPhase.Break, phaseEndsAt = at(12, 10))
        assertEquals(at(12, 35), onBreak.projectedEnd(now))
    }

    @Test
    fun `the kept list applies once it's due`() {
        val allowed = AlwaysAllowed().choosing(maps, rules = listOf(lock))
        assertEquals(emptyList<UsageApp>(), allowed.appsAt(at(23, 59)))
        assertEquals(listOf(maps), allowed.appsAt(at(0, day = 20)))
        assertTrue(allowed.allows(maps.packageName, at(0, day = 20)))
        assertEquals(allowed, allowed.settled(at(23, 59)))
        assertEquals(AlwaysAllowed(listOf(maps)), allowed.settled(at(0, day = 20)))
        // The picker opens on the list as last chosen.
        assertEquals(listOf(maps), allowed.latestWish)
        assertNull(AlwaysAllowed(listOf(maps)).pending)
    }

    @Test
    fun `a list chosen after the kept one is due compares with it`() {
        val allowed = AlwaysAllowed().choosing(maps, rules = listOf(lock))
        // Past midnight, Maps is allowed: adding Uber while the limit holds only makes Uber wait.
        val tomorrow = at(9, day = 20)
        val limitTomorrow = limit.copy(activeSince = at(8, day = 20))
        val more = allowed.choosing(listOf(maps, uber), listOf(limitTomorrow), null, tomorrow, zone)
        assertEquals(AlwaysAllowed(listOf(maps), AllowedChange(at(0, day = 21), listOf(maps, uber))), more)
    }
}
