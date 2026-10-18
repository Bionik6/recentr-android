package com.recentr.core.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreaksTest {

    /** Sunday 18 October 2026. */
    private val today = LocalDate.of(2026, 10, 18)

    /** [minutes] newest first, today first; null = no data. */
    private fun days(vararg minutes: Int?): Sequence<StreakDay> = minutes.asSequence().mapIndexed { index, value ->
        StreakDay(today.minusDays(index.toLong()), hasData = value != null, minutes = value ?: 0)
    }

    @Test
    fun `days over the goal use a saver instead of breaking the streak`() {
        val streak = Streaks.current(days(60, 90, 200, 80, 70, 300, 300, 300, 50), goalMinutes = 120)

        // 16 Oct is saved; 13 and 12 Oct use the two other savers and 11 Oct breaks the streak.
        assertEquals(StreakDayStatus.Saved, streak.statuses[today.minusDays(2)])
        assertEquals(StreakDayStatus.OnTarget, streak.statuses[today.minusDays(4)])
        assertEquals(today.minusDays(2), streak.lastSavedDay)
        // The oldest saved days do not start a streak: they give their saver back.
        assertEquals(5, streak.days)
        assertEquals(2, streak.saversLeft)
    }

    @Test
    fun `without savers the first day over the goal ends the streak`() {
        val streak = Streaks.current(days(60, 90, 200, 80), goalMinutes = 120, saversPerMonth = 0)

        assertEquals(2, streak.days)
        assertEquals(0, streak.saversLeft)
        assertNull(streak.lastSavedDay)
    }

    @Test
    fun `a day without data ends the streak but today always counts`() {
        assertEquals(2, Streaks.current(days(10, 20, null, 30), goalMinutes = 60).days)
        assertEquals(1, Streaks.current(days(null, null), goalMinutes = 60).days)
    }

    @Test
    fun `savers are counted per calendar month`() {
        // 2 Oct back to 26 Sep: one saver a month, five days over the goal from 1 Oct.
        val start = LocalDate.of(2026, 10, 2)
        val newestFirst = listOf(10, 200, 200, 200, 200, 200, 10).mapIndexed { index, minutes ->
            StreakDay(start.minusDays(index.toLong()), true, minutes)
        }.asSequence()

        val streak = Streaks.current(newestFirst, goalMinutes = 60, saversPerMonth = 1)

        // 2 Oct on target, 1 Oct saved (October's saver), 30 Sep saved (September's), 29 Sep breaks.
        // The trailing saves give their savers back: only 2 Oct counts.
        assertEquals(1, streak.days)
    }

    @Test
    fun `runs apply savers going forward and reset on gaps`() {
        val chronological = listOf(30, 30, 200, 30, 200, 200, 30).mapIndexed { index, minutes ->
            StreakDay(LocalDate.of(2026, 10, 1).plusDays(index.toLong()), true, minutes)
        } + StreakDay(LocalDate.of(2026, 10, 10), true, 30)

        val runs = Streaks.runs(chronological, goalMinutes = 60, saversPerMonth = 2)

        assertEquals(listOf(1, 2, 3, 4, 5, 0, 1, 1), runs)
        assertEquals(5, Streaks.best(chronological, goalMinutes = 60, saversPerMonth = 2))
    }
}
