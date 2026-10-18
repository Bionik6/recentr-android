package com.recentr.core.data

import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsagePeriod
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * The app blocker's state: which opened app is on hold ([HoldState]) given the rules, the focus
 * session and the device's apps, the apps opened anyway, and today's use of the apps under a
 * daily limit. Read by the accessibility service and the hold screen.
 */
class AppHoldRepository(
    private val rules: RulesRepository,
    private val focus: FocusRepository,
    private val installedApps: InstalledAppsRepository,
    private val screenTime: ScreenTimeRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val _openUntil = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** Apps opened anyway, and until when (epoch millis). */
    val openUntil: StateFlow<Map<String, Long>> = _openUntil.asStateFlow()

    @Volatile
    private var usedToday: Map<String, Int>? = null

    /** Why [packageName] is on hold at [now], or null when it may be used. */
    fun reason(packageName: String, now: Long): HoldReason? = HoldState(
        rules = rules.rules.value,
        focus = focus.run.value,
        protectedApps = installedApps.protectedPackages,
        openUntil = _openUntil.value,
        usedToday = usedToday,
    ).reasonFor(packageName, now, zone())

    /** "Open anyway" through [rule]: [packageName] stays open for the rule's unlock length, using one unlock. */
    fun openAnyway(rule: Rule, packageName: String, now: Long) {
        _openUntil.update { open ->
            open.filterValues { it > now } + (packageName to now + rule.unlockDurationMinutes * 60_000L)
        }
        rules.update(rule.id) { it.copy(unlocksUsedToday = it.unlocksUsedToday + 1) }
    }

    /** Re-reads today's minutes per app while a daily limit is active (device numbers only). */
    suspend fun refreshUsage(now: Long) {
        val limited = rules.rules.value.any { it.type == RuleType.Limit && it.isEnforcing(now) }
        usedToday = if (limited && screenTime.source.value == ScreenTimeSource.Device) {
            screenTime.summary(UsagePeriod.Today)
                .filterIsInstance<ScreenTimeState.Ready>()
                .first()
                .summary.mostUsed.associate { it.app.packageName to it.minutes }
        } else {
            null
        }
    }
}
