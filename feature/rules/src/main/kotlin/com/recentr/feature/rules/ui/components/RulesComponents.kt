package com.recentr.feature.rules.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recentr.core.data.unlockMethodEnabled
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.SettingToggleRow
import com.recentr.core.designsystem.component.TemplateColor
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.Rule
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.unlockLabel

/** Sunken rounded container grouping setting rows (Figma "Settings", 4dp padding/gap). */
@Composable
internal fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RecentrTheme.colors.bgSunken)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * "Strict mode" row of sessions and routines: tangerine shield tile and the toggle. Strict mode
 * leaves no unlocks but emergency ones while the rule is engaged; turned off then, it goes off
 * when the rule's engagement ends.
 */
@Composable
internal fun StrictToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    SettingToggleRow(
        label = stringResource(R.string.rules_strict_mode),
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = RecentrIcon.Shield,
        iconTint = colors.accentTangerine,
        iconBackground = colors.alphaTangerine14,
        caption = stringResource(R.string.rules_strict_caption),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A line of information in a settings group: an icon and a caption ("Resets automatically at midnight."). */
@Composable
internal fun InfoRow(text: String, icon: RecentrIcon, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bgSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconImage(icon, tint = colors.textTertiary, size = 16.dp)
        Text(text, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
    }
}

/**
 * "Unlock method" row of locks, sessions and routines (41 "How do you unlock?"). Strict mode
 * disables unlocks: while it's on, the row keeps the method, dimmed, and says why.
 */
@Composable
internal fun UnlockMethodRow(
    rule: Rule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: RecentrIcon = RecentrIcon.Hourglass,
) {
    val enabled = rule.unlockMethodEnabled
    SettingRow(
        label = stringResource(R.string.rules_row_unlock_method),
        value = unlockLabel(rule),
        onClick = onClick,
        icon = icon,
        caption = if (enabled) null else stringResource(R.string.rules_unlock_strict_caption),
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Sheet hero: 84dp sticker, editable title with pencil, centered subtitle. */
@Composable
internal fun RuleSheetHeader(
    sticker: Sticker,
    title: String,
    subtitle: String,
    onEditTitle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StickerImage(sticker, size = 84.dp)
        Spacer(Modifier.height(6.dp))
        val editLabel = stringResource(R.string.rules_edit_name)
        Row(
            modifier = Modifier
                .clip(RecentrShapes.sm)
                .clickable(role = Role.Button, onClick = onEditTitle)
                .semantics { contentDescription = editLabel }
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = RecentrTheme.typography.title1,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            RecentrIconImage(RecentrIcon.Pencil, tint = colors.textTertiary, size = 19.2.dp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = RecentrTheme.typography.bodyRegular,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Uppercase caption above a block ("SUGGESTIONS", "SOCIAL", "STICKER"). */
@Composable
internal fun SectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = RecentrTheme.typography.captionMedium,
        color = RecentrTheme.colors.textTertiary,
        modifier = modifier.fillMaxWidth(),
    )
}

private val MiniCardShadow = Shadow(radius = 19.84.dp, color = Color(0x1F0E1220), offset = DpOffset(0.dp, 7.44.dp))

/**
 * Template card scaled to 62% (session "Suggestions" row, 102.3×130.2 in the design). Its width
 * comes from [modifier] (the row shares it out), its height keeps the design's proportions. The
 * design-system [com.recentr.core.designsystem.component.TemplateCard] has fixed 16dp paddings.
 */
@Composable
internal fun MiniTemplateCard(
    title: String,
    badge: String,
    sticker: Sticker,
    color: TemplateColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val type = RecentrTheme.typography
    Column(
        modifier = modifier
            .aspectRatio(102.3f / 130.2f)
            .recentrShadow(MiniCardShadow, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(color.brush)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(9.92.dp),
        verticalArrangement = Arrangement.spacedBy(3.72.dp),
    ) {
        Text(
            badge,
            style = type.captionMedium.copy(fontSize = 8.06.sp, lineHeight = 10.88.sp),
            color = colors.textOnDark,
            maxLines = 1,
            modifier = Modifier
                .clip(RecentrShapes.full)
                .background(colors.alphaWhite24)
                .padding(start = 6.2.dp, end = 7.44.dp, top = 3.72.dp, bottom = 3.72.dp),
        )
        Spacer(Modifier.weight(1f))
        StickerImage(sticker, size = 37.2.dp)
        Text(
            title,
            style = type.title2.copy(fontSize = 13.64.sp, lineHeight = 16.64.sp),
            color = colors.textOnDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.88.dp))
    }
}

/** Green success toast ("Session started · 30 min"). */
@Composable
internal fun RulesToastPill(text: String, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Row(
        modifier = modifier
            .recentrShadow(RecentrElevation.float, RecentrShapes.full)
            .clip(RecentrShapes.full)
            .background(colors.feedbackSuccess)
            .padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconImage(RecentrIcon.Check, tint = colors.textOnDark)
        Text(text, style = RecentrTheme.typography.headline, color = colors.textOnDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Collapsible group header of the rules list ("⌄ Locks  1"). */
@Composable
internal fun RuleGroupHeader(
    label: String,
    count: Int?,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val rotation by animateFloatAsState(if (expanded) 0f else -90f, label = "groupChevron")
    Row(
        modifier = modifier
            .clip(RecentrShapes.xs)
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(top = 8.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconImage(RecentrIcon.ChevronDown, tint = colors.textTertiary, size = 18.dp, modifier = Modifier.rotate(rotation))
        Text(label, style = RecentrTheme.typography.headline, color = colors.textSecondary)
        if (count != null) {
            Text("  $count", style = RecentrTheme.typography.captionMedium, color = colors.textTertiary)
        }
    }
}

/**
 * Chip with an independently tinted icon (the "Strict mode" chip has a tangerine shield). Same
 * metrics as [RecentrChip].
 */
@Composable
internal fun IconTintChip(label: String, icon: RecentrIcon, iconTint: Color, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    Row(
        modifier = modifier
            .clip(RecentrShapes.full)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, RecentrShapes.full)
            // Same metrics as the Outline RecentrChip (stroke included in the size, 32dp high).
            .padding(start = 11.dp, end = 13.dp, top = 7.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconImage(icon, tint = iconTint, size = 16.08.dp)
        Text(label, style = RecentrTheme.typography.captionMedium, color = colors.textSecondary, maxLines = 1)
    }
}

/** Draws a [width] border on top of the content (active rule card = 2dp success outline). */
internal fun Modifier.overlayBorder(color: Color, width: Dp, cornerRadius: Dp): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx() - stroke / 2),
        style = Stroke(stroke),
    )
}

/** Pill CTA with a custom fill: danger red (not in the DS button styles). */
@Composable
internal fun RulesPillButton(
    text: String,
    onClick: () -> Unit,
    style: RulesPillStyle,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val brush: Brush = when (style) {
        RulesPillStyle.Danger -> SolidColor(colors.feedbackDanger)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RecentrTheme.sizes.button)
            .clip(RecentrShapes.full)
            .background(brush)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RecentrTheme.typography.headline, color = colors.textOnDark, maxLines = 1)
    }
}

internal enum class RulesPillStyle { Danger }

/** Display/Hero value ("45 min", "5 seconds") shrinking to fit on narrow screens. */
@Composable
internal fun HeroValueText(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = RecentrTheme.typography.displayHero.copy(color = RecentrTheme.colors.textPrimary, textAlign = TextAlign.Center),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 36.sp, maxFontSize = 72.sp, stepSize = 2.sp),
        modifier = modifier,
    )
}

/** Row of preset chips; the selected one is the black "Selected" chip, the others outlined. */
@Composable
internal fun PresetChips(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalRecentrHaptics.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            RecentrChip(
                label = label,
                style = if (index == selectedIndex) RecentrChipStyle.Selected else RecentrChipStyle.Outline,
                onClick = {
                    if (index != selectedIndex) haptics.selection()
                    onSelect(index)
                },
            )
        }
    }
}

