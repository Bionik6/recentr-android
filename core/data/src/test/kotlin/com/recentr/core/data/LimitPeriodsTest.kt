package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class LimitPeriodsTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val hour = 3_600_000L

    /** Monday 19 Oct 2026. */
    private val monday = LocalDate.of(2026, 10, 19)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun utc(text: String): Long = Instant.parse(text).toEpochMilli()

    private fun LimitReset.start(now: Long) = periodStart(now, zone)

    @Test
    fun `midnight counts the local day`() {
        assertEquals(at(monday, 0), LimitReset.Midnight.start(at(monday, 0)))
        assertEquals(at(monday, 0), LimitReset.Midnight.start(at(monday, 23, 59)))
        assertEquals(at(monday.minusDays(1), 0), LimitReset.Midnight.start(at(monday, 0) - 1))
        assertTrue(LimitReset.Midnight.resetBetween(at(monday, 23, 59), at(monday.plusDays(1), 0), zone))
        assertFalse(LimitReset.Midnight.resetBetween(at(monday, 0), at(monday, 23, 59), zone))
    }

    @Test
    fun `4 AM counts the night with the evening before`() {
        assertEquals(at(monday.minusDays(1), 4), LimitReset.FourAm.start(at(monday, 0)))
        assertEquals(at(monday.minusDays(1), 4), LimitReset.FourAm.start(at(monday, 3, 59)))
        assertEquals(at(monday, 4), LimitReset.FourAm.start(at(monday, 4)))
        assertEquals(at(monday, 4), LimitReset.FourAm.start(at(monday, 23, 59)))

        assertTrue(LimitReset.FourAm.resetBetween(at(monday, 3, 59), at(monday, 4), zone))
        assertFalse(LimitReset.FourAm.resetBetween(at(monday.minusDays(1), 23, 0), at(monday, 3, 59), zone))
        assertFalse(LimitReset.FourAm.resetBetween(at(monday, 4), at(monday.plusDays(1), 3, 59), zone))
    }

    @Test
    fun `Monday counts the week`() {
        val sunday = monday.minusDays(1)
        assertEquals(at(monday.minusWeeks(1), 0), LimitReset.Monday.start(at(sunday, 23, 59)))
        assertEquals(at(monday, 0), LimitReset.Monday.start(at(monday, 0)))
        assertEquals(at(monday, 0), LimitReset.Monday.start(at(monday.plusDays(6), 23, 59)))

        assertTrue(LimitReset.Monday.resetBetween(at(sunday, 23, 59), at(monday, 0), zone))
        assertFalse(LimitReset.Monday.resetBetween(at(monday, 0), at(monday.plusDays(6), 23, 59), zone))
        // Midnight on any other day isn't a reset.
        assertFalse(LimitReset.Monday.resetBetween(at(monday, 23, 59), at(monday.plusDays(1), 0), zone))
    }

    @Test
    fun `clocks going forward make a 23-hour day and night`() {
        // Sunday 29 March 2026, Paris: 2:00 CET → 3:00 CEST.
        val sunday = LocalDate.of(2026, 3, 29)
        assertEquals(utc("2026-03-28T23:00:00Z"), LimitReset.Midnight.start(at(sunday, 12)))
        assertEquals(23 * hour, LimitReset.Midnight.start(at(sunday.plusDays(1), 0)) - LimitReset.Midnight.start(at(sunday, 12)))

        // 3:30 CEST comes right after the gap: still Saturday's night.
        assertEquals(utc("2026-03-28T03:00:00Z"), LimitReset.FourAm.start(at(sunday, 3, 30)))
        assertEquals(utc("2026-03-29T02:00:00Z"), LimitReset.FourAm.start(at(sunday, 4)))
        assertEquals(23 * hour, LimitReset.FourAm.start(at(sunday, 4)) - LimitReset.FourAm.start(at(sunday, 3, 59)))

        // The week from Monday 23 to Monday 30 March is an hour short.
        assertEquals(utc("2026-03-22T23:00:00Z"), LimitReset.Monday.start(at(sunday, 23, 59)))
        assertEquals(utc("2026-03-29T22:00:00Z"), LimitReset.Monday.start(at(sunday.plusDays(1), 0)))
    }

    @Test
    fun `clocks going back make a 25-hour day and night`() {
        // Sunday 25 October 2026, Paris: 3:00 CEST → 2:00 CET, so 2:30 happens twice.
        val sunday = LocalDate.of(2026, 10, 25)
        val firstHalfPastTwo = utc("2026-10-25T00:30:00Z")
        val secondHalfPastTwo = utc("2026-10-25T01:30:00Z")
        assertEquals(2, Instant.ofEpochMilli(firstHalfPastTwo).atZone(zone).hour)
        assertEquals(2, Instant.ofEpochMilli(secondHalfPastTwo).atZone(zone).hour)
        assertEquals(ZoneOffset.ofHours(1), Instant.ofEpochMilli(secondHalfPastTwo).atZone(zone).offset)

        // Both 2:30s belong to Saturday's night, which ends at 4:00 CET, 25 hours after it began.
        val saturdayNight = utc("2026-10-24T02:00:00Z")
        assertEquals(saturdayNight, LimitReset.FourAm.start(firstHalfPastTwo))
        assertEquals(saturdayNight, LimitReset.FourAm.start(secondHalfPastTwo))
        assertFalse(LimitReset.FourAm.resetBetween(firstHalfPastTwo, secondHalfPastTwo, zone))
        assertEquals(utc("2026-10-25T03:00:00Z"), LimitReset.FourAm.start(at(sunday, 4)))
        assertEquals(25 * hour, LimitReset.FourAm.start(at(sunday, 4)) - saturdayNight)

        assertEquals(25 * hour, LimitReset.Midnight.start(at(sunday.plusDays(1), 0)) - LimitReset.Midnight.start(at(sunday, 12)))

        // Sunday night, after the change, is still the week that began on Monday 19 (CEST).
        assertEquals(utc("2026-10-18T22:00:00Z"), LimitReset.Monday.start(at(sunday, 23, 59)))
        assertEquals(utc("2026-10-25T23:00:00Z"), LimitReset.Monday.start(at(sunday.plusDays(1), 0)))
    }

    @Test
    fun `a limit's use is read from its own period`() {
        val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
        val usage = LimitUsage(mapOf(LimitReset.Midnight to mapOf(tiktok.packageName to 20), LimitReset.Monday to mapOf(tiktok.packageName to 140)))
        val daily = Rule("d", RuleType.Limit, "TikTok", apps = listOf(tiktok), limitMinutes = 60)
        assertEquals(20, usage.of(daily))
        assertEquals(140, usage.of(daily.copy(limitReset = LimitReset.Monday)))
        assertNull(usage.of(daily.copy(limitReset = LimitReset.FourAm)))
    }
}
