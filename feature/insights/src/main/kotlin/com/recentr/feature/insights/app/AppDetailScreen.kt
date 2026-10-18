package com.recentr.feature.insights.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppId
import com.recentr.core.model.AppUsageDetail
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.CardTitle
import com.recentr.feature.insights.ui.HourBarsChart
import com.recentr.feature.insights.ui.InsightsCard
import com.recentr.feature.insights.ui.InsightsScaffold
import com.recentr.feature.insights.ui.InsightsTopBar
import com.recentr.feature.insights.ui.LoadingBlock
import com.recentr.feature.insights.ui.MoreMenuButton
import com.recentr.feature.insights.ui.UsageAppIcon
import com.recentr.feature.insights.ui.clock
import com.recentr.feature.insights.ui.duration
import com.recentr.feature.insights.ui.labelRes

/**
 * 100 · Insights — App detail: time and opens over the period, the hourly profile, the longest
 * session and first open, what Recentr noticed, and the daily limit CTA.
 *
 * @param onLimit opens the limit editor: the existing rule ([Rule.id]) or a new one for the app.
 * @param onBlock creates a lock rule for the app.
 */
@Composable
internal fun AppDetailScreen(
    state: AppDetailUiState,
    onBack: () -> Unit,
    onLimit: (app: AppId, existingRuleId: String?) -> Unit,
    onBlock: (AppId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val detail = state.detail
    val catalogue = detail?.app?.catalogue
    InsightsScaffold(
        modifier = modifier,
        topBar = {
            InsightsTopBar(
                title = detail?.app?.label.orEmpty(),
                subtitle = detail?.let {
                    stringResource(R.string.insights_app_subtitle, stringResource(it.app.category.labelRes), stringResource(state.period.labelRes))
                },
                onBack = onBack,
                trailing = if (catalogue != null) {
                    { MoreMenuButton(listOf(stringResource(R.string.insights_block_app, catalogue.displayName) to { onBlock(catalogue) })) }
                } else {
                    null
                },
            )
        },
        bottomBar = if (catalogue != null) {
            {
                RecentrButton(
                    text = stringResource(if (state.limitRule != null) R.string.insights_edit_limit else R.string.insights_set_limit),
                    onClick = { onLimit(catalogue, state.limitRule?.id) },
                    style = RecentrButtonStyle.Brand,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            null
        },
    ) {
        if (detail == null) {
            Spacer(Modifier.height(22.dp))
            if (state.loading) {
                LoadingBlock(72.dp)
                Spacer(Modifier.height(28.dp))
                LoadingBlock(166.dp)
            } else {
                Text(stringResource(R.string.insights_no_apps), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
            }
            return@InsightsScaffold
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            UsageAppIcon(detail.app, size = 72.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(duration(detail.minutes), style = RecentrTheme.typography.displayLarge, color = colors.textPrimary, maxLines = 1)
                OpensLine(detail, state.limitRule, state.limitUsedMinutes)
            }
        }
        Spacer(Modifier.height(28.dp))
        InsightsCard(spacing = 10.dp, contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)) {
            CardTitle(stringResource(R.string.insights_by_hour))
            HourBarsChart(detail.hourlyMinutes)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniTile(
                label = stringResource(R.string.insights_longest_session),
                value = duration(detail.longestSessionMinutes),
                sticker = Sticker.Timer,
                modifier = Modifier.weight(1f),
            )
            MiniTile(
                label = stringResource(if (detail.days > 1) R.string.insights_usual_first_open else R.string.insights_first_open),
                value = detail.firstOpenMinute?.let { clock(it) } ?: stringResource(R.string.insights_no_value),
                sticker = Sticker.Bolt,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
        StickyNote(
            title = stringResource(R.string.insights_noticed),
            body = noticedText(detail),
            modifier = Modifier.fillMaxWidth().graphicsLayer { rotationZ = -2f },
        )
    }
}

/** "23 opens · 14m over your limit" (danger red once over today's limit). */
@Composable
private fun OpensLine(detail: AppUsageDetail, limit: Rule?, usedMinutes: Int?) {
    val colors = RecentrTheme.colors
    val opens = pluralStringResource(R.plurals.insights_opens, detail.opens, detail.opens)
    val budget = limit?.limitMinutes
    val (text, color) = when {
        detail.period != UsagePeriod.Today || budget == null || usedMinutes == null -> opens to colors.textSecondary
        usedMinutes > budget -> stringResource(R.string.insights_over_limit, opens, duration(usedMinutes - budget)) to colors.feedbackDanger
        else -> stringResource(R.string.insights_left_in_limit, opens, duration(budget - usedMinutes)) to colors.feedbackSuccess
    }
    Text(text, style = RecentrTheme.typography.captionMedium, color = color, maxLines = 2)
}

/** Hours of the day Recentr reads the app's time in. */
private enum class DayPart(val hours: Set<Int>) {
    Night(setOf(22, 23, 0, 1, 2, 3, 4)),
    Morning((5..8).toSet()),
    Work((9..16).toSet()),
    Evening((17..21).toSet()),
}

/**
 * The "Recentr noticed" note: barely used, checked in short bursts, mostly at one part of the day
 * (with the matching routine), or spread through the day.
 */
@Composable
private fun noticedText(detail: AppUsageDetail): String {
    val app = detail.app.label
    if (detail.minutes < 5) return stringResource(R.string.insights_noticed_idle, app)
    val opensPerDay = detail.opens / detail.days.coerceAtLeast(1)
    if (opensPerDay >= 15 && detail.minutes < detail.opens * 3) return stringResource(R.string.insights_noticed_checks, app, opensPerDay)
    val total = detail.hourlyMinutes.sum().coerceAtLeast(1)
    val (part, minutes) = DayPart.entries.map { part -> part to part.hours.sumOf { detail.hourlyMinutes[it] } }.maxBy { it.second }
    if (minutes * 2 < total) return stringResource(R.string.insights_noticed_spread, app)
    return stringResource(
        when (part) {
            DayPart.Night -> R.string.insights_noticed_night
            DayPart.Morning -> R.string.insights_noticed_morning
            DayPart.Work -> R.string.insights_noticed_work
            DayPart.Evening -> R.string.insights_noticed_evening
        },
        app,
    )
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun AppDetailPreview() {
    val hourly = listOf(0, 0, 0, 1, 2, 1, 1, 2, 1, 1, 2, 3, 1, 1, 1, 2, 2, 2, 3, 4, 6, 8, 5, 2)
    RecentrTheme {
        AppDetailScreen(
            state = AppDetailUiState(
                period = UsagePeriod.Today,
                detail = AppUsageDetail(UsageApp.of(AppId.TikTok), UsagePeriod.Today, 54, 23, hourly, 22, 7 * 60 + 12, 1),
                loading = false,
                limitRule = Rule("social", RuleType.Limit, "Social time", apps = listOf(AppId.TikTok), limitMinutes = 40),
                limitUsedMinutes = 54,
            ),
            onBack = {},
            onLimit = { _, _ -> },
            onBlock = {},
        )
    }
}
