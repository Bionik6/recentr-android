package com.recentr.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** The accelerometer's steps (see [StepDetector]), on made-up readings 50 times a second. */
class StepDetectorTest {

    private fun readings(seconds: Double, magnitude: (Double) -> Double): List<Pair<Double, Double>> =
        generateSequence(0.0) { it + 1.0 / 50 }.takeWhile { it < seconds }.map { magnitude(it) to it }.toList()

    /** A walk's bounces: one a step, with the smaller jolt that comes with each. */
    private fun walk(stepsPerSecond: Double, bounce: Double): (Double) -> Double = { time ->
        1 + bounce * sin(2 * PI * stepsPerSecond * time) + bounce * 0.4 * sin(2 * PI * 2 * stepsPerSecond * time + 0.7)
    }

    private fun StepDetector.add(readings: List<Pair<Double, Double>>, from: Double = 0.0) = apply {
        readings.forEach { (magnitude, time) -> add(magnitude, from + time) }
    }

    @Test
    fun `counts each step of a walk`() {
        // In hand (small bounces), in a pocket (big ones), and slowly.
        listOf(1.8 to 0.12, 1.6 to 0.08, 2.0 to 0.4, 1.2 to 0.1).forEach { (stepsPerSecond, bounce) ->
            val steps = StepDetector().add(readings(20 / stepsPerSecond + 0.3, walk(stepsPerSecond, bounce))).steps
            assertTrue("$stepsPerSecond steps a second, bounce $bounce: $steps", steps in 19..21)
        }
    }

    @Test
    fun `shows the first steps within two seconds`() {
        assertTrue(StepDetector().add(readings(2.0, walk(1.8, 0.12))).steps >= 3)
    }

    @Test
    fun `a phone lying still takes no steps`() {
        assertEquals(0, StepDetector().add(readings(30.0) { 1 + 0.01 * sin(37 * it) }).steps)
    }

    @Test
    fun `lifting the phone isn't a step`() {
        val lifted = readings(6.0) { time -> if (time in 2.0..2.4) 1 + 0.35 * sin(PI * (time - 2) / 0.4) else 1.0 }
        assertEquals(0, StepDetector().add(lifted).steps)
    }

    @Test
    fun `a pause starts the walk over`() {
        val twoBounces = readings(1.1, walk(1.8, 0.12))
        val detector = StepDetector().add(twoBounces).add(readings(3.0) { 1.0 }, from = 1.1).add(twoBounces, from = 4.1)
        // Two bounces, a pause, two more: never three in a row, so not a walk.
        assertEquals(0, detector.steps)
    }
}
