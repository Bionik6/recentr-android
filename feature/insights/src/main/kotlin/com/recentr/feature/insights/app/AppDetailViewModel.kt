package com.recentr.feature.insights.app

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.ScreenTimeState
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsagePeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
internal data class AppDetailUiState(
    val period: UsagePeriod,
    val detail: AppUsageDetail? = null,
    val loading: Boolean = true,
    /** The daily limit covering this app, if any. */
    val limitRule: Rule? = null,
    /** Minutes the apps of [limitRule] used today (the limit is shared by its apps). */
    val limitUsedMinutes: Int? = null,
)

/** 100 · App detail: one app over the period chosen in the overview, and its daily limit. */
internal class AppDetailViewModel(
    private val packageName: String,
    private val period: UsagePeriod,
    private val screenTime: ScreenTimeRepository,
    rules: RulesRepository,
) : ViewModel() {

    private val local = MutableStateFlow(AppDetailUiState(period))

    val state: StateFlow<AppDetailUiState> = combine(local, rules.rules) { state, ruleList ->
        val app = state.detail?.app
        state.copy(limitRule = ruleList.firstOrNull { it.type == RuleType.Limit && it.enabled && app != null && it.targets(app.packageName) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    init {
        viewModelScope.launch {
            val detail = screenTime.appDetail(packageName, period)
            local.update { it.copy(detail = detail, loading = false) }
            // Today's use of the limit's apps, for "14m over your limit".
            if (period == UsagePeriod.Today && detail != null) {
                val today = screenTime.summary(UsagePeriod.Today).filterIsInstance<ScreenTimeState.Ready>().first().summary
                val rule = state.value.limitRule ?: rules.rules.value.firstOrNull { rule ->
                    rule.type == RuleType.Limit && rule.enabled && rule.targets(detail.app.packageName)
                }
                if (rule != null) {
                    val used = today.mostUsed.filter { rule.targets(it.app.packageName) }.sumOf { it.minutes }
                    local.update { it.copy(limitUsedMinutes = used) }
                }
            }
        }
    }

    companion object {
        fun factory(packageName: String, period: UsagePeriod) = viewModelFactory {
            initializer { AppDetailViewModel(packageName, period, RecentrData.screenTime, RecentrData.rules) }
        }
    }
}
