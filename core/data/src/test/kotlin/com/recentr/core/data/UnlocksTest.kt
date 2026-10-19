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
        assertEquals(UnlockStep.Reason, lock(UnlockMethod.Reason).unlockStep(now, 3))
        assertEquals(UnlockStep.Retype, lock(UnlockMethod.Retype).unlockStep(now, 3))
        assertEquals(UnlockStep.Scan, lock(UnlockMethod.ScanCode).unlockStep(now, 3))
        assertNull(lock(UnlockMethod.None).unlockStep(now, 3))
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
