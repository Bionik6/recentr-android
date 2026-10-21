package com.recentr.feature.home

import com.recentr.core.data.EmptyScreenTimeRepository
import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeSource
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.data.UsageAccessScreenTimeRepository
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
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

/**
 * Home without usage access: the empty numbers it reads then are not a day without screen time,
 * so it asks for the access ("Connect") instead of showing "0m · Under your goal".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeUsageAccessTest {

    /** The phone's usage once access is granted: 38 minutes, 13 pickups, 38 notifications. */
    private class DeviceScreenTime : ScreenTimeRepository {
        override val source: StateFlow<ScreenTimeSource> = MutableStateFlow(ScreenTimeSource.Device)

        override fun summary(range: UsageRange): Flow<ScreenTimeState> = flow {
            emit(ScreenTimeState.Syncing)
            emit(ScreenTimeState.Ready(summary(totalMinutes = 38, pickups = 13, notifications = 38)))
        }

        override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> = emptyList()
        override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? = null
        override suspend fun pickups(range: UsageRange): PickupStats =
            PickupStats(UsagePeriod.Today, 0, 0, null, List(24) { 0 }, emptyList())
        override suspend fun appMinutesSince(start: Long): Map<String, Int> = emptyMap()
        override suspend fun firstDataDay(): LocalDate? = null

        private fun summary(totalMinutes: Int, pickups: Int, notifications: Int) = ScreenTimeSummary(
            period = UsagePeriod.Today,
            totalMinutes = totalMinutes,
            decreaseMinutes = 0,
            pickups = pickups,
            notifications = notifications,
            hourlyMinutes = List(24) { 0 },
            dailyMinutes = List(7) { 0 },
            mostUsed = emptyList(),
            longestOfflineMinutes = 0,
            focusPercent = 100,
            socialPercent = 0,
            funPercent = 0,
            streakDays = 0,
        )
    }

    private val dispatcher = UnconfinedTestDispatcher()

    /** Usage access as Settings has it; [UsageAccessScreenTimeRepository.recheckUsageAccess] reads it again. */
    private var granted = false
    private val screenTime = UsageAccessScreenTimeRepository(
        hasUsageAccess = { granted },
        device = DeviceScreenTime(),
        noAccess = EmptyScreenTimeRepository(),
    )

    /** A view model whose state is collected, like the screen does. */
    private fun TestScope.viewModel(): HomeViewModel {
        val vm = HomeViewModel(
            screenTime = screenTime,
            profile = flowOf(UserProfile()),
            saveGoal = {},
            rules = RulesRepository(file = null),
            clockTicks = flowOf(Unit),
        )
        backgroundScope.launch { vm.uiState.collect {} }
        return vm
    }

    private val HomeViewModel.now get() = uiState.value

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `without usage access Home asks for it instead of showing an empty day`() = runTest(dispatcher) {
        val vm = viewModel()

        assertFalse(vm.now.showSkeleton)
        assertTrue(vm.now.needsUsageAccess)
        assertFalse(vm.now.usageAccess)
    }

    @Test
    fun `with usage access Home shows the device numbers`() = runTest(dispatcher) {
        granted = true
        screenTime.recheckUsageAccess()
        val vm = viewModel()

        assertFalse(vm.now.needsUsageAccess)
        assertEquals(38, vm.now.summary?.totalMinutes)
        assertEquals(13, vm.now.summary?.pickups)
        assertEquals(38, vm.now.summary?.notifications)
    }

    @Test
    fun `access granted in Settings brings the device numbers back on Home`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.now.needsUsageAccess)

        // Back from Settings › Usage access: MainActivity.onResume rechecks it.
        granted = true
        screenTime.recheckUsageAccess()

        assertFalse(vm.now.needsUsageAccess)
        assertEquals(38, vm.now.summary?.totalMinutes)
    }

    @Test
    fun `access revoked in Settings asks for it again`() = runTest(dispatcher) {
        granted = true
        screenTime.recheckUsageAccess()
        val vm = viewModel()
        assertFalse(vm.now.needsUsageAccess)

        granted = false
        screenTime.recheckUsageAccess()

        assertTrue(vm.now.needsUsageAccess)
    }

    @Test
    fun `while it syncs Home shows the skeleton, not the request`() {
        val syncing = HomeUiState(summary = null, usageAccess = false, isSyncing = true)

        assertTrue(syncing.showSkeleton)
        assertFalse(syncing.needsUsageAccess)
    }
}
