package com.recentr.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.data.UserPreferencesRepository
import com.recentr.core.model.Rule
import com.recentr.core.model.ScreenTimeSummary
import com.recentr.core.model.UsagePeriod
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Home & Insight state holder: screen-time summary for the selected period (with the Syncing
 * state of the repository), the daily goal from the user preferences and the next rule.
 */
class HomeViewModel(
    private val screenTime: ScreenTimeRepository,
    private val userPreferences: UserPreferencesRepository,
    rules: RulesRepository,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    showMascot: Flow<Boolean> = flowOf(true),
) : ViewModel() {

    private data class LocalState(
        val period: UsagePeriod = UsagePeriod.Today,
        val summary: ScreenTimeSummary? = null,
        val isSyncing: Boolean = true,
        val wellSpent: Set<String> = emptySet(),
        val grouping: UsageGrouping = UsageGrouping.Apps,
        /** Bumped on each sync so the "next rule" is recomputed against the clock. */
        val syncCount: Int = 0,
    )

    private val local = MutableStateFlow(LocalState())
    private var syncJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        local,
        userPreferences.profile.map { it.dailyGoalMinutes },
        rules.rules,
        showMascot,
    ) { local, goal, ruleList, mascot ->
        val clock = now()
        HomeUiState(
            period = local.period,
            summary = local.summary,
            isSyncing = local.isSyncing,
            dailyGoalMinutes = goal,
            nextRule = nextRule(ruleList, clock),
            activeSession = activeSession(ruleList, clock),
            wellSpent = local.wellSpent,
            grouping = local.grouping,
            todayIndex = clock.dayOfWeek.value - 1,
            showMascot = mascot,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        sync()
        // Usage access granted (or revoked) in Settings: demo ↔ device numbers.
        viewModelScope.launch { screenTime.source.drop(1).collect { sync() } }
    }

    fun selectPeriod(period: UsagePeriod) {
        if (period == local.value.period && !local.value.isSyncing) return
        local.update { it.copy(period = period) }
        sync()
    }

    /** Refresh button: re-aggregates screen time (shows the Syncing skeleton). */
    fun refresh() = sync()

    fun saveDailyGoal(minutes: Int) {
        viewModelScope.launch { userPreferences.setDailyGoal(minutes) }
    }

    fun toggleWellSpent(packageName: String) {
        local.update { state ->
            state.copy(
                wellSpent = if (packageName in state.wellSpent) state.wellSpent - packageName else state.wellSpent + packageName,
            )
        }
    }

    fun selectGrouping(grouping: UsageGrouping) {
        local.update { it.copy(grouping = grouping) }
    }

    private fun sync() {
        syncJob?.cancel()
        val period = local.value.period
        syncJob = viewModelScope.launch {
            screenTime.summary(period).collect { state ->
                when (state) {
                    ScreenTimeState.Syncing -> local.update { it.copy(isSyncing = true) }
                    is ScreenTimeState.Ready -> local.update {
                        it.copy(summary = state.summary, isSyncing = false, syncCount = it.syncCount + 1)
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    screenTime = RecentrData.screenTime,
                    userPreferences = RecentrData.userPreferences,
                    rules = RecentrData.rules,
                    showMascot = RecentrData.settings.settings.map { it.showMascotOnHome },
                )
            }
        }

        /** The running session that ends first, if any. */
        internal fun activeSession(rules: List<Rule>, now: LocalDateTime): ActiveSessionUi? {
            val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return rules
                .filter { it.isRunning(nowMillis) }
                .mapNotNull { rule -> rule.sessionEndsAt?.let { end -> ActiveSessionUi(rule.id, rule.name, rule.type, end) } }
                .minByOrNull { it.endsAt }
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
