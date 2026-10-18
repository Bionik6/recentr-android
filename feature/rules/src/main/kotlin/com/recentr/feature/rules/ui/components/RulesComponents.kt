package com.recentr.feature.rules.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.component.RecentrToggle
import com.recentr.core.designsystem.component.TemplateColor
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.feature.rules.R

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
 * Setting row with the tangerine shield tile and a toggle ("Strict mode"). The design-system
 * [com.recentr.core.designsystem.component.SettingToggleRow] only has the neutral icon tile.
 */
@Composable
internal fun StrictToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = RecentrTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bgSurface)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.alphaTangerine14),
            contentAlignment = Alignment.Center,
        ) {
            RecentrIconImage(RecentrIcon.Shield, tint = colors.accentTangerine, size = 19.2.dp)
        }
        Text(label, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
        RecentrToggle(checked = checked, onCheckedChange = null)
    }
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
 * Template card scaled to 62% (session "Suggestions" row, 102.3×130.2 in the design). The
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
            .size(width = 102.3.dp, height = 130.2.dp)
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
        Text(text, style = RecentrTheme.typography.headline, color = colors.textOnDark, maxLines = 1)
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

private val StrictGlow = Shadow(radius = 12.dp, color = Color(0x59FF661A), offset = DpOffset(0.dp, 10.dp))

/** Pill CTA with a custom fill: strict-mode gradient or danger red (not in the DS button styles). */
@Composable
internal fun RulesPillButton(
    text: String,
    onClick: () -> Unit,
    style: RulesPillStyle,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val brush: Brush = when (style) {
        RulesPillStyle.Strict -> com.recentr.core.designsystem.theme.RecentrGradients.strict
        RulesPillStyle.Danger -> SolidColor(colors.feedbackDanger)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (style == RulesPillStyle.Strict) Modifier.recentrShadow(StrictGlow, RecentrShapes.full) else Modifier)
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

internal enum class RulesPillStyle { Strict, Danger }

/** "− value +" stepper with round icon buttons (session length, wait time, daily budget). */
@Composable
internal fun ValueStepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 52.dp,
    buttonStyle: RecentrIconButtonStyle = RecentrIconButtonStyle.Sunken,
    decreaseEnabled: Boolean = true,
    increaseEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconButton(
            icon = RecentrIcon.Minus,
            contentDescription = stringResource(R.string.rules_decrease),
            onClick = onDecrease,
            style = buttonStyle,
            size = buttonSize,
            enabled = decreaseEnabled,
        )
        HeroValueText(value, Modifier.weight(1f))
        RecentrIconButton(
            icon = RecentrIcon.Plus,
            contentDescription = stringResource(R.string.rules_increase),
            onClick = onIncrease,
            style = buttonStyle,
            size = buttonSize,
            enabled = increaseEnabled,
        )
    }
}

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
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            RecentrChip(
                label = label,
                style = if (index == selectedIndex) RecentrChipStyle.Selected else RecentrChipStyle.Outline,
                onClick = { onSelect(index) },
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
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, style = RecentrTheme.typography.captionMedium, color = colors.feedbackDanger, maxLines = 1)
        Text(value, style = RecentrTheme.typography.displayLarge, color = colors.textPrimary, maxLines = 1)
        Text(caption, style = RecentrTheme.typography.captionRegular, color = colors.textTertiary, maxLines = 1)
    }
}

/** M T W T F S S day toggles (42dp discs). */
@Composable
internal fun DayDots(
    days: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val letters = androidx.compose.ui.platform.LocalContext.current.resources.getStringArray(R.array.rules_day_letters)
    val names = androidx.compose.ui.platform.LocalContext.current.resources.getStringArray(R.array.rules_day_long)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..7).forEach { day ->
            val selected = day in days
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RecentrShapes.full)
                    .background(if (selected) colors.actionPrimary else colors.bgSunken)
                    .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle(day) })
                    .semantics { contentDescription = names[day - 1] },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letters[day - 1],
                    style = RecentrTheme.typography.captionMedium,
                    color = if (selected) colors.textOnAction else colors.textSecondary,
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
