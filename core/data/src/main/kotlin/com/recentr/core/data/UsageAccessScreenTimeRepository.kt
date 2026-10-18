package com.recentr.core.data

import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.UsagePeriod
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * [RecentrData.screenTime]: the [device] numbers while usage access is granted, the [demo] ones
 * otherwise. [recheckUsageAccess] runs on every resume (the onboarding sends the user to Settings,
 * who can also revoke the access there); a change updates [source] so Home re-syncs.
 */
class UsageAccessScreenTimeRepository(
    private val hasUsageAccess: () -> Boolean,
    private val device: ScreenTimeRepository,
    private val demo: ScreenTimeRepository,
) : ScreenTimeRepository {

    private val current = MutableStateFlow(currentSource())

    override val source: StateFlow<ScreenTimeSource> = current.asStateFlow()

    fun recheckUsageAccess() {
        current.value = currentSource()
    }

    override fun summary(period: UsagePeriod): Flow<ScreenTimeState> = flow {
        if (current.value == ScreenTimeSource.Device) {
            emitAll(
                device.summary(period).catch { error ->
                    // Access revoked while the app was in the foreground: fall back to the demo.
                    if (error !is SecurityException) throw error
                    recheckUsageAccess()
                    emitAll(demo.summary(period))
                },
            )
        } else {
            emitAll(demo.summary(period))
        }
    }

    override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> = fromSource { it.dailyStats(from, to) }

    override suspend fun appDetail(packageName: String, period: UsagePeriod): AppUsageDetail? =
        fromSource { it.appDetail(packageName, period) }

    override suspend fun pickups(period: UsagePeriod): PickupStats = fromSource { it.pickups(period) }

    /** Runs [read] on the current source, falling back to the demo when access was just revoked. */
    private suspend fun <T> fromSource(read: suspend (ScreenTimeRepository) -> T): T {
        if (current.value != ScreenTimeSource.Device) return read(demo)
        return try {
            read(device)
        } catch (_: SecurityException) {
            recheckUsageAccess()
            read(demo)
        }
    }

    private fun currentSource(): ScreenTimeSource =
        if (hasUsageAccess()) ScreenTimeSource.Device else ScreenTimeSource.Demo
}
