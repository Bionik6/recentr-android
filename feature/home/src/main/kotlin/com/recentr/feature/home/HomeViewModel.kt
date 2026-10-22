package com.recentr.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeSource
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.data.heldUntil
import com.recentr.core.model.AlwaysAllowed
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
 * routine or the session or routine blocking apps now (a countdown only: a session can't be
 * stopped, it ends on its own). A pull to refresh ([refresh]) reads them
 * again under its spinner, keeping the numbers on screen meanwhile. Without usage access the
 * numbers are empty, and [HomeUiState.needsUsageAccess] asks for it instead of showing them.
 *
 * From the second rule, until the card is closed ([dismissAlwaysAllowedCard]), Home shows the apps
 * always allowed, or that there are none yet ([HomeUiState.showAlwaysAllowedCard]).
 *
 * @param profile the stored profile (daily goal, avatar); [saveGoal] stores a new daily goal.
 * @param alwaysAllowed the apps always allowed; [alwaysAllowedCardDismissed] whether the card was
 *   closed, which [dismissCard] stores.
 */
class HomeViewModel(
    private val screenTime: ScreenTimeRepository,
    profile: Flow<UserProfile>,
    private val saveGoal: suspend (minutes: Int) -> Unit,
    rules: RulesRepository,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    showMascot: Flow<Boolean> = flowOf(true),
    clockTicks: Flow<Unit> = minuteTicks(),
    alwaysAllowed: Flow<AlwaysAllowed> = flowOf(AlwaysAllowed()),
    alwaysAllowedCardDismissed: Flow<Boolean> = flowOf(false),
    private val dismissCard: suspend () -> Unit = {},
) : ViewModel() {

    private data class LocalState(
        val period: UsagePeriod = UsagePeriod.Today,
        val summary: ScreenTimeSummary? = null,
        /** Whether [summary] was read with usage access (device numbers) or without (empty). */
        val usageAccess: Boolean = true,
        val isSyncing: Boolean = true,
        /** A pull to refresh is reading the numbers again. */
        val refreshing: Boolean = false,
        val grouping: UsageGrouping = UsageGrouping.Apps,
        /** Bumped on each sync so the "next rule" is recomputed against the clock. */
        val syncCount: Int = 0,
    )

    private val local = MutableStateFlow(LocalState())
    private var syncJob: Job? = null

    /** Settings › Appearance › "Show mascot on Home", the apps always allowed and whether their card was closed. */
    private val preferences = combine(showMascot, alwaysAllowed, alwaysAllowedCardDismissed, ::Triple)

    val uiState: StateFlow<HomeUiState> = combine(
        local,
        profile,
        rules.rules,
        preferences,
        clockTicks,
    ) { local, profile, ruleList, (mascot, allowed, cardDismissed), _ ->
        val clock = now()
        HomeUiState(
            period = local.period,
            summary = local.summary,
            usageAccess = local.usageAccess,
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
            showAlwaysAllowedCard = showsAlwaysAllowedCard(ruleList, cardDismissed),
            alwaysAllowedApps = allowed.settled(clock.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).latestWish,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        sync()
        // Usage access granted (or revoked) in Settings: empty ↔ device numbers.
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

    /** The card's ×: it doesn't come back. */
    fun dismissAlwaysAllowedCard() {
        viewModelScope.launch { dismissCard() }
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
                            it.copy(
                                summary = state.summary,
                                usageAccess = screenTime.source.value == ScreenTimeSource.Device,
                                isSyncing = false,
                                refreshing = false,
                                syncCount = it.syncCount + 1,
                            )
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
                    alwaysAllowed = RecentrData.alwaysAllowed.allowed,
                    alwaysAllowedCardDismissed = RecentrData.settings.settings.map { it.alwaysAllowedCardDismissed },
                    dismissCard = { RecentrData.settings.update { it.copy(alwaysAllowedCardDismissed = true) } },
                )
            }
        }

        /**
         * Whether Home shows the Always allowed card: from the second rule made, paused or not (the
         * first one is enough to take in), until the card is closed. It shows the apps allowed, or
         * says there are none yet.
         */
        internal fun showsAlwaysAllowedCard(rules: List<Rule>, dismissed: Boolean): Boolean =
            !dismissed && rules.size >= 2

        /**
         * What blocks apps right now and ends first, if anything: a running session, or a routine
         * inside its window.
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
         * When the rule's countdown ends, as on the Rules list: the running session, or the window
         * a routine is in. Null for locks and limits, which hold without one.
         */
        private fun Rule.blockingUntil(now: Long, zone: ZoneId): Long? = when (type) {
            RuleType.Session -> sessionEndsAt?.takeIf { isRunning(now) }
            RuleType.Bedtime -> heldUntil(now, zone)?.takeIf { isEnforcing(now, zone) }
            RuleType.Lock, RuleType.Limit -> null
        }

        /**
         * Earliest upcoming start among the enabled routines that aren't paused: the only rules
         * with days and hours.
         */
        internal fun nextRule(rules: List<Rule>, now: LocalDateTime): NextRuleUi? {
            val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return rules
                .asSequence()
                .filter { it.type == RuleType.Bedtime && it.enabled && (it.pausedUntil == null || it.pausedUntil!! < nowMillis) }
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
