package com.recentr.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

/** The duration picker's ticks: where they fall, which are labelled, and landing on them. */
class DurationScaleTest {

    @Test
    fun `ticks fall every step and on the end`() {
        assertEquals(listOf(15, 30, 45, 60), DurationScale(15..60, step = 15).values)
        // An end off the steps still gets its tick.
        assertEquals(listOf(5, 10, 15, 20, 22), DurationScale(5..22, step = 5).values)
        assertEquals(listOf(3), DurationScale(3..3).values)
    }

    @Test
    fun `labels fall on multiples of labelEvery`() {
        val scale = DurationScale(15..120, step = 15, labelEvery = 60)
        assertEquals(listOf(60, 120), scale.values.filter(scale::isLabelled))
        // Six steps by default.
        assertEquals(30, DurationScale(5..480, step = 5).labelEvery)
    }

    @Test
    fun `a value off the scale lands on the nearest tick`() {
        val scale = DurationScale(15..720, step = 15)
        assertEquals(255, scale.nearest(260))
        assertEquals(270, scale.nearest(265))
        assertEquals(15, scale.nearest(0))
        assertEquals(720, scale.nearest(900))
        // A tie goes to the shorter.
        assertEquals(10, DurationScale(10..100, step = 10).nearest(15))
    }

    @Test
    fun `steps stay within the scale`() {
        val scale = DurationScale(5..480, step = 5)
        assertEquals(50, scale.stepped(45, 1))
        assertEquals(40, scale.stepped(45, -1))
        assertEquals(5, scale.stepped(5, -1))
        assertEquals(480, scale.stepped(480, 1))
        // From off the scale, it moves from the nearest tick.
        assertEquals(50, scale.stepped(47, 1))
    }
}
