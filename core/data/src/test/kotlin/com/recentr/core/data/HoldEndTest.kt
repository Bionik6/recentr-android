package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * A routine's or a session's apps come back the moment its time is up: the hold screen lets go
 * as soon as [HoldState.reasonFor] says nothing holds the app, and the blocker puts nothing on
 * hold past then (docs/RULES.md).
 */
class HoldEndTest {

    private val zone = ZoneId.of("Europe/Paris")

    /** [day] Oct 2026 at [hour]:[minutes]:[seconds]; Saturday 24 Oct by default. Clocks go back an hour early on Sunday 25. */
    private fun at(day: Int = 24, hour: Int = 0, minutes: Int = 0, seconds: Int = 0): Long =
        LocalDate.of(2026, 10, day).atTime(hour, minutes, seconds).atZone(zone).toInstant().toEpochMilli()

    private val second = 1_000L
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)

    private fun routine(id: String, schedule: RuleSchedule) =
        Rule(id, RuleType.Bedtime, id, apps = listOf(tiktok), schedule = schedule)

    /** Weekends, 9:00 to 12:00. */
    private val weekendZen = routine("weekend-zen", RuleSchedule(9 * 60, 12 * 60, setOf(6, 7)))

    /** Every night, 23:00 to 7:00. */
    private val nightPause = routine("night-pause", RuleSchedule(23 * 60, 7 * 60))

    /** Weekdays, 9:00 to 17:00. */
    private val workingTime = routine("working-time", RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()))

    /** Weekends, all day. */
    private val wholeWeekend = routine("whole-weekend", RuleSchedule(0, 0, setOf(6, 7)))

    /** Every day, 12:30 to 12:40. */
    private val tenMinutes = routine("ten-minutes", RuleSchedule(12 * 60 + 30, 12 * 60 + 40))

    /** Started Saturday at 11:50 for 30 minutes. */
    private val deepWork = Rule(
        "deep-work", RuleType.Session, "Deep work", apps = listOf(tiktok),
        durationMinutes = 30, activeSince = at(hour = 11, minutes = 50), activeUntil = at(hour = 12, minutes = 20),
    )

    private fun Rule.holds(at: Long): Boolean =
        HoldState(rules = listOf(this), focus = null, protectedApps = emptySet()).reasonFor(tiktok.packageName, at, zone) != null

    @Test
    fun `a routine holds its apps to the last second of its window, and not a moment longer`() {
        assertTrue(weekendZen.holds(at(hour = 11, minutes = 59, seconds = 59)))
        assertTrue(!weekendZen.holds(at(hour = 12)))
        assertTrue(!weekendZen.holds(at(hour = 12, seconds = 1)))
        assertTrue(!weekendZen.holds(at(hour = 12, minutes = 2)))
        // Sunday's window too, and none on Monday.
        assertTrue(weekendZen.holds(at(day = 25, hour = 11, minutes = 59, seconds = 59)))
        assertTrue(!weekendZen.holds(at(day = 25, hour = 12)))
        assertTrue(!weekendZen.holds(at(day = 26, hour = 10)))
    }

    @Test
    fun `an overnight routine lets go in the morning, the night the clocks go back too`() {
        assertTrue(nightPause.holds(at(day = 25, hour = 6, minutes = 59, seconds = 59)))
        assertTrue(!nightPause.holds(at(day = 25, hour = 7)))
        assertTrue(nightPause.holds(at(day = 26, hour = 6, minutes = 59, seconds = 59)))
        assertTrue(!nightPause.holds(at(day = 26, hour = 7)))
    }

    @Test
    fun `a whole-day routine lets go at midnight after its last day`() {
        assertTrue(wholeWeekend.holds(at(day = 25, hour = 23, minutes = 59, seconds = 59)))
        assertTrue(!wholeWeekend.holds(at(day = 26)))
    }

    @Test
    fun `a short routine lets go at its end`() {
        assertTrue(tenMinutes.holds(at(hour = 12, minutes = 39, seconds = 59)))
        assertTrue(!tenMinutes.holds(at(hour = 12, minutes = 40)))
    }

    @Test
    fun `a session lets go at its end, before it's even settled`() {
        assertTrue(deepWork.holds(at(hour = 12, minutes = 19, seconds = 59)))
        // Still saved as running ([RulesRepository.settle] clears it a few seconds later): it holds nothing.
        assertNotNull(deepWork.activeSince)
        assertTrue(!deepWork.holds(at(hour = 12, minutes = 20)))
        assertTrue(!deepWork.holds(at(hour = 12, minutes = 20, seconds = 1)))
    }

    @Test
    fun `a session's countdown and a routine's end at the moment they let go`() {
        assertEquals(at(hour = 12, minutes = 20), deepWork.heldUntil(at(hour = 12), zone))
        assertEquals(at(hour = 12), weekendZen.heldUntil(at(hour = 11, minutes = 59, seconds = 59), zone))
        assertEquals(at(day = 25, hour = 7), nightPause.heldUntil(at(hour = 23, minutes = 30), zone))
        assertNull(weekendZen.heldUntil(at(hour = 12), zone))
    }

    @Test
    fun `a routine's end is at its hour on the clock, the nights the clocks change`() {
        // Back an hour at 3:00 on Sunday 25 October: still 7:00.
        assertEquals(at(day = 25, hour = 7), nightPause.heldUntil(at(day = 25, hour = 1), zone))
        // Forward an hour at 2:00 on Sunday 29 March: 7:00, not 8:00.
        val march = LocalDate.of(2026, 3, 29)
        val sevenOnMarch29 = march.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(sevenOnMarch29, nightPause.heldUntil(march.atTime(1, 0).atZone(zone).toInstant().toEpochMilli(), zone))
        assertTrue(nightPause.holds(sevenOnMarch29 - second))
        assertTrue(!nightPause.holds(sevenOnMarch29))
        // And Weekend Zen starts at 9:00 that day.
        assertEquals(march.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), weekendZen.schedule!!.nextStartAfter(sevenOnMarch29, zone))
    }

    /**
     * Whatever the moment a routine or a session holds the app, it lets go at the end of its time
     * (of its last window, when the next one starts as it ends), held to the second before.
     */
    @Test
    fun `whenever a routine or a session holds an app, it lets go once its time is up`() {
        val rules = listOf(weekendZen, nightPause, workingTime, wholeWeekend, tenMinutes, deepWork)
        var checked = 0
        // Saturday 24 to Tuesday 27, minute by minute, a few seconds into each.
        var now = at(hour = 0, seconds = 7)
        while (now < at(day = 27)) {
            for (rule in rules) {
                if (!rule.holds(now)) continue
                val end = rule.letsGoAt(now)
                assertNotNull("${rule.id} holds at $now and never lets go", end)
                assertTrue("${rule.id} lets go before its end, ${end!!}", rule.holds(end - second))
                assertTrue("${rule.id} still holds at its end, $end", !rule.holds(end))
                checked++
            }
            now += 60 * second
        }
        assertTrue(checked > 1_000)
    }

    /** When [this] lets its apps go, holding them at [now]: the session's end, or its windows' last end within a week. */
    private fun Rule.letsGoAt(now: Long): Long? {
        sessionEndsAt?.let { return it }
        val schedule = schedule ?: return null
        var end = schedule.windowEndAt(now, zone) ?: return null
        repeat(7) {
            if (!holds(end)) return end
            end = schedule.windowEndAt(end, zone) ?: return end
        }
        return null
    }
}
