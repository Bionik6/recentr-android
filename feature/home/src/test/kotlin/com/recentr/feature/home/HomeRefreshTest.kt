package com.recentr.feature.home

import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeSource
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Home's pull to refresh: the numbers are read again under the spinner, never behind a skeleton. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRefreshTest {

    /** Each summary waits for the test to [answer] it. */
    private class FakeScreenTime : ScreenTimeRepository {
        val pending = mutableListOf<CompletableDeferred<Int>>()
        override val source = MutableStateFlow(ScreenTimeSource.Device)

        override fun summary(range: UsageRange): Flow<ScreenTimeState> = flow {
            emit(ScreenTimeState.Syncing)
            val answer = CompletableDeferred<Int>().also { pending += it }
            emit(ScreenTimeState.Ready(summary(answer.await())))
        }

        /** The last summary asked for gets [totalMinutes] of screen time. */
        fun answer(totalMinutes: Int) = pending.last().complete(totalMinutes)

        override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> = emptyList()
        override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? = null
        override suspend fun pickups(range: UsageRange): PickupStats =
            PickupStats(UsagePeriod.Today, 0, 0, null, List(24) { 0 }, emptyList())
        override suspend fun appMinutesSince(start: Long): Map<String, Int> = emptyMap()
        override suspend fun firstDataDay(): LocalDate? = null

        private fun summary(totalMinutes: Int) = ScreenTimeSummary(
            period = UsagePeriod.Today,
            totalMinutes = totalMinutes,
            decreaseMinutes = 0,
            pickups = 0,
            notifications = 0,
            hourlyMinutes = List(24) { 0 },
            dailyMinutes = List(7) { 0 },
            mostUsed = emptyList(),
            longestOfflineMinutes = 0,
            focusPercent = 0,
            socialPercent = 0,
            funPercent = 0,
            streakDays = 0,
        )
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val screenTime = FakeScreenTime()

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
    fun `the first load shows the skeleton, not the pull spinner`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.now.showSkeleton)
        assertFalse(vm.now.isRefreshing)
        screenTime.answer(30)
        assertFalse(vm.now.showSkeleton)
        assertEquals(30, vm.now.summary?.totalMinutes)
    }

    @Test
    fun `a pull to refresh reads the numbers again and keeps the old ones up meanwhile`() = runTest(dispatcher) {
        val vm = viewModel()
        screenTime.answer(30)

        vm.refresh()
        assertTrue(vm.now.isRefreshing)
        assertFalse(vm.now.showSkeleton)
        assertEquals(30, vm.now.summary?.totalMinutes)
        assertEquals(2, screenTime.pending.size)

        screenTime.answer(45)
        assertFalse(vm.now.isRefreshing)
        assertEquals(45, vm.now.summary?.totalMinutes)
    }

    @Test
    fun `pulling again while it refreshes reads once`() = runTest(dispatcher) {
        val vm = viewModel()
        screenTime.answer(30)
        vm.refresh()
        vm.refresh()
        assertEquals(2, screenTime.pending.size)
        screenTime.answer(31)
        assertFalse(vm.now.isRefreshing)
        // Done: the next pull reads again.
        vm.refresh()
        assertEquals(3, screenTime.pending.size)
    }

    @Test
    fun `a pull before the first numbers keeps the skeleton until they come`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.refresh()
        assertTrue(vm.now.showSkeleton)
        assertNull(vm.now.summary)
        screenTime.answer(12)
        assertFalse(vm.now.isRefreshing)
        assertEquals(12, vm.now.summary?.totalMinutes)
    }

    @Test
    fun `the spinner goes when a new source replaces the refresh`() = runTest(dispatcher) {
        val vm = viewModel()
        screenTime.answer(30)
        vm.refresh()
        assertTrue(vm.now.isRefreshing)
        // Usage access revoked meanwhile: a full sync (skeleton) takes over.
        screenTime.source.value = ScreenTimeSource.NoAccess
        assertFalse(vm.now.isRefreshing)
        assertTrue(vm.now.showSkeleton)
        screenTime.answer(0)
        assertFalse(vm.now.showSkeleton)
    }
}
