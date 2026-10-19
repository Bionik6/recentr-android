package com.recentr.feature.insights.app

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.RulesRepository
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.periodStart
import com.recentr.core.data.usedOf
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsagePeriod
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    /** Minutes the apps of [limitRule] used in its current period (the limit is shared by its apps). */
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
            // The limit's apps' use since it last reset (midnight, 4:00 or Monday), for "14m over your limit".
            if (period == UsagePeriod.Today && detail != null) {
                val rule = state.value.limitRule ?: rules.rules.value.firstOrNull { rule ->
                    rule.type == RuleType.Limit && rule.enabled && rule.targets(detail.app.packageName)
                }
                if (rule != null) {
                    val start = rule.limitReset.periodStart(System.currentTimeMillis(), ZoneId.systemDefault())
                    val used = rule.usedOf(screenTime.appMinutesSince(start))
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
