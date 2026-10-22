package com.recentr.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.haptics.SensoryFeedback
import com.recentr.core.designsystem.motion.LocalReduceMotion
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.motion.pressScale
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** The values a [DurationPicker] offers: every [step] from the range's start, and its end. */
@Immutable
data class DurationScale(val range: IntRange, val step: Int = 5, val labelEvery: Int = step * 6) {
    val values: List<Int> = buildList {
        addAll(range.first..range.last step step.coerceAtLeast(1))
        if (last() != range.last) add(range.last)
    }

    private val labelStep = labelEvery.coerceAtLeast(1)

    /** A long tick, with its label. */
    fun isLabelled(value: Int): Boolean = value % labelStep == 0

    /** Where the value nearest [value] sits, the closer to the start on a tie. */
    fun indexNearest(value: Int): Int {
        val clamped = value.coerceIn(values.first(), values.last())
        return values.indices.minBy { abs(values[it] - clamped) }
    }

    /** The value nearest [value], so one off the scale lands on a tick. */
    fun nearest(value: Int): Int = values[indexNearest(value)]

    /** The value [offset] ticks from [value], held within the scale. */
    fun stepped(value: Int, offset: Int): Int = values[(indexNearest(value) + offset).coerceIn(values.indices)]
}

/** "45m", "1h", "2h15". */
@Composable
fun compactMinutes(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.ds_duration_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.ds_duration_hours, minutes / 60)
    else -> stringResource(R.string.ds_duration_hours_minutes, minutes / 60, minutes % 60)
}

/**
 * Picks a duration on a ruler: the value on top, ticks that scroll under a fixed needle, and
 * presets to jump to. The value is minutes unless the formats say otherwise (seconds, rounds…).
 *
 * @param contentDescription what the value is, for TalkBack.
 * @param range the first and last ticks.
 * @param step the gap between two ticks.
 * @param labelEvery the values that are its multiples get a long tick and a label; six steps by
 *   default.
 * @param presets values to jump to, under the ruler. Those out of the range are left out.
 * @param presetsTitle above the presets, or nothing.
 * @param tickSpacing the room between two ticks.
 * @param format the value on top ("1h30").
 * @param compactFormat the ticks' labels and the presets; [format] by default.
 */
@Composable
fun DurationPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    contentDescription: String,
    modifier: Modifier = Modifier,
    step: Int = 5,
    labelEvery: Int = step * 6,
    presets: List<Int> = emptyList(),
    presetsTitle: String? = stringResource(R.string.ds_duration_quick_select),
    tickSpacing: Dp = 12.dp,
    format: @Composable (Int) -> String = { compactMinutes(it) },
    compactFormat: @Composable (Int) -> String = format,
) {
    val scale = remember(range, step, labelEvery) { DurationScale(range, step, labelEvery) }
    val shownPresets = remember(presets, range) { presets.filter { it in range } }
    val labels = scale.values.filter(scale::isLabelled).associateWith { compactFormat(it) }
    val valueText = format(value)
    val index = scale.indexNearest(value)
    SensoryFeedback(trigger = value) { if (scale.isLabelled(value)) mediumImpact() else selection() }
    // A value off the scale lands on its nearest tick.
    LaunchedEffect(scale, value) {
        if (scale.nearest(value) != value) onValueChange(scale.nearest(value))
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    stateDescription = valueText
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = index.toFloat(),
                        range = 0f..scale.values.lastIndex.toFloat(),
                        steps = (scale.values.size - 2).coerceAtLeast(0),
                    )
                    setProgress { target ->
                        val picked = scale.values[target.roundToInt().coerceIn(scale.values.indices)]
                        if (picked != value) onValueChange(picked)
                        true
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = valueText,
                style = RecentrTheme.typography.displayHero.copy(
                    color = RecentrTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 44.sp, maxFontSize = 72.sp, stepSize = 2.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            DurationRuler(value, onValueChange, scale, labels, tickSpacing)
        }
        if (shownPresets.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (presetsTitle != null) {
                    Text(
                        presetsTitle.uppercase(),
                        style = RecentrTheme.typography.captionMedium,
                        color = RecentrTheme.colors.textTertiary,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    shownPresets.forEach { preset ->
                        val description = format(preset)
                        PresetChip(compactFormat(preset), description, selected = preset == value) { onValueChange(preset) }
                    }
                }
            }
        }
    }
}

private val TickWidth = 2.dp
private val LongTick = 36.dp
private val ShortTick = 18.dp
private val LabelHeight = 30.dp

/**
 * The ticks, sliding under the needle. The tick under it is the value: dragging and flinging the
 * ruler sets it, and it slides to a value set from elsewhere (a preset, TalkBack).
 */
