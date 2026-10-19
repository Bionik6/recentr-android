package com.recentr.feature.insights.pickups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recentr.core.data.RecentrData
import com.recentr.core.data.ScreenTimeRepository
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppId
import com.recentr.core.model.AppOpenCount
import com.recentr.core.model.PickupStats
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import com.recentr.feature.insights.ui.RangeName
import com.recentr.feature.insights.ui.name
import com.recentr.feature.insights.ui.rangeLabel
import java.time.LocalDate
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.CardTitle
import com.recentr.feature.insights.ui.IconChip
import com.recentr.feature.insights.ui.InsightsCard
import com.recentr.feature.insights.ui.InsightsScaffold
import com.recentr.feature.insights.ui.InsightsTopBar
import com.recentr.feature.insights.ui.LoadingBlock
import com.recentr.feature.insights.ui.MoreMenuButton
import com.recentr.feature.insights.ui.PickupHeatStrip
import com.recentr.feature.insights.ui.UsageAppIcon
import com.recentr.feature.insights.ui.clock
import com.recentr.feature.insights.ui.hourLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
internal data class PickupsUiState(
    /** The day, week or month chosen in the overview. */
    val range: UsageRange,
    val stats: PickupStats? = null,
    val today: LocalDate = LocalDate.now(),
) {
    val period: UsagePeriod get() = range.period(today)
}

/** 101 · Pickups over the day, week or month chosen in the overview. */
internal class PickupsViewModel(range: UsageRange, screenTime: ScreenTimeRepository) : ViewModel() {
    private val _state = MutableStateFlow(PickupsUiState(range))
    val state: StateFlow<PickupsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = PickupsUiState(range, screenTime.pickups(range)) }
    }

    companion object {
        fun factory(range: UsageRange) = viewModelFactory {
            initializer { PickupsViewModel(range, RecentrData.screenTime) }
        }
    }
}

/**
 * 101 · Insights — Pickups: the count (a day over several days), the first pickup, when they
 * happen, the apps opened first and a tip.
 */
@Composable
internal fun PickupsScreen(
    state: PickupsUiState,
    onBack: () -> Unit,
    onStartBedtime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val stats = state.stats
    InsightsScaffold(
        modifier = modifier,
        topBar = {
            InsightsTopBar(
                title = stringResource(R.string.insights_pickups),
                subtitle = rangeLabel(state.range, state.today),
                onBack = onBack,
                trailing = { MoreMenuButton(listOf(stringResource(R.string.insights_start_bedtime) to onStartBedtime)) },
            )
        },
    ) {
        if (stats == null) {
            Spacer(Modifier.height(30.dp))
            LoadingBlock(150.dp)
            Spacer(Modifier.height(32.dp))
            LoadingBlock(124.dp)
            return@InsightsScaffold
        }
        val multiDay = state.period == UsagePeriod.ThisWeek || state.period == UsagePeriod.ThisMonth
        val count = stats.dailyAverage
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(count.toString(), style = RecentrTheme.typography.displayNumber, color = colors.textPrimary, maxLines = 1)
            Text(
                pluralStringResource(
                    when (state.range.name(state.today)) {
                        RangeName.Today -> R.plurals.insights_pickups_today
                        RangeName.Yesterday -> R.plurals.insights_pickups_yesterday
                        is RangeName.Day -> R.plurals.insights_pickups_that_day
                        else -> R.plurals.insights_pickups_a_day
                    },
                    count,
                ),
                style = RecentrTheme.typography.headline,
                color = colors.textSecondary,
            )
            IconChip(
                label = stats.firstPickupMinute?.let {
                    stringResource(if (multiDay) R.string.insights_usual_first_pickup else R.string.insights_first_pickup, clock(it))
                } ?: stringResource(R.string.insights_no_pickup),
                icon = RecentrIcon.Clock,
            )
        }
        Spacer(Modifier.height(18.dp))
        InsightsCard(spacing = 10.dp) {
            CardTitle(stringResource(R.string.insights_when_pick_up))
            PickupHeatStrip(stats.hourlyPickups)
        }
        Spacer(Modifier.height(12.dp))
        InsightsCard(spacing = 10.dp) {
            CardTitle(stringResource(R.string.insights_opened_first))
            if (stats.firstOpens.isEmpty()) {
                Text(stringResource(R.string.insights_no_first_open), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
            }
            stats.firstOpens.take(3).forEach { FirstOpenRow(it) }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.accentNoteMint)
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StickerImage(Sticker.Leaf, size = 36.dp)
            Text(pickupTip(stats), style = RecentrTheme.typography.captionMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FirstOpenRow(open: AppOpenCount) {
    val colors = RecentrTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        UsageAppIcon(open.app, size = 30.dp)
        Text(
            open.app.label,
            style = RecentrTheme.typography.bodyMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.insights_open_count, open.count), style = RecentrTheme.typography.headline, color = colors.textPrimary)
    }
}

/**
 * The tip under the cards: an early first pickup (phone in the bedroom), an app that is almost
 * always the first one opened, busy evenings, or a general nudge.
 */
@Composable
private fun pickupTip(stats: PickupStats): String {
    val first = stats.firstPickupMinute
    val reflex = stats.firstOpens.firstOrNull()?.takeIf { it.count * 10 >= stats.pickups * 3 && stats.pickups >= 5 }
    val evening = (18..23).sumOf { stats.hourlyPickups[it] }
    return when {
        first != null && first < 7 * 60 + 30 -> stringResource(R.string.insights_tip_bedroom, hourLabel(first / 60))
        reflex != null -> stringResource(R.string.insights_tip_reflex, reflex.app.label)
        stats.pickups > 0 && evening * 10 >= stats.pickups * 4 -> stringResource(R.string.insights_tip_evening)
        else -> stringResource(R.string.insights_tip_default)
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun PickupsPreview() {
    RecentrTheme {
        PickupsScreen(
            state = PickupsUiState(
                range = UsageRange.of(UsagePeriod.Today, LocalDate.now()),
                stats = PickupStats(
                    period = UsagePeriod.Today,
                    pickups = 38,
                    days = 1,
                    firstPickupMinute = 7 * 60 + 2,
                    hourlyPickups = listOf(0, 0, 0, 0, 0, 0, 2, 2, 4, 4, 1, 1, 2, 2, 1, 1, 4, 4, 6, 6, 6, 6, 2, 2),
                    firstOpens = listOf(
                        AppOpenCount(UsageApp.of(AppId.Instagram), 14),
                        AppOpenCount(UsageApp.of(AppId.X), 9),
                        AppOpenCount(UsageApp.of(AppId.Gmail), 6),
                    ),
                ),
            ),
            onBack = {},
            onStartBedtime = {},
        )
    }
}
