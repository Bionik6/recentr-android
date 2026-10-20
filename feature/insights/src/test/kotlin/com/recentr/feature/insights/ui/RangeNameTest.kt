package com.recentr.feature.insights.ui

import com.recentr.core.model.RangeUnit
import com.recentr.core.model.UsageRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** How the date selector names what it shows. */
class RangeNameTest {

    /** Monday 19 Oct 2026. */
    private val today = LocalDate.of(2026, 10, 19)

    private fun name(unit: RangeUnit, date: LocalDate) = UsageRange.of(unit, date).name(today)

    @Test
    fun `today and yesterday by name, other days by date`() {
        assertEquals(RangeName.Today, name(RangeUnit.Day, today))
        assertEquals(RangeName.Yesterday, name(RangeUnit.Day, today.minusDays(1)))
        assertEquals(RangeName.Day(LocalDate.of(2026, 10, 17), withYear = false), name(RangeUnit.Day, LocalDate.of(2026, 10, 17)))
        assertEquals(RangeName.Day(LocalDate.of(2025, 12, 31), withYear = true), name(RangeUnit.Day, LocalDate.of(2025, 12, 31)))
    }

    @Test
    fun `this week and last week by name, other weeks by their days`() {
        assertEquals(RangeName.ThisWeek, name(RangeUnit.Week, today.plusDays(3)))
        assertEquals(RangeName.LastWeek, name(RangeUnit.Week, LocalDate.of(2026, 10, 14)))
        assertEquals(
            RangeName.Week(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4), withYear = false),
            name(RangeUnit.Week, LocalDate.of(2026, 10, 1)),
        )
        // A week across the new year shows it.
        assertEquals(
            RangeName.Week(LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4), withYear = true),
            name(RangeUnit.Week, LocalDate.of(2026, 1, 1)),
        )
    }

    @Test
    fun `months by name, with the year when not this one`() {
        assertEquals(RangeName.Month(YearMonth.of(2026, 10), withYear = false), name(RangeUnit.Month, today))
        assertEquals(RangeName.Month(YearMonth.of(2025, 11), withYear = true), name(RangeUnit.Month, LocalDate.of(2025, 11, 3)))
    }
}
