package com.recentr.feature.focus.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.FocusSessionRecord
import com.recentr.feature.focus.FocusHistoryUiState
import com.recentr.feature.focus.R
import com.recentr.feature.focus.ui.CardDivider
import com.recentr.feature.focus.ui.FocusCard
import com.recentr.feature.focus.ui.FocusMenu
import com.recentr.feature.focus.ui.FocusTopBar
import com.recentr.feature.focus.ui.formatClock
import com.recentr.feature.focus.ui.formatDuration
import com.recentr.feature.focus.ui.sticker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

/** 97 · Focus — History: this week's total and bars, then the session list. */
@Composable
internal fun FocusHistoryScreen(
    state: FocusHistoryUiState,
    onBack: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = false)
    val colors = RecentrTheme.colors
    var menu by rememberSaveable { mutableStateOf(false) }
    BackdropBox(Backdrop.Aurora, modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            FocusTopBar(
                title = stringResource(R.string.history_title),
                subtitle = null,
                titleColor = colors.textPrimary,
                subtitleColor = colors.textSecondary,
                leading = { RecentrIconButton(RecentrIcon.ChevronLeft, stringResource(R.string.focus_back), onBack) },
                trailing = {
                    Box {
                        RecentrIconButton(RecentrIcon.More, stringResource(R.string.focus_more), { menu = true })
                        FocusMenu(
                            expanded = menu,
                            onDismissRequest = { menu = false },
                            items = listOf(stringResource(R.string.history_clear) to onClear),
                        )
                    }
                },
            )
            Spacer(Modifier.height(18.dp))
            WeekCard(state)
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.history_sessions),
                style = RecentrTheme.typography.headline,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
            if (state.sessions.isEmpty()) {
                Text(stringResource(R.string.history_empty), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
            } else {
                FocusCard {
                    state.sessions.forEachIndexed { index, session ->
                        if (index > 0) CardDivider()
                        SessionRow(session)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** "This week" card: total, change versus last week, Monday → Sunday bars (today in sky). */
@Composable
private fun WeekCard(state: FocusHistoryUiState) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val letters = stringArrayResource(R.array.focus_day_letters)
    Column(
        Modifier
            .fillMaxWidth()
            .recentrShadow(RecentrElevation.card, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.history_this_week), style = RecentrTheme.typography.captionMedium, color = colors.textSecondary)
                Text(formatDuration(state.weekMinutes), style = RecentrTheme.typography.displayLarge, color = colors.textPrimary, maxLines = 1)
            }
            val delta = state.weekMinutes - state.lastWeekMinutes
            if (delta != 0) {
                RecentrChip(
                    label = (if (delta > 0) "+" else "−") + formatDuration(delta.absoluteValue),
                    style = if (delta > 0) RecentrChipStyle.Success else RecentrChipStyle.Neutral,
                    icon = RecentrIcon.Chart,
                )
            }
        }
        val max = state.week.maxOrNull()?.coerceAtLeast(1) ?: 1
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            state.week.forEachIndexed { index, minutes ->
                val today = index == state.todayIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Bars scale to the best day (110dp); empty days keep a 6dp stub.
                    Box(
                        Modifier
                            .size(width = 30.dp, height = (110f * minutes / max).coerceAtLeast(6f).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    minutes == 0 -> colors.bgSunken
                                    today -> colors.brandSky
                                    else -> colors.brandSkyLight
                                },
                            ),
                    )
                    Text(
                        letters[index],
                        style = RecentrTheme.typography.labelSmall,
                        color = if (today) colors.brandSky else colors.textTertiary,
                    )
                }
            }
        }
    }
}

/** Session row: 36dp sticker, title + "Today, 9:57 AM", duration. */
@Composable
private fun SessionRow(session: FocusSessionRecord) {
    val colors = RecentrTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(session.kind.sticker, size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(session.title, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(whenText(session.startedAt), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary, maxLines = 1)
        }
        Text(formatDuration(session.focusedMinutes), style = RecentrTheme.typography.headline, color = colors.textPrimary, maxLines = 1)
    }
}

/** "Today, 9:57 AM", "Yesterday, 9:10 PM", "Thu, 2:00 PM" (or "12 Oct, …" beyond a week). */
@Composable
private fun whenText(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val time = formatClock(epochMillis)
    return when {
        date == today -> stringResource(R.string.history_today, time)
        date == today.minusDays(1) -> stringResource(R.string.history_yesterday, time)
        date.isAfter(today.minusDays(7)) -> stringResource(R.string.history_day, date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), time)
        else -> stringResource(
            R.string.history_day,
            "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
            time,
        )
    }
}
