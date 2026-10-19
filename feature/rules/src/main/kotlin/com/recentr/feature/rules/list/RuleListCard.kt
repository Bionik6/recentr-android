package com.recentr.feature.rules.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.data.heldUntil
import com.recentr.core.designsystem.component.OverlappingInstalledAppIcons
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RuleCard
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppId
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.asSticker
import com.recentr.feature.rules.ui.compactDuration
import com.recentr.feature.rules.ui.countdown
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.habitRepeatLabel
import com.recentr.feature.rules.ui.scheduleLabel
import com.recentr.feature.rules.ui.components.IconTintChip
import com.recentr.feature.rules.ui.components.overlayBorder
import java.time.ZoneId

/**
 * When the card's countdown ends: the running session, or the scheduled window a session or
 * routine is in. Null when nothing counts down (locks, limits and habits hold without one).
 */
internal fun Rule.runsUntil(now: Long): Long? = when {
    isRunning(now) -> sessionEndsAt
    (type == RuleType.Session || type == RuleType.Bedtime) && isEnforcing(now) -> heldUntil(now, ZoneId.systemDefault())
    else -> null
}

/** Figma "Rule Card" filled from a [Rule] at time [now] (status chip, schedule chip, value). */
@Composable
internal fun RuleListCard(rule: Rule, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val runsUntil = rule.runsUntil(now)
    val running = runsUntil != null
    val paused = rule.isPaused(now)
    val value = when {
        paused -> stringResource(R.string.rules_value_paused)
        runsUntil != null -> stringResource(R.string.rules_value_left, countdown(runsUntil - now))
        else -> when (rule.type) {
            RuleType.Lock -> stringResource(R.string.rules_value_locked)
            RuleType.Session -> stringResource(
                R.string.rules_value_used_of,
                compactDuration(rule.usedMinutes),
                compactDuration(rule.schedule?.lengthMinutes ?: rule.durationMinutes ?: 0),
            )
            RuleType.Limit -> stringResource(
                R.string.rules_value_used_of,
                compactDuration(rule.usedMinutes),
                compactDuration(rule.limitMinutes ?: 0),
            )
            RuleType.Bedtime -> compactDuration(rule.usedMinutes)
            RuleType.Habit -> "${rule.habitTasks.count { it.done }}/${rule.habitTasks.size}"
        }
    }
    RuleCard(
        title = rule.name,
        sticker = rule.sticker.asSticker,
        streak = rule.streakDays.takeIf { it > 0 },
        apps = { OverlappingInstalledAppIcons(rule.apps.take(3).map { it.packageName }, size = 24.dp, overlap = 6.dp) },
        value = value,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (running) Modifier.overlayBorder(colors.feedbackSuccess, 2.dp, 24.dp) else Modifier),
    ) {
        when {
            paused -> {
                RecentrChip(stringResource(R.string.rules_status_paused))
                RecentrChip(scheduleChip(rule), style = RecentrChipStyle.Outline)
            }
            running -> {
                RecentrChip(
                    stringResource(if (rule.type == RuleType.Bedtime) R.string.rules_status_blocking else R.string.rules_status_ongoing),
                    style = RecentrChipStyle.Success,
                )
                if (rule.strictMode) {
                    IconTintChip(stringResource(R.string.rules_strict_mode), RecentrIcon.Shield, colors.accentTangerine)
                } else {
                    RecentrChip(scheduleChip(rule), style = RecentrChipStyle.Outline)
                }
            }
            else -> {
                RecentrChip(statusLabel(rule, now))
                RecentrChip(scheduleChip(rule), style = RecentrChipStyle.Outline)
            }
        }
    }
}

@Composable
private fun statusLabel(rule: Rule, now: Long): String = when (rule.type) {
    RuleType.Lock -> rule.unlocksPerDay?.let { stringResource(R.string.rules_status_unlocks, rule.unlocksUsedToday, it) }
        ?: stringResource(R.string.rules_status_unlocks_unlimited)
    RuleType.Session -> stringResource(
        if (rule.schedule?.isActiveAt(now) == true) R.string.rules_status_ongoing else R.string.rules_status_upcoming,
    )
    RuleType.Limit -> stringResource(
        if (rule.usedMinutes >= (rule.limitMinutes ?: Int.MAX_VALUE)) R.string.rules_status_times_up else R.string.rules_status_ongoing,
    )
    RuleType.Bedtime -> stringResource(
        if (rule.schedule?.isActiveAt(now) == true) R.string.rules_status_blocking else R.string.rules_status_upcoming,
    )
    RuleType.Habit -> stringResource(R.string.rules_status_tasks, rule.habitTasks.count { it.done }, rule.habitTasks.size)
}

@Composable
private fun scheduleChip(rule: Rule): String = when (rule.type) {
    RuleType.Limit -> stringResource(R.string.rules_all_day)
    RuleType.Habit -> habitRepeatLabel(rule.habitRepeat)
    RuleType.Session -> if (rule.schedule == null && rule.durationMinutes != null) durationLabel(rule.durationMinutes!!) else scheduleLabel(rule.schedule)
    else -> scheduleLabel(rule.schedule)
}

@Preview
@Composable
private fun RuleListCardPreview() {
    RecentrTheme {
        RuleListCard(
            rule = Rule(
                id = "1",
                type = RuleType.Limit,
                name = "Social time",
                apps = listOf(AppId.Instagram, AppId.X, AppId.TikTok).map { UsageApp.of(it) },
                limitMinutes = 135,
                usedMinutes = 10,
                streakDays = 3,
            ),
            now = 0L,
            onClick = {},
        )
    }
}
