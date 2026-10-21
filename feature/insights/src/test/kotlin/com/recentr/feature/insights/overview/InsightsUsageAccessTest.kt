package com.recentr.feature.insights.overview

import com.recentr.core.data.EmptyScreenTimeRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeSource
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.data.UsageAccessScreenTimeRepository
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.RangeUnit
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
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
 * The Insights tab without usage access: its empty days, weeks and months are not time away from
 * the phone, so it asks for the access ("Connect") instead of showing them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsUsageAccessTest {

    /** Wednesday 21 Oct 2026. */
    private val today = LocalDate.of(2026, 10, 21)

    /** The phone's usage once access is granted: 38 minutes a day since 1 Oct. */
    private class DeviceScreenTime : ScreenTimeRepository {
        override val source: StateFlow<ScreenTimeSource> = MutableStateFlow(ScreenTimeSource.Device)

        override fun summary(range: UsageRange): Flow<ScreenTimeState> = flow {
            emit(ScreenTimeState.Syncing)
            emit(
                ScreenTimeState.Ready(
                    ScreenTimeSummary(
                        period = UsagePeriod.Today,
                        totalMinutes = 38,
                        decreaseMinutes = 0,
                        pickups = 13,
                        notifications = 38,
                        hourlyMinutes = List(24) { 0 },
                        dailyMinutes = List(7) { 0 },
                        mostUsed = emptyList(),
                        longestOfflineMinutes = 0,
                        focusPercent = 100,
                        socialPercent = 0,
                        funPercent = 0,
                        streakDays = 0,
                        range = range,
                    ),
                ),
            )
        }

        override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> =
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
                .map { DayStats(it, hasData = true, minutes = 38, pickups = 13) }
                .toList()

        override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? = null
        override suspend fun pickups(range: UsageRange): PickupStats =
            PickupStats(UsagePeriod.Today, 0, 0, null, List(24) { 0 }, emptyList())
        override suspend fun appMinutesSince(start: Long): Map<String, Int> = emptyMap()
        override suspend fun firstDataDay(): LocalDate? = LocalDate.of(2026, 10, 1)
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
    private fun TestScope.viewModel(): InsightsOverviewViewModel {
        val vm = InsightsOverviewViewModel(UsageRange.of(RangeUnit.Day, today), screenTime, flowOf(150), today = { today })
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    private val InsightsOverviewViewModel.now get() = state.value

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `without usage access the day asks for it instead of showing no screen time`() = runTest(dispatcher) {
        val vm = viewModel()

        assertFalse(vm.now.loading)
        assertTrue(vm.now.needsUsageAccess)
    }

    @Test
    fun `without usage access the month asks for it too`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.selectTab(RangeUnit.Month)

        assertTrue(vm.now.needsUsageAccess)
    }

    @Test
    fun `with usage access the tab shows the device numbers`() = runTest(dispatcher) {
        granted = true
        screenTime.recheckUsageAccess()
        val vm = viewModel()

        assertFalse(vm.now.needsUsageAccess)
        assertEquals(38, vm.now.summary?.totalMinutes)
        vm.selectTab(RangeUnit.Month)
        assertFalse(vm.now.needsUsageAccess)
        assertEquals(38, vm.now.toughestDay?.minutes)
    }

    @Test
    fun `access granted in Settings brings the numbers and the days before`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.now.needsUsageAccess)
        assertFalse(vm.now.canGoBack)

        // Back from Settings › Usage access: MainActivity.onResume rechecks it.
        granted = true
        screenTime.recheckUsageAccess()

        assertFalse(vm.now.needsUsageAccess)
        assertEquals(38, vm.now.summary?.totalMinutes)
        assertTrue(vm.now.canGoBack)
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
}
