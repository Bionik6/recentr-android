package com.recentr.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow

/** Figma "Progress" — onboarding progress bar (220×6 by default). */
@Composable
fun OnboardingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    width: Dp = 220.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "onboardingProgress")
    Box(
        modifier
            .width(width)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(RecentrTheme.colors.bgSunken),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(3.dp))
                .background(RecentrTheme.colors.brandSky),
        )
    }
}

/** Figma "Onboarding Header" — back · progress · skip (56dp high, 24dp gutters). */
@Composable
fun OnboardingHeader(
    progress: Float,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (onBack != null) {
            RecentrIconButton(
                icon = RecentrIcon.ChevronLeft,
                contentDescription = stringResource(R.string.ds_back),
                onClick = onBack,
            )
        } else {
            Spacer(Modifier.size(40.dp))
        }
        OnboardingProgress(progress)
        if (onSkip != null) {
            Text(
                text = stringResource(R.string.ds_skip),
                style = RecentrTheme.typography.bodyMedium,
                color = RecentrTheme.colors.textTertiary,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(40.dp)
                    .clip(RecentrShapes.xs)
                    .clickable(role = Role.Button, onClick = onSkip),
            )
        }
    }
}

/** What [OptionRow] shows on its trailing edge. */
enum class OptionTrailing {
    /** A chevron, replaced by a sky check once selected. */
    Check,

    /** A checkbox: multi-select lists (onboarding "What's wrong"). */
    Checkbox,

    /** A radio button: single choice (onboarding "Time spent", Home period, Language). */
    Radio,

    None,
}

/** Figma "Option Row" — quiz / choice row with leading icon tile and a trailing [OptionTrailing]. */
@Composable
fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: RecentrIcon? = null,
    trailing: OptionTrailing = OptionTrailing.Check,
    /** [Role.Checkbox] for multi-select lists, [Role.RadioButton] for single choice. */
    role: Role = if (trailing == OptionTrailing.Radio) Role.RadioButton else Role.Checkbox,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    // Figma includes the stroke in the row size: 62dp by default, 64dp when selected.
    val border = if (selected) 2.dp else 1.dp
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.bgSurface)
            .border(border, if (selected) colors.brandSky else colors.borderSubtle, shape)
            .then(
                if (role == Role.Checkbox) {
                    Modifier.toggleable(value = selected, role = role, onValueChange = { onClick() })
                } else {
                    Modifier.selectable(selected = selected, role = role, onClick = onClick)
                },
            )
            .padding(start = 14.dp + border, end = 16.dp + border, top = 12.dp + border, bottom = 12.dp + border),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) colors.auroraBlue else colors.bgSunken),
                contentAlignment = Alignment.Center,
            ) {
                RecentrIconImage(icon, tint = if (selected) colors.brandSky else colors.textPrimary)
            }
        }
        Text(
            text = label,
            style = RecentrTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        // The row carries the click and semantics: the controls are drawn only (null callbacks).
        when (trailing) {
            OptionTrailing.Check -> RecentrIconImage(
                if (selected) RecentrIcon.Check else RecentrIcon.ChevronRight,
                tint = if (selected) colors.brandSky else colors.textTertiary,
            )
            OptionTrailing.Checkbox -> RecentrCheckbox(checked = selected, onCheckedChange = null)
            OptionTrailing.Radio -> RecentrRadioButton(selected = selected, onClick = null)
            OptionTrailing.None -> Unit
        }
    }
}

/**
 * Figma "Text Field" — 56dp single-line input. Empty = sunken + placeholder, Focused = white
 * with a 2dp sky outline and sky caret, Filled = sunken + clear button.
 */
@Composable
fun RecentrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = RecentrTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    // Text set from outside (pre-filled answers, clear button) puts the caret at the end.
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
        singleLine = true,
        textStyle = RecentrTheme.typography.bodyMedium.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.brandSky),
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.height(56.dp),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(shape)
                    .background(if (focused) colors.bgSurface else colors.bgSunken)
                    .then(if (focused) Modifier.border(2.dp, colors.brandSky, shape) else Modifier)
                    .padding(start = 18.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = RecentrTheme.typography.bodyMedium, color = colors.textTertiary, maxLines = 1)
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RecentrShapes.full)
                            .clickable(role = Role.Button) { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        RecentrIconImage(
                            RecentrIcon.X,
                            contentDescription = stringResource(R.string.ds_clear),
                            tint = colors.textTertiary,
                            size = 18.dp,
                        )
                    }
                }
            }
        },
    )
}

/** Figma "Review Card" — five stars, title, quote and author. */
@Composable
fun ReviewCard(
    title: String,
    body: String,
    name: String,
    initial: String,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.control, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { RecentrIconImage(RecentrIcon.StarFill, size = 16.8.dp, tint = colors.accentTangerine) }
        }
        Text(title, style = RecentrTheme.typography.headline, color = colors.textPrimary)
        Text(body, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(24.dp).clip(RecentrShapes.full).background(colors.auroraBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, style = RecentrTheme.typography.labelSmall, color = colors.brandSky)
            }
            Text(name, style = RecentrTheme.typography.captionMedium, color = colors.textPrimary)
        }
    }
}

/**
 * Figma "Alert" — neutral permission-style dialog (280dp). On Android it is used as the in-app
 * rationale shown before sending the user to the system permission screen.
 */
@Composable
fun RecentrAlertDialog(
    title: String,
    body: String,
    primaryText: String,
    onPrimary: () -> Unit,
    onDismissRequest: () -> Unit,
    secondaryText: String? = null,
    onSecondary: () -> Unit = onDismissRequest,
) {
    val colors = RecentrTheme.colors
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Figma dims with overlay/scrim (ink 36%) instead of the platform's darker default.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0.36f) }
        Column(
            Modifier
                .width(280.dp)
                .recentrShadow(RecentrElevation.float, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(colors.bgSurface.copy(alpha = 0.97f)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = RecentrTheme.typography.headline, color = colors.textPrimary, textAlign = TextAlign.Center)
                Text(body, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary, textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
            Row(Modifier.fillMaxWidth().height(50.dp)) {
                if (secondaryText != null) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button, onClick = onSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(secondaryText, style = RecentrTheme.typography.bodyRegular, color = colors.brandSky)
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors.borderSubtle))
                }
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button, onClick = onPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(primaryText, style = RecentrTheme.typography.headline, color = colors.brandSky)
                }
            }
        }
    }
}
