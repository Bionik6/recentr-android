package com.recentr.feature.rules.allowed

import com.recentr.core.data.AlwaysAllowedRepository
import com.recentr.core.data.FocusRepository
import com.recentr.core.data.RulesRepository
import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Settings › Always allowed: the apps allowed, the ones waiting to join, and when they do. */
@OptIn(ExperimentalCoroutinesApi::class)
class AlwaysAllowedViewModelTest {

    private val zone = ZoneId.systemDefault()

    /** [day] Oct 2026 at [hour]:[minutes]; Monday 19 Oct by default. */
    private fun at(hour: Int, minutes: Int = 0, day: Int = 19): Long =
        LocalDate.of(2026, 10, day).atTime(hour, minutes).atZone(zone).toInstant().toEpochMilli()

    private val maps = UsageApp("com.google.android.apps.maps", "Maps", AppCategory.Other)
    private val uber = UsageApp("com.ubercab", "Uber", AppCategory.Other)
    private val bank = UsageApp("com.bank", "bank", AppCategory.Other)

    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = MutableStateFlow(at(12))
    private val rules = RulesRepository(file = null, zone = { zone })
    private val repository = AlwaysAllowedRepository(rules = rules, focus = FocusRepository(), zone = { zone }, clock = { clock.value })

    /** On since this morning, holding Uber. */
    private val lock = Rule("l", RuleType.Lock, "Rides", apps = listOf(uber), activeSince = at(9))

    private fun TestScope.viewModel(): AlwaysAllowedViewModel {
        val vm = AlwaysAllowedViewModel(repository, clock = { clock.value }, ticks = clock)
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `apps no rule holds are allowed at once, by name`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.allow(listOf(maps, bank))
        assertEquals(listOf(bank, maps), vm.state.value.apps)
        assertTrue(vm.state.value.waiting.isEmpty())
        assertNull(vm.state.value.joinsAt)
    }

    @Test
    fun `an app an engaged rule holds waits until midnight, then joins`() = runTest(dispatcher) {
        rules.upsert(lock)
        val vm = viewModel()
        vm.allow(listOf(maps, uber))
        assertEquals(listOf(maps), vm.state.value.apps)
        assertEquals(listOf(uber), vm.state.value.waiting)
        assertEquals(at(0, day = 20), vm.state.value.joinsAt)
        assertEquals(JoinTime.Midnight, JoinTime.of(vm.state.value.joinsAt!!, clock.value, zone))
        assertEquals(listOf(maps, uber), vm.state.value.latestWish)
        // The clock passes midnight: Uber joins, and the list is saved that way.
        clock.value = at(0, 1, day = 20)
        assertEquals(listOf(maps, uber), vm.state.value.apps)
        assertNull(vm.state.value.joinsAt)
        assertEquals(AlwaysAllowed(listOf(maps, uber)), repository.allowed.value)
    }

    @Test
    fun `removing applies now, waiting or not, and dropping keeps the apps allowed`() = runTest(dispatcher) {
        rules.upsert(lock)
        val vm = viewModel()
        vm.allow(listOf(maps))
        vm.allow(listOf(maps, uber))
        vm.remove(maps)
        assertTrue(vm.state.value.apps.isEmpty())
        assertEquals(listOf(uber), vm.state.value.waiting)
        vm.remove(uber)
        assertTrue(vm.state.value.waiting.isEmpty())
        assertNull(vm.state.value.joinsAt)

        // Allowed together, Maps joins now and Uber waits.
        vm.allow(listOf(maps, uber))
        assertEquals(listOf(maps), vm.state.value.apps)
        assertEquals(listOf(uber), vm.state.value.waiting)
        vm.dropChanges()
        assertEquals(listOf(maps), vm.state.value.apps)
        assertTrue(vm.state.value.waiting.isEmpty())
    }

    @Test
    fun `the notice says when the waiting apps join`() {
        assertEquals(JoinTime.Midnight, JoinTime.of(at(0, day = 20), at(12), zone))
        assertEquals(JoinTime.Today(at(17)), JoinTime.of(at(17), at(12), zone))
        assertEquals(JoinTime.Tomorrow(at(7, day = 20)), JoinTime.of(at(7, day = 20), at(23), zone))
        assertEquals(JoinTime.Later(at(9, day = 22)), JoinTime.of(at(9, day = 22), at(12), zone))
    }
}
