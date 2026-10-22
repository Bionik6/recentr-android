package com.recentr.feature.rules.allowed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.AlwaysAllowedRepository
import com.recentr.core.data.RecentrData
import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.UsageApp
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** Settings › Always allowed at [now]: the apps allowed, and the ones waiting to join. */
@Immutable
internal data class AlwaysAllowedUiState(
    val allowed: AlwaysAllowed = AlwaysAllowed(),
    val now: Long = System.currentTimeMillis(),
) {
    private val settled: AlwaysAllowed get() = allowed.settled(now)

    /** The apps allowed now, by name. */
    val apps: List<UsageApp> get() = settled.apps.byName()

    /** The apps waiting to join, by name. */
    val waiting: List<UsageApp> get() = settled.waiting.byName()

    /** When the [waiting] apps join, or null when none wait. */
    val joinsAt: Long? get() = settled.pending?.takeIf { waiting.isNotEmpty() }?.appliesAt

    /** What the picker opens on: the apps allowed and the ones waiting. */
    val latestWish: List<UsageApp> get() = settled.latestWish

    private fun List<UsageApp>.byName() = sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}

/** When the apps waiting join, for "Your changes apply …". */
internal sealed interface JoinTime {
    /** The next midnight: a lock's or a limit's. */
    data object Midnight : JoinTime

    /** Later today, at [at]. */
    data class Today(val at: Long) : JoinTime

    /** Tomorrow, at [at] (an overnight routine's morning). */
    data class Tomorrow(val at: Long) : JoinTime

    /** Another day, at [at]. */
    data class Later(val at: Long) : JoinTime

    companion object {
        fun of(appliesAt: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): JoinTime {
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val at = Instant.ofEpochMilli(appliesAt).atZone(zone)
            return when (at.toLocalDate()) {
                today -> Today(appliesAt)
                today.plusDays(1) -> if (at.toLocalTime() == LocalTime.MIDNIGHT) Midnight else Tomorrow(appliesAt)
                else -> Later(appliesAt)
            }
        }
    }
}

/**
 * Settings › Always allowed (docs/RULES.md › "Always allowed"): the apps no rule and no focus
 * session puts on hold. A new choice from the picker ([allow]) goes through the edit policy: the
 * apps whose rules are engaged wait to join. Removing one ([remove]) and dropping what waits
 * ([dropChanges]) apply now. The [ticks] let the waiting apps join while the screen is open.
 */
internal class AlwaysAllowedViewModel(
    private val repository: AlwaysAllowedRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    ticks: Flow<Long> = secondTicks(),
) : ViewModel() {

    val state: StateFlow<AlwaysAllowedUiState> = combine(repository.allowed, ticks.onEach(repository::settle)) { allowed, now ->
        AlwaysAllowedUiState(allowed, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlwaysAllowedUiState(repository.allowed.value, clock()))

    /** The picker's "Allow": [apps] in place of the list and of what waits. */
    fun allow(apps: List<UsageApp>) {
        repository.choose(apps, clock())
    }

    fun remove(app: UsageApp) = repository.remove(app.packageName, clock())

    fun dropChanges() = repository.dropPending()

    companion object {
        private fun secondTicks(): Flow<Long> = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(1_000)
            }
        }

        val Factory = viewModelFactory {
            initializer { AlwaysAllowedViewModel(RecentrData.alwaysAllowed) }
        }
    }
}
