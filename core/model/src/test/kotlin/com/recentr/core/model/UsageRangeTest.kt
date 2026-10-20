package com.recentr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Insights' days, weeks and months: their bounds, the arrows, and switching between them. */
class UsageRangeTest {

    /** Monday 19 Oct 2026. */
    private val today = LocalDate.of(2026, 10, 19)

    private fun day(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)

    @Test
    fun `a week runs Monday to Sunday and a month its whole calendar`() {
        val week = UsageRange.of(RangeUnit.Week, day(10, 22))
        assertEquals(day(10, 19), week.start)
        assertEquals(day(10, 25), week.end)
        val month = UsageRange.of(RangeUnit.Month, day(2, 14))
        assertEquals(day(2, 1), month.start)
        assertEquals(day(2, 28), month.end)
        assertEquals(day(2, 29, 2028), UsageRange.of(RangeUnit.Month, day(2, 3, 2028)).end)
    }

    @Test
    fun `a week across two months`() {
        val week = UsageRange.of(RangeUnit.Week, day(10, 1))
        assertEquals(day(9, 28), week.start)
        assertEquals(day(10, 4), week.end)
        assertTrue(day(9, 30) in week)
        assertFalse(day(10, 5) in week)
    }

    @Test
    fun `the arrows step one unit`() {
        assertEquals(UsageRange.of(RangeUnit.Day, day(10, 18)), UsageRange.of(RangeUnit.Day, today).previous())
        assertEquals(UsageRange.of(RangeUnit.Week, day(10, 12)), UsageRange.of(RangeUnit.Week, today).previous())
        assertEquals(UsageRange.of(RangeUnit.Month, day(9, 1)), UsageRange.of(RangeUnit.Month, today).previous())
        assertEquals(UsageRange.of(RangeUnit.Month, day(12, 1, 2025)), UsageRange.of(RangeUnit.Month, day(1, 9)).previous())
        // 31 Jan → the month after is February, not 3 March.
        assertEquals(day(2, 1), UsageRange.of(RangeUnit.Month, day(1, 31)).next().start)
    }

    @Test
    fun `nothing after the current day, week or month`() {
        assertFalse(UsageRange.of(RangeUnit.Day, today).hasNext(today))
        assertTrue(UsageRange.of(RangeUnit.Day, day(10, 18)).hasNext(today))
        // This week (Monday) and this month have nothing after them.
        assertFalse(UsageRange.of(RangeUnit.Week, today).hasNext(today))
        assertFalse(UsageRange.of(RangeUnit.Month, today).hasNext(today))
        // Last week leads to this week, which started today.
        assertTrue(UsageRange.of(RangeUnit.Week, day(10, 12)).hasNext(today))
    }

    @Test
    fun `nothing before the first day with data`() {
        val firstDay = day(10, 9)
        assertTrue(UsageRange.of(RangeUnit.Day, day(10, 10)).hasPrevious(firstDay))
        assertFalse(UsageRange.of(RangeUnit.Day, firstDay).hasPrevious(firstDay))
        // The week of 5 Oct holds 9 Oct: reachable from the week of 12 Oct, but not further.
        assertTrue(UsageRange.of(RangeUnit.Week, day(10, 12)).hasPrevious(firstDay))
        assertFalse(UsageRange.of(RangeUnit.Week, day(10, 5)).hasPrevious(firstDay))
        assertFalse(UsageRange.of(RangeUnit.Month, today).hasPrevious(firstDay))
        assertTrue(UsageRange.of(RangeUnit.Month, today).hasPrevious(day(9, 30)))
        assertFalse(UsageRange.of(RangeUnit.Day, today).hasPrevious(null))
    }

    @Test
    fun `a current range has its days up to today, a past one all of them`() {
        assertEquals(listOf(today), UsageRange.of(RangeUnit.Week, today).daysUntil(today))
        assertEquals(7, UsageRange.of(RangeUnit.Week, day(10, 12)).daysUntil(today).size)
        assertEquals(19, UsageRange.of(RangeUnit.Month, today).daysUntil(today).size)
        assertEquals(30, UsageRange.of(RangeUnit.Month, day(9, 1)).daysUntil(today).size)
        assertTrue(UsageRange.of(RangeUnit.Day, day(10, 20)).daysUntil(today).isEmpty())
    }

    @Test
    fun `switching unit keeps the day looked at`() {
        // Today's week → today.
        assertEquals(UsageRange.of(RangeUnit.Day, today), UsageRange.of(RangeUnit.Week, today).switchedTo(RangeUnit.Day, today))
        // A past week → its Sunday; a past month → its last day.
        assertEquals(UsageRange.of(RangeUnit.Day, day(10, 11)), UsageRange.of(RangeUnit.Week, day(10, 5)).switchedTo(RangeUnit.Day, today))
        assertEquals(UsageRange.of(RangeUnit.Day, day(9, 30)), UsageRange.of(RangeUnit.Month, day(9, 1)).switchedTo(RangeUnit.Day, today))
        // A day → its week and month.
        assertEquals(UsageRange.of(RangeUnit.Week, day(9, 28)), UsageRange.of(RangeUnit.Day, day(10, 2)).switchedTo(RangeUnit.Week, today))
        assertEquals(UsageRange.of(RangeUnit.Month, day(10, 1)), UsageRange.of(RangeUnit.Day, day(10, 2)).switchedTo(RangeUnit.Month, today))
    }

    @Test
    fun `Home periods are ranges of today's calendar, and ranges take a period's layout`() {
        assertEquals(UsageRange.of(RangeUnit.Day, day(10, 18)), UsageRange.of(UsagePeriod.Yesterday, today))
        assertEquals(UsageRange.of(RangeUnit.Week, today), UsageRange.of(UsagePeriod.ThisWeek, today))
        assertEquals(UsagePeriod.Today, UsageRange.of(RangeUnit.Day, today).period(today))
        assertEquals(UsagePeriod.Yesterday, UsageRange.of(RangeUnit.Day, day(9, 2)).period(today))
        assertEquals(UsagePeriod.ThisWeek, UsageRange.of(RangeUnit.Week, day(9, 2)).period(today))
        assertEquals(UsagePeriod.ThisMonth, UsageRange.of(RangeUnit.Month, day(9, 2)).period(today))
    }
}
