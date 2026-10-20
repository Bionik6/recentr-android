package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.AppSettings
import com.recentr.core.model.LimitAction
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UnlocksTest {

    private val zone = ZoneId.of("Europe/Paris")
    private fun at(day: LocalDate, hour: Int, minute: Int = 0) = day.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    /** Monday 19 Oct 2026, 10:00. */
    private val today = LocalDate.of(2026, 10, 19)
    private val now = at(today, 10)
    private val minute = 60_000L

    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
    private fun lock(method: UnlockMethod = UnlockMethod.Wait) =
        Rule("night", RuleType.Lock, "Night Routine", apps = listOf(tiktok), unlockMethod = method, unlocksPerDay = 5)

    @Test
    fun `each unlock method has its step`() {
        assertEquals(UnlockStep.Wait, lock(UnlockMethod.Wait).unlockStep(now, emergencyUnlocksLeft = 3))
        assertEquals(UnlockStep.Retype, lock(UnlockMethod.Retype).unlockStep(now, 3))
        assertEquals(UnlockStep.Scan, lock(UnlockMethod.ScanCode).unlockStep(now, 3))
        assertEquals(UnlockStep.Walk, lock(UnlockMethod.Walk).unlockStep(now, 3))
    }

    @Test
    fun `a walk is a way out of how do you unlock`() {
        assertEquals(UnlockMethod.Walk, UNLOCK_METHODS.last())
        assertEquals(UnlockMethod.Walk, lock(UnlockMethod.Walk).unlockMethodInUse)
        assertEquals(UnlockMethod.Walk, routine(UnlockMethod.Walk).unlockMethodInUse)
        assertTrue(lock(UnlockMethod.Walk).canOpenAnyway(now, zone))
        // It counts against the day's unlocks like any other.
        assertNull(lock(UnlockMethod.Walk).copy(unlocksUsedToday = 5).unlockStep(now, 3))
    }

    @Test
    fun `a strict walk only lets emergencies through`() {
        val strict = Rule(
            "s", RuleType.Session, "Deep work", apps = listOf(tiktok),
            durationMinutes = 60, activeSince = now - 10 * minute, strictMode = true, unlockMethod = UnlockMethod.Walk,
        )
        assertEquals(UnlockStep.Emergency, strict.unlockStep(now, emergencyUnlocksLeft = 1))
        assertEquals(UnlockStep.Walk, strict.copy(activeSince = now - 70 * minute).unlockStep(now, emergencyUnlocksLeft = 0))
    }

    @Test
    fun `a reason left over from before waits a few seconds`() {
        // "Give a good reason" is gone from "How do you unlock?".
        assertEquals(UnlockMethod.Wait, lock(UnlockMethod.Reason).unlockMethodInUse)
        assertEquals(UnlockStep.Wait, lock(UnlockMethod.Reason).unlockStep(now, 3))
    }

    @Test
    fun `no unlocks left over from before waits a few seconds`() {
        // "No unlocks at all" is gone from "How do you unlock?": Strict mode is the only way to have none.
        assertEquals(UnlockMethod.Wait, lock(UnlockMethod.None).unlockMethodInUse)
        assertEquals(UnlockStep.Wait, lock(UnlockMethod.None).unlockStep(now, 3))
        assertEquals(UnlockStep.Wait, lock(UnlockMethod.Strict).unlockStep(now, 3))
        val session = Rule(
            "s", RuleType.Session, "Deep work", apps = listOf(tiktok),
            durationMinutes = 60, activeSince = now - 10 * minute, unlockMethod = UnlockMethod.None,
        )
        assertEquals(UnlockStep.Wait, session.unlockStep(now, 3))
        assertEquals(UnlockStep.Wait, routine(UnlockMethod.None).unlockStep(now, 3, zone))
        // Strict mode still lets emergencies through only.
        assertEquals(UnlockStep.Emergency, session.copy(strictMode = true).unlockStep(now, 3))
        // A habit opens with its tasks, not a wait.
        val habit = Rule("h", RuleType.Habit, "Before TikTok", apps = listOf(tiktok), unlockMethod = UnlockMethod.None)
        assertEquals(UnlockMethod.None, habit.unlockMethodInUse)
    }

    @Test
    fun `the wait lasts five seconds at least`() {
        assertEquals(5, MIN_WAIT_SECONDS)
        assertEquals(5, lock().copy(waitSeconds = 3).waitSecondsInUse)
        assertEquals(5, lock().copy(waitSeconds = 0).waitSecondsInUse)
        assertEquals(10, lock().waitSecondsInUse)
        assertEquals(30, lock().copy(waitSeconds = 30).waitSecondsInUse)
    }

    @Test
    fun `no step once the day's unlocks are used`() {
        assertNull(lock().copy(unlocksUsedToday = 5).unlockStep(now, 3))
        assertEquals(UnlockStep.Wait, lock().copy(unlocksPerDay = null, unlocksUsedToday = 9).unlockStep(now, 3))
    }

    @Test
    fun `a limit that asks for a reason asks for one`() {
        val limit = Rule("m", RuleType.Limit, "Social", apps = listOf(tiktok), limitMinutes = 60, limitAction = LimitAction.AskReason)
        assertEquals(UnlockStep.Reason, limit.unlockStep(now, 3))
        // "When time is up" decides, not an unlock method the limit can't show.
        assertEquals(UnlockMethod.Reason, limit.copy(unlockMethod = UnlockMethod.ScanCode).unlockMethodInUse)
        assertEquals(UnlockStep.Reason, limit.copy(unlockMethod = UnlockMethod.ScanCode).unlockStep(now, 3))
    }

    @Test
    fun `a limit that locks stays locked until it resets`() {
        // Saved with the default unlock method (Wait), which a limit never shows.
        val limit = Rule("m", RuleType.Limit, "Social", apps = listOf(tiktok), limitMinutes = 60, limitAction = LimitAction.Lock)
        assertEquals(UnlockMethod.Wait, limit.unlockMethod)
        assertEquals(UnlockMethod.None, limit.unlockMethodInUse)
        assertFalse(limit.canOpenAnyway(now, zone))
        assertNull(limit.unlockStep(now, emergencyUnlocksLeft = 3, zone = zone))
        UnlockMethod.entries.forEach { method -> assertNull(limit.copy(unlockMethod = method).unlockStep(now, 3, zone)) }
    }

    /** A routine from 9 to 11 AM, so [now] is inside it. */
    private fun routine(method: UnlockMethod, strict: Boolean = false) = Rule(
        "r", RuleType.Bedtime, "Bedtime", apps = listOf(tiktok),
        schedule = RuleSchedule(9 * 60, 11 * 60), strictMode = strict, unlockMethod = method, unlocksPerDay = 5,
    )

    @Test
    fun `a routine opens with the unlock method it shows`() {
        assertEquals(UnlockStep.Retype, routine(UnlockMethod.Retype).unlockStep(now, 3, zone))
        assertEquals(UnlockStep.Scan, routine(UnlockMethod.ScanCode).unlockStep(now, 3, zone))
        assertEquals(UnlockStep.Wait, routine(UnlockMethod.Wait).unlockStep(now, 3, zone))
        assertNull(routine(UnlockMethod.Retype).copy(unlocksUsedToday = 5).unlockStep(now, 3, zone))
    }

    @Test
    fun `a strict routine only lets emergencies through, inside its window`() {
        val strict = routine(UnlockMethod.Retype, strict = true)
        assertFalse(strict.canOpenAnyway(now, zone))
        assertEquals(UnlockStep.Emergency, strict.unlockStep(now, emergencyUnlocksLeft = 1, zone = zone))
        assertNull(strict.unlockStep(now, emergencyUnlocksLeft = 0, zone = zone))
        // Turned off, its unlock method is back.
        assertEquals(UnlockStep.Retype, strict.copy(strictMode = false).unlockStep(now, 0, zone))
    }

    @Test
    fun `strict mode disables the unlock method and keeps it`() {
        val session = Rule("s", RuleType.Session, "Deep work", apps = listOf(tiktok), durationMinutes = 60, unlockMethod = UnlockMethod.Retype)
        assertTrue(session.unlockMethodEnabled)
        assertTrue(routine(UnlockMethod.Wait).unlockMethodEnabled)
        assertTrue(lock().unlockMethodEnabled)

        val strict = session.copy(strictMode = true)
        assertFalse(strict.unlockMethodEnabled)
        assertFalse(routine(UnlockMethod.Wait, strict = true).unlockMethodEnabled)
        // Running or not: Strict mode is on, so no unlock method would ever be asked for.
        assertFalse(strict.copy(activeSince = now - 10 * minute).unlockMethodEnabled)
        // The chosen method isn't lost.
        assertEquals(UnlockMethod.Retype, strict.unlockMethod)
        assertEquals(UnlockMethod.Retype, strict.copy(strictMode = false).unlockMethod)
        assertTrue(strict.copy(strictMode = false).unlockMethodEnabled)
    }

    @Test
    fun `strict mode only lets emergencies through`() {
        val strict = Rule(
            "s", RuleType.Session, "Deep work", apps = listOf(tiktok),
            durationMinutes = 60, activeSince = now - 10 * minute, strictMode = true, unlockMethod = UnlockMethod.Retype,
        )
        assertEquals(UnlockStep.Emergency, strict.unlockStep(now, emergencyUnlocksLeft = 1))
        assertNull(strict.unlockStep(now, emergencyUnlocksLeft = 0))
        // Once the session is over, Strict mode lets go.
        assertEquals(UnlockStep.Retype, strict.copy(activeSince = now - 70 * minute).unlockStep(now, emergencyUnlocksLeft = 0))
    }

    @Test
    fun `a reason needs twenty letters`() {
        assertFalse(UnlockReason.isEnough("Work:  "))
        assertEquals("Work: ", UnlockReason.withSuggestion("", "Work"))
        assertEquals("Reply fast just bored", UnlockReason.withSuggestion("Reply fast ", "Just bored"))
        val reason = "Work: send the deck to Leo"
        assertEquals(26, UnlockReason.length("  $reason \n"))
        assertTrue(UnlockReason.isEnough(reason))
    }

    @Test
    fun `retyping without a typo`() {
        val target = "I know it."
        var retype = Retype()
        for (count in 1..target.length) retype = retype.typed(target.take(count), target)
        assertFalse(retype.hasTypo)
        assertTrue(retype.isDone(target))
        // Backspacing a correct letter is fine.
        assertEquals(Retype("I kno"), Retype("I know").typed("I kno", target))
    }

    @Test
    fun `a typo starts it over`() {
        val target = "I know it."
        val typo = Retype().typed("I", target).typed("I x", target)
        assertTrue(typo.hasTypo)
        assertFalse(typo.isDone(target))
        assertEquals(2, typo.matching(target))
        // More typing is ignored until it starts over.
        assertEquals("I x", typo.typed("I xy", target).typed)
        val again = typo.startOver()
        assertEquals("", again.typed)
        assertFalse(again.hasTypo)
    }

    @Test
    fun `pasting counts as a typo`() {
        assertTrue(Retype().typed("I know it.", "I know it.").hasTypo)
    }

    @Test
    fun `a walk counts from the step counter's first reading`() {
        // The counter adds up every step since the phone started.
        val start = Walk().counted(5_230f)
        assertEquals(5_230f, start.start)
        assertEquals(0, start.steps)
        assertEquals(Walk.STEPS, start.left)
        assertEquals(0f, start.progress, 0.0001f)

        val walking = start.counted(5_242f)
        assertEquals(12, walking.steps)
        assertEquals(8, walking.left)
        assertEquals(0.6f, walking.progress, 0.0001f)
        assertFalse(walking.isDone)
        // The first reading stays the start.
        assertEquals(5_230f, walking.start)
    }

    @Test
    fun `twenty steps and the walk is done`() {
        assertEquals(20, Walk.STEPS)
        val done = Walk().counted(100f).counted(119f).counted(120f)
        assertTrue(done.isDone)
        assertEquals(0, done.left)
        // Steps past the goal don't count.
        assertEquals(20, done.counted(160f).steps)
        assertEquals(1f, done.counted(160f).progress, 0.0001f)
    }

    @Test
    fun `steps never go back`() {
        val walking = Walk().counted(100f).counted(110f)
        // The phone restarted: its counter starts over.
        assertEquals(10, walking.counted(3f).steps)
        assertEquals(10, walking.counted(105f).steps)
    }

    @Test
    fun `a pretend walk steps one at a time`() {
        var walk = Walk()
        repeat(Walk.STEPS + 3) { walk = walk.stepped() }
        assertEquals(Walk.STEPS, walk.steps)
        assertTrue(walk.isDone)
        assertNull(walk.start)
    }

    @Test
    fun `the pass opens its own rule only`() {
        val rule = lock(UnlockMethod.ScanCode)
        val code = UnlockPass.code(rule)
        assertEquals(4, code.length)
        assertEquals(code, UnlockPass.code(rule.copy(name = "Renamed")))
        assertTrue(UnlockPass.opens(rule, UnlockPass.payload(rule)))
        assertTrue(UnlockPass.opens(rule, code))
        assertTrue(UnlockPass.opens(rule, " #$code "))
        assertFalse(UnlockPass.opens(rule, if (code == "1234") "4321" else "1234"))
        val other = rule.copy(id = "other")
        assertFalse(UnlockPass.opens(rule, UnlockPass.payload(other)))
        assertFalse(UnlockPass.opens(rule, "recentr-pass:${rule.id}:0000"))
    }

    @Test
    fun `a camera frame of the printed pass reads back its payload`() {
        val rule = lock(UnlockMethod.ScanCode).copy(id = "5b0c7e1e-6a7b-4d1c-9c55-0f5e4a3b2c1d")
        val modules = UnlockPass.modules(rule)
        // The frame: 4 px a module, a white quiet zone, rows padded like a camera's Y plane.
        val scale = 4
        val quiet = 4
        val side = (modules.size + 2 * quiet) * scale
        val rowStride = side + 16
        val frame = ByteArray(rowStride * side) { 0xFF.toByte() }
        modules.forEachIndexed { y, row ->
            row.forEachIndexed { x, dark ->
                if (dark) {
                    for (dy in 0 until scale) for (dx in 0 until scale) {
                        frame[((y + quiet) * scale + dy) * rowStride + (x + quiet) * scale + dx] = 0
                    }
                }
            }
        }
        assertEquals(UnlockPass.payload(rule), QrReader().read(frame, side, side, rowStride))
        assertNull(QrReader().read(ByteArray(side * side) { 0xFF.toByte() }, side, side))
    }

    @Test
    fun `three emergency unlocks a month`() {
        var settings = AppSettings()
        assertEquals(3, EmergencyUnlocks.left(settings, now, zone))
        settings = EmergencyUnlocks.spend(settings, now, zone)
        settings = EmergencyUnlocks.spend(settings, now + minute, zone)
        assertEquals(1, EmergencyUnlocks.left(settings, now, zone))
        settings = EmergencyUnlocks.spend(settings, now + 2 * minute, zone)
        assertEquals(0, EmergencyUnlocks.left(settings, now, zone))
        // They come back on the 1st of next month.
        val reset = EmergencyUnlocks.resetAt(now, zone)
        assertEquals(at(LocalDate.of(2026, 11, 1), 0), reset)
        assertEquals(0, EmergencyUnlocks.left(settings, reset - 1, zone))
        assertEquals(3, EmergencyUnlocks.left(settings, reset, zone))
        assertEquals(1, EmergencyUnlocks.spend(settings, reset, zone).emergencyUnlocksUsed)
    }

    @Test
    fun `stayed off since the block started`() {
        // A session: since it started.
        val session = Rule("s", RuleType.Session, "S", durationMinutes = 60, activeSince = now - 25 * minute)
        assertEquals(now - 25 * minute, session.blockStart(now, zone))
        // An all-day lock: since midnight.
        assertEquals(at(today, 0), lock().blockStart(now, zone))
        // A night routine at 1 AM: since 11 PM the day before.
        val night = Rule("n", RuleType.Bedtime, "Night", schedule = RuleSchedule(23 * 60, 7 * 60))
        assertEquals(at(today.minusDays(1), 23), night.blockStart(at(today, 1), zone))
        assertEquals(at(today, 23), night.blockStart(at(today, 23, 30), zone))
        // Work hours at 10 AM: since 9.
        val work = Rule("w", RuleType.Lock, "Work", schedule = RuleSchedule(9 * 60, 17 * 60))
        assertEquals(at(today, 9), work.blockStart(now, zone))
        // A limit doesn't know when its budget ran out.
        assertNull(Rule("m", RuleType.Limit, "M", limitMinutes = 30).blockStart(now, zone))
    }
}
