package com.recentr.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.rememberBarsGrowth
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

private fun Modifier.clickableIf(onClick: (() -> Unit)?, role: Role = Role.Button) =
    if (onClick != null) clickable(role = role, onClick = onClick) else this

/** White surface card with the Elevation/Card shadow (radius 28 by default). */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = RecentrTheme.radius.card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.card, shape)
            .clip(shape)
            .background(RecentrTheme.colors.bgSurface)
            .clickableIf(onClick),
        content = content,
    )
}

/**
 * Figma "Rule Card" — Lock / Session / Limit / Routine rule summary.
 *
 * @param chips status + schedule chips (use [RecentrChip]).
 * @param apps overlapping app icons shown after the [leadingIcon] ([OverlappingInstalledAppIcons]).
 * @param valueColor the [value]'s colour, primary text by default.
 */
@Composable
fun RuleCard(
    title: String,
    sticker: Sticker,
    modifier: Modifier = Modifier,
    streak: Int? = null,
    apps: @Composable () -> Unit = {},
    value: String? = null,
    valueColor: Color? = null,
    leadingIcon: RecentrIcon = RecentrIcon.Lock,
    onClick: (() -> Unit)? = null,
    chips: @Composable RowScope.() -> Unit = {},
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.control, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .clickableIf(onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StickerImage(sticker, size = 36.dp)
            Text(
                text = title,
                style = RecentrTheme.typography.headline,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (streak != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    RecentrIconImage(RecentrIcon.Flame, tint = colors.accentTangerine, size = 16.08.dp)
                    ChipText(streak.toString(), colors.accentTangerine)
                }
            }
            RecentrIconImage(RecentrIcon.ChevronRight, tint = colors.textTertiary, size = 18.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecentrIconImage(leadingIcon, tint = colors.textTertiary, size = 16.08.dp)
            apps()
            Spacer(Modifier.weight(1f))
            if (value != null) {
                // Title 2, 4sp smaller (18sp, as on iOS). One line, flush right: it shrinks (to 70%,
                // as on iOS) rather than losing its last word to a hidden second line when the apps
                // leave it little room ("7:45:14 left"). Tabular digits keep a countdown from
                // shifting as it ticks.
                val title2 = RecentrTheme.typography.title2
                val scale = 18f / title2.fontSize.value
                BasicText(
                    text = value,
                    style = title2.copy(
                        fontSize = 18.sp,
                        lineHeight = title2.lineHeight * scale,
                        color = valueColor ?: colors.textPrimary,
                        textAlign = TextAlign.End,
                        fontFeatureSettings = "tnum",
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 18.sp, stepSize = 1.sp),
                )
            }
        }
    }
}

/**
 * Installed apps' launcher icons, by package name, overlapping by [overlap] (Rule card, app picker
 * summaries).
 */
@Composable
fun OverlappingInstalledAppIcons(packageNames: List<String>, size: Dp = 24.dp, overlap: Dp = 6.dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(-overlap)) {
        packageNames.forEach { InstalledAppIcon(it, size = size) }
    }
}

/** One stat column of the [TodayCard]. */
@Immutable
data class TodayStat(val label: String, val value: String, val valueColor: Color? = null)

/**
 * Figma "Today Card" — decrease / pickups / notifications + 24h usage bars.
 * The [chart] slot defaults to an [HourlyUsageChart].
 */
@Composable
fun TodayCard(
    stats: List<TodayStat>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    chart: @Composable () -> Unit,
) {
    val colors = RecentrTheme.colors
    val shape = RecentrShapes.card
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.raised, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .clickableIf(onClick)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stats.forEach { stat ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stat.label, style = RecentrTheme.typography.captionRegular, color = colors.textTertiary, maxLines = 1)
                    Text(
                        stat.value,
                        style = RecentrTheme.typography.title2,
                        color = stat.valueColor ?: colors.textPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
        chart()
    }
}

/**
 * 24 hourly bars (7dp wide, rounded 3.5). Each value is the minutes used during that hour;
 * a full hour reaches the 72dp max height. Empty hours draw a 4dp sunken stub.
 */
