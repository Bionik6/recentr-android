package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.HabitTask
import com.recentr.core.model.LimitReset
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val sunday = LocalDate.of(2026, 10, 18)
    private val monday = sunday.plusDays(1)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun repository(at: Long = now) =
        RulesRepository(folder.root.resolve("rules.json"), zone = { zone }, now = at, writeDispatcher = Dispatchers.Unconfined)

    /** [minutes] per package in the period of [reset]. */
    private fun used(reset: LimitReset, vararg minutes: Pair<String, Int>) = LimitUsage(mapOf(reset to mapOf(*minutes)))

    private fun limit(id: String, reset: LimitReset) =
        Rule(id, RuleType.Limit, "Limit", apps = listOf(whatsApp), limitMinutes = 60, limitReset = reset)

    @Test
    fun `starts empty without a saved file`() {
        val repo = repository()
        assertTrue(repo.rules.value.isEmpty())
    }

    @Test
    fun `rules survive a restart`() {
        val rule = Rule(
            id = "r",
            type = RuleType.Session,
            name = "Work Hours",
            apps = listOf(whatsApp),
            schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()),
            pausedUntil = Long.MAX_VALUE,
            habitTasks = listOf(HabitTask("t", "Read")),
        )
        repository().upsert(rule)

        val reopened = repository()
        assertEquals(listOf(rule), reopened.rules.value)
        assertTrue(reopened.rule("r")!!.targets("com.whatsapp"))
    }

    @Test
    fun `a file saved with templates still opens`() {
        folder.root.resolve("rules.json").writeText(
            """{"rules":[{"id":"r","type":"Lock","name":"WhatsApp"}],"templates":[{"id":"saved-1","type":"Session","name":"Old"}]}""",
        )
        assertEquals(listOf("r"), repository().rules.value.map { it.id })
    }

    @Test
    fun `deleting and erasing are saved too`() {
        repository().apply {
            upsert(Rule("a", RuleType.Lock, "A"))
            upsert(Rule("b", RuleType.Lock, "B"))
            delete("a")
        }
        assertEquals(listOf("b"), repository().rules.value.map { it.id })

        repository().reset()
        assertTrue(repository().rules.value.isEmpty())
    }

    @Test
    fun `daily counters start over on a new day`() {
        val repo = repository()
        repo.upsert(
            Rule(
                id = "h",
                type = RuleType.Habit,
                name = "Before X opens",
                unlocksUsedToday = 3,
                habitTasks = listOf(HabitTask("t", "Read", done = true)),
            ),
        )
        repo.settle(now + 60_000)
        assertEquals(3, repo.rule("h")!!.unlocksUsedToday)

        repo.settle(now + day)
        val reset = repository(now + day).rule("h")!!
        assertEquals(0, reset.unlocksUsedToday)
        assertFalse(reset.habitTasks.single().done)
    }

    @Test
    fun `today's use of a daily limit's apps is shown on the limit and saved`() {
        val youTube = UsageApp("com.google.android.youtube", "YouTube", AppCategory.Entertainment)
        val repo = repository()
        repo.upsert(Rule("m", RuleType.Limit, "Daily limit", apps = listOf(youTube, whatsApp), limitMinutes = 60))
        repo.upsert(Rule("l", RuleType.Lock, "WhatsApp", apps = listOf(whatsApp)))

        repo.recordUsage(used(LimitReset.Midnight, youTube.packageName to 4, whatsApp.packageName to 3, "com.other" to 50))
        assertEquals(7, repo.rule("m")!!.usedMinutes)
        // Only limits count their apps' use.
        assertEquals(0, repo.rule("l")!!.usedMinutes)
        assertEquals(7, repository().rule("m")!!.usedMinutes)

        // An app not used today counts for nothing, and the next reading replaces the last one.
        repo.recordUsage(used(LimitReset.Midnight, youTube.packageName to 12))
        assertEquals(12, repository().rule("m")!!.usedMinutes)
    }

    @Test
    fun `a limit's use starts over on a new day`() {
        val repo = repository()
        repo.upsert(Rule("m", RuleType.Limit, "Daily limit", apps = listOf(whatsApp), limitMinutes = 60))
        repo.recordUsage(used(LimitReset.Midnight, whatsApp.packageName to 45))
        repo.settle(at(sunday, 23, 59))
        assertEquals(45, repo.rule("m")!!.usedMinutes)
        repo.settle(at(monday, 0, 0))
        assertEquals(0, repo.rule("m")!!.usedMinutes)
    }

    @Test
    fun `each limit shows the use of its own period`() {
        val repo = repository()
        repo.upsert(limit("night", LimitReset.FourAm))
        repo.upsert(limit("week", LimitReset.Monday))
        repo.upsert(limit("day", LimitReset.Midnight).copy(usedMinutes = 12))

        repo.recordUsage(LimitUsage(mapOf(LimitReset.FourAm to mapOf(whatsApp.packageName to 30), LimitReset.Monday to mapOf(whatsApp.packageName to 200))))
        assertEquals(30, repo.rule("night")!!.usedMinutes)
        assertEquals(200, repo.rule("week")!!.usedMinutes)
        // Today's use wasn't read: the last reading stays.
        assertEquals(12, repo.rule("day")!!.usedMinutes)
    }

    @Test
    fun `a 4 AM limit keeps its minutes past midnight and starts over at 4 00`() {
        val repo = repository(at(sunday, 22, 0))
        repo.upsert(limit("night", LimitReset.FourAm).copy(unlocksUsedToday = 2))
        repo.upsert(limit("day", LimitReset.Midnight))
        repo.recordUsage(used(LimitReset.FourAm, whatsApp.packageName to 45))
        repo.recordUsage(used(LimitReset.Midnight, whatsApp.packageName to 45))

        repo.settle(at(monday, 0, 30))
        assertEquals(45, repo.rule("night")!!.usedMinutes)
        assertEquals(0, repo.rule("day")!!.usedMinutes)
        // Unlocks stay counted per calendar day.
        assertEquals(0, repo.rule("night")!!.unlocksUsedToday)

        repo.settle(at(monday, 3, 59))
        assertEquals(45, repo.rule("night")!!.usedMinutes)
        repo.settle(at(monday, 4, 0))
        assertEquals(0, repo.rule("night")!!.usedMinutes)
    }

    @Test
    fun `a weekly limit keeps its minutes all week and starts over on Monday`() {
        val repo = repository(at(sunday, 22, 0))
        repo.upsert(limit("week", LimitReset.Monday))
        repo.recordUsage(used(LimitReset.Monday, whatsApp.packageName to 200))
        repo.settle(at(sunday, 23, 59))
        assertEquals(200, repo.rule("week")!!.usedMinutes)
        repo.settle(at(monday, 0, 0))
        assertEquals(0, repo.rule("week")!!.usedMinutes)

        repo.recordUsage(used(LimitReset.Monday, whatsApp.packageName to 30))
        // Every day of the week, the clocks going back on Sunday 25 included.
        (1L..6L).forEach { days ->
            repo.settle(at(monday.plusDays(days), 23, 59))
            assertEquals(30, repo.rule("week")!!.usedMinutes)
        }
        repo.settle(at(monday.plusWeeks(1), 0, 0))
        assertEquals(0, repo.rule("week")!!.usedMinutes)
    }

    @Test
    fun `a limit's period is remembered across a restart`() {
        val repo = repository(at(sunday, 22, 0))
        repo.upsert(limit("night", LimitReset.FourAm))
        repo.settle(at(monday, 3, 0))
        repo.recordUsage(used(LimitReset.FourAm, whatsApp.packageName to 45))

        // Reopened before 4:00, the evening's minutes still count…
        repository(at(monday, 3, 30)).apply { settle(at(monday, 3, 30)) }
        assertEquals(45, repository().rule("night")!!.usedMinutes)
        // …and after 4:00, they start over even though the app wasn't running at 4:00.
        val reopened = repository(at(monday, 9, 0))
        reopened.settle(at(monday, 9, 0))
        assertEquals(0, reopened.rule("night")!!.usedMinutes)
    }

    @Test
    fun `a file with only the counters' day still starts the day over`() {
        folder.root.resolve("rules.json").writeText(
            """{"rules":[{"id":"m","type":"Limit","name":"L","limitMinutes":60,"usedMinutes":45}],"countersDay":${sunday.toEpochDay()}}""",
        )
        val repo = repository(at(monday, 9, 0))
        repo.settle(at(sunday, 23, 0))
        assertEquals(45, repo.rule("m")!!.usedMinutes)
        repo.settle(at(monday, 9, 0))
        assertEquals(0, repo.rule("m")!!.usedMinutes)
    }

    @Test
    fun `ended one-shot sessions are removed and kept removed`() {
        val repo = repository()
        repo.upsert(Rule("s", RuleType.Session, "Focus", durationMinutes = 30, activeSince = now, oneShot = true))
        repo.settle(now + 31 * 60_000)
        assertNull(repo.rule("s"))
        assertNull(repository().rule("s"))
    }
}
