package com.recentr.feature.insights.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.data.AchievementsSnapshot
import com.recentr.core.data.StreakDay
import com.recentr.core.data.StreakDayStatus
import com.recentr.core.data.StreakState
import com.recentr.core.data.Streaks
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.Achievement
import com.recentr.core.model.AchievementProgress
import com.recentr.core.model.CalmLevel
import com.recentr.core.model.DayStats
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.InsightsCard
import com.recentr.feature.insights.ui.InsightsCardShape
import com.recentr.feature.insights.ui.InsightsScaffold
import com.recentr.feature.insights.ui.InsightsTopBar
import com.recentr.feature.insights.ui.LoadingBlock
import com.recentr.feature.insights.ui.ProgressTrack
import com.recentr.feature.insights.ui.copy
import com.recentr.feature.insights.ui.weekdayName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** How a day of "This week" is drawn (Figma 108 "Dot"). */
internal enum class WeekDot { OnTarget, Saved, Missed, Today, Empty }

/** The streak badges, in the order they come. */
private val StreakBadges = listOf(Achievement.SevenDayStreak, Achievement.TwoCalmWeeks, Achievement.CalmMonth)

/**
 * 108 · Achievements — Streak: the flame and the current streak, this week's days (flames, saved
 * days, today), the streak savers left this month and the next streak badge.
 */
@Composable
internal fun StreakScreen(
    snapshot: AchievementsSnapshot?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val colors = RecentrTheme.colors
    var help by rememberSaveable { mutableStateOf(false) }
    InsightsScaffold(
        modifier = modifier,
        topBar = {
            InsightsTopBar(
                title = stringResource(R.string.streak_title),
                onBack = onBack,
                trailing = {
                    RecentrIconButton(icon = RecentrIcon.Help, contentDescription = stringResource(R.string.streak_help), onClick = { help = true })
                },
            )
        },
    ) {
        if (snapshot == null) {
            Spacer(Modifier.height(12.dp))
            LoadingBlock(240.dp)
            return@InsightsScaffold
        }
        val streak = snapshot.streak
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            StickerImage(Sticker.Flame, size = 120.dp)
            Text(streak.days.toString(), style = RecentrTheme.typography.displayNumber, color = colors.textPrimary, maxLines = 1)
            Text(
                if (streak.days > 0 && streak.days >= snapshot.bestStreak) {
                    stringResource(R.string.streak_best_yet)
                } else {
                    stringResource(R.string.streak_best, pluralStringResource(R.plurals.insights_days, snapshot.bestStreak, snapshot.bestStreak))
                },
                style = RecentrTheme.typography.headline,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(20.dp))
        InsightsCard {
            Text(stringResource(R.string.streak_this_week), style = RecentrTheme.typography.headline, color = colors.textPrimary)
            WeekRow(weekDots(snapshot.days, streak, snapshot.goalMinutes, today))
        }
        Spacer(Modifier.height(12.dp))
        SaversCard(streak)
        Spacer(Modifier.height(12.dp))
        NextStreakBadge(snapshot.badges, streak.days)
    }
    if (help) {
        RecentrAlertDialog(
            title = stringResource(R.string.streak_help),
            body = stringResource(R.string.streak_help_body),
            primaryText = stringResource(R.string.streak_help_ok),
            onPrimary = { help = false },
            onDismissRequest = { help = false },
        )
    }
}

/** Monday → Sunday of [today]'s week: the streak's days, then the goal for the days before it. */
internal fun weekDots(days: List<DayStats>, streak: StreakState, goalMinutes: Int, today: LocalDate): List<WeekDot> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val byDate = days.associateBy { it.date }
    return (0L until 7L).map { offset ->
        val date = monday.plusDays(offset)
        val day = byDate[date]
        when {
            date == today -> WeekDot.Today
            date.isAfter(today) -> WeekDot.Empty
            streak.statuses[date] == StreakDayStatus.Saved -> WeekDot.Saved
            streak.statuses[date] == StreakDayStatus.OnTarget -> WeekDot.OnTarget
            day == null || !day.hasData -> WeekDot.Empty
            day.minutes <= goalMinutes -> WeekDot.OnTarget
            else -> WeekDot.Missed
        }
    }
}

