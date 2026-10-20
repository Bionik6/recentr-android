package com.recentr.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Insights' Day / Week / Month. */
enum class RangeUnit { Day, Week, Month }

/**
 * A calendar day, week (Monday → Sunday) or month: the current one or one before it (Insights'
 * date selector). [start] is the day itself, the week's Monday or the month's 1st ([of] makes one).
 */
data class UsageRange private constructor(val unit: RangeUnit, val start: LocalDate) {

    /** The last day of the range, inclusive. */
    val end: LocalDate
        get() = when (unit) {
            RangeUnit.Day -> start
            RangeUnit.Week -> start.plusDays(6)
            RangeUnit.Month -> start.plusMonths(1).minusDays(1)
        }

    val month: YearMonth get() = YearMonth.from(start)

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun previous(): UsageRange = shifted(-1)

    fun next(): UsageRange = shifted(1)

    /** Whether the next range has started by [today] (never past the current one). */
    fun hasNext(today: LocalDate): Boolean = !next().start.isAfter(today)

    /** Whether the previous range has a day on or after [firstDay], the first day with data. */
    fun hasPrevious(firstDay: LocalDate?): Boolean = firstDay != null && !previous().end.isBefore(firstDay)

    /** Its days up to [today]: all of them for a past range, none for a future one. */
    fun daysUntil(today: LocalDate): List<LocalDate> =
        generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) && !it.isAfter(today) }.toList()

    /**
     * The same moment in another [unit]: the range holding this one's last day so far (today for a
     * current range, its last day for a past one).
     */
    fun switchedTo(unit: RangeUnit, today: LocalDate): UsageRange = of(unit, minOf(end, today))

    /**
     * The Home period whose layout it takes: [UsagePeriod.Today] for today, [UsagePeriod.Yesterday]
     * for any day before (one day, hourly chart), [UsagePeriod.ThisWeek] / [UsagePeriod.ThisMonth]
     * for any week / month (per-day chart).
     */
    fun period(today: LocalDate): UsagePeriod = when (unit) {
        RangeUnit.Day -> if (start == today) UsagePeriod.Today else UsagePeriod.Yesterday
        RangeUnit.Week -> UsagePeriod.ThisWeek
        RangeUnit.Month -> UsagePeriod.ThisMonth
    }

    private fun shifted(by: Long): UsageRange = UsageRange(
        unit,
        when (unit) {
            RangeUnit.Day -> start.plusDays(by)
            RangeUnit.Week -> start.plusWeeks(by)
            RangeUnit.Month -> start.plusMonths(by)
        },
    )

    companion object {
        /** The [unit] holding [date]. */
        fun of(unit: RangeUnit, date: LocalDate): UsageRange = UsageRange(
            unit,
            when (unit) {
                RangeUnit.Day -> date
                RangeUnit.Week -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                RangeUnit.Month -> date.withDayOfMonth(1)
            },
        )

        /** A Home period, on the calendar of [today]. */
        fun of(period: UsagePeriod, today: LocalDate): UsageRange = when (period) {
            UsagePeriod.Today -> of(RangeUnit.Day, today)
            UsagePeriod.Yesterday -> of(RangeUnit.Day, today.minusDays(1))
            UsagePeriod.ThisWeek -> of(RangeUnit.Week, today)
            UsagePeriod.ThisMonth -> of(RangeUnit.Month, today)
        }
    }
}
