package com.recentr.core.data

import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.DayStats
import com.recentr.core.model.PickupStats
import com.recentr.core.model.UsageRange
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * [RecentrData.screenTime]: the [device] numbers while usage access is granted, the [noAccess]
 * ones (empty) otherwise. [recheckUsageAccess] runs on every resume (the onboarding sends the user to Settings,
 * who can also revoke the access there); a change updates [source] so Home re-syncs.
 */
class UsageAccessScreenTimeRepository(
    private val hasUsageAccess: () -> Boolean,
    private val device: ScreenTimeRepository,
    private val noAccess: ScreenTimeRepository,
) : ScreenTimeRepository {

    private val current = MutableStateFlow(currentSource())

    override val source: StateFlow<ScreenTimeSource> = current.asStateFlow()

    fun recheckUsageAccess() {
        current.value = currentSource()
    }

    override fun summary(range: UsageRange): Flow<ScreenTimeState> = flow {
        if (current.value == ScreenTimeSource.Device) {
            emitAll(
                device.summary(range).catch { error ->
                    // Access revoked while the app was in the foreground: nothing to show any more.
                    if (error !is SecurityException) throw error
                    recheckUsageAccess()
                    emitAll(noAccess.summary(range))
                },
            )
        } else {
            emitAll(noAccess.summary(range))
        }
    }

    override suspend fun dailyStats(from: LocalDate, to: LocalDate): List<DayStats> = fromSource { it.dailyStats(from, to) }

    override suspend fun appDetail(packageName: String, range: UsageRange): AppUsageDetail? =
        fromSource { it.appDetail(packageName, range) }

    override suspend fun pickups(range: UsageRange): PickupStats = fromSource { it.pickups(range) }

    override suspend fun appMinutesSince(start: Long): Map<String, Int> = fromSource { it.appMinutesSince(start) }

    override suspend fun firstDataDay(): LocalDate? = fromSource { it.firstDataDay() }

    /** Runs [read] on the current source, falling back to [noAccess] when access was just revoked. */
    private suspend fun <T> fromSource(read: suspend (ScreenTimeRepository) -> T): T {
        if (current.value != ScreenTimeSource.Device) return read(noAccess)
        return try {
            read(device)
        } catch (_: SecurityException) {
            recheckUsageAccess()
            read(noAccess)
        }
    }

    private fun currentSource(): ScreenTimeSource =
        if (hasUsageAccess()) ScreenTimeSource.Device else ScreenTimeSource.NoAccess
}
