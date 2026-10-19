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
internal fun Rule.runsUntil(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? = when {
    isRunning(now) -> sessionEndsAt
    (type == RuleType.Session || type == RuleType.Bedtime) && isEnforcing(now, zone) -> heldUntil(now, zone)
    else -> null
}

/** The status chip of a rule card. [Ongoing] and [Blocking] read green: the rule is on now. */
internal enum class CardStatus {
    Paused, Ongoing, Blocking, Upcoming, TimesUp, Unlocks, Tasks;

    val active: Boolean get() = this == Ongoing || this == Blocking
}

internal fun Rule.cardStatus(now: Long, zone: ZoneId = ZoneId.systemDefault()): CardStatus = when {
    isPaused(now) -> CardStatus.Paused
    runsUntil(now, zone) != null -> if (type == RuleType.Bedtime) CardStatus.Blocking else CardStatus.Ongoing
    else -> when (type) {
        RuleType.Lock -> CardStatus.Unlocks
        RuleType.Session -> if (schedule?.isActiveAt(now, zone) == true) CardStatus.Ongoing else CardStatus.Upcoming
        RuleType.Limit -> if (usedMinutes >= (limitMinutes ?: Int.MAX_VALUE)) CardStatus.TimesUp else CardStatus.Ongoing
        RuleType.Bedtime -> if (schedule?.isActiveAt(now, zone) == true) CardStatus.Blocking else CardStatus.Upcoming
        RuleType.Habit -> CardStatus.Tasks
    }
}

/** The value at the end of a rule card. */
internal sealed interface CardValue {
    data object Paused : CardValue
    /** A running session or routine: the countdown to its end. */
    data class Left(val millis: Long) : CardValue
    data object Locked : CardValue
    data class UsedOf(val usedMinutes: Int, val totalMinutes: Int) : CardValue
    data class Minutes(val minutes: Int) : CardValue
    data class Tasks(val done: Int, val total: Int) : CardValue
}

/**
 * The card's value, or null for nothing: a routine shows the minutes it held apps only once there
 * is a whole one (no "0s" while it waits for its window, nor in its first minute).
 */
internal fun Rule.cardValue(now: Long, zone: ZoneId = ZoneId.systemDefault()): CardValue? {
    if (isPaused(now)) return CardValue.Paused
    runsUntil(now, zone)?.let { return CardValue.Left(it - now) }
    return when (type) {
        RuleType.Lock -> CardValue.Locked
        RuleType.Session -> CardValue.UsedOf(usedMinutes, schedule?.lengthMinutes ?: durationMinutes ?: 0)
        RuleType.Limit -> CardValue.UsedOf(usedMinutes, limitMinutes ?: 0)
        RuleType.Bedtime -> if (usedMinutes >= 1) CardValue.Minutes(usedMinutes) else null
        RuleType.Habit -> CardValue.Tasks(habitTasks.count { it.done }, habitTasks.size)
    }
}

/** Figma "Rule Card" filled from a [Rule] at time [now] (status chip, schedule chip, value). */
@Composable
internal fun RuleListCard(rule: Rule, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val status = rule.cardStatus(now)
    val running = rule.runsUntil(now) != null
    val value = when (val v = rule.cardValue(now)) {
        null -> null
        CardValue.Paused -> stringResource(R.string.rules_value_paused)
        is CardValue.Left -> stringResource(R.string.rules_value_left, countdown(v.millis))
        CardValue.Locked -> stringResource(R.string.rules_value_locked)
        is CardValue.UsedOf -> stringResource(R.string.rules_value_used_of, compactDuration(v.usedMinutes), compactDuration(v.totalMinutes))
        is CardValue.Minutes -> compactDuration(v.minutes)
        is CardValue.Tasks -> "${v.done}/${v.total}"
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
        RecentrChip(statusLabel(rule, status), style = if (status.active && rule.enabled) RecentrChipStyle.Success else RecentrChipStyle.Neutral)
        if (running && rule.strictMode) {
            IconTintChip(stringResource(R.string.rules_strict_mode), RecentrIcon.Shield, colors.accentTangerine)
        } else {
            RecentrChip(scheduleChip(rule), style = RecentrChipStyle.Outline)
        }
    }
}

@Composable
private fun statusLabel(rule: Rule, status: CardStatus): String = when (status) {
    CardStatus.Paused -> stringResource(R.string.rules_status_paused)
    CardStatus.Ongoing -> stringResource(R.string.rules_status_ongoing)
    CardStatus.Blocking -> stringResource(R.string.rules_status_blocking)
    CardStatus.Upcoming -> stringResource(R.string.rules_status_upcoming)
    CardStatus.TimesUp -> stringResource(R.string.rules_status_times_up)
    CardStatus.Unlocks -> rule.unlocksPerDay?.let { stringResource(R.string.rules_status_unlocks, rule.unlocksUsedToday, it) }
        ?: stringResource(R.string.rules_status_unlocks_unlimited)
    CardStatus.Tasks -> stringResource(R.string.rules_status_tasks, rule.habitTasks.count { it.done }, rule.habitTasks.size)
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