@Composable
private fun DurationRuler(
    value: Int,
    onValueChange: (Int) -> Unit,
    scale: DurationScale,
    labels: Map<Int, String>,
    tickSpacing: Dp,
) {
    val colors = RecentrTheme.colors
    val reduceMotion = LocalReduceMotion.current
    val pitch = with(LocalDensity.current) { (TickWidth + tickSpacing).toPx() }
    val lastIndex = scale.values.lastIndex
    val maxPosition = lastIndex * pitch
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    // How far the ruler has slid, in pixels: the tick under the needle is position / pitch.
    var position by remember(scale, pitch) { mutableFloatStateOf(scale.indexNearest(value) * pitch) }
    var underNeedle by remember(scale, pitch) { mutableIntStateOf(scale.indexNearest(value)) }
    // Only the person's own dragging and flinging sets the value: sliding to a preset passes others.
    var isUserDriven by remember { mutableStateOf(false) }
    val dragState = rememberDraggableState { delta ->
        position = (position - delta).coerceIn(0f, maxPosition)
        val index = (position / pitch).roundToInt().coerceIn(0, lastIndex)
        if (index != underNeedle) {
            underNeedle = index
            if (isUserDriven && scale.values[index] != currentValue) currentOnValueChange(scale.values[index])
        }
    }
    val decay = rememberSplineBasedDecay<Float>()

    LaunchedEffect(scale, value) {
        val target = scale.indexNearest(value) * pitch
        if (isUserDriven || position == target) return@LaunchedEffect
        dragState.drag(MutatePriority.Default) {
            if (reduceMotion) {
                dragBy(position - target)
            } else {
                animate(position, target, animationSpec = MotionSpecs.snappy(0.35f)) { moved, _ -> dragBy(position - moved) }
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = RecentrTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
    val measured: Map<Int, TextLayoutResult> = remember(labels, labelStyle) {
        labels.mapValues { (_, label) -> textMeasurer.measure(label, labelStyle) }
    }
    val tickColor = colors.textTertiary
    val longTickColor = colors.textSecondary
    val labelColor = colors.textTertiary
    val selectedLabelColor = colors.textPrimary
    val needleColor = colors.textPrimary

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(LabelHeight + LongTick)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { isUserDriven = true },
                    onDragStopped = { velocity ->
                        // Glide on, then settle on the nearest tick.
                        dragState.drag(MutatePriority.UserInput) {
                            if (!reduceMotion && abs(velocity) > 1f) {
                                var previous = 0f
                                AnimationState(initialValue = 0f, initialVelocity = -velocity).animateDecay(decay) {
                                    val before = position
                                    dragBy(-(this.value - previous))
                                    previous = this.value
                                    if (position == before && (position == 0f || position == maxPosition)) cancelAnimation()
                                }
                            }
                            val target = (position / pitch).roundToInt().coerceIn(0, lastIndex) * pitch
                            animate(position, target, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { moved, _ ->
                                dragBy(position - moved)
                            }
                        }
                        isUserDriven = false
                    },
                ),
        ) {
            Canvas(
                Modifier
                    .matchParentSize()
                    // The ends fade out.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.horizontalGradient(0f to Color.Transparent, 0.3f to Color.Black, 0.7f to Color.Black, 1f to Color.Transparent),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                val center = size.width / 2
                val first = floor((position - center) / pitch).toInt().coerceAtLeast(0)
                val last = ceil((position + center) / pitch).toInt().coerceAtMost(lastIndex)
                val tickWidth = TickWidth.toPx()
                for (index in first..last) {
                    val tick = scale.values[index]
                    val x = center + index * pitch - position
                    val isLabelled = scale.isLabelled(tick)
                    val height = (if (isLabelled) LongTick else ShortTick).toPx()
                    drawLine(
                        color = if (isLabelled) longTickColor else tickColor,
                        start = Offset(x, size.height - height),
                        end = Offset(x, size.height),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round,
                    )
                    measured[tick]?.let { label ->
                        drawText(
                            label,
                            color = if (index == underNeedle) selectedLabelColor else labelColor,
                            topLeft = Offset(x - label.size.width / 2f, 0f),
                        )
                    }
                }
            }
            // The needle, over the tick under it.
            Canvas(Modifier.matchParentSize()) {
                val needle = 3.dp.toPx()
                val height = (LongTick + 6.dp).toPx()
                drawLine(
                    color = needleColor,
                    start = Offset(size.width / 2, size.height - height + 3.dp.toPx()),
                    end = Offset(size.width / 2, size.height + 3.dp.toPx() - needle / 2),
                    strokeWidth = needle,
                    cap = StrokeCap.Round,
                )
            }
        }
        Canvas(Modifier.size(width = 14.dp, height = 12.dp)) {
            val triangle = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(triangle, needleColor)
        }
    }
}

/** A preset under the ruler: black once it's the value. */
@Composable
private fun PresetChip(label: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    val background by animateColorAsState(
        if (selected) colors.actionPrimary else colors.bgSunken,
        MotionSpecs.smooth(0.2f),
        label = "presetBackground",
    )
    val content by animateColorAsState(
        if (selected) colors.textOnAction else colors.textPrimary,
        MotionSpecs.smooth(0.2f),
        label = "presetContent",
    )
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = RecentrTheme.typography.bodyMedium,
        color = content,
        maxLines = 1,
        modifier = Modifier
            .pressScale(interactionSource, pressedScale = 0.96f)
            .clip(RecentrShapes.full)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Preview(widthDp = 402, showBackground = true)
@Composable
private fun DurationPickerPreview() {
    RecentrTheme {
        var goal by rememberSaveable { mutableIntStateOf(225) }
        Column(Modifier.padding(24.dp)) {
            DurationPicker(
                value = goal,
                onValueChange = { goal = it },
                range = 15..720,
                contentDescription = "Daily goal",
                labelEvery = 30,
                presets = listOf(120, 180, 225, 260, 300),
            )
        }
    }
}
