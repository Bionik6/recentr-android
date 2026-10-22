package com.recentr.core.data

import com.recentr.core.model.AllowedChange
import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
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

class AlwaysAllowedRepositoryTest {

    @get:JUnitRule
    val folder = TemporaryFolder()

    private val zone = ZoneId.of("Europe/Paris")
    /** Monday 19 Oct 2026, 12:00. */
    private val now = LocalDate.of(2026, 10, 19).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val midnight = LocalDate.of(2026, 10, 20).atStartOfDay(zone).toInstant().toEpochMilli()

    private val maps = UsageApp("com.google.android.apps.maps", "Maps", AppCategory.Other)
    private val uber = UsageApp("com.ubercab", "Uber", AppCategory.Other)

    private val file get() = folder.root.resolve("always_allowed.json")
    private val rules = RulesRepository(file = null, zone = { zone })
    private val focus = FocusRepository(zone = { zone })

    private fun repository() =
        AlwaysAllowedRepository(file, rules, focus, zone = { zone }, writeDispatcher = Dispatchers.Unconfined, clock = { now })

    /** On since this morning, holding Uber. */
    private val lock = Rule("l", RuleType.Lock, "Rides", apps = listOf(uber), activeSince = now - 3_600_000L)

    @Test
    fun `starts with nothing allowed`() {
        assertEquals(AlwaysAllowed(), repository().allowed.value)
        assertFalse(file.exists())
    }

    @Test
    fun `an app no rule holds is allowed now, and stays allowed after a restart`() {
        repository().choose(listOf(maps))
        assertEquals(AlwaysAllowed(listOf(maps)), repository().allowed.value)
    }

    @Test
    fun `an app an engaged rule holds waits for midnight, then joins`() {
        rules.upsert(lock)
        val repo = repository()
        val chosen = repo.choose(listOf(maps, uber))
        // Maps, which nothing holds, joins now.
        assertEquals(AlwaysAllowed(listOf(maps), AllowedChange(midnight, listOf(maps, uber))), chosen)
        assertEquals(chosen, repo.allowed.value)
        // Kept across a restart.
        val reopened = repository()
        assertEquals(chosen, reopened.allowed.value)
        reopened.settle(midnight - 1)
        assertEquals(chosen, reopened.allowed.value)
        reopened.settle(midnight)
        assertEquals(AlwaysAllowed(listOf(maps, uber)), reopened.allowed.value)
        assertEquals(AlwaysAllowed(listOf(maps, uber)), repository().allowed.value)
    }

    @Test
    fun `removing an app applies now, and drops what waits once nothing's left`() {
        val repo = repository()
        repo.choose(listOf(maps))
        rules.upsert(lock)
        repo.choose(listOf(maps, uber))
        repo.remove(maps.packageName)
        assertEquals(AlwaysAllowed(emptyList(), AllowedChange(midnight, listOf(uber))), repo.allowed.value)
        repo.remove(uber.packageName)
        assertEquals(AlwaysAllowed(), repo.allowed.value)
        assertEquals(AlwaysAllowed(), repository().allowed.value)
    }

    @Test
    fun `dropping the kept change keeps the list in effect`() {
        val repo = repository()
        repo.choose(listOf(maps))
        rules.upsert(lock)
        repo.choose(listOf(maps, uber))
        repo.dropPending()
        assertEquals(AlwaysAllowed(listOf(maps)), repo.allowed.value)
        assertNull(repository().allowed.value.pending)
    }

    @Test
    fun `reset allows nothing again`() {
        val repo = repository()
        repo.choose(listOf(maps))
        repo.reset()
        assertEquals(AlwaysAllowed(), repo.allowed.value)
        assertEquals(AlwaysAllowed(), repository().allowed.value)
    }

    @Test
    fun `an unreadable file starts empty`() {
        file.writeText("{not json")
        assertTrue(repository().allowed.value.apps.isEmpty())
    }
}