/** Tilted white time card ("FROM 11:00 PM · tonight", detail "WEEKDAYS 9 – 5"). */
@Composable
internal fun TimeCard(
    label: String,
    value: String,
    caption: String,
    rotation: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) = TimeCard(label, value, rotation, modifier, onClick = onClick) {
    Text(caption, style = RecentrTheme.typography.captionRegular, color = RecentrTheme.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * A time card with any [footer] under its value: a caption, or a chip saying how things stand.
 * [outline] borders it in a colour, as a running rule's card.
 */
@Composable
internal fun TimeCard(
    label: String,
    value: String,
    rotation: Float,
    modifier: Modifier = Modifier,
    outline: Color? = null,
    onClick: (() -> Unit)? = null,
    footer: @Composable () -> Unit,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .recentrShadow(RecentrElevation.float, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, shape)
            .then(if (outline != null) Modifier.border(2.dp, outline, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, style = RecentrTheme.typography.captionMedium, color = colors.feedbackDanger, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Shrinks rather than clips when the card is held narrower than its value ("21:16 – 21:30",
        // a countdown of "14h56m"). Tabular digits keep a countdown from shifting as it ticks.
        BasicText(
            value,
            style = RecentrTheme.typography.displayLarge.copy(color = colors.textPrimary, fontFeatureSettings = "tnum"),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 17.sp, maxFontSize = RecentrTheme.typography.displayLarge.fontSize, stepSize = 1.sp),
        )
        footer()
    }
}

/** M T W T F S S day toggles (42dp discs), easing between states (`.snappy(duration: 0.2)`) with a tick. */
@Composable
internal fun DayDots(
    days: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val letters = androidx.compose.ui.platform.LocalContext.current.resources.getStringArray(R.array.rules_day_letters)
    val names = androidx.compose.ui.platform.LocalContext.current.resources.getStringArray(R.array.rules_day_long)
    val haptics = LocalRecentrHaptics.current
    // The seven chips share the row (42dp at most), so narrow screens never clip Sunday.
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        (1..7).forEach { day ->
            val selected = day in days
            val fill by animateColorAsState(
                if (selected) colors.actionPrimary else colors.bgSunken,
                MotionSpecs.smooth(0.2f),
                label = "dayFill",
            )
            val ink by animateColorAsState(
                if (selected) colors.textOnAction else colors.textSecondary,
                MotionSpecs.smooth(0.2f),
                label = "dayInk",
            )
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 42.dp)
                    .aspectRatio(1f)
                    .clip(RecentrShapes.full)
                    .background(fill)
                    .toggleable(value = selected, role = Role.Checkbox, onValueChange = {
                        haptics.selection()
                        onToggle(day)
                    })
                    .semantics { contentDescription = names[day - 1] },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letters[day - 1],
                    style = RecentrTheme.typography.captionMedium,
                    color = ink,
                )
            }
        }
    }
}

/** Spacer helper for exact Figma gaps inside columns. */
@Composable
internal fun Gap(height: Dp) = Spacer(Modifier.height(height))

@Composable
internal fun HGap(width: Dp) = Spacer(Modifier.width(width))
