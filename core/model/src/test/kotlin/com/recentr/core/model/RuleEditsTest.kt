package com.recentr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Changing a rule while it's engaged (docs/RULES.md): what applies now, and what waits for its end. */
class RuleEditsTest {

    private val zone = ZoneId.of("Europe/Paris")

    /** [day] Oct 2026 at [hour]:[minutes]; Monday 19 Oct by default. */
    private fun at(hour: Int, minutes: Int = 0, day: Int = 19): Long =
        LocalDate.of(2026, 10, day).atTime(hour, minutes).atZone(zone).toInstant().toEpochMilli()

    private val whatsApp = UsageApp("com.whatsapp", "WhatsApp", AppCategory.Social)
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
    private val youTube = UsageApp("com.google.android.youtube", "YouTube", AppCategory.Entertainment)

    /** On since 9:00 this morning. */
    private val lock = Rule(
        "lock", RuleType.Lock, "Socials",
        apps = listOf(whatsApp, tiktok),
        activeSince = at(9),
        unlocksPerDay = 5,
        unlockDurationMinutes = 5,
        unlocksUsedToday = 2,
        streakDays = 4,
        streakFrom = at(0),
    )

    /** Weekdays 9:00 → 17:00. */
    private val workHours = Rule("work", RuleType.Bedtime, "Work hours", apps = listOf(tiktok), schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()))

    /** Started at 10:00 for 30 minutes. */
    private val session = Rule("s", RuleType.Session, "Deep work", apps = listOf(tiktok), durationMinutes = 30, activeSince = at(10), activeUntil = at(10, 30))

    /** Started at 8:00 with an hour, 20 minutes used. */
    private val limit = Rule("m", RuleType.Limit, "Social time", apps = listOf(tiktok), limitMinutes = 60, usedMinutes = 20, activeSince = at(8))

    private val now = at(12)

    private fun Rule.editedAt(at: Long = now, change: (Rule) -> Rule) = edit(change(latestWish), at, zone)

    // Engagement

    @Test
    fun `each kind is engaged its own way and ends its own way`() {
        assertEquals(at(10, 30), session.engagementEnd(at(10, 10), zone))
        assertNull(session.engagementEnd(at(10, 30), zone))
        assertEquals(at(17), workHours.engagementEnd(at(12), zone))
        assertNull(workHours.engagementEnd(at(18), zone))
        // Inside a pause, a routine isn't engaged.
        assertNull(workHours.copy(pausedUntil = at(13)).engagementEnd(at(12), zone))
        // A lock is on until it's turned off: its changes apply at midnight.
        assertEquals(at(0, day = 20), lock.engagementEnd(at(12), zone))
        assertNull(lock.copy(activeSince = null).engagementEnd(at(12), zone))
        // A limit from Start until midnight, budget spent or not.
        assertEquals(at(0, day = 20), limit.engagementEnd(at(23, 59), zone))
        assertEquals(at(0, day = 20), limit.copy(usedMinutes = 90).engagementEnd(at(12), zone))
        assertNull(limit.engagementEnd(at(0, day = 20), zone))
        assertNull(limit.copy(activeSince = null).engagementEnd(at(12), zone))
    }

    @Test
    fun `an overnight routine's window ends the next morning`() {
        val bedtime = workHours.copy(schedule = RuleSchedule(22 * 60, 7 * 60))
        assertEquals(at(7, day = 20), bedtime.engagementEnd(at(23), zone))
        assertEquals(at(7, day = 20), bedtime.engagementEnd(at(6, day = 20), zone))
        // A whole day ends at midnight.
        val weekend = workHours.copy(schedule = RuleSchedule(0, 0, setOf(6, 7)))
        assertEquals(at(0, day = 25), weekend.engagementEnd(at(15, day = 24), zone))
    }

    // Not engaged

    @Test
    fun `a rule that isn't engaged takes every change now and drops what was kept`() {
        val off = lock.copy(activeSince = null, pending = RuleChange(at(0, day = 20), lock.copy(unlocksPerDay = 10)))
        val edit = off.edit(off.copy(apps = listOf(whatsApp), unlocksPerDay = null, name = "Chats"), now, zone)
        assertEquals(listOf(whatsApp), edit.now.apps)
        assertNull(edit.now.unlocksPerDay)
        assertEquals("Chats", edit.now.name)
        assertNull(edit.later)
        assertNull(edit.now.pending)
        assertFalse(edit.blocksMore)
    }

    @Test
    fun `what the rule used and counted stays its own`() {
        val stale = lock.copy(activeSince = null, unlocksUsedToday = 0, streakDays = 0, streakFrom = null)
        val off = lock.copy(activeSince = null)
        val edit = off.edit(stale.copy(name = "Chats"), now, zone)
        assertEquals(2, edit.now.unlocksUsedToday)
        assertEquals(4, edit.now.streakDays)
        assertEquals(at(0), edit.now.streakFrom)
    }

    // Presentation

    @Test
    fun `name, sticker, note and reminder apply right away`() {
        val edit = lock.editedAt { it.copy(name = "Chats", sticker = RuleSticker.Note, note = "Read instead", reminderMinutesBefore = 10) }
        assertEquals("Chats", edit.now.name)
        assertEquals(RuleSticker.Note, edit.now.sticker)
        assertEquals("Read instead", edit.now.note)
        assertEquals(10, edit.now.reminderMinutesBefore)
        assertNull(edit.later)
        assertNull(edit.now.pending)
    }

    // Apps

    @Test
    fun `adding apps applies now, after asking`() {
        val edit = lock.editedAt { it.copy(apps = it.apps + youTube) }
        assertEquals(listOf(whatsApp, tiktok, youTube), edit.now.apps)
        assertNull(edit.later)
        assertTrue(edit.blocksMore)
        // The same apps in another order block nothing more.
        val reordered = lock.editedAt { it.copy(apps = listOf(tiktok, whatsApp)) }
        assertEquals(listOf(tiktok, whatsApp), reordered.now.apps)
        assertFalse(reordered.blocksMore)
    }

    @Test
    fun `removing an app keeps the whole choice for later`() {
        val edit = lock.editedAt { it.copy(apps = listOf(whatsApp, youTube)) }
        assertEquals(lock.apps, edit.now.apps)
        assertEquals(listOf(whatsApp, youTube), edit.later?.apps)
        assertEquals(RuleChange(at(0, day = 20), edit.later), edit.now.pending)
        assertFalse(edit.blocksMore)
    }

    @Test
    fun `adding apps to a limit with time left blocks nothing yet`() {
        val edit = limit.editedAt { it.copy(apps = it.apps + youTube) }
        assertEquals(listOf(tiktok, youTube), edit.now.apps)
        assertFalse(edit.blocksMore)
        assertTrue(limit.copy(usedMinutes = 60).editedAt { it.copy(apps = it.apps + youTube) }.blocksMore)
    }

    // Days and hours

    @Test
    fun `a wider routine window applies now, a narrower one at the end of the window`() {
        val wider = workHours.editedAt { it.copy(schedule = RuleSchedule(8 * 60, 18 * 60, (1..6).toSet())) }
        assertEquals(RuleSchedule(8 * 60, 18 * 60, (1..6).toSet()), wider.now.schedule)
        assertNull(wider.later)

        val narrower = workHours.editedAt { it.copy(schedule = RuleSchedule(10 * 60, 17 * 60, (1..5).toSet())) }
        assertEquals(workHours.schedule, narrower.now.schedule)
        assertEquals(at(17), narrower.now.pending?.appliesAt)

        val dayOff = workHours.editedAt { it.copy(schedule = RuleSchedule(9 * 60, 17 * 60, (1..4).toSet())) }
        assertEquals(workHours.schedule, dayOff.now.schedule)
        assertEquals(RuleSchedule(9 * 60, 17 * 60, (1..4).toSet()), dayOff.later?.schedule)
    }

    @Test
    fun `a window covers another minute by minute, overnight ones from their start day`() {
        val night = RuleSchedule(22 * 60, 7 * 60, setOf(7))
        // Sunday 22:00 runs into Monday 7:00.
        assertTrue(RuleSchedule(0, 8 * 60, setOf(1)).covers(RuleSchedule(0, 7 * 60, setOf(1))))
        assertTrue(RuleSchedule(21 * 60, 8 * 60, setOf(7)).covers(night))
        assertFalse(RuleSchedule(23 * 60, 7 * 60, setOf(7)).covers(night))
        // A whole Sunday and Monday morning cover it too.
        assertTrue(RuleSchedule(0, 0, setOf(7)).covers(RuleSchedule(22 * 60, 23 * 60, setOf(7))))
        assertFalse(RuleSchedule(0, 0, setOf(7)).covers(night))
        assertTrue(RuleSchedule(22 * 60, 7 * 60, (1..7).toSet()).covers(night))
    }

    // Session length

    @Test
    fun `a running session never gets shorter, a longer length makes it last longer`() {
        val shorter = session.editedAt(at(10, 10)) { it.copy(durationMinutes = 15) }
        assertEquals(15, shorter.now.durationMinutes)
        assertEquals(at(10, 30), shorter.now.sessionEndsAt)
        assertNull(shorter.later)

        val longer = session.editedAt(at(10, 10)) { it.copy(durationMinutes = 60) }
        assertEquals(at(11), longer.now.sessionEndsAt)
        assertNull(longer.later)
        // A session started before its end was kept: same rules.
        assertEquals(at(10, 30), session.copy(activeUntil = null).editedAt(at(10, 10)) { it.copy(durationMinutes = 15) }.now.sessionEndsAt)
    }

    // Limit budget

    @Test
    fun `a smaller budget applies now, a bigger one at midnight`() {
        val smaller = limit.editedAt { it.copy(limitMinutes = 45) }
        assertEquals(45, smaller.now.limitMinutes)
        assertFalse(smaller.blocksMore)

        val bigger = limit.editedAt { it.copy(limitMinutes = 90) }
        assertEquals(60, bigger.now.limitMinutes)
        assertEquals(90, bigger.later?.limitMinutes)
        assertEquals(at(0, day = 20), bigger.now.pending?.appliesAt)
    }

    @Test
    fun `a budget lowered to what's already used blocks right away, after asking`() {
        assertTrue(limit.editedAt { it.copy(limitMinutes = 15) }.blocksMore)
        assertTrue(limit.editedAt { it.copy(limitMinutes = 20) }.blocksMore)
        // Spent already: nothing more is blocked.
        assertFalse(limit.copy(usedMinutes = 75).editedAt { it.copy(limitMinutes = 15) }.blocksMore)
    }

    // Unlocks

    @Test
    fun `a harder unlock method or a longer wait applies now`() {
        val retype = lock.editedAt { it.copy(unlockMethod = UnlockMethod.Retype) }
        assertEquals(UnlockMethod.Retype, retype.now.unlockMethod)
        assertNull(retype.later)
        val longerWait = lock.editedAt { it.copy(waitSeconds = 30) }
        assertEquals(30, longerWait.now.waitSeconds)
        assertNull(longerWait.later)
        val scan = lock.copy(unlockMethod = UnlockMethod.Retype).editedAt { it.copy(unlockMethod = UnlockMethod.ScanCode) }
        assertEquals(UnlockMethod.ScanCode, scan.now.unlockMethod)
    }

    @Test
    fun `a lighter unlock method or a shorter wait waits, as one setting`() {
        val lighter = lock.copy(unlockMethod = UnlockMethod.ScanCode).editedAt { it.copy(unlockMethod = UnlockMethod.Wait, waitSeconds = 60) }
        assertEquals(UnlockMethod.ScanCode, lighter.now.unlockMethod)
        assertEquals(10, lighter.now.waitSeconds)
        assertEquals(UnlockMethod.Wait, lighter.later?.unlockMethod)
        val shorterWait = lock.copy(waitSeconds = 30).editedAt { it.copy(waitSeconds = 5) }
        assertEquals(30, shorterWait.now.waitSeconds)
        assertEquals(5, shorterWait.later?.waitSeconds)
    }

    @Test
    fun `fewer and shorter unlocks apply now, more and longer ones at midnight`() {
        assertEquals(3, lock.editedAt { it.copy(unlocksPerDay = 3) }.now.unlocksPerDay)
        val more = lock.editedAt { it.copy(unlocksPerDay = 10) }
        assertEquals(5, more.now.unlocksPerDay)
        assertEquals(10, more.later?.unlocksPerDay)
        // Unlimited is the most.
        assertEquals(5, lock.editedAt { it.copy(unlocksPerDay = null) }.now.unlocksPerDay)
        assertEquals(5, lock.copy(unlocksPerDay = null).editedAt { it.copy(unlocksPerDay = 5) }.now.unlocksPerDay)

        assertEquals(1, lock.editedAt { it.copy(unlockDurationMinutes = 1) }.now.unlockDurationMinutes)
        val longer = lock.editedAt { it.copy(unlockDurationMinutes = 15) }
        assertEquals(5, longer.now.unlockDurationMinutes)
        assertEquals(15, longer.later?.unlockDurationMinutes)
    }

    @Test
    fun `an edit that reinforces and loosens applies its reinforcing parts now and the rest later`() {
        val edit = lock.editedAt { it.copy(unlocksPerDay = 3, unlockDurationMinutes = 15, name = "Chats") }
        assertEquals(3, edit.now.unlocksPerDay)
        assertEquals(5, edit.now.unlockDurationMinutes)
        assertEquals("Chats", edit.now.name)
        // The whole draft is kept.
        assertEquals(3, edit.later?.unlocksPerDay)
        assertEquals(15, edit.later?.unlockDurationMinutes)
    }

    // Strict mode

    @Test
    fun `strict mode turns on now and off when the session ends`() {
        val on = session.editedAt(at(10, 10)) { it.copy(strictMode = true) }
        assertTrue(on.now.strictMode)
        assertNull(on.later)
        val strict = session.copy(strictMode = true)
        val off = strict.editedAt(at(10, 10)) { it.copy(strictMode = false) }
        assertTrue(off.now.strictMode)
        assertEquals(RuleChange(at(10, 30), off.later), off.now.pending)
        assertFalse(off.later!!.strictMode)
    }

    // Pause, turn off, delete

    @Test
    fun `a routine paused during its window pauses when the window ends`() {
        val edit = workHours.editedAt { it.copy(pausedUntil = Long.MAX_VALUE) }
        assertNull(edit.now.pausedUntil)
        assertEquals(at(17), edit.now.pending?.appliesAt)
        assertEquals(Long.MAX_VALUE, edit.later?.pausedUntil)
        // Outside its window, right away; resuming always applies now.
        val evening = workHours.editedAt(at(18)) { it.copy(pausedUntil = Long.MAX_VALUE) }
        assertEquals(Long.MAX_VALUE, evening.now.pausedUntil)
        val paused = workHours.copy(pausedUntil = Long.MAX_VALUE)
        assertNull(paused.editedAt { it.copy(pausedUntil = null) }.now.pausedUntil)
    }

    @Test
    fun `a lock turned off stays on until midnight`() {
        val edit = lock.editedAt { it.copy(activeSince = null) }
        assertEquals(at(9), edit.now.activeSince)
        assertEquals(at(0, day = 20), edit.now.pending?.appliesAt)
        val applied = edit.now.applying(edit.now.pending!!)!!
        assertNull(applied.activeSince)
        assertEquals(2, applied.unlocksUsedToday)
        assertEquals(4, applied.streakDays)
    }

    @Test
    fun `a kept change applies on the rule as it is then`() {
        val edit = lock.editedAt { it.copy(unlocksPerDay = 10) }
        val later = edit.now.copy(unlocksUsedToday = 4, streakDays = 5)
        val applied = later.applying(later.pending!!)!!
        assertEquals(10, applied.unlocksPerDay)
        assertEquals(4, applied.unlocksUsedToday)
        assertEquals(5, applied.streakDays)
        assertEquals(at(9), applied.activeSince)
        assertNull(applied.pending)
    }

    @Test
    fun `deleting an engaged rule waits for the end of its engagement`() {
        val kept = lock.deleted(now, zone)!!
        assertEquals(RuleChange(at(0, day = 20), null), kept.pending)
        assertNull(kept.applying(kept.pending!!))
        assertNull(lock.copy(activeSince = null).deleted(now, zone))
        assertEquals(at(10, 30), session.deleted(at(10, 10), zone)?.pending?.appliesAt)
        // Editing it meanwhile keeps the delete.
        assertEquals(kept.pending, kept.editedAt { it.copy(unlocksPerDay = 10) }.now.pending)
        assertEquals(kept.pending, kept.editedAt { it.copy(name = "Chats") }.now.pending)
    }

    @Test
    fun `editing again starts from the kept change and replaces it`() {
        val first = lock.editedAt { it.copy(unlocksPerDay = 10) }.now
        assertEquals(10, first.latestWish.unlocksPerDay)
        // The editor opens on the kept change; this draft no longer loosens anything.
        val second = first.editedAt { it.copy(unlocksPerDay = 5, name = "Chats") }
        assertNull(second.now.pending)
        assertEquals("Chats", second.now.name)
        // Or it loosens something else: the new draft replaces the kept one.
        val third = first.editedAt { it.copy(unlockDurationMinutes = 15) }
        assertEquals(10, third.later?.unlocksPerDay)
        assertEquals(15, third.now.pending?.rule?.unlockDurationMinutes)
    }

    @Test
    fun `a session holds while it runs, a limit once its budget is spent`() {
        assertTrue(session.isHolding(at(10, 10), zone))
        assertFalse(session.isHolding(at(10, 30), zone))
        assertFalse(limit.isHolding(now, zone))
        assertTrue(limit.copy(usedMinutes = 60).isHolding(now, zone))
        assertTrue(lock.isHolding(now, zone))
        assertFalse(lock.copy(activeSince = null).isHolding(now, zone))
    }

    // Settings › Change locks & limits anytime

    @Test
    fun `with changes anytime, a lock that's on changes right away`() {
        val off = lock.edit(lock.copy(activeSince = null), now, zone, changesAnytime = true)
        assertNull(off.now.activeSince)
        assertNull(off.later)
        assertNull(off.now.pending)
        val easier = lock.edit(lock.copy(unlocksPerDay = 10, apps = listOf(whatsApp)), now, zone, changesAnytime = true)
        assertEquals(10, easier.now.unlocksPerDay)
        assertEquals(listOf(whatsApp), easier.now.apps)
        assertNull(easier.later)
        assertEquals(at(9), easier.now.activeSince)
        assertEquals(2, easier.now.unlocksUsedToday)
        // Blocking doesn't change: adding apps to a lock that's on still asks first.
        assertTrue(lock.edit(lock.copy(apps = lock.apps + youTube), now, zone, changesAnytime = true).blocksMore)
    }

    @Test
    fun `with changes anytime, a started limit's bigger budget applies now`() {
        val bigger = limit.edit(limit.copy(limitMinutes = 90), now, zone, changesAnytime = true)
        assertEquals(90, bigger.now.limitMinutes)
        assertNull(bigger.later)
        assertNull(bigger.now.pending)
        // Still started, with what it used.
        assertEquals(at(8), bigger.now.activeSince)
        assertEquals(20, bigger.now.usedMinutes)
        assertTrue(limit.edit(limit.copy(limitMinutes = 15), now, zone, changesAnytime = true).blocksMore)
    }

    @Test
    fun `with changes anytime, a lock or a limit is deleted right away`() {
        assertNull(lock.changesWaitUntil(now, zone, changesAnytime = true))
        assertNull(lock.deleted(now, zone, changesAnytime = true))
        assertNull(limit.deleted(now, zone, changesAnytime = true))
        // Off, the setting changes nothing.
        assertNotNull(lock.deleted(now, zone))
        assertEquals(at(0, day = 20), lock.changesWaitUntil(now, zone))
    }

    @Test
    fun `changes anytime leaves sessions and routines waiting`() {
        val narrower = workHours.edit(workHours.copy(schedule = RuleSchedule(10 * 60, 17 * 60, (1..5).toSet())), now, zone, changesAnytime = true)
        assertEquals(workHours.schedule, narrower.now.schedule)
        assertEquals(at(17), narrower.now.pending?.appliesAt)
        assertEquals(at(17), workHours.deleted(now, zone, changesAnytime = true)?.pending?.appliesAt)
        val strict = session.copy(strictMode = true)
        val off = strict.edit(strict.copy(strictMode = false), at(10, 10), zone, changesAnytime = true)
        assertTrue(off.now.strictMode)
        assertEquals(at(10, 30), off.now.pending?.appliesAt)
    }
}
