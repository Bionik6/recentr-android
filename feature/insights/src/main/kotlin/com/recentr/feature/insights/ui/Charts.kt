package com.recentr.feature.insights.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.motion.rememberBarsGrowth
import com.recentr.core.designsystem.motion.rememberFill
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.DayStats
import com.recentr.feature.insights.R
import java.time.LocalDate
import java.time.YearMonth

/**
 * Figma 99 "Chart" (118dp): seven 30dp bars, Monday → Sunday, rising from a 100dp baseline with
 * the dashed tangerine goal line. Days over the goal are `brand/sky`, the others
 * `brand/sky-light`; days without data are 4dp sunken stubs.
 */
@Composable
internal fun WeekGoalChart(
    minutes: List<Int?>,
    goalMinutes: Int,
    dayLabels: List<String>,
    goalLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val growth = rememberBarsGrowth(minutes.size, staggerMillis = 50, key = minutes)
    val scale = maxOf(minutes.maxOf { it ?: 0 }, goalMinutes, 1).toFloat()
    val plotHeight = 100.dp
    val top = 96.dp
    val goalY = plotHeight - top * (goalMinutes / scale)
    val description = dayLabels.zip(minutes).joinToString { (day, value) ->
        "$day ${value?.let { "${it / 60}h ${it % 60}m" } ?: "–"}"
    }
    Box(modifier.fillMaxWidth().height(118.dp).semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(plotHeight)) {
            val slot = size.width / minutes.size
            val barWidth = 30.dp.toPx().coerceAtMost(slot - 4.dp.toPx())
            val radius = CornerRadius(10.dp.toPx())
            val maxHeight = top.toPx()
            minutes.forEachIndexed { index, value ->
                val x = slot * index + (slot - barWidth) / 2f
                if (value == null || value == 0) {
                    val stub = 4.dp.toPx()
                    drawRoundRect(colors.bgSunken, Offset(x, size.height - stub), Size(barWidth, stub), CornerRadius(stub / 2))
                } else {
                    val height = (maxHeight * value / scale).coerceAtLeast(4.dp.toPx()) * growth(index)
                    drawRoundRect(
                        color = if (value > goalMinutes) colors.brandSky else colors.brandSkyLight,
                        topLeft = Offset(x, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = radius,
                    )
                }
            }
            val y = goalY.toPx()
            drawLine(
                color = colors.accentTangerine,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.05.dp.toPx(), 4.04.dp.toPx())),
            )
        }
        Text(
            goalLabel,
            style = RecentrTheme.typography.labelSmall,
            color = colors.accentTangerine,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = if (goalY >= 18.dp) goalY - 18.dp else goalY + 4.dp),
        )
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    label,
                    style = RecentrTheme.typography.labelSmall,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Figma 100 "By hour" chart (104dp): 24 bars of 9dp spread over the width, rising from an 84dp
 * baseline (80dp for the busiest hour). Peak hours (≥ 75% of the busiest) are `brand/sky`, empty
 * hours 3dp sunken stubs; 12 AM · 6 AM · 12 PM · 6 PM under the bars.
 */
@Composable
internal fun HourBarsChart(hourly: List<Int>, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val growth = rememberBarsGrowth(hourly.size, staggerMillis = 20, key = hourly)
    val max = hourly.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peak = hourly.maxOrNull() ?: 0
    val busiest = hourly.indices.maxByOrNull { hourly[it] }
    val description = if (peak > 0 && busiest != null) "${hourLabel(busiest)}: ${peak}m" else ""
    BoxWithConstraints(modifier.fillMaxWidth().height(104.dp).semantics { contentDescription = description }) {
        val barWidth = 9.dp
        val pitch = (maxWidth - barWidth) / 23
        Canvas(Modifier.fillMaxWidth().height(84.dp)) {
            val bar = barWidth.toPx()
            val step = pitch.toPx()
            val radius = CornerRadius(3.dp.toPx())
            hourly.forEachIndexed { hour, minutes ->
                val x = step * hour
                if (minutes <= 0) {
                    val stub = 3.dp.toPx()
                    drawRoundRect(colors.bgSunken, Offset(x, size.height - stub), Size(bar, stub), radius)
                } else {
                    val height = (80.dp.toPx() * minutes / max).coerceAtLeast(4.dp.toPx()) * growth(hour)
                    drawRoundRect(
                        color = if (minutes * 4 >= peak * 3) colors.brandSky else colors.brandSkyLight,
                        topLeft = Offset(x, size.height - height),
                        size = Size(bar, height),
                        cornerRadius = radius,
                    )
                }
            }
        }
        listOf(0, 6, 12, 18)
            .forEachIndexed { index, hour ->
                Text(
                    hourLabel(hour),
                    style = RecentrTheme.typography.labelSmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    modifier = Modifier.offset(x = pitch * (index * 6), y = 88.dp).clearAndSetSemantics { },
                )
            }
    }
}

/** Colour of a pickup-heat or calendar cell from how busy it was (0…1 of the busiest). */
internal fun heatColor(fraction: Float, sunken: Color, aurora: Color, light: Color, sky: Color, deep: Color): Color = when {
    fraction <= 0f -> sunken
    fraction <= 0.25f -> aurora
    fraction <= 0.5f -> light
    fraction <= 0.75f -> sky
    else -> deep
}