@Composable
fun HourlyUsageChart(
    minutesPerHour: List<Int>,
    modifier: Modifier = Modifier,
    axisLabels: List<String> = listOf("12 AM", "6 AM", "12 PM", "6 PM"),
    barColor: Color = RecentrTheme.colors.brandSky,
) {
    val colors = RecentrTheme.colors
    val growth = rememberBarsGrowth(minutesPerHour.size, staggerMillis = 20)
    BoxWithConstraints(modifier.fillMaxWidth().height(112.dp)) {
        val barWidth = 7.dp
        val count = minutesPerHour.size.coerceAtLeast(1)
        val step = (maxWidth - barWidth) / (count - 1).coerceAtLeast(1)
        Box(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .drawBehind {
                    val w = barWidth.toPx()
                    val stepPx = step.toPx()
                    val radius = CornerRadius(3.5.dp.toPx())
                    minutesPerHour.forEachIndexed { index, minutes ->
                        val full = if (minutes <= 0) 4.dp.toPx() else (72.dp.toPx() * (minutes.coerceAtMost(60) / 60f)).coerceAtLeast(4.dp.toPx())
                        val h = full * growth(index)
                        drawRoundRect(
                            color = if (minutes <= 0) colors.bgSunken else barColor,
                            topLeft = Offset(index * stepPx, size.height - h),
                            size = Size(w, h),
                            cornerRadius = radius,
                        )
                    }
                },
        )
        axisLabels.forEachIndexed { index, label ->
            val barIndex = index * count / axisLabels.size
            Text(
                text = label,
                style = RecentrTheme.typography.labelSmall,
                color = colors.textTertiary,
                maxLines = 1,
                modifier = Modifier.offset(x = if (barIndex == 0) 0.dp else step * barIndex - 2.dp, y = 96.dp),
            )
        }
    }
}

/** Figma "Add Card" — dashed "+ add" placeholder card. */
@Composable
fun AddCard(
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.alphaWhite80)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = colors.borderDashed,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(24.dp.toPx() - stroke / 2),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.5.dp.toPx(), 4.5.dp.toPx()))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            // + the 1.5dp dashed stroke, which Figma counts in the card size.
            .padding(horizontal = 17.5.dp, vertical = 15.5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RecentrShapes.full).background(colors.bgSunken),
            contentAlignment = Alignment.Center,
        ) {
            RecentrIconImage(RecentrIcon.Plus, tint = colors.textSecondary, size = 18.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = RecentrTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1)
            Text(hint, style = RecentrTheme.typography.captionRegular, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Figma "Mini Tile" — sticker + label + value (streak, next rule…). */
@Composable
fun MiniTile(
    label: String,
    value: String,
    sticker: Sticker,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .recentrShadow(RecentrElevation.control, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .clickableIf(onClick)
            .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(sticker, size = 43.2.dp)
        Column(Modifier.weight(1f)) {
            Text(label, style = RecentrTheme.typography.captionRegular, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = RecentrTheme.typography.headline, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Figma "Rule Tile" — tile of the "New rule" grid sheet. */
@Composable
fun RuleTile(
    label: String,
    sticker: Sticker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .height(124.dp)
            .clip(shape)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StickerImage(sticker, size = 72.dp)
        Text(label, style = RecentrTheme.typography.headline, color = colors.textPrimary, maxLines = 1)
    }
}

/** Figma "Sticky Note" — yellow note for reasons and reflections. */
@Composable
fun StickyNote(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    maxBodyLines: Int = Int.MAX_VALUE,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.raised, shape)
            .clip(shape)
            .background(colors.accentNote)
            .clickableIf(onClick)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = RecentrTheme.typography.headline, color = colors.accentNoteInk)
        Text(
            body,
            style = RecentrTheme.typography.noteHandwritten,
            color = colors.accentNoteInk,
            maxLines = maxBodyLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Editable [StickyNote] ("Send us a note", "What did you finish?"): the body is a text field in
 * the handwritten note style, [placeholder] shown while empty.
 */
@Composable
fun StickyNoteField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 2,
    /** Applied to the text field itself, e.g. a focus requester. */
    fieldModifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.raised, shape)
            .clip(shape)
            .background(colors.accentNote)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = RecentrTheme.typography.headline, color = colors.accentNoteInk)
        // Text set from outside (a suggestion chip) puts the caret at the end.
        var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
        BasicTextField(
            value = fieldValue,
            onValueChange = {
                fieldValue = it
                if (it.text != value) onValueChange(it.text)
            },
            textStyle = RecentrTheme.typography.noteHandwritten.copy(color = colors.accentNoteInk),
            cursorBrush = SolidColor(colors.accentNoteInk),
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth().then(fieldModifier),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, style = RecentrTheme.typography.noteHandwritten, color = colors.accentNoteInk.copy(alpha = 0.45f))
                    }
                    inner()
                }
            },
        )
    }
}

/** Grey sheet/drag grabber (fill/grabber). */
@Composable
fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(36.dp)
            .height(5.dp)
            .clip(RecentrShapes.full)
            .background(RecentrTheme.colors.fillGrabber),
    )
}
