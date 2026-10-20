package com.recentr.feature.insights.app

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.usedOf
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
internal data class AppDetailUiState(
    /** The day, week or month chosen in the overview. */
    val range: UsageRange,
    val today: LocalDate = LocalDate.now(),
    val detail: AppUsageDetail? = null,
    val loading: Boolean = true,
    /** The limit covering this app, if any. */
    val limitRule: Rule? = null,
    /** Minutes the apps of [limitRule] used since its Start today (the limit is shared by its apps); null when it isn't started. */
    val limitUsedMinutes: Int? = null,
) {
    /** One day's hours or several days' (today only shows the limit's use). */
    val period: UsagePeriod get() = range.period(today)
}

/** 100 · App detail: one app over the day, week or month chosen in the overview, and its limit. */
internal class AppDetailViewModel(
    private val packageName: String,
    private val range: UsageRange,
    private val screenTime: ScreenTimeRepository,
    rules: RulesRepository,
) : ViewModel() {

    private val local = MutableStateFlow(AppDetailUiState(range))

    val state: StateFlow<AppDetailUiState> = combine(local, rules.rules) { state, ruleList ->
        val app = state.detail?.app
        state.copy(limitRule = ruleList.firstOrNull { it.type == RuleType.Limit && it.enabled && app != null && it.targets(app.packageName) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    init {
        viewModelScope.launch {
            val detail = screenTime.appDetail(packageName, range)
            local.update { it.copy(detail = detail, loading = false) }
            // The limit's apps' use since its Start today, for "14m over your limit".
            if (state.value.period == UsagePeriod.Today && detail != null) {
                val rule = state.value.limitRule ?: rules.rules.value.firstOrNull { rule ->
                    rule.type == RuleType.Limit && rule.enabled && rule.targets(detail.app.packageName)
                }
                val start = rule?.activeSince?.takeIf { rule.isEnforcing(System.currentTimeMillis()) }
                if (rule != null && start != null) {
                    val used = rule.usedOf(screenTime.appMinutesSince(start))
                    local.update { it.copy(limitUsedMinutes = used) }
                }
            }
        }
    }

    companion object {
        fun factory(packageName: String, range: UsageRange) = viewModelFactory {
            initializer { AppDetailViewModel(packageName, range, RecentrData.screenTime, RecentrData.rules) }
        }
    }
}
