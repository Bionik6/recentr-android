package com.recentr.feature.home

import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeSource
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.model.AllowedChange
import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.AppCategory
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Home's Always allowed card: once there's a rule, until it's closed, with the apps allowed. */
@OptIn(ExperimentalCoroutinesApi::class)
class AlwaysAllowedCardTest {

    private object NoScreenTime : ScreenTimeRepository {
        override val source = MutableStateFlow(ScreenTimeSource.Device)
        override fun summary(range: UsageRange): Flow<ScreenTimeState> = emptyFlow()
        override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> = emptyList()
        override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? = null
        override suspend fun pickups(range: UsageRange): PickupStats =
            PickupStats(UsagePeriod.Today, 0, 0, null, List(24) { 0 }, emptyList())
        override suspend fun appMinutesSince(start: Long): Map<String, Int> = emptyMap()
        override suspend fun firstDataDay(): LocalDate? = null
    }

    private val maps = UsageApp("com.google.android.apps.maps", "Maps", AppCategory.Other)
    private val uber = UsageApp("com.ubercab", "Uber", AppCategory.Other)
    private val lock = Rule("l", RuleType.Lock, "Socials", apps = listOf(UsageApp("com.instagram.android", "Instagram", AppCategory.Social)))

    private val dispatcher = UnconfinedTestDispatcher()
    private val rules = RulesRepository(file = null)
    private val allowed = MutableStateFlow(AlwaysAllowed())
    private val dismissed = MutableStateFlow(false)

    private fun TestScope.viewModel(): HomeViewModel {
        val vm = HomeViewModel(
            screenTime = NoScreenTime,
            profile = flowOf(UserProfile()),
            saveGoal = {},
            rules = rules,
            clockTicks = flowOf(Unit),
            alwaysAllowed = allowed,
            alwaysAllowedCardDismissed = dismissed,
            dismissCard = { dismissed.value = true },
        )
        backgroundScope.launch { vm.uiState.collect {} }
        return vm
    }

    private val HomeViewModel.shows get() = uiState.value.showAlwaysAllowedCard

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `it waits for the first rule`() = runTest(dispatcher) {
        val vm = viewModel()
        assertFalse(vm.shows)
        rules.upsert(lock)
        assertTrue(vm.shows)
    }

    @Test
    fun `it shows the apps allowed, those waiting to join included`() = runTest(dispatcher) {
        rules.upsert(lock)
        val vm = viewModel()
        assertEquals(emptyList<UsageApp>(), vm.uiState.value.alwaysAllowedApps)
        allowed.value = AlwaysAllowed(listOf(maps))
        assertTrue(vm.shows)
        assertEquals(listOf(maps), vm.uiState.value.alwaysAllowedApps)
        allowed.value = AlwaysAllowed(listOf(maps), pending = AllowedChange(Long.MAX_VALUE, listOf(maps, uber)))
        assertTrue(vm.shows)
        assertEquals(listOf(maps, uber), vm.uiState.value.alwaysAllowedApps)
    }

    @Test
    fun `closed, it doesn't come back`() = runTest(dispatcher) {
        rules.upsert(lock)
        val vm = viewModel()
        vm.dismissAlwaysAllowedCard()
        assertFalse(vm.shows)
        rules.upsert(lock.copy(id = "other"))
        assertFalse(vm.shows)
    }
}
