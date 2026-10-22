package com.recentr.core.data

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import java.time.ZoneId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * The app blocker's state: which opened app is on hold ([HoldState]) given the rules, the focus
 * session, the device's apps and the apps always allowed, the apps opened anyway, and the use of
 * the apps of each limit since its Start ([LimitUsage]). Read by the accessibility service and the
 * hold screen.
 */
class AppHoldRepository(
    private val rules: RulesRepository,
    private val focus: FocusRepository,
    private val installedApps: InstalledAppsRepository,
    private val screenTime: ScreenTimeRepository,
    private val settings: SettingsRepository,
    private val alwaysAllowed: AlwaysAllowedRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _unlocks = MutableStateFlow(AppUnlocks())

    /** Apps opened anyway, and past which rules. */
    val unlocks: StateFlow<AppUnlocks> = _unlocks.asStateFlow()

    @Volatile
    private var limitUsage: LimitUsage? = null

    private fun state(): HoldState = HoldState(
        rules = rules.rules.value,
        focus = focus.run.value,
        protectedApps = installedApps.protectedPackages,
        alwaysAllowed = alwaysAllowed.allowed.value,
        unlocks = _unlocks.value,
        limitUsage = limitUsage,
    )

    /** Why [packageName] is on hold at [now], or null when it may be used. */
    fun reason(packageName: String, now: Long): HoldReason? = state().reasonFor(packageName, now, zone())

    /**
     * "Unlock anyway" through [rule]: [packageName] stays open for [minutes] past the rules holding
     * it now ([HoldState.liftedBy]), using one of [rule]'s unlocks.
     */
    fun openAnyway(rule: Rule, packageName: String, now: Long, minutes: Int = rule.unlockDurationMinutes) {
        val lifted = state().liftedBy(rule, packageName, now, zone())
        _unlocks.update { it.open(packageName, lifted, now + minutes * 60_000L) }
        rules.update(rule.id) { it.copy(unlocksUsedToday = it.unlocksUsedToday + 1) }
    }

    /**
     * Emergency unlock (54): [packageName] stays open for [EmergencyUnlocks.MINUTES] past every
     * rule holding it now, Strict mode and limits included, spending one of the month's emergency
     * unlocks; their streaks start over. False when none are left.
     */
    suspend fun emergencyUnlock(packageName: String, now: Long): Boolean = withContext(NonCancellable) {
        // Both or neither, even if the hold screen goes away meanwhile: never spent without opening.
        if (!settings.spendEmergencyUnlock(now, zone())) return@withContext false
        val held = state().holding(packageName, now, zone())
        _unlocks.update { it.open(packageName, held, now + EmergencyUnlocks.MINUTES * 60_000L) }
        rules.breakStreaks(held.map { it.id }, now)
        true
    }

    /** "Lock it again" (55): ends [packageName]'s unlock now. */
    fun lockAgain(packageName: String, now: Long) {
        _unlocks.update { it.lockAgain(packageName, now) }
    }

    /**
     * Since when [packageName] has been kept closed by [rule], for the shield's "You've stayed off
     * for…": the rule's [blockStart], or the end of the app's last unlock past it if that came later.
     */
    fun stayedOffSince(rule: Rule, packageName: String, now: Long): Long? =
        listOfNotNull(rule.blockStart(now, zone()), _unlocks.value.lastEnd(packageName, rule, now)).maxOrNull()

    /**
     * Re-reads the minutes per app since the Start of each limit running today (device numbers
     * only), for the limits to trip and for their cards ([RulesRepository.recordUsage]).
     */
    suspend fun refreshUsage() {
        val now = clock()
        val starts = rules.rules.value
            .filter { it.type == RuleType.Limit && it.isEnforcing(now, zone()) }
            .mapNotNull { it.activeSince }
            .toSet()
        limitUsage = if (starts.isNotEmpty() && screenTime.source.value == ScreenTimeSource.Device) {
            LimitUsage(starts.associateWith { screenTime.appMinutesSince(it) }).also(rules::recordUsage)
        } else {
            null
        }
    }
}
