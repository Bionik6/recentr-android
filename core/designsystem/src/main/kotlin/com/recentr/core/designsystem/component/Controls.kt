package com.recentr.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.AppIcon
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrGradients
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

/**
 * The native Android switch (Material 3 [Switch]) in the Figma "Toggle" colours: green track when
 * on, sunken outlined track when off. Pass a null [onCheckedChange] when the whole row toggles
 * (the row then carries the [Role.Switch] semantics).
 */
@Composable
fun RecentrToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = RecentrTheme.colors
    val haptics = LocalRecentrHaptics.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { change ->
            { value: Boolean ->
                haptics.toggle(value)
                change(value)
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.bgSurface,
            checkedTrackColor = colors.feedbackSuccess,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = colors.textTertiary,
            uncheckedTrackColor = colors.bgSunken,
            uncheckedBorderColor = colors.borderDashed,
            disabledCheckedThumbColor = colors.bgSurface,
            disabledCheckedTrackColor = colors.feedbackSuccess.copy(alpha = 0.45f),
            disabledCheckedBorderColor = Color.Transparent,
        ),
    )
}

/**
 * The native Android checkbox (Material 3 [Checkbox]) in the Figma "Checkbox" colours: sky with a
 * white check when checked, dashed-border outline otherwise. 24dp when [onCheckedChange] is null
 * (the whole row toggles and carries the [Role.Checkbox] semantics), 48dp touch target otherwise.
 */
@Composable
fun RecentrCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = RecentrTheme.colors
    val haptics = LocalRecentrHaptics.current
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { change ->
            { value: Boolean ->
                haptics.selection()
                change(value)
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = colors.brandSky,
            uncheckedColor = colors.borderDashed,
            checkmarkColor = Color.White,
        ),
    )
}

/**
 * The native Android radio button (Material 3 [RadioButton]) in the Figma "Radio" colours: sky ring
 * and dot when selected, tertiary ring otherwise. 24dp when [onClick] is null (the whole row is
 * selectable and carries the [Role.RadioButton] semantics), 48dp touch target otherwise.
 */
@Composable
fun RecentrRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = RecentrTheme.colors
    val haptics = LocalRecentrHaptics.current
    RadioButton(
        selected = selected,
        onClick = onClick?.let { click ->
            {
                haptics.selection()
                click()
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = RadioButtonDefaults.colors(
            selectedColor = colors.brandSky,
            unselectedColor = colors.textTertiary,
        ),
    )
}

/** Leading 32dp icon tile shared by setting rows. */
@Composable
private fun SettingIcon(icon: RecentrIcon, tint: Color, background: Color) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        RecentrIconImage(icon, tint = tint, size = 19.2.dp)
    }
}

/**
 * Figma "Setting Row" (Trailing = Value) — label, value and chevron. A null or empty [value] only
 * keeps the chevron. [iconTint] / [iconBackground] recolour the icon tile (tangerine "Strict
 * mode"), [labelColor] the label (danger "Cancel subscription").
 */
@Composable
fun SettingRow(
    label: String,
    value: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: RecentrIcon? = null,
    valueColor: Color = RecentrTheme.colors.textSecondary,
    iconTint: Color = RecentrTheme.colors.textPrimary,
    iconBackground: Color = RecentrTheme.colors.bgSunken,
    labelColor: Color = RecentrTheme.colors.textPrimary,
) {
    val colors = RecentrTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bgSurface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) SettingIcon(icon, iconTint, iconBackground)
        Text(label, style = RecentrTheme.typography.bodyMedium, color = labelColor, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!value.isNullOrEmpty()) {
            Text(value, style = RecentrTheme.typography.bodyRegular, color = valueColor, maxLines = 1)
        }
        RecentrIconImage(RecentrIcon.ChevronRight, tint = colors.textTertiary, size = 19.2.dp)
    }
}

/**
 * Figma "Setting Row" (Trailing = Toggle). [iconTint] / [iconBackground] recolour the icon tile
 * (tangerine "Strict mode"); a [caption] shows under the label; a disabled row keeps its state
 * but can't be switched.
 */
