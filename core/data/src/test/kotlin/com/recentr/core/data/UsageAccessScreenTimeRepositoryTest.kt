package com.recentr.core.data

import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Device numbers while usage access is granted, empty ones otherwise — and the source says which. */
class UsageAccessScreenTimeRepositoryTest {

    private val zone = ZoneId.of("Africa/Dakar")
    private val today = LocalDate.of(2026, 10, 21)
    private val range = UsageRange.of(UsagePeriod.Today, today)

    /** One day of phone use: 38 minutes, or a [SecurityException] once access is revoked. */
    private inner class DeviceScreenTime : ScreenTimeRepository {
        var revoked = false
        override val source: StateFlow<ScreenTimeSource> = MutableStateFlow(ScreenTimeSource.Device)

        override fun summary(range: UsageRange): Flow<ScreenTimeState> = flow {
            emit(ScreenTimeState.Syncing)
            checkAccess()
            val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val events = listOf(
                UsageEvent(start + HOUR, UsageEventType.ScreenInteractive),
                UsageEvent(start + HOUR, UsageEventType.ActivityResumed, WHATSAPP, "$WHATSAPP.Main"),
                UsageEvent(start + HOUR + 38 * MINUTE, UsageEventType.ActivityPaused, WHATSAPP, "$WHATSAPP.Main"),
                UsageEvent(start + HOUR + 38 * MINUTE, UsageEventType.ScreenNonInteractive),
            )
            val day = ScreenTimeAggregation.day(today, start, start + 24 * HOUR, events)
            emit(
                ScreenTimeState.Ready(
                    ScreenTimeAggregation.summarize(
                        period = UsagePeriod.Today,
                        days = listOf(day),
                        zone = zone,
                        resolveApp = { null },
                        baselineDailyMinutes = 0,
                        streakDays = 0,
                        previousTotalMinutes = null,
                        range = range,
                    ),
                ),
            )
        }

        override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> {
            checkAccess()
            return listOf(DayStats(today, hasData = true, minutes = 38, pickups = 1))
        }

        override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? = null
        override suspend fun pickups(range: UsageRange): PickupStats =
            ScreenTimeAggregation.pickupStats(UsagePeriod.Today, emptyList(), zone) { null }
        override suspend fun appMinutesSince(start: Long): Map<String, Int> {
            checkAccess()
            return mapOf(WHATSAPP to 38)
        }
        override suspend fun firstDataDay(): LocalDate? = today

        private fun checkAccess() {
            if (revoked) throw SecurityException("Usage access revoked")
        }
    }

    private var granted = false
    private val device = DeviceScreenTime()
    private val repository = UsageAccessScreenTimeRepository(
        hasUsageAccess = { granted },
        device = device,
        noAccess = EmptyScreenTimeRepository { zone },
    )

    private fun totalMinutes(): Int = runBlocking {
        (repository.summary(range).last() as ScreenTimeState.Ready).summary.totalMinutes
    }

    @Test
    fun `without usage access the numbers are empty and the source says so`() {
        assertEquals(ScreenTimeSource.NoAccess, repository.source.value)
        assertEquals(0, totalMinutes())
        runBlocking {
            assertFalse(repository.dailyStats(today, today).single().hasData)
            assertTrue(repository.appMinutesSince(0).isEmpty())
        }
    }

    @Test
    fun `with usage access the numbers are the device's`() {
        granted = true
        repository.recheckUsageAccess()

        assertEquals(ScreenTimeSource.Device, repository.source.value)
        assertEquals(38, totalMinutes())
        runBlocking {
            assertEquals(38, repository.dailyStats(today, today).single().minutes)
            assertEquals(mapOf(WHATSAPP to 38), repository.appMinutesSince(0))
        }
    }

    @Test
    fun `access granted in Settings counts from the next recheck`() {
        assertEquals(0, totalMinutes())

        granted = true
        // Not until the app comes back to the front (MainActivity.onResume).
        assertEquals(0, totalMinutes())
        repository.recheckUsageAccess()

        assertEquals(ScreenTimeSource.Device, repository.source.value)
        assertEquals(38, totalMinutes())
    }

    @Test
    fun `access revoked while Recentr is open falls back to empty numbers`() {
        granted = true
        repository.recheckUsageAccess()
        assertEquals(38, totalMinutes())

        granted = false
        device.revoked = true

        assertEquals(0, totalMinutes())
        assertEquals(ScreenTimeSource.NoAccess, repository.source.value)
    }

    @Test
    fun `a read refused by the system falls back to empty days`() {
        granted = true
        repository.recheckUsageAccess()
        granted = false
        device.revoked = true

        runBlocking {
            assertFalse(repository.dailyStats(today, today).single().hasData)
        }
        assertEquals(ScreenTimeSource.NoAccess, repository.source.value)
    }

    private companion object {
        const val WHATSAPP = "com.whatsapp"
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
