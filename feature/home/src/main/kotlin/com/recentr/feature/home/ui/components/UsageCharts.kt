package com.recentr.feature.home.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.format.clockTimeShort
import com.recentr.core.designsystem.motion.rememberBarsGrowth
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.feature.home.R

/** Figma reference width of the card charts (354 card − 2 × 20 padding). */
private const val ChartReferenceWidth = 314f

/**
 * "Chart · Hourly" of the Today card: 24 bars of 7dp (13.2dp pitch at the 314dp reference
 * width), a full hour reaching 72dp, empty hours drawn as 4dp sunken stubs. Unlike the design
 * system [com.recentr.core.designsystem.component.HourlyUsageChart], each bar can take its own
 * colour (the Quality page paints focus hours in `feedback/success`).
 */
@Composable
internal fun HourlyBarsChart(
    minutesPerHour: List<Int>,
    modifier: Modifier = Modifier,
    barColor: (hour: Int) -> Color,
) {
    val colors = RecentrTheme.colors
    val heights = minutesPerHour.mapIndexed { index, minutes ->
        animatedFraction(minutes.coerceIn(0, 60) / 60f, index)
    }
    val barColors = minutesPerHour.indices.map { hour -> if (minutesPerHour[hour] <= 0) colors.bgSunken else barColor(hour) }
    val growth = rememberBarsGrowth(minutesPerHour.size, staggerMillis = 20)
    BoxWithConstraints(modifier.fillMaxWidth().height(112.dp)) {
        val scale = maxWidth / ChartReferenceWidth
        Canvas(Modifier.fillMaxWidth().height(88.dp)) {
            val pitch = 13.2f * scale.toPx()
            val barWidth = 7.dp.toPx()
            val stub = 4.dp.toPx()
            val maxHeight = 72.dp.toPx()
            heights.forEachIndexed { index, fraction ->
                val h = (maxHeight * fraction.value).coerceAtLeast(stub) * growth(index)
                drawRoundRect(
                    color = barColors[index],
                    topLeft = Offset(index * pitch, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(3.5.dp.toPx()),
                )
            }
        }
        val labels = listOf(
            clockTimeShort(0) to 0f,
            clockTimeShort(6 * 60) to 76f,
            clockTimeShort(12 * 60) to 156f,
            clockTimeShort(18 * 60) to 236f,
        )
        labels.forEach { (label, x) ->
            Text(
                text = label,
                style = RecentrTheme.typography.labelSmall,
                color = colors.textTertiary,
                maxLines = 1,
                modifier = Modifier.offset(x = scale * x, y = 96.dp),
            )
        }
    }
}

/**
 * "Chart · Week" of the This week card: 7 bars (28dp, radius 10) reaching 76dp for the busiest
 * day, the best (lowest) day in `feedback/success`, a dashed line at the daily average and the
 * weekday letters (today's in `text/primary`).
 */
@Composable
internal fun WeekBarsChart(
    dailyMinutes: List<Int>,
    averageMinutes: Int,
    bestIndex: Int,
    todayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val max = dailyMinutes.maxOrNull()?.coerceAtLeast(1) ?: 1
    val fractions = dailyMinutes.mapIndexed { index, minutes -> animatedFraction(minutes / max.toFloat(), index) }
    val average = animatedFraction((averageMinutes / max.toFloat()).coerceIn(0f, 1.3f), 7)
    val letters = stringArrayResource(R.array.home_day_letters)
    val growth = rememberBarsGrowth(dailyMinutes.size, staggerMillis = 50)
    BoxWithConstraints(modifier.fillMaxWidth().height(130.dp)) {
        val scale = maxWidth / ChartReferenceWidth
        Canvas(Modifier.fillMaxWidth().height(100.dp)) {
            val unit = scale.toPx()
            val barWidth = 28 * unit
            val maxHeight = 76.dp.toPx()
            fractions.forEachIndexed { index, fraction ->
                val h = (maxHeight * fraction.value).coerceAtLeast(10.dp.toPx()) * growth(index)
                drawRoundRect(
                    color = if (index == bestIndex) colors.feedbackSuccess else colors.brandSky,
                    topLeft = Offset((8 + index * 44) * unit, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                )
            }
            val y = size.height - maxHeight * average.value
            drawLine(
                color = colors.borderDashed,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.03f * unit, 4.03f * unit), 2.01f * unit),
            )
        }
        letters.forEachIndexed { index, letter ->
            Text(
                text = letter,
                style = RecentrTheme.typography.labelSmall,
                color = if (index == todayIndex) colors.textPrimary else colors.textTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .offset(x = scale * (index * 44f), y = 110.dp)
                    .width(scale * 44f),
            )
        }
    }
}

@Composable
private fun animatedFraction(target: Float, index: Int): State<Float> =
    animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 450, delayMillis = index * 12),
        label = "bar$index",
    )
