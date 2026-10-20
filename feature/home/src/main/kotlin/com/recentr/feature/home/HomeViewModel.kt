package com.recentr.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.data.heldUntil
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UserProfile
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Home & Insight state holder: screen-time summary for the selected period (with the Syncing
 * state of the repository), the daily goal and avatar from the user preferences, and the next
 * rule or the session or routine blocking apps now. A pull to refresh ([refresh]) reads them
 * again under its spinner, keeping the numbers on screen meanwhile.
 *
 * @param profile the stored profile (daily goal, avatar); [saveGoal] stores a new daily goal.
 */
class HomeViewModel(
    private val screenTime: ScreenTimeRepository,
    profile: Flow<UserProfile>,
    private val saveGoal: suspend (minutes: Int) -> Unit,
    rules: RulesRepository,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    showMascot: Flow<Boolean> = flowOf(true),
    clockTicks: Flow<Unit> = minuteTicks(),
) : ViewModel() {

    private data class LocalState(
        val period: UsagePeriod = UsagePeriod.Today,
        val summary: ScreenTimeSummary? = null,
        val isSyncing: Boolean = true,
        /** A pull to refresh is reading the numbers again. */
        val refreshing: Boolean = false,
        val grouping: UsageGrouping = UsageGrouping.Apps,
        /** Bumped on each sync so the "next rule" is recomputed against the clock. */
        val syncCount: Int = 0,
    )

    private val local = MutableStateFlow(LocalState())
    private var syncJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        local,
        profile,
        rules.rules,
        showMascot,
        clockTicks,
    ) { local, profile, ruleList, mascot, _ ->
        val clock = now()
        HomeUiState(
            period = local.period,
            summary = local.summary,
            isSyncing = local.isSyncing,
            isRefreshing = local.refreshing,
            dailyGoalMinutes = profile.dailyGoalMinutes,
            nextRule = nextRule(ruleList, clock),
            activeSession = activeSession(ruleList, clock),
            grouping = local.grouping,
            todayIndex = clock.dayOfWeek.value - 1,
            showMascot = mascot,
            avatarInitial = profile.initial,
            avatarColor = profile.avatarColor,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        sync()
        // Usage access granted (or revoked) in Settings: demo ↔ device numbers.
        viewModelScope.launch { screenTime.source.drop(1).collect { sync() } }
    }

    fun saveDailyGoal(minutes: Int) {
        viewModelScope.launch { saveGoal(minutes) }
    }

    /** Pull to refresh: screen time read again (the next rule follows), without the skeleton. */
    fun refresh() {
        if (local.value.refreshing) return
        local.update { it.copy(refreshing = true) }
        sync(quiet = true)
    }

    fun selectGrouping(grouping: UsageGrouping) {
        local.update { it.copy(grouping = grouping) }
    }

    /** Reads the screen time; [quiet] (a pull to refresh) leaves the numbers up while it does. */
    private fun sync(quiet: Boolean = false) {
        syncJob?.cancel()
        val period = local.value.period
        syncJob = viewModelScope.launch {
            try {
                screenTime.summary(period).collect { state ->
                    when (state) {
                        ScreenTimeState.Syncing -> if (!quiet) local.update { it.copy(isSyncing = true) }
                        is ScreenTimeState.Ready -> local.update {
                            it.copy(summary = state.summary, isSyncing = false, refreshing = false, syncCount = it.syncCount + 1)
                        }
                    }
                }
            } finally {
                // Also when it failed or a newer sync replaced it: the spinner doesn't stay up.
                if (quiet) local.update { it.copy(refreshing = false) }
            }
        }
    }

    companion object {
        /**
         * Ticks now and on every minute boundary, so a routine's window that opens or closes
         * while Home is on screen shows up (windows start and end on the minute).
         */
        private fun minuteTicks(): Flow<Unit> = flow {
            while (true) {
                emit(Unit)
                delay(60_000 - System.currentTimeMillis() % 60_000)
            }
        }

        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    screenTime = RecentrData.screenTime,
                    profile = RecentrData.userPreferences.profile,
                    saveGoal = RecentrData.userPreferences::setDailyGoal,
                    rules = RecentrData.rules,
                    showMascot = RecentrData.settings.settings.map { it.showMascotOnHome },
                )
            }
        }

        /**
         * What blocks apps right now and ends first, if anything: a running session, or a
         * scheduled session or routine inside its window.
         */
        internal fun activeSession(
            rules: List<Rule>,
            now: LocalDateTime,
            zone: ZoneId = ZoneId.systemDefault(),
        ): ActiveSessionUi? {
            val nowMillis = now.atZone(zone).toInstant().toEpochMilli()
            return rules
                .mapNotNull { rule -> rule.blockingUntil(nowMillis, zone)?.let { end -> ActiveSessionUi(rule.id, rule.name, rule.type, end) } }
                .minByOrNull { it.endsAt }
        }

        /**
         * When the rule's countdown ends, as on the Rules list: the running session, or the
         * scheduled window a session or routine is in. Null for locks, limits and habits.
         */
        private fun Rule.blockingUntil(now: Long, zone: ZoneId): Long? = when {
            isRunning(now) -> sessionEndsAt
            (type == RuleType.Session || type == RuleType.Bedtime) && isEnforcing(now, zone) -> heldUntil(now, zone)
            else -> null
        }

        /** Earliest upcoming start among the enabled, scheduled, not paused rules. */
        internal fun nextRule(rules: List<Rule>, now: LocalDateTime): NextRuleUi? {
            val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return rules
                .asSequence()
                .filter { it.enabled && (it.pausedUntil == null || it.pausedUntil!! < nowMillis) }
                .mapNotNull { rule ->
                    val schedule = rule.schedule ?: return@mapNotNull null
                    (0..7).asSequence()
                        .map { offset -> now.toLocalDate().plusDays(offset.toLong()) }
                        .filter { date -> date.dayOfWeek.value in schedule.days }
                        .map { date -> date.atStartOfDay().plusMinutes(schedule.startMinute.toLong()) }
                        .firstOrNull { start -> start.isAfter(now) }
                        ?.let { start -> rule to start }
                }
                .minByOrNull { (_, start) -> start }
                ?.let { (rule, start) ->
                    NextRuleUi(
                        ruleId = rule.id,
                        name = rule.name,
                        type = rule.type,
                        startMinute = rule.schedule!!.startMinute,
                        day = if (start.toLocalDate() == now.toLocalDate()) null else start.dayOfWeek,
                    )
                }
        }
    }
}
