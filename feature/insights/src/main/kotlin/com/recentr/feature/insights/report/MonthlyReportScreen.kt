package com.recentr.feature.insights.report

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.feature.insights.R
import com.recentr.feature.insights.ui.ChangeChip
import com.recentr.feature.insights.ui.InsightsCard
import com.recentr.feature.insights.ui.InsightsTopBar
import com.recentr.feature.insights.ui.MoreMenuButton
import com.recentr.feature.insights.ui.duration
import com.recentr.feature.insights.ui.monthName
import com.recentr.feature.insights.ui.signedCount
import com.recentr.feature.insights.ui.signedDuration
import java.time.YearMonth

/** Milliseconds each story segment takes to fill. */
private const val SEGMENT_MILLIS = 1_100

/**
 * 103 · Insights — Monthly report: a story over the sky — the time won back, the calendar and the
 * mascot, then the month's numbers against last month. The four story bars fill one after the
 * other, revealing each part.
 */
@Composable
internal fun MonthlyReportScreen(
    state: MonthlyReportUiState,
    onClose: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = true)
    val colors = RecentrTheme.colors
    val report = state.report
    var replays by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(if (state.reduceMotion) 4f else 0f) }
    LaunchedEffect(report != null, state.reduceMotion, replays) {
        if (report == null) return@LaunchedEffect
        if (state.reduceMotion) {
            progress.snapTo(4f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(4f, tween(4 * SEGMENT_MILLIS, easing = LinearEasing))
        }
    }
    fun revealed(segment: Float) = ((progress.value - segment) * 3f).coerceIn(0f, 1f)

    BackdropBox(Backdrop.Sky, modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            StoryBars(progress.value, Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp))
            InsightsTopBar(
                title = "",
                onBack = onClose,
                backIcon = RecentrIcon.X,
                onSky = true,
                trailing = {
                    MoreMenuButton(
                        listOf(
                            stringResource(R.string.insights_replay) to { replays++ },
                            stringResource(R.string.insights_share_month) to onShare,
                        ),
                        onSky = true,
                    )
                },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 5.dp),
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(20.dp))
                Column(
                    Modifier.graphicsLayer { alpha = revealed(0f) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RecentrChip(
                        label = stringResource(R.string.insights_report_chip, monthName(report?.month ?: YearMonth.now())),
                        style = RecentrChipStyle.OnSky,
                    )
                    Text(
                        report?.let { wonBackText(it.wonBackMinutes) } ?: "–",
                        style = RecentrTheme.typography.displayHero,
                        color = colors.textOnDark,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(stringResource(R.string.insights_won_back_month), style = RecentrTheme.typography.headline, color = colors.textOnDarkMuted)
                }
                Spacer(Modifier.height(32.dp))
                ReportArt(Modifier.graphicsLayer { alpha = revealed(1f) })
                Spacer(Modifier.height(20.dp))
                if (report != null) {
                    StatsCard(report, revealed = { revealed(2f + it / 2f) })
                }
                Spacer(Modifier.height(24.dp))
            }
            RecentrButton(
                text = stringResource(R.string.insights_share_month),
                onClick = onShare,
                enabled = report != null,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
            )
        }
    }
}

/** "31 hours" (or "45 minutes" under an hour). */
@Composable
internal fun wonBackText(minutes: Int): String =
    if (minutes >= 60) pluralStringResource(R.plurals.insights_hours, minutes / 60, minutes / 60) else pluralStringResource(R.plurals.insights_minutes, minutes, minutes)

/** Four 3dp story bars: white when done, alpha white 24% otherwise. */
@Composable
private fun StoryBars(progress: Float, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Row(modifier.fillMaxWidth().height(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) { index ->
            val fill = (progress - index).coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.alphaWhite24)
                    .drawBehind { drawRect(colors.textOnDark, size = Size(size.width * fill, size.height)) },
            )
        }
    }
}

/** Figma "Art" (280×170): the calendar sticker tilted 8° and the hourglass mascot. */
@Composable
private fun ReportArt(modifier: Modifier = Modifier) {
    Box(modifier.size(width = 280.dp, height = 170.dp)) {
        StickerImage(
            Sticker.Calendar,
            size = 140.dp,
            modifier = Modifier.offset(x = (-0.42).dp, y = 29.06.dp).graphicsLayer { rotationZ = 8f },
        )
        Mascot(MascotMood.Calm, width = 115.2.dp, modifier = Modifier.offset(x = 164.dp, y = 14.dp))
    }
}

/** Figma "Stats": daily average, pickups a day and best streak, with their change chips. */
@Composable
private fun StatsCard(report: MonthlyReport, revealed: (Int) -> Float) {
    val colors = RecentrTheme.colors
    InsightsCard(spacing = 0.dp, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
        StatRow(
            sticker = Sticker.Timer,
            label = stringResource(R.string.insights_daily_average),
            value = duration(report.dailyAverageMinutes),
            chip = report.dailyAverageChange?.takeIf { it != 0 }?.let { signedDuration(it) to (it < 0) },
            modifier = Modifier.graphicsLayer { alpha = revealed(0) },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        StatRow(
            sticker = Sticker.Lock,
            label = stringResource(R.string.insights_pickups_a_day_label),
            value = report.pickupsPerDay.toString(),
            chip = report.pickupsPerDayChange?.takeIf { it != 0 }?.let { signedCount(it) to (it < 0) },
            modifier = Modifier.graphicsLayer { alpha = revealed(1) },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        StatRow(
            sticker = Sticker.Flame,
            label = stringResource(R.string.insights_best_streak),
            value = pluralStringResource(R.plurals.insights_days, report.bestStreak, report.bestStreak),
            chip = if (report.newBestStreak) stringResource(R.string.insights_new) to true else null,
            modifier = Modifier.graphicsLayer { alpha = revealed(2) },
        )
    }
}

@Composable
private fun StatRow(sticker: Sticker, label: String, value: String, chip: Pair<String, Boolean>?, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Row(
        modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(sticker, size = 36.dp)
        Text(label, style = RecentrTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, modifier = Modifier.weight(1f))
        Text(value, style = RecentrTheme.typography.headline, color = colors.textPrimary, maxLines = 1)
        if (chip != null) ChangeChip(chip.first, good = chip.second)
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun MonthlyReportPreview() {
    RecentrTheme {
        MonthlyReportScreen(
            state = MonthlyReportUiState(
                report = MonthlyReport(YearMonth.of(2026, 10), 31 * 60, 130, -48, 38, -12, 12, true),
                reduceMotion = true,
            ),
            onClose = {},
            onShare = {},
        )
    }
}
