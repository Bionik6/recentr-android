package com.recentr.feature.rules.list

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.feature.rules.R
import com.recentr.feature.rules.model.TemplateCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Sections of the rules list, in display order. */
internal enum class RuleGroup(
    val type: RuleType,
    @param:StringRes val label: Int,
    @param:StringRes val addTitle: Int?,
    @param:StringRes val addHint: Int?,
    /** Shown (with its add card) even when empty. */
    val alwaysShown: Boolean,
) {
    Locks(RuleType.Lock, R.string.rules_group_locks, null, null, false),
    Sessions(RuleType.Session, R.string.rules_group_sessions, R.string.rules_add_session, R.string.rules_add_session_hint, true),
    Limits(RuleType.Limit, R.string.rules_group_limits, null, null, false),
    Routines(RuleType.Bedtime, R.string.rules_group_routines, R.string.rules_add_routine, R.string.rules_add_routine_hint, true),
    Habits(RuleType.Habit, R.string.rules_group_habits, R.string.rules_add_habit, R.string.rules_add_habit_hint, false),
}

@Immutable
internal data class RuleSection(val group: RuleGroup, val rules: List<Rule>)

@Immutable
internal data class RulesUiState(
    val now: Long = System.currentTimeMillis(),
    val sections: List<RuleSection> = emptyList(),
    val activeCount: Int = 0,
    val pausedCount: Int = 0,
    val savedTemplates: List<Rule> = emptyList(),
    val showTemplates: Boolean = false,
    val collapsed: Set<RuleGroup> = emptySet(),
    val templatesCollapsed: Boolean = false,
    val filter: TemplateCategory? = null,
)

private data class ListFlags(
    val showTemplates: Boolean = false,
    val collapsed: Set<RuleGroup> = emptySet(),
    val templatesCollapsed: Boolean = false,
    val filter: TemplateCategory? = null,
)

internal class RulesViewModel(private val repository: RulesRepository) : ViewModel() {

    private val flags = MutableStateFlow(ListFlags())

    /** Wall clock ticking every second: drives session countdowns and settles ended sessions. */
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }.onEach { repository.settle(it) }

    val state: StateFlow<RulesUiState> = combine(
        repository.rules,
        repository.savedTemplates,
        clock,
        flags,
    ) { rules, saved, now, flags ->
        val sections = RuleGroup.entries.mapNotNull { group ->
            val inGroup = rules.filter { it.type == group.type }.sortedByDescending { it.isRunning(now) }
            if (inGroup.isEmpty() && !group.alwaysShown) null else RuleSection(group, inGroup)
        }
        val paused = rules.count { it.isPaused(now) }
        RulesUiState(
            now = now,
            sections = sections,
            activeCount = rules.size - paused,
            pausedCount = paused,
            savedTemplates = saved,
            showTemplates = flags.showTemplates,
            collapsed = flags.collapsed,
            templatesCollapsed = flags.templatesCollapsed,
            filter = flags.filter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    fun showTemplates(show: Boolean) = flags.update { it.copy(showTemplates = show) }

    fun toggleGroup(group: RuleGroup) = flags.update {
        it.copy(collapsed = if (group in it.collapsed) it.collapsed - group else it.collapsed + group)
    }

    fun toggleTemplatesGroup() = flags.update { it.copy(templatesCollapsed = !it.templatesCollapsed) }

    fun setFilter(category: TemplateCategory?) = flags.update { it.copy(filter = category) }

    companion object {
        val Factory = viewModelFactory {
            initializer { RulesViewModel(RecentrData.rules) }
        }
    }
}
