package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleChange
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule as JUnitRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.ZoneId

class RulesRepositoryTest {

    @get:JUnitRule
    val folder = TemporaryFolder()

    private val zone = ZoneId.of("Europe/Paris")
    /** Sunday 18 Oct 2026, 11:00. */
    private val now = LocalDate.of(2026, 10, 18).atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
    private val day = 24 * 3_600_000L
    private val whatsApp = UsageApp("com.whatsapp", "WhatsApp", AppCategory.Social)
    private val tiktok = UsageApp("com.zhiliaoapp.musically", "TikTok", AppCategory.Social)
    private val sunday = LocalDate.of(2026, 10, 18)
    private val monday = sunday.plusDays(1)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun repository(at: Long = now) =
        RulesRepository(folder.root.resolve("rules.json"), zone = { zone }, now = at, writeDispatcher = Dispatchers.Unconfined, clock = { at })

    private val file get() = folder.root.resolve("rules.json")

    /** [minutes] per package since [start]. */
    private fun used(start: Long, vararg minutes: Pair<String, Int>) = LimitUsage(mapOf(start to mapOf(*minutes)))

    private val lock = Rule("l", RuleType.Lock, "Socials", apps = listOf(whatsApp, tiktok), unlocksPerDay = 5)

    /** [lock] created and activated at [now] (Sunday 11:00). */
    private fun RulesRepository.activeLock(): Rule {
        save(lock)
        start("l", now = now)
        return rule("l")!!
    }

    @Test
    fun `starts empty without a saved file`() {
        val repo = repository()
        assertTrue(repo.rules.value.isEmpty())
    }

    @Test
    fun `rules survive a restart, with what they keep for later`() {
        val rule = Rule(
            id = "r",
            type = RuleType.Bedtime,
            name = "Work Hours",
            apps = listOf(whatsApp),
            schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()),
            pausedUntil = Long.MAX_VALUE,
            streakDays = 4,
            streakFrom = now - day,
            pending = RuleChange(now + day, Rule("r", RuleType.Bedtime, "Work Hours", apps = emptyList())),
        )
        repository().upsert(rule)

