package com.recentr.feature.rules.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
import com.recentr.feature.rules.ui.PendingKind
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.pendingKind
import com.recentr.feature.rules.ui.scheduleLabel
import com.recentr.feature.rules.ui.startCountdown
import com.recentr.feature.rules.ui.components.IconTintChip
import com.recentr.feature.rules.ui.components.overlayBorder
import java.time.ZoneId

/**
 * When the card's countdown ends: the running session, or the window a routine is in. Null when
 * nothing counts down (locks and limits hold without one).
 */
internal fun Rule.runsUntil(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? = when (type) {
    RuleType.Session -> sessionEndsAt?.takeIf { isRunning(now) }
    RuleType.Bedtime -> heldUntil(now, zone)?.takeIf { isEnforcing(now, zone) }
    RuleType.Lock, RuleType.Limit -> null
}

/**
 * The status chip of a rule card. [Ongoing] and [Blocking] read green: the rule is on now. [Ready]:
 * a session or a limit waiting for Start. [Off]: a lock waiting for Activate.
 */
internal enum class CardStatus {
    Paused, Ongoing, Blocking, Upcoming, Ready, TimesUp, Unlocks, Off;

    val active: Boolean get() = this == Ongoing || this == Blocking
}

internal fun Rule.cardStatus(now: Long, zone: ZoneId = ZoneId.systemDefault()): CardStatus = when {
    isPaused(now) -> CardStatus.Paused
    else -> when (type) {
        RuleType.Session -> if (isRunning(now)) CardStatus.Ongoing else CardStatus.Ready
        RuleType.Bedtime -> if (isEnforcing(now, zone)) CardStatus.Blocking else CardStatus.Upcoming
        RuleType.Lock -> if (isEnforcing(now, zone)) CardStatus.Unlocks else CardStatus.Off
        RuleType.Limit -> when {
            !isEnforcing(now, zone) -> CardStatus.Ready
            usedMinutes >= (limitMinutes ?: Int.MAX_VALUE) -> CardStatus.TimesUp
            else -> CardStatus.Ongoing
        }
    }
}

/** The value at the end of a rule card. */
internal sealed interface CardValue {
    data object Paused : CardValue
    /** A running session or routine: the countdown to its end. */
    data class Left(val millis: Long) : CardValue
    /** A routine waiting for its window: the countdown to its start. */
    data class StartsIn(val millis: Long) : CardValue
    data object Locked : CardValue
    /** A lock waiting for Activate. */
    data object Off : CardValue
    data class UsedOf(val usedMinutes: Int, val totalMinutes: Int) : CardValue
    /** A limit waiting for Start: its budget. */
    data class Minutes(val minutes: Int) : CardValue
}

/**
 * The card's value, or null for nothing: a routine waiting for its window counts down to its
 * start, a started limit reads "used / budget" and one waiting for Start its budget; a saved
 * session waiting for Start has its length on its chip already.
 */
internal fun Rule.cardValue(now: Long, zone: ZoneId = ZoneId.systemDefault()): CardValue? {
    if (isPaused(now)) return CardValue.Paused
    runsUntil(now, zone)?.let { return CardValue.Left(it - now) }
    return when (type) {
        RuleType.Lock -> if (isEnforcing(now, zone)) CardValue.Locked else CardValue.Off
        RuleType.Session -> null
        RuleType.Limit -> if (isEnforcing(now, zone)) CardValue.UsedOf(usedMinutes, limitMinutes ?: 0) else CardValue.Minutes(limitMinutes ?: 0)
        RuleType.Bedtime -> schedule
            ?.takeIf { enabled && !it.isActiveAt(now, zone) }
            ?.nextStartAfter(now, zone)
            ?.let { CardValue.StartsIn(it - now) }
    }
}

/** App icons a rule card shows; the others read "+ N apps". */
internal const val CARD_APPS = 5

/** The apps whose icons a card shows, and how many more it has. */
internal fun Rule.cardApps(): Pair<List<UsageApp>, Int> = apps.take(CARD_APPS) to (apps.size - CARD_APPS).coerceAtLeast(0)

/** Figma "Rule Card" filled from a [Rule] at time [now] (status chip, schedule chip, value). */
@Composable
internal fun RuleListCard(rule: Rule, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val status = rule.cardStatus(now)
    val running = rule.runsUntil(now) != null
    val cardValue = rule.cardValue(now)
    val value = when (val v = cardValue) {
        null -> null
        CardValue.Paused -> stringResource(R.string.rules_value_paused)
        is CardValue.Left -> stringResource(R.string.rules_value_left, countdown(v.millis))
        is CardValue.StartsIn -> stringResource(R.string.rules_value_starts_in, startCountdown(v.millis))
        CardValue.Locked -> stringResource(R.string.rules_value_locked)
        CardValue.Off -> stringResource(R.string.rules_value_off)
        is CardValue.UsedOf -> stringResource(R.string.rules_value_used_of, compactDuration(v.usedMinutes), compactDuration(v.totalMinutes))
        is CardValue.Minutes -> compactDuration(v.minutes)
    }
    RuleCard(
        title = rule.name,
        sticker = rule.sticker.asSticker,
        streak = rule.streakDays.takeIf { it > 0 },
        apps = { CardApps(rule) },
        value = value,
        // A routine waiting for its window: its countdown in secondary.
        valueColor = colors.textSecondary.takeIf { cardValue is CardValue.StartsIn },
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (running) Modifier.overlayBorder(colors.feedbackSuccess, 2.dp, 24.dp) else Modifier),
    ) {
        RecentrChip(statusLabel(rule, status), style = if (status.active && rule.enabled) RecentrChipStyle.Success else RecentrChipStyle.Neutral)
        if (running && rule.strictMode) {
            IconTintChip(stringResource(R.string.rules_strict_mode), RecentrIcon.Shield, colors.accentTangerine)
        } else {
            RecentrChip(scheduleChip(rule, now), style = RecentrChipStyle.Outline)
        }
    }
}

