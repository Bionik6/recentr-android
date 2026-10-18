package com.recentr.feature.insights.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.data.UserPreferencesRepository
import com.recentr.core.designsystem.component.DailyGoalSheet
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.DayStats
import com.recentr.core.model.UserProfile
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.GoalRing
import com.recentr.feature.insights.ui.InsightsCard
import com.recentr.feature.insights.ui.InsightsScaffold
import com.recentr.feature.insights.ui.InsightsTopBar
import com.recentr.feature.insights.ui.ProgressTrack
import com.recentr.feature.insights.ui.compactDuration
import com.recentr.feature.insights.ui.duration
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class DailyGoalUiState(
    val goalMinutes: Int = UserProfile().dailyGoalMinutes,
    /** The last seven days, oldest first, today last; null while loading. */
    val days: List<DayStats>? = null,
) {
    val daysOnTarget: Int get() = days.orEmpty().count { it.hasData && it.minutes <= goalMinutes }
}

/** 104 · Daily goal: the last seven days against the goal of the user preferences. */
internal class DailyGoalViewModel(
    private val screenTime: ScreenTimeRepository,
    private val userPreferences: UserPreferencesRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val days = MutableStateFlow<List<DayStats>?>(null)

    val state: StateFlow<DailyGoalUiState> = combine(userPreferences.profile.map { it.dailyGoalMinutes }, days) { goal, days ->
        DailyGoalUiState(goal, days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyGoalUiState())

    init {
        viewModelScope.launch {
            val date = today()
            days.value = screenTime.dailyStats(date.minusDays(6), date)
        }
    }

    fun saveGoal(minutes: Int) {
        viewModelScope.launch { userPreferences.setDailyGoal(minutes) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { DailyGoalViewModel(RecentrData.screenTime, RecentrData.userPreferences) }
        }
    }
}

/**
 * 104 · Insights — Daily goal: the ring of days on target this past week, one bar per day
 * (green within the goal, tangerine over it) and the goal editor.
 */
@Composable
internal fun DailyGoalScreen(
    state: DailyGoalUiState,
    onBack: () -> Unit,
    onSaveGoal: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    val days = state.days.orEmpty()
    InsightsScaffold(
        modifier = modifier,
        topBar = {
            InsightsTopBar(
                title = stringResource(R.string.insights_daily_goal),
                onBack = onBack,
                trailing = {
                    RecentrIconButton(
                        icon = RecentrIcon.Pencil,
                        contentDescription = stringResource(R.string.insights_edit_goal),
                        onClick = { editing = true },
                    )
                },
            )
        },
        bottomBar = {
            RecentrButton(
                text = stringResource(R.string.insights_adjust_goal),
                onClick = { editing = true },
                style = RecentrButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GoalRing(fraction = if (days.isEmpty()) 0f else state.daysOnTarget / 7f)
            StickerImage(Sticker.Target, size = 96.dp)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.insights_days_on_target, state.daysOnTarget, 7),
                style = RecentrTheme.typography.title2,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.insights_goal_under, duration(state.goalMinutes)),
                style = RecentrTheme.typography.bodyRegular,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
        if (days.isNotEmpty()) {
            val names = stringArrayResource(R.array.insights_weekday_short)
            // The busiest day fills 88% of the track, like the design.
            val scale = maxOf(days.maxOf { it.minutes }, state.goalMinutes, 1) / 0.885f
            InsightsCard(spacing = 10.dp) {
                days.forEach { day ->
                    val name = names[day.date.dayOfWeek.value - 1]
                    val value = if (day.hasData) duration(day.minutes) else stringResource(R.string.insights_no_value)
                    Row(
                        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$name: $value" },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(name, style = RecentrTheme.typography.captionMedium, color = colors.textSecondary, modifier = Modifier.width(34.dp), maxLines = 1)
                        ProgressTrack(
                            fraction = if (day.hasData) day.minutes / scale else 0f,
                            height = 10.dp,
                            color = if (day.minutes <= state.goalMinutes) colors.feedbackSuccess else colors.accentTangerine,
                            modifier = Modifier.weight(1f),
                        )
                        Text(value, style = RecentrTheme.typography.captionMedium, color = colors.textPrimary, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(56.dp))
                    }
                }
            }
        }
    }
    if (editing) {
        DailyGoalSheet(
            currentGoalMinutes = state.goalMinutes,
            onSave = onSaveGoal,
            onDismissRequest = { editing = false },
            formatMinutes = { compactDuration(it) },
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun DailyGoalPreview() {
    val today = LocalDate.now()
    val minutes = listOf(125, 190, 100, 140, 175, 75, 130)
    RecentrTheme {
        DailyGoalScreen(
            state = DailyGoalUiState(
                goalMinutes = 150,
                days = minutes.mapIndexed { index, value -> DayStats(today.minusDays(6L - index), true, value, 20) },
            ),
            onBack = {},
            onSaveGoal = {},
        )
    }
}