        val reopened = repository()
        assertEquals(listOf(rule), reopened.rules.value)
        assertTrue(reopened.rule("r")!!.targets("com.whatsapp"))
        assertTrue(file.readText().contains("\"version\":1"))
    }

    @Test
    fun `a file saved with templates still opens`() {
        file.writeText(
            """{"rules":[{"id":"r","type":"Lock","name":"WhatsApp"}],"templates":[{"id":"saved-1","type":"Session","name":"Old"}]}""",
        )
        assertEquals(listOf("r"), repository().rules.value.map { it.id })
    }

    @Test
    fun `deleting a rule that isn't engaged and erasing are saved too`() {
        repository().apply {
            upsert(Rule("a", RuleType.Lock, "A"))
            upsert(Rule("b", RuleType.Lock, "B"))
            assertTrue(delete("a"))
        }
        assertEquals(listOf("b"), repository().rules.value.map { it.id })

        repository().reset()
        assertTrue(repository().rules.value.isEmpty())
    }

    @Test
    fun `the unlocks used start over on a new day`() {
        val repo = repository()
        repo.upsert(lock.copy(activeSince = now, unlocksUsedToday = 3))
        repo.settle(now + 60_000)
        assertEquals(3, repo.rule("l")!!.unlocksUsedToday)

        repo.settle(now + day)
        assertEquals(0, repository(now + day).rule("l")!!.unlocksUsedToday)
    }

    @Test
    fun `a file with only the counters' day still starts the day over`() {
        file.writeText(
            """{"rules":[{"id":"l","type":"Lock","name":"L","unlocksUsedToday":4}],"countersDay":${sunday.toEpochDay()}}""",
        )
        val repo = repository(at(monday, 9, 0))
        repo.settle(at(sunday, 23, 0))
        assertEquals(4, repo.rule("l")!!.unlocksUsedToday)
        repo.settle(at(monday, 9, 0))
        assertEquals(0, repo.rule("l")!!.unlocksUsedToday)
    }

    // Start, Activate

    @Test
    fun `creating a rule only saves it`() {
        val repo = repository()
        val edit = repo.save(Rule("s", RuleType.Session, "Focus", apps = listOf(tiktok), durationMinutes = 30))
        assertNull(edit.later)
        assertNull(repo.rule("s")!!.activeSince)
        assertFalse(repo.rule("s")!!.isRunning(now))
        assertEquals(now, repo.rule("s")!!.streakFrom)
    }

    @Test
    fun `a session runs from Start for its length, then stays saved`() {
        val repo = repository()
        repo.upsert(Rule("s", RuleType.Session, "Focus", durationMinutes = 30))
        repo.start("s", now = now)
        assertTrue(repo.rule("s")!!.isRunning(now + 60_000))
        assertEquals(now + 30 * 60_000, repo.rule("s")!!.sessionEndsAt)
        repo.settle(now + 31 * 60_000)
        val ended = repo.rule("s")!!
        assertNull(ended.activeSince)
        assertNull(ended.activeUntil)
        assertFalse(ended.isRunning(now + 31 * 60_000))
        // Its run counts for its streak, and it's still there after a restart.
        assertEquals(1, ended.streakDays)
        assertEquals("Focus", repository(now + 32 * 60_000).rule("s")!!.name)

        val later = now + 3 * 3_600_000L
        repo.start("s", now = later)
        assertEquals(later + 30 * 60_000, repo.rule("s")!!.sessionEndsAt)
        repo.settle(later + 30 * 60_000)
        assertFalse(repo.rule("s")!!.isRunning(later + 30 * 60_000))
        // A second run the same day doesn't add a day to the streak.
        assertEquals(1, repo.rule("s")!!.streakDays)
    }

    @Test
    fun `a lock is on from Activate and stays on`() {
        val repo = repository()
        repo.save(lock)
        assertFalse(repo.rule("l")!!.isEnforcing(now, zone))
        repo.start("l", now = now)
        assertEquals(now, repo.rule("l")!!.activeSince)
        repo.settle(now + 3 * day)
        assertTrue(repo.rule("l")!!.isEnforcing(now + 3 * day, zone))
        // Activating it again keeps it on since then.
        repo.start("l", now = now + day)
        assertEquals(now, repo.rule("l")!!.activeSince)
    }

    @Test
    fun `turning a lock off takes effect at midnight`() {
        val repo = repository()
        val edit = repo.save(repo.activeLock().copy(activeSince = null))
        assertNotNull(edit.later)
        assertTrue(repo.rule("l")!!.isEnforcing(at(sunday, 23, 59), zone))
        assertEquals(RuleChange(at(monday, 0), edit.later), repo.rule("l")!!.pending)
        repo.settle(at(sunday, 23, 59))
        assertNotNull(repo.rule("l")!!.activeSince)
        repo.settle(at(monday, 0))
        val off = repo.rule("l")!!
        assertNull(off.activeSince)
        assertNull(off.pending)
        assertFalse(off.isEnforcing(at(monday, 0), zone))
        // The day it was on counted.
        assertEquals(1, off.streakDays)
    }

    @Test
    fun `keep it on drops the turn-off`() {
        val repo = repository()
        repo.save(repo.activeLock().copy(activeSince = null))
        repo.dropPending("l")
        repo.settle(at(monday, 0))
        assertEquals(now, repo.rule("l")!!.activeSince)
    }

    // Limits

    @Test
    fun `a limit counts its apps' use from Start, then ends at midnight and waits for the next Start`() {
        val youTube = UsageApp("com.google.android.youtube", "YouTube", AppCategory.Entertainment)
        val repo = repository()
        repo.save(Rule("m", RuleType.Limit, "Limit", apps = listOf(youTube, whatsApp), limitMinutes = 60))
        repo.upsert(lock.copy(apps = listOf(whatsApp)))
        // Not started: nothing is counted.
        repo.recordUsage(used(now, youTube.packageName to 40))
        assertEquals(0, repo.rule("m")!!.usedMinutes)

        repo.start("m", now = now)
        assertTrue(repo.rule("m")!!.isEnforcing(now, zone))
        repo.recordUsage(used(now, youTube.packageName to 4, whatsApp.packageName to 3, "com.other" to 50))
        assertEquals(7, repo.rule("m")!!.usedMinutes)
        // Only limits count their apps' use, and it's saved.
        assertEquals(0, repo.rule("l")!!.usedMinutes)
        assertEquals(7, repository().rule("m")!!.usedMinutes)
        // The next reading replaces the last one; another Start's isn't this one's.
        repo.recordUsage(used(now, youTube.packageName to 70))
        assertEquals(70, repo.rule("m")!!.usedMinutes)
        assertTrue(repo.rule("m")!!.isHolding(at(sunday, 23, 59), zone))
        repo.recordUsage(used(now - 1, youTube.packageName to 5))
        assertEquals(70, repo.rule("m")!!.usedMinutes)
        // Started today, it isn't started again: its budget doesn't start over.
        repo.start("m", now = at(sunday, 20))
        assertEquals(now, repo.rule("m")!!.activeSince)
        assertEquals(70, repo.rule("m")!!.usedMinutes)

        repo.settle(at(sunday, 23, 59))
        assertEquals(now, repo.rule("m")!!.activeSince)
        repo.settle(at(monday, 0))
        val ended = repo.rule("m")!!
        assertNull(ended.activeSince)
        assertEquals(0, ended.usedMinutes)
        assertFalse(ended.isEnforcing(at(monday, 9), zone))
        // It doesn't start again on its own.
        repo.settle(at(monday.plusDays(1), 9))
        assertNull(repo.rule("m")!!.activeSince)
    }

    @Test
    fun `a limit's bigger budget waits for midnight, for the next Start`() {
        val repo = repository()
        repo.save(Rule("m", RuleType.Limit, "Limit", apps = listOf(whatsApp), limitMinutes = 60))
        repo.start("m", now = now)
        repo.save(repo.rule("m")!!.copy(limitMinutes = 120))
        assertEquals(60, repo.rule("m")!!.limitMinutes)
        repo.settle(at(monday, 0))
        assertEquals(120, repo.rule("m")!!.limitMinutes)
        assertNull(repo.rule("m")!!.activeSince)
    }

    // Changes while engaged

    @Test
    fun `what loosens an engaged rule waits, what reinforces it applies now`() {
        val repo = repository()
        val edit = repo.save(repo.activeLock().copy(apps = listOf(whatsApp), unlocksPerDay = 3, name = "Chats"))
        val saved = repo.rule("l")!!
        assertEquals("Chats", saved.name)
        assertEquals(3, saved.unlocksPerDay)
        assertEquals(listOf(whatsApp, tiktok), saved.apps)
        assertEquals(listOf(whatsApp), edit.later?.apps)
        // Kept across a restart, applied at midnight on what the rule counted since.
        repo.update("l") { it.copy(unlocksUsedToday = 2) }
        val reopened = repository(at(sunday, 22))
        assertEquals(saved.pending, reopened.rule("l")!!.pending)
        reopened.settle(at(monday, 0, 1))
        val applied = reopened.rule("l")!!
        assertEquals(listOf(whatsApp), applied.apps)
        assertEquals(3, applied.unlocksPerDay)
        assertNull(applied.pending)
        assertEquals(now, applied.activeSince)
    }

    @Test
    fun `a running session's kept change applies when it ends`() {
        val repo = repository()
        repo.upsert(Rule("s", RuleType.Session, "Focus", apps = listOf(tiktok), durationMinutes = 30, strictMode = true))
        repo.start("s", now = now)
        repo.save(repo.rule("s")!!.copy(strictMode = false, durationMinutes = 15))
        assertTrue(repo.rule("s")!!.strictMode)
        assertEquals(15, repo.rule("s")!!.durationMinutes)
        assertEquals(now + 30 * 60_000, repo.rule("s")!!.sessionEndsAt)
        repo.settle(now + 30 * 60_000)
        val ended = repo.rule("s")!!
        assertFalse(ended.strictMode)
        assertNull(ended.activeSince)
        assertEquals(1, ended.streakDays)
    }

    @Test
    fun `deleting an engaged rule waits for the end of its engagement`() {
        val repo = repository()
        repo.activeLock()
        assertFalse(repo.delete("l"))
        assertEquals(RuleChange(at(monday, 0), null), repo.rule("l")!!.pending)
        repo.settle(at(sunday, 23, 59))
        assertNotNull(repo.rule("l"))
        repo.settle(at(monday, 0))
        assertNull(repo.rule("l"))
        assertNull(repository(at(monday, 1)).rule("l"))
    }

    @Test
    fun `keep rule drops a kept delete`() {
        val repo = repository()
        repo.activeLock()
        repo.delete("l")
        repo.dropPending("l")
        repo.settle(at(monday, 0))
        assertNotNull(repo.rule("l"))
    }

    // Migration

    @Test
    fun `rules made before the four kinds are migrated once`() {
        file.writeText(
            """
            {"rules":[
              {"id":"habit","type":"Habit","name":"Read first","habitTasks":[{"id":"t","title":"Read","done":true}],"habitRepeat":"Weekdays"},
              {"id":"work","type":"Session","name":"Work","schedule":{"startMinute":540,"endMinute":1020,"days":[1,2,3,4,5]},"sticker":"Bolt","unlockMethod":"None","unlocksPerDay":null,"reminderMinutesBefore":10},
              {"id":"night","type":"Lock","name":"Nights","schedule":{"startMinute":1320,"endMinute":420},"strictMode":true},
              {"id":"focus","type":"Session","name":"Focus","durationMinutes":30,"pausedUntil":9223372036854775807,"unlockMethod":"Strict","oneShot":true},
              {"id":"socials","type":"Lock","name":"Socials","pausedUntil":${now + 3_600_000},"strictMode":true,"unlockMethod":"Reason","unlocksPerDay":3},
              {"id":"off","type":"Lock","name":"Off","pausedUntil":9223372036854775807},
              {"id":"limit","type":"Limit","name":"Daily limit","limitMinutes":60,"usedMinutes":45,"limitReset":"FourAm","limitAction":"AskReason","strictMode":true,"activeSince":${now - 3_600_000}},
              {"id":"bedtime","type":"Bedtime","name":"Bedtime","schedule":{"startMinute":1380,"endMinute":420},"pausedUntil":9223372036854775807,"strictMode":true}
            ],"settledAt":$now}
            """.trimIndent(),
        )
        val repo = repository()
        val rules = repo.rules.value.associateBy { it.id }
        // Habits are gone.
        assertEquals(listOf("work", "night", "focus", "socials", "off", "limit", "bedtime"), repo.rules.value.map { it.id })

        // A session with a weekly schedule and a lock with hours become routines, keeping their sticker.
        val work = rules.getValue("work")
        assertEquals(RuleType.Bedtime, work.type)
        assertEquals(RuleSticker.Bolt, work.sticker)
        assertEquals(RuleSchedule(540, 1020, (1..5).toSet()), work.schedule)
        assertEquals(10, work.reminderMinutesBefore)
        // "No unlocks" becomes the default: wait, five unlocks a day.
        assertEquals(UnlockMethod.Wait, work.unlockMethod)
        assertEquals(5, work.unlocksPerDay)
        val night = rules.getValue("night")
        assertEquals(RuleType.Bedtime, night.type)
        assertEquals(RuleSticker.Lock, night.sticker)
        assertTrue(night.strictMode)

        // A session without one stays a session; only routines pause.
        val focus = rules.getValue("focus")
        assertEquals(RuleType.Session, focus.type)
        assertNull(focus.pausedUntil)
        assertEquals(UnlockMethod.Wait, focus.unlockMethod)

        // A lock without hours stays on from now, paused or not, unless it was paused until turned back on.
        val socials = rules.getValue("socials")
        assertEquals(now, socials.activeSince)
        assertNull(socials.pausedUntil)
        assertFalse(socials.strictMode)
        assertEquals(UnlockMethod.Wait, socials.unlockMethod)
        assertEquals(3, socials.unlocksPerDay)
        assertNull(rules.getValue("off").activeSince)
        assertNull(rules.getValue("off").pausedUntil)

        // A limit waits for Start, with no Strict mode.
        val limit = rules.getValue("limit")
        assertNull(limit.activeSince)
        assertEquals(0, limit.usedMinutes)
        assertFalse(limit.strictMode)
        assertEquals(60, limit.limitMinutes)

        // A routine keeps its pause and Strict mode.
        val bedtime = rules.getValue("bedtime")
        assertEquals(Long.MAX_VALUE, bedtime.pausedUntil)
        assertTrue(bedtime.strictMode)

        // Saved as they are now: read again later, nothing is migrated twice.
        val text = file.readText()
        assertTrue(text.contains("\"version\":1"))
        assertFalse(text.contains("limitReset"))
        assertFalse(text.contains("Habit"))
        assertEquals(now, repository(now + day).rule("socials")!!.activeSince)
    }

    @Test
    fun `a migrated file keeps its rules when they can't all be read`() {
        // An unknown unlock method left over from before decodes, migrated to a wait.
        file.writeText("""{"rules":[{"id":"l","type":"Lock","name":"L","unlockMethod":"None"}]}""")
        assertEquals(UnlockMethod.Wait, repository().rule("l")!!.unlockMethod)
    }

    // Streaks

    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)

    /** Weekdays 6:00 → 9:00. */
    private val morning = Rule(
        "m",
        RuleType.Bedtime,
        "Morning Routine",
        apps = listOf(whatsApp),
        schedule = RuleSchedule(6 * 60, 9 * 60, (1..5).toSet()),
    )

    @Test
    fun `a routine saved before streaks counts this morning's window once it's over`() {
        // The 6:00 → 9:00 routine ran this morning, but its card still read "0 days".
        file.writeText(
            """{"rules":[{"id":"m","type":"Bedtime","name":"Morning Routine",""" +
                """"schedule":{"startMinute":360,"endMinute":540,"days":[1,2,3,4,5]}}],"settledAt":${at(monday, 5)}}""",
        )
        val repo = repository(at(monday, 5))
        repo.settle(at(monday, 8))
        assertEquals(0, repo.rule("m")!!.streakDays)
        repo.settle(at(monday, 9, 1))
        assertEquals(1, repo.rule("m")!!.streakDays)
        // Saved, and counted once.
        assertEquals(1, repository(at(monday, 10)).apply { settle(at(monday, 23)) }.rule("m")!!.streakDays)
        repo.settle(at(tuesday, 9, 1))
        assertEquals(2, repo.rule("m")!!.streakDays)
    }

    @Test
    fun `a new rule counts its windows from when it was made`() {
        val repo = repository(at(monday, 7))
        repo.save(morning)
        assertEquals(at(monday, 7), repo.rule("m")!!.streakFrom)
        repo.settle(at(monday, 9, 30))
        assertEquals(0, repo.rule("m")!!.streakDays)
        repo.settle(at(tuesday, 9, 30))
        assertEquals(1, repo.rule("m")!!.streakDays)
    }

    @Test
    fun `editing a rule keeps its streak`() {
        val repo = repository(at(monday, 0))
        repo.upsert(morning)
        repo.settle(at(monday, 9, 30))
        repo.save(repo.rule("m")!!.copy(name = "Mornings", schedule = RuleSchedule(6 * 60, 10 * 60, (1..5).toSet())), now = at(monday, 9, 30))
        repo.settle(at(tuesday, 10, 30))
        assertEquals(2, repo.rule("m")!!.streakDays)
    }

    @Test
    fun `pausing over a window breaks the streak and resuming counts again`() {
        val repo = repository(at(monday, 0))
        repo.upsert(morning.copy(streakDays = 4))
        repo.pause("m", Long.MAX_VALUE, now = at(monday, 5))
        assertEquals(0, repo.rule("m")!!.streakDays)
        repo.settle(at(tuesday, 9, 30))
        assertEquals(0, repo.rule("m")!!.streakDays)
        repo.resume("m", now = at(tuesday, 12))
        repo.settle(at(wednesday, 9, 30))
        assertEquals(1, repo.rule("m")!!.streakDays)
    }

    @Test
    fun `a routine paused during its window pauses when the window ends`() {
        val repo = repository(at(monday, 0))
        repo.upsert(morning.copy(streakDays = 4))
        repo.pause("m", Long.MAX_VALUE, now = at(monday, 7))
        assertNull(repo.rule("m")!!.pausedUntil)
        assertEquals(at(monday, 9), repo.rule("m")!!.pending?.appliesAt)
        repo.settle(at(monday, 9, 1))
        val paused = repo.rule("m")!!
        assertEquals(Long.MAX_VALUE, paused.pausedUntil)
        // This morning's window counted before the pause, the next ones are lost to it.
        assertEquals(0, paused.streakDays)
        assertNull(paused.pending)
    }

    @Test
    fun `a pause between windows keeps the streak`() {
        val repo = repository(at(monday, 0))
        repo.upsert(morning)
        repo.settle(at(monday, 9, 30))
        repo.pause("m", at(monday, 18), now = at(monday, 10))
        repo.settle(at(tuesday, 9, 30))
        assertEquals(2, repo.rule("m")!!.streakDays)
    }

    @Test
    fun `an emergency unlock breaks the streaks of the rules it went past only`() {
        val repo = repository(at(monday, 0))
        repo.upsert(morning.copy(streakDays = 6))
        repo.upsert(morning.copy(id = "other", streakDays = 6))
        repo.breakStreaks(listOf("m"), at(monday, 7))
        repo.settle(at(monday, 9, 30))
        assertEquals(0, repo.rule("m")!!.streakDays)
        assertEquals(7, repo.rule("other")!!.streakDays)
        repo.settle(at(tuesday, 9, 30))
        assertEquals(1, repo.rule("m")!!.streakDays)
    }

    @Test
    fun `a limit's day counts with its minutes before they start over`() {
        val repo = repository(at(monday, 0))
        repo.save(Rule("l", RuleType.Limit, "Limit", apps = listOf(whatsApp), limitMinutes = 30))
        repo.start("l", now = at(monday, 8))
        repo.recordUsage(used(at(monday, 8), whatsApp.packageName to 20))
        repo.settle(at(tuesday, 0, 1))
        assertEquals(1, repo.rule("l")!!.streakDays)
        assertEquals(0, repo.rule("l")!!.usedMinutes)
        repo.start("l", now = at(tuesday, 8))
        repo.recordUsage(used(at(tuesday, 8), whatsApp.packageName to 45))
        repo.settle(at(wednesday, 0, 1))
        assertEquals(0, repo.rule("l")!!.streakDays)
    }
}
