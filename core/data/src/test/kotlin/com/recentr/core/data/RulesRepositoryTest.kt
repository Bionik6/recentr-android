package com.recentr.core.data

import com.recentr.core.model.AppCategory
import com.recentr.core.model.HabitTask
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

    private fun repository(at: Long = now) =
        RulesRepository(folder.root.resolve("rules.json"), zone = { zone }, now = at, writeDispatcher = Dispatchers.Unconfined)

    @Test
    fun `starts empty without a saved file`() {
        val repo = repository()
        assertTrue(repo.rules.value.isEmpty())
        assertTrue(repo.savedTemplates.value.isEmpty())
    }

    @Test
    fun `rules and templates survive a restart`() {
        val rule = Rule(
            id = "r",
            type = RuleType.Session,
            name = "Work Hours",
            apps = listOf(whatsApp),
            schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()),
            pausedUntil = Long.MAX_VALUE,
            habitTasks = listOf(HabitTask("t", "Read")),
        )
        repository().apply {
            upsert(rule)
            saveTemplate(rule)
        }

        val reopened = repository()
        assertEquals(listOf(rule), reopened.rules.value)
        assertEquals(1, reopened.savedTemplates.value.size)
        assertTrue(reopened.rule("r")!!.targets("com.whatsapp"))
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
    fun `ended one-shot sessions are removed and kept removed`() {
        val repo = repository()
        repo.upsert(Rule("s", RuleType.Session, "Focus", durationMinutes = 30, activeSince = now, oneShot = true))
        repo.settle(now + 31 * 60_000)
        assertNull(repo.rule("s"))
        assertNull(repository().rule("s"))
    }
}
