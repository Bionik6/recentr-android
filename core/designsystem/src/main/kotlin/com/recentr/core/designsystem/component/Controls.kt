package com.recentr.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.AppIcon
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrGradients
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

private val KnobShadow = Shadow(radius = 4.dp, color = Color(0x2E000000), offset = DpOffset(0.dp, 2.dp))

/** Figma "Toggle" — 51×31 switch (green when on). Semantics: [Role.Switch]. */
@Composable
fun RecentrToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = RecentrTheme.colors
    val track by animateColorAsState(if (checked) colors.feedbackSuccess else colors.fillGrabber, label = "toggleTrack")
    val knobOffset by animateDpAsState(if (checked) 22.dp else 2.dp, label = "toggleKnob")
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RecentrShapes.full)
            .background(track)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            Modifier
                .offset(x = knobOffset, y = 2.dp)
                .size(27.dp)
                .recentrShadow(KnobShadow, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** Figma "Checkbox" — 24dp, radius 8, sky when checked. */
@Composable
fun RecentrCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(shape)
            .background(if (checked) colors.brandSky else colors.bgSurface)
            .then(if (!checked) Modifier.border(1.5.dp, colors.borderDashed, shape) else Modifier)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) RecentrIconImage(RecentrIcon.Check, tint = Color.White, size = 18.dp)
    }
}

/** Leading 32dp icon tile shared by setting rows. */
@Composable
private fun SettingIcon(icon: RecentrIcon) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(RecentrTheme.colors.bgSunken),
        contentAlignment = Alignment.Center,
    ) {
        RecentrIconImage(icon, tint = RecentrTheme.colors.textPrimary, size = 19.2.dp)
    }
}

/** Figma "Setting Row" (Trailing = Value) — label, value and chevron. */
@Composable
fun SettingRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: RecentrIcon? = null,
    valueColor: Color = RecentrTheme.colors.textSecondary,
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
        if (icon != null) SettingIcon(icon)
        Text(label, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = RecentrTheme.typography.bodyRegular, color = valueColor, maxLines = 1)
        RecentrIconImage(RecentrIcon.ChevronRight, tint = colors.textTertiary, size = 19.2.dp)
    }
}

/** Figma "Setting Row" (Trailing = Toggle). */
@Composable
fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: RecentrIcon? = null,
) {
    val colors = RecentrTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bgSurface)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) SettingIcon(icon)
        Text(label, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
        RecentrToggle(checked = checked, onCheckedChange = null)
    }
}

/** Figma "Segmented" — N-option segmented control (3 in the design). */
@Composable
fun RecentrSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    Row(
        modifier = modifier
            .clip(RecentrShapes.full)
            .background(colors.bgSunken)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (selected) Modifier.recentrShadow(RecentrElevation.card, RecentrShapes.full) else Modifier)
                    .clip(RecentrShapes.full)
                    .background(if (selected) colors.bgSurface else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = RecentrTheme.typography.captionMedium,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                )
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, size = 32.dp, contentDescription = null)
        Text(name, style = RecentrTheme.typography.bodyMedium, color = RecentrTheme.colors.textPrimary, modifier = Modifier.weight(1f))
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
