package com.recentr.feature.rules.editor

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.recentr.core.data.InstalledAppsRepository
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.RuleEditorRoute
import com.recentr.feature.rules.model.BuiltInTemplate
import com.recentr.feature.rules.model.RuleFactory
import com.recentr.feature.rules.model.RulesMessages
import com.recentr.feature.rules.ui.durationText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
internal data class RuleEditorUiState(
    val draft: Rule,
    /** Creating (vs editing an existing rule). */
    val isNew: Boolean,
    /** The sheet title shows the rule name (renamed / template / existing) instead of the type. */
    val customTitle: Boolean,
    /** A new session offers "Save as template" (not when started from Home). */
    val offerTemplate: Boolean = true,
)

/** Create / edit sheet of a rule (Figma 31, 35, 38, 39, 40) — works on an in-memory draft. */
internal class RuleEditorViewModel(
    route: RuleEditorRoute,
    private val repository: RulesRepository,
    private val installedApps: InstalledAppsRepository,
    private val factory: RuleFactory,
    private val string: (Int, Array<out Any>) -> String,
    /** Settings › Strict mode: new sessions and routines start strict. */
    strictByDefault: Flow<Boolean> = flowOf(false),
) : ViewModel() {

    private val _state = MutableStateFlow(initialState(route))
    val state: StateFlow<RuleEditorUiState> = _state.asStateFlow()

    init {
        val initial = _state.value
        if (initial.isNew && initial.draft.canBeStrict && !initial.draft.strictMode) {
            viewModelScope.launch {
                if (strictByDefault.first()) _state.update { it.copy(draft = it.draft.copy(strictMode = true)) }
            }
        }
        // A built-in template blocks the device's feeds: fill them in once the apps are read.
        val template = route.templateId?.let(BuiltInTemplate::byId)
        if (initial.isNew && template != null && installedApps.apps.value == null) {
            viewModelScope.launch {
                val apps = factory.fromTemplate(template, installedApps.load()).apps
                _state.update { if (it.draft.apps.isEmpty()) it.copy(draft = it.draft.copy(apps = apps)) else it }
            }
        }
    }

    private fun initialState(route: RuleEditorRoute): RuleEditorUiState {
        route.ruleId?.let(repository::rule)?.let { return RuleEditorUiState(it, isNew = false, customTitle = true) }
        val builtIn = route.templateId?.let(BuiltInTemplate::byId)
        val saved = route.templateId?.let(repository::savedTemplate)
        val draft = when {
            builtIn != null -> factory.fromTemplate(builtIn, installedApps.apps.value.orEmpty())
            saved != null -> factory.fromSavedTemplate(saved)
            else -> factory.newRule(RuleType.valueOf(route.type))
        }
        val state = RuleEditorUiState(draft, isNew = true, customTitle = builtIn != null || saved != null, offerTemplate = route.offerTemplate)
        val apps = route.apps?.split(',')?.mapNotNull(installedApps::app).orEmpty()
        return if (apps.isEmpty()) state else state.withApps(apps)
    }

    fun update(transform: (Rule) -> Rule) = _state.update { it.copy(draft = transform(it.draft)) }

    /** New apps; rules that are still unnamed take a name built from them ("Instagram & X"). */
    fun setApps(apps: List<UsageApp>) = _state.update { it.withApps(apps) }

    private fun RuleEditorUiState.withApps(apps: List<UsageApp>): RuleEditorUiState {
        val draft = draft.copy(apps = apps)
        val named = if (customTitle) draft else when (draft.type) {
            RuleType.Lock -> draft.copy(name = factory.lockName(apps))
            RuleType.Limit -> draft.copy(name = factory.limitName(apps))
            RuleType.Habit -> draft.copy(name = factory.habitName(apps))
            else -> draft
        }
        return copy(draft = named)
    }

    fun rename(name: String, sticker: RuleSticker) = _state.update {
        it.copy(draft = it.draft.copy(name = name.ifBlank { it.draft.name }, sticker = sticker), customTitle = true)
    }

    /** Session "Suggestions": take the template's name, length and sticker. */
    fun applySuggestion(template: BuiltInTemplate) = _state.update {
        it.copy(
            draft = it.draft.copy(
                name = string(template.title, emptyArray()),
                durationMinutes = template.schedule.lengthMinutes,
                sticker = template.sticker,
            ),
            customTitle = true,
        )
    }

    fun saveAsTemplate() {
        repository.saveTemplate(_state.value.draft)
        RulesMessages.show(string(R.string.rules_template_saved, emptyArray()))
    }

    /** Creates / saves the rule. A new unscheduled session starts right away. */
    fun commit() {
        val state = _state.value
        val draft = state.draft
        when {
            state.isNew && draft.type == RuleType.Session && draft.schedule == null -> {
                repository.upsert(draft.copy(activeSince = System.currentTimeMillis(), oneShot = true))
                val length = durationText(draft.durationMinutes ?: 30, string)
                RulesMessages.show(string(R.string.rules_session_started, arrayOf(length)))
            }
            state.isNew -> {
                repository.upsert(draft)
                RulesMessages.show(string(R.string.rules_rule_created, arrayOf(draft.name)))
            }
            else -> {
                repository.upsert(draft)
                RulesMessages.show(string(R.string.rules_rule_saved, emptyArray()))
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val string: (Int, Array<out Any>) -> String = { id, args -> app.getString(id, *args) }
                val repository = RecentrData.rules
                RuleEditorViewModel(
                    route = createSavedStateHandle().toRoute<RuleEditorRoute>(),
                    repository = repository,
                    installedApps = RecentrData.installedApps,
                    factory = RuleFactory(string, repository::newId),
                    string = string,
                    strictByDefault = RecentrData.settings.settings.map { it.strictByDefault },
                )
            }
        }
    }
}
