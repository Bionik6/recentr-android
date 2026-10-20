package com.recentr.feature.rules.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleEdit
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.core.model.edit
import com.recentr.core.model.latestWish
import com.recentr.feature.rules.R
import com.recentr.feature.rules.RuleDetailRoute
import com.recentr.feature.rules.model.RulesMessages
import com.recentr.feature.rules.ui.durationText
import com.recentr.feature.rules.ui.pendingWhen
import com.recentr.feature.rules.ui.savedToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

@Immutable
internal data class RuleDetailUiState(
    val rule: Rule? = null,
    val now: Long = System.currentTimeMillis(),
    /** False until the first emission (distinguishes "loading" from "deleted"). */
    val loaded: Boolean = false,
)

/** "Pause Work Hours?" options (Figma 45). */
internal enum class PauseOption(val label: Int) {
    OneHour(R.string.rules_pause_hour),
    UntilTomorrow(R.string.rules_pause_tomorrow),
    UntilResumed(R.string.rules_pause_manual),
}

/**
 * Rule detail (Figma 44): the live rule; Start (sessions, limits), Activate and Turn off (locks),
 * Pause and Resume (routines); option edits; delete; dropping a change kept for later. Every change
 * goes through the edit policy ([RulesRepository.save]): while the rule is engaged, what loosens it
 * waits for the end of its engagement — for a lock or a limit, unless Settings › Change locks &
 * limits anytime is on. [clock] arguments format when ("7:00 AM").
 */
internal class RuleDetailViewModel(
    private val ruleId: String,
    private val repository: RulesRepository,
    private val string: (Int, Array<out Any>) -> String,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    /** Settings › Change locks & limits anytime. */
    changesLocksAnytime: Flow<Boolean> = flowOf(false),
) : ViewModel() {

    /**
     * Settings › Change locks & limits anytime: a lock or a limit that's on changes right away, and
     * Activate and Start don't ask first.
     */
    val changesAnytime: StateFlow<Boolean> = changesLocksAnytime.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }.onEach { repository.settle(it) }

    val state: StateFlow<RuleDetailUiState> = combine(
        repository.rules.map { rules -> rules.firstOrNull { it.id == ruleId } },
        clock,
    ) { rule, now -> RuleDetailUiState(rule, now, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RuleDetailUiState(repository.rule(ruleId)))

    /** What [transform] on the rule's latest wish would do now: to ask first when it blocks more apps right away. */
    fun preview(transform: (Rule) -> Rule): RuleEdit? =
        repository.rule(ruleId)?.let { it.edit(transform(it.latestWish), System.currentTimeMillis(), zone(), changesAnytime.value) }

    /**
     * Saves [transform] of the rule's latest wish. What waits for the end of the engagement is said
     * in a toast; what applies now needs none.
     */
    fun update(clock: (Long) -> String, transform: (Rule) -> Rule) {
        val rule = repository.rule(ruleId) ?: return
        val edit = repository.save(transform(rule.latestWish), changesAnytime = changesAnytime.value)
        if (edit.later != null) RulesMessages.show(savedToast(edit.now, string, clock))
    }

    fun rename(clock: (Long) -> String, name: String, sticker: RuleSticker) = update(clock) {
        it.copy(name = name.ifBlank { it.name }, sticker = sticker)
    }

    fun setApps(clock: (Long) -> String, apps: List<UsageApp>) = update(clock) { it.copy(apps = apps) }

    /**
     * Pauses the routine. During a window the pause starts when the window ends, and lasts from
     * there: an hour, the rest of that day, or until it's turned back on.
     */
    fun pause(option: PauseOption) {
        val rule = repository.rule(ruleId) ?: return
        val now = System.currentTimeMillis()
        val from = rule.engagementEnd(now, zone()) ?: now
        val until = when (option) {
            PauseOption.OneHour -> from + 60 * 60_000L
            PauseOption.UntilTomorrow -> Instant.ofEpochMilli(from).atZone(zone()).toLocalDate().plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli()
            PauseOption.UntilResumed -> Long.MAX_VALUE
        }
        val edit = repository.pause(ruleId, until, now) ?: return
        RulesMessages.show(
            if (edit.later != null) {
                string(R.string.rules_pending_pause, emptyArray())
            } else {
                string(R.string.rules_paused_toast, arrayOf(rule.name))
            },
        )
    }

    fun resume() {
        repository.resume(ruleId)
        repository.rule(ruleId)?.let { RulesMessages.show(string(R.string.rules_resumed_toast, arrayOf(it.name))) }
    }

    /** Start (a session for its length, a limit for today) or Activate (a lock): it holds from now on. */
    fun start() {
        repository.start(ruleId)
        val rule = repository.rule(ruleId) ?: return
        RulesMessages.show(
            when (rule.type) {
                RuleType.Session -> string(R.string.rules_session_started, arrayOf(durationText(rule.durationMinutes ?: 30, string)))
                RuleType.Limit -> string(R.string.rules_limit_started, arrayOf(durationText(rule.limitMinutes ?: 0, string)))
                RuleType.Lock -> string(R.string.rules_lock_activated, arrayOf(rule.name))
                RuleType.Bedtime -> return
            },
        )
    }

    /** Turns the lock off: a loosening, so it goes off at midnight (right away with Change locks & limits anytime). */
    fun turnOff() {
        val rule = repository.rule(ruleId) ?: return
        val edit = repository.save(rule.latestWish.copy(activeSince = null), changesAnytime = changesAnytime.value)
        if (edit.later != null) RulesMessages.show(string(R.string.rules_pending_turn_off, emptyArray()))
    }

    /** Deletes the rule, right away or, while it's engaged, once its engagement ends. */
    fun delete(clock: (Long) -> String) {
        val rule = repository.rule(ruleId) ?: return
        if (repository.delete(ruleId, changesAnytime = changesAnytime.value)) {
            RulesMessages.show(string(R.string.rules_deleted_toast, arrayOf(rule.name)))
        } else {
            val change = repository.rule(ruleId)?.pending ?: return
            RulesMessages.show(
                pendingWhen(
                    rule.type, change,
                    midnight = R.string.rules_pending_delete_midnight,
                    sessionEnd = R.string.rules_pending_delete_session,
                    at = R.string.rules_pending_delete_at,
                    string = string,
                    clock = clock,
                ),
            )
        }
    }

    /** "Drop changes", "Keep rule", "Keep it on", "Cancel": the change kept for later is dropped. */
    fun dropPending() = repository.dropPending(ruleId)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                RuleDetailViewModel(
                    ruleId = createSavedStateHandle().toRoute<RuleDetailRoute>().ruleId,
                    repository = RecentrData.rules,
                    string = { id, args -> app.getString(id, *args) },
                    changesLocksAnytime = RecentrData.settings.settings.map { it.changesLocksAnytime },
                )
            }
        }
    }
}
