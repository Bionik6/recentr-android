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
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.RuleDetailRoute
import com.recentr.feature.rules.model.RulesMessages
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
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

/** Rule detail (Figma 44): live rule, pause / resume, inline option edits, delete. */
internal class RuleDetailViewModel(
    private val ruleId: String,
    private val repository: RulesRepository,
    private val string: (Int, Array<out Any>) -> String,
) : ViewModel() {

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

    fun update(transform: (Rule) -> Rule) = repository.update(ruleId, transform)

    fun rename(name: String, sticker: RuleSticker) = repository.update(ruleId) {
        it.copy(name = name.ifBlank { it.name }, sticker = sticker)
    }

    fun setApps(apps: List<UsageApp>) = repository.update(ruleId) { it.copy(apps = apps) }

    fun pause(option: PauseOption) {
        val now = System.currentTimeMillis()
        val until = when (option) {
            PauseOption.OneHour -> now + 60 * 60_000L
            PauseOption.UntilTomorrow -> LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            PauseOption.UntilResumed -> Long.MAX_VALUE
        }
        repository.pause(ruleId, until)
        repository.rule(ruleId)?.let { RulesMessages.show(string(R.string.rules_paused_toast, arrayOf(it.name))) }
    }

    fun resume() {
        repository.resume(ruleId)
        repository.rule(ruleId)?.let { RulesMessages.show(string(R.string.rules_resumed_toast, arrayOf(it.name))) }
    }

    fun delete() {
        val name = repository.rule(ruleId)?.name
        repository.delete(ruleId)
        if (name != null) RulesMessages.show(string(R.string.rules_deleted_toast, arrayOf(name)))
    }

    fun strictLockedNotice() = RulesMessages.show(string(R.string.rules_strict_locked_toast, emptyArray()))

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                RuleDetailViewModel(
                    ruleId = createSavedStateHandle().toRoute<RuleDetailRoute>().ruleId,
                    repository = RecentrData.rules,
                    string = { id, args -> app.getString(id, *args) },
                )
            }
        }
    }
}