/** Figma 101 "Heat": twelve two-hour cells (36dp, 8dp corners) shaded by pickups. */
@Composable
internal fun PickupHeatStrip(hourlyPickups: List<Int>, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val cells = (0 until 12).map { hourlyPickups[it * 2] + hourlyPickups[it * 2 + 1] }
    val max = cells.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            cells.forEachIndexed { index, count ->
                val description = stringResource(R.string.insights_heat_cd, hourLabel(index * 2), hourLabel(index * 2 + 2), count)
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            heatColor(
                                count.toFloat() / max,
                                colors.bgSunken, colors.auroraBlue, colors.brandSkyLight, colors.brandSky, colors.brandSkyDeep,
                            ),
                        )
                        .semantics { contentDescription = description },
                )
            }
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18, 0)
                .forEach { Text(hourLabel(it), style = RecentrTheme.typography.labelSmall, color = colors.textTertiary) }
        }
    }
}

/** How a calendar day is shaded (Figma 102 legend). */
internal enum class CalendarShade { None, UnderGoal, Light, Medium, Strong, Deep }

/** Shade of a day: mint under the goal, then blue deepening with the time over it. */
internal fun calendarShade(day: DayStats?, goalMinutes: Int, today: LocalDate): CalendarShade = when {
    day == null || !day.hasData || day.date.isAfter(today) -> CalendarShade.None
    day.minutes <= goalMinutes -> CalendarShade.UnderGoal
    day.minutes <= goalMinutes * 1.25f -> CalendarShade.Light
    day.minutes <= goalMinutes * 1.5f -> CalendarShade.Medium
    day.minutes <= goalMinutes * 2f -> CalendarShade.Strong
    else -> CalendarShade.Deep
}

/**
 * Figma 102 "Calendar": M → S initials, one row per week of [month] (42dp days, 12dp corners,
 * 6dp gaps), today outlined, then the "Under goal · Less ▢▢▢▢ More" legend.
 */
@Composable
internal fun MonthCalendar(
    month: YearMonth,
    days: Map<LocalDate, DayStats>,
    goalMinutes: Int,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val initials = stringArrayResource(R.array.insights_weekday_initials)
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val weeks = cells.chunked(7).map { it + List(7 - it.size) { null } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            initials.forEach {
                Text(it, style = RecentrTheme.typography.labelSmall, color = colors.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).height(42.dp))
                    } else {
                        CalendarDay(date, days[date], goalMinutes, today, Modifier.weight(1f))
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp).clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(colors.auroraMint)
            Text(stringResource(R.string.insights_under_goal), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.insights_less), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
            listOf(colors.auroraBlue, colors.brandSkyLight, colors.brandSky, colors.brandSkyDeep).forEach { LegendDot(it) }
            Text(stringResource(R.string.insights_more_legend), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
}

@Composable
private fun CalendarDay(date: LocalDate, day: DayStats?, goalMinutes: Int, today: LocalDate, modifier: Modifier) {
    val colors = RecentrTheme.colors
    val shade = calendarShade(day, goalMinutes, today)
    val (background, content) = when (shade) {
        CalendarShade.None -> Color.Transparent to colors.textTertiary
        CalendarShade.UnderGoal -> colors.auroraMint to colors.textPrimary
        CalendarShade.Light -> colors.auroraBlue to colors.textPrimary
        CalendarShade.Medium -> colors.brandSkyLight to colors.textPrimary
        CalendarShade.Strong -> colors.brandSky to colors.textOnDark
        CalendarShade.Deep -> colors.brandSkyDeep to colors.textOnDark
    }
    val shape = RoundedCornerShape(12.dp)
    val label = shortDate(date)
    val description = if (shade == CalendarShade.None) {
        stringResource(R.string.insights_day_cd_none, label)
    } else {
        stringResource(R.string.insights_day_cd, label, duration(day?.minutes ?: 0))
    }
    Box(
        modifier
            .height(42.dp)
            .clip(shape)
            .background(background)
            .then(if (date == today) Modifier.border(2.dp, colors.textPrimary, shape) else Modifier)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(date.dayOfMonth.toString(), style = RecentrTheme.typography.captionMedium, color = content, maxLines = 1)
    }
}

/**
 * Figma 104 "Ring" (190dp): a 16dp `bg/sunken` track and the green progress arc from 12 o'clock,
 * sweeping in on first show (`.spring(duration: 1.1, bounce: 0.2).delay(0.2)`).
 */
@Composable
internal fun GoalRing(fraction: Float, modifier: Modifier = Modifier, size: Dp = 190.dp, stroke: Dp = 16.dp) {
    val colors = RecentrTheme.colors
    val sweep = rememberFill(fraction.coerceIn(0f, 1f), delayMillis = 200, duration = 1.1f)
    Canvas(modifier.size(size)) {
        val width = stroke.toPx()
        val inset = width / 2f
        val arcSize = Size(this.size.width - width, this.size.height - width)
        drawArc(colors.bgSunken, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(width))
        if (fraction > 0f) {
            drawArc(
                color = colors.feedbackSuccess,
                startAngle = -90f,
                sweepAngle = 360f * sweep.value.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width, cap = StrokeCap.Butt),
            )
        }
    }
}

/** A segment of the "By category" stack. */
internal data class StackSegment(val color: Color, val share: Float)

/** Figma 99 "Stack": 12dp rounded segments 3dp apart, widths by share. */
@Composable
internal fun CategoryStack(segments: List<StackSegment>, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Row(modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val visible = segments.filter { it.share > 0f }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(12.dp).clip(shape).background(colors.bgSunken))
        }
        visible.forEach { segment ->
            Box(Modifier.weight(segment.share).height(12.dp).clip(shape).background(segment.color))
        }
    }
}
