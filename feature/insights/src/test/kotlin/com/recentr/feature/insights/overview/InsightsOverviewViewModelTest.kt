package com.recentr.feature.insights.overview

import com.recentr.core.data.EmptyScreenTimeRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeSource
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.RangeUnit
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

/** The Insights date selector: where it opens, where the arrows go, and what each range loads. */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsOverviewViewModelTest {

    /** Monday 19 Oct 2026. */
    private val today = LocalDate.of(2026, 10, 19)

    /** Empty numbers, tagged with the range asked for; the device has data from 5 Oct. */
    private class FakeScreenTime(private val firstDay: LocalDate?) : ScreenTimeRepository {
        private val empty = EmptyScreenTimeRepository()
        val summariesAsked = mutableListOf<UsageRange>()
        val monthsAsked = mutableListOf<Pair<LocalDate, LocalDate>>()
        override val source: StateFlow<ScreenTimeSource> = MutableStateFlow(ScreenTimeSource.Device)
        override fun summary(range: UsageRange): Flow<ScreenTimeState> {
            summariesAsked += range
            return empty.summary(range)
        }
        override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> {
            monthsAsked += from to to
            return empty.dailyStats(from, to)
        }
        override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? = null
        override suspend fun pickups(range: UsageRange): PickupStats = empty.pickups(range)
        override suspend fun appMinutesSince(start: Long): Map<String, Int> = emptyMap()
        override suspend fun firstDataDay(): LocalDate? = firstDay
    }

    private lateinit var screenTime: FakeScreenTime
    private val dispatcher = UnconfinedTestDispatcher()

    /** A view model whose state is collected, like the screen does. */
    private fun TestScope.viewModel(firstDay: LocalDate? = LocalDate.of(2026, 10, 5)): InsightsOverviewViewModel {
        screenTime = FakeScreenTime(firstDay)
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
    fun `opens on today's Day tab`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(UsageRange.of(RangeUnit.Day, today), vm.now.range)
        assertEquals(listOf(UsageRange.of(RangeUnit.Day, today)), screenTime.summariesAsked)
        assertEquals(UsageRange.of(RangeUnit.Day, today), vm.now.summary?.range)
        // Nothing after today; the days before can be looked at.
        assertFalse(vm.now.canGoForward)
        assertTrue(vm.now.canGoBack)
    }

    @Test
    fun `the arrows walk through the days down to the first one with data`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.previous()
        assertEquals(UsageRange.of(RangeUnit.Day, today.minusDays(1)), vm.now.range)
        assertEquals(UsageRange.of(RangeUnit.Day, today.minusDays(1)), vm.now.summary?.range)
        repeat(20) { vm.previous() }
        assertEquals(UsageRange.of(RangeUnit.Day, LocalDate.of(2026, 10, 5)), vm.now.range)
        assertFalse(vm.now.canGoBack)
        repeat(20) { vm.next() }
        assertEquals(UsageRange.of(RangeUnit.Day, today), vm.now.range)
        assertFalse(vm.now.canGoForward)
    }

    @Test
    fun `without data it stays on today`() = runTest(dispatcher) {
        val vm = viewModel(firstDay = null)
        vm.previous()
        assertEquals(UsageRange.of(RangeUnit.Day, today), vm.now.range)
    }

    @Test
    fun `switching tab keeps the day looked at`() = runTest(dispatcher) {
        val vm = viewModel()
        repeat(9) { vm.previous() } // Saturday 10 Oct
        vm.selectTab(RangeUnit.Week)
        assertEquals(UsageRange.of(RangeUnit.Week, LocalDate.of(2026, 10, 5)), vm.now.range)
        assertEquals(UsageRange.of(RangeUnit.Week, LocalDate.of(2026, 10, 5)), screenTime.summariesAsked.last())
        assertFalse(vm.now.canGoBack)
        vm.next()
        assertEquals(UsageRange.of(RangeUnit.Week, LocalDate.of(2026, 10, 12)), vm.now.range)
        vm.selectTab(RangeUnit.Day)
        // A past week → its Sunday.
        assertEquals(UsageRange.of(RangeUnit.Day, LocalDate.of(2026, 10, 18)), vm.now.range)
    }

    @Test
    fun `a month reads its days for the calendar, past months included`() = runTest(dispatcher) {
        val vm = viewModel(firstDay = LocalDate.of(2026, 8, 20))
        vm.selectTab(RangeUnit.Month)
        assertEquals(LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31), screenTime.monthsAsked.last())
        vm.previous()
        assertEquals(LocalDate.of(2026, 9, 1) to LocalDate.of(2026, 9, 30), screenTime.monthsAsked.last())
        vm.previous()
        assertEquals(LocalDate.of(2026, 8, 1) to LocalDate.of(2026, 8, 31), screenTime.monthsAsked.last())
        assertFalse(vm.now.canGoBack)
        assertEquals(31, vm.now.monthDays?.size)
    }

    @Test
    fun `Home's usage card opens its period`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.show(UsagePeriod.ThisWeek)
        assertEquals(UsageRange.of(RangeUnit.Week, today), vm.now.range)
        vm.show(UsagePeriod.Today)
        assertEquals(UsageRange.of(RangeUnit.Day, today), vm.now.range)
    }
}