@Composable
private fun WeekRow(dots: List<WeekDot>) {
    val colors = RecentrTheme.colors
    val initials = stringArrayResource(R.array.insights_weekday_initials)
    val names = stringArrayResource(R.array.insights_weekday_full)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        dots.forEachIndexed { index, dot ->
            val description = stringResource(
                when (dot) {
                    WeekDot.OnTarget -> R.string.streak_day_on_target
                    WeekDot.Saved -> R.string.streak_day_saved
                    WeekDot.Missed -> R.string.streak_day_missed
                    WeekDot.Today -> R.string.streak_day_today
                    WeekDot.Empty -> R.string.streak_day_empty
                },
                names[index],
            )
            Column(
                Modifier.clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val shape = CircleShape
                val base = Modifier.size(38.dp).clip(shape)
                when (dot) {
                    WeekDot.OnTarget -> Box(base.background(colors.alphaTangerine14), contentAlignment = Alignment.Center) {
                        StickerImage(Sticker.Flame, size = 28.dp)
                    }
                    WeekDot.Saved -> Box(base.background(colors.accentNoteMint), contentAlignment = Alignment.Center) {
                        StickerImage(Sticker.Shield, size = 26.dp)
                    }
                    WeekDot.Missed -> Box(base.background(colors.bgSunken), contentAlignment = Alignment.Center) {
                        RecentrIconImage(RecentrIcon.X, tint = colors.textTertiary, size = 16.dp)
                    }
                    WeekDot.Today -> Box(base.background(colors.bgSurface).border(2.dp, colors.accentTangerine, shape))
                    WeekDot.Empty -> Box(base.background(colors.bgSunken))
                }
                Text(
                    initials[index],
                    style = RecentrTheme.typography.labelSmall,
                    color = if (dot == WeekDot.Today) colors.accentTangerine else colors.textTertiary,
                )
            }
        }
    }
}

/** Figma "Savers": mint card, shield, "2 streak savers left" and when the last one was used. */
@Composable
private fun SaversCard(streak: StreakState) {
    val colors = RecentrTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(InsightsCardShape).background(colors.accentNoteMint).padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(Sticker.Shield, size = 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                pluralStringResource(R.plurals.streak_savers_left, streak.saversLeft, streak.saversLeft),
                style = RecentrTheme.typography.headline,
                color = colors.textPrimary,
            )
            val saved = streak.lastSavedDay
            Text(
                when {
                    saved != null -> stringResource(R.string.streak_saver_used, weekdayName(saved))
                    streak.saversLeft <= 0 -> stringResource(R.string.streak_savers_out)
                    else -> stringResource(R.string.streak_saver_unused)
                },
                style = RecentrTheme.typography.captionRegular,
                color = colors.textSecondary,
            )
        }
    }
}

/** Figma "Next": the next streak badge and the days left to it. */
@Composable
private fun NextStreakBadge(badges: List<AchievementProgress>, current: Int) {
    val colors = RecentrTheme.colors
    val earned = badges.filter { it.earned }.map { it.achievement }.toSet()
    val next = StreakBadges.firstOrNull { it !in earned && it.target > current } ?: StreakBadges.firstOrNull { it.target > current }
    InsightsCard(contentPadding = PaddingValues(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            StickerImage(next?.copy?.sticker ?: Sticker.Medal, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (next == null) {
                        stringResource(R.string.streak_all_done)
                    } else {
                        pluralStringResource(R.plurals.streak_days_to, next.target - current, next.target - current, stringResource(next.copy.title))
                    },
                    style = RecentrTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                ProgressTrack(if (next == null) 1f else current.toFloat() / next.target, color = colors.accentTangerine)
            }
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun StreakPreview() {
    val today = LocalDate.now()
    val days = (0L..20L).map { DayStats(today.minusDays(20 - it), true, if (it == 17L) 200 else 90, 20) }
    val streak = Streaks.current(days.asReversed().asSequence().map { StreakDay(it.date, true, it.minutes) }, 150)
    RecentrTheme {
        StreakScreen(
            snapshot = AchievementsSnapshot(
                badges = Achievement.entries.map { AchievementProgress(it, 0, null) },
                level = CalmLevel.of(640),
                streak = streak,
                bestStreak = streak.days,
                uncelebrated = emptyList(),
                days = days,
                goalMinutes = 150,
                startedOn = today.minusDays(23),
                firstLockApps = emptyList(),
            ),
            onBack = {},
            today = today,
        )
    }
}