@Composable
private fun statusLabel(rule: Rule, status: CardStatus): String = when (status) {
    CardStatus.Paused -> stringResource(R.string.rules_status_paused)
    CardStatus.Ongoing -> stringResource(R.string.rules_status_ongoing)
    CardStatus.Blocking -> stringResource(R.string.rules_status_blocking)
    CardStatus.Upcoming -> stringResource(R.string.rules_status_upcoming)
    CardStatus.Ready -> stringResource(R.string.rules_status_ready)
    CardStatus.TimesUp -> stringResource(R.string.rules_status_times_up)
    CardStatus.Unlocks -> rule.unlocksPerDay?.let { stringResource(R.string.rules_status_unlocks, rule.unlocksUsedToday, it) }
        ?: stringResource(R.string.rules_status_unlocks_unlimited)
    CardStatus.Off -> stringResource(R.string.rules_status_off)
}

/** Up to [CARD_APPS] overlapping icons, then "+ N apps". */
@Composable
private fun CardApps(rule: Rule) {
    val (shown, more) = rule.cardApps()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        OverlappingInstalledAppIcons(shown.map { it.packageName }, size = 24.dp, overlap = 6.dp)
        if (more > 0) {
            Text(
                pluralStringResource(R.plurals.rules_more_apps, more, more),
                style = RecentrTheme.typography.captionMedium,
                color = RecentrTheme.colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

/**
 * The card's second chip: a session's length, a routine's days and hours, "Until midnight" for a
 * started limit or a lock turning off, "Budget for today" for a limit waiting for Start.
 */
@Composable
private fun scheduleChip(rule: Rule, now: Long): String = when (rule.type) {
    RuleType.Limit -> stringResource(if (rule.isEnforcing(now)) R.string.rules_until_midnight else R.string.rules_budget_today)
    RuleType.Session -> durationLabel(rule.durationMinutes ?: 30)
    RuleType.Lock -> stringResource(if (rule.pendingKind == PendingKind.TurnOff) R.string.rules_until_midnight else R.string.rules_all_day)
    RuleType.Bedtime -> scheduleLabel(rule.schedule)
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
                activeSince = 0L,
                streakDays = 3,
            ),
            now = 0L,
            onClick = {},
        )
    }
}