@Composable
fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: RecentrIcon? = null,
    iconTint: Color = RecentrTheme.colors.textPrimary,
    iconBackground: Color = RecentrTheme.colors.bgSunken,
    caption: String? = null,
    enabled: Boolean = true,
) {
    val colors = RecentrTheme.colors
    val haptics = LocalRecentrHaptics.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bgSurface)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) { value ->
                haptics.toggle(value)
                onCheckedChange(value)
            }
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) SettingIcon(icon, iconTint, iconBackground)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary)
            if (caption != null) {
                Text(caption, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
            }
        }
        RecentrToggle(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Figma settings "Group": sunken 24dp container stacking setting rows (4dp padding and gap). */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
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

/** Upper-case section caption above a settings group ("GENERAL", "THEME"). */
@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = RecentrTheme.typography.captionMedium,
        color = RecentrTheme.colors.textTertiary,
        modifier = modifier,
    )
}

/**
 * Figma "Segmented" — N-option segmented control (3 in the design). The white thumb slides to the
 * chosen option (the iOS `matchedGeometryEffect`, `.snappy(duration: 0.3)`), with a selection tick.
 */
@Composable
fun RecentrSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val haptics = LocalRecentrHaptics.current
    BoxWithConstraints(
        modifier = modifier
            .clip(RecentrShapes.full)
            .background(colors.bgSunken)
            .padding(4.dp),
    ) {
        val gap = 4.dp
        val count = options.size.coerceAtLeast(1)
        val segment = (maxWidth - gap * (count - 1)) / count
        val thumbOffset by animateDpAsState(
            targetValue = (segment + gap) * selectedIndex.coerceIn(0, count - 1),
            animationSpec = MotionSpecs.snappy(0.3f),
            label = "segmentedThumb",
        )
        Box(Modifier.matchParentSize()) {
            if (selectedIndex in options.indices) {
                Box(
                    Modifier
                        .offset(x = thumbOffset)
                        .width(segment)
                        .fillMaxHeight()
                        .recentrShadow(RecentrElevation.card, RecentrShapes.full)
                        .clip(RecentrShapes.full)
                        .background(colors.bgSurface),
                )
            }
        }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val labelColor by animateColorAsState(
                    if (selected) colors.textPrimary else colors.textSecondary,
                    MotionSpecs.smooth(0.3f),
                    label = "segmentedLabel",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RecentrShapes.full)
                        // The sliding thumb is the feedback: a ripple would grey it while it moves.
                        .selectable(selected = selected, interactionSource = null, indication = null, role = Role.Tab) {
                            if (!selected) haptics.selection()
                            onSelect(index)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = RecentrTheme.typography.captionMedium, color = labelColor, maxLines = 1)
                }
            }
        }
    }
}

/** Figma "App Row" — app picker row: icon, name, checkbox. */
@Composable
fun AppRow(
    app: DemoApp,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    name: String = app.displayName,
) {
    AppRow(name, checked, onCheckedChange, modifier) { AppIcon(app, size = 32.dp, contentDescription = null) }
}

/** [AppRow] with any 32dp [icon] (an installed app's launcher icon: [InstalledAppIcon]). */
@Composable
fun AppRow(
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val haptics = LocalRecentrHaptics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox) { value ->
                haptics.selection()
                onCheckedChange(value)
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            name,
            style = RecentrTheme.typography.bodyMedium,
            color = RecentrTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        RecentrCheckbox(checked = checked, onCheckedChange = null)
    }
}

/** Figma "Template Card" · Color. */
enum class TemplateColor(val brush: Brush) {
    Sky(RecentrGradients.templateSky),
    Night(RecentrGradients.templateNight),
    Mint(RecentrGradients.templateMint),
    Sunset(RecentrGradients.templateSunset),
}

/** Figma "Template Card" — gradient rule template tile (165×210 in the design). */
@Composable
fun TemplateCard(
    title: String,
    body: String,
    duration: String,
    sticker: Sticker,
    color: TemplateColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.float, RecentrShapes.card)
            .clip(RecentrShapes.card)
            .background(color.brush)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RecentrChip(label = duration, style = RecentrChipStyle.OnSky)
        Spacer(Modifier.weight(1f))
        StickerImage(sticker, size = 60.dp)
        Text(title, style = RecentrTheme.typography.title2, color = colors.textOnDark)
        Text(body, style = RecentrTheme.typography.captionRegular, color = colors.textOnDarkMuted)
    }
}
