package com.recentr.feature.focus.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.recentr.core.designsystem.component.BlockingOffBanner
import com.recentr.core.designsystem.component.OverlappingInstalledAppIcons
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrToggle
import com.recentr.core.designsystem.format.countdown
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.FocusConfig
import com.recentr.core.model.FocusRun
import com.recentr.core.model.UsageApp
import com.recentr.feature.focus.R
import com.recentr.feature.focus.ui.CardDivider
import com.recentr.feature.focus.ui.FocusCard
import com.recentr.feature.focus.ui.FocusDial
import com.recentr.feature.focus.ui.FocusMenu
import com.recentr.feature.focus.ui.RowLabel
import com.recentr.feature.focus.ui.mixTitle
import kotlinx.coroutines.delay

private val Presets = listOf(25, 45, 60, 90)

private enum class SetupSheet { Sounds, Block, Rounds, Rename }

/**
 * 91 · Focus — Set up, as the Timer tab: dial, presets, sound / block / rounds rows and "Start
 * focus" right above the floating tab bar ([contentPadding] bottom). While a session runs ([run])
 * the CTA goes back to it with the time left.
 */
@Composable
internal fun FocusSetupScreen(
    config: FocusConfig,
    run: FocusRun?,
    contentPadding: PaddingValues,
    onUpdate: ((FocusConfig) -> FocusConfig) -> Unit,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onOpenLog: () -> Unit,
    modifier: Modifier = Modifier,
    /** Whether app blocking (the accessibility service) is on: blocked apps need it. */
    blockingOn: Boolean = true,
    onTurnOnBlocking: () -> Unit = {},
    /** The rules' "Choose apps" ([initial] picked), for the apps on hold: the apps picked, or dismissed. */
    chooseApps: @Composable (initial: List<UsageApp>, onDone: (List<UsageApp>) -> Unit, onDismiss: () -> Unit) -> Unit = { _, _, _ -> },
) {
    val haptics = LocalRecentrHaptics.current
    SystemBarsAppearance(lightIcons = false)
    val colors = RecentrTheme.colors
    var sheet by rememberSaveable { mutableStateOf<SetupSheet?>(null) }
    var menu by rememberSaveable { mutableStateOf(false) }
    var confirmStrict by rememberSaveable { mutableStateOf(false) }
    val bottomInset = contentPadding.calculateBottomPadding()
    BackdropBox(Backdrop.Aurora, modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RecentrTheme.spacing.screenGutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.focus_title),
                        style = RecentrTheme.typography.displayLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    Box {
                        RecentrIconButton(icon = RecentrIcon.More, contentDescription = stringResource(R.string.focus_more), onClick = { menu = true })
                        FocusMenu(
                            expanded = menu,
                            onDismissRequest = { menu = false },
                            items = listOf(
                                stringResource(R.string.focus_rename) to { sheet = SetupSheet.Rename },
                                stringResource(R.string.focus_log) to onOpenLog,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(44.dp))
                Box(contentAlignment = Alignment.Center) {
                    FocusDial(
                        minutes = config.focusMinutes,
                        onMinutesChange = { minutes -> onUpdate { it.copy(focusMinutes = minutes) } },
                        contentDescription = stringResource(R.string.focus_dial_description, config.focusMinutes),
                        min = FocusConfig.MIN_FOCUS,
                        max = FocusConfig.MAX_FOCUS,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // The minutes ("180:00"), inside the ring: a long round shrinks to fit.
                        BasicText(
                            stringResource(R.string.focus_dial_value, config.focusMinutes),
                            style = RecentrTheme.typography.displayHero.copy(
                                color = colors.textPrimary,
                                fontSize = 60.sp,
                                lineHeight = 60.sp,
                                letterSpacing = (-0.03).em,
                                fontFeatureSettings = "tnum",
                            ),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 30.sp, maxFontSize = 60.sp, stepSize = 1.sp),
                            modifier = Modifier.widthIn(max = 182.dp),
                        )
                        Text(
                            config.label,
                            style = RecentrTheme.typography.captionMedium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(role = Role.Button) { sheet = SetupSheet.Rename },
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Presets.forEach { minutes ->
                        RecentrChip(
                            label = stringResource(R.string.focus_minutes_preset, minutes),
                            style = if (config.focusMinutes == minutes) RecentrChipStyle.Selected else RecentrChipStyle.Outline,
                            onClick = {
                                if (config.focusMinutes != minutes) haptics.selection()
                                onUpdate { it.copy(focusMinutes = minutes) }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                FocusCard {
                    SetupRow(Sticker.Headphones, stringResource(R.string.focus_sound), onClick = { sheet = SetupSheet.Sounds }) {
                        Text(mixTitle(config.sounds), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary, maxLines = 1)
                    }
                    CardDivider()
                    SetupRow(Sticker.Lock, stringResource(R.string.focus_block), onClick = { sheet = SetupSheet.Block }) {
                        if (config.blockedApps.isEmpty()) {
                            Text(stringResource(R.string.focus_block_none), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
                        } else {
                            OverlappingInstalledAppIcons(config.blockedApps.take(4).map { it.packageName }, size = 24.dp, overlap = 6.dp)
                            if (config.blockedApps.size > 4) {
                                Text(
                                    stringResource(R.string.focus_block_more, config.blockedApps.size - 4),
                                    style = RecentrTheme.typography.captionMedium,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                    CardDivider()
                    SetupRow(Sticker.Timer, stringResource(R.string.focus_rounds), onClick = { sheet = SetupSheet.Rounds }) {
                        Text(
                            stringResource(R.string.focus_rounds_value, config.rounds, config.focusMinutes),
                            style = RecentrTheme.typography.bodyRegular,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    CardDivider()
                    // The session going on's, or the next one's. Turning it on asks first.
                    StrictModeRow(
                        checked = run?.isStrict ?: config.strictMode,
                        enabled = run == null,
                        onCheckedChange = { on ->
                            if (on) confirmStrict = true else onUpdate { it.copy(strictMode = false) }
                        },
                    )
                }
                // Under the form, what Strict mode will hold.
                if (run == null && config.strictMode) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.focus_strict_caption),
                        style = RecentrTheme.typography.captionMedium,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
                if (config.blockedApps.isNotEmpty() && !blockingOn) {
                    Spacer(Modifier.height(12.dp))
                    BlockingOffBanner(onClick = onTurnOnBlocking)
                }
                // Room for the CTA (8 + 54) and a breathing gap above it, like the Rules tab.
                Spacer(Modifier.height(bottomInset + 8.dp + 54.dp + 28.dp))
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomInset + 96.dp)
                .background(
                    Brush.verticalGradient(
                        0f to colors.bgPage.copy(alpha = 0f),
                        0.45f to colors.bgPage.copy(alpha = 0.92f),
                        1f to colors.bgPage,
                    ),
                ),
        )
        StartButton(
            run = run,
            onStart = onStart,
            onResume = onResume,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = bottomInset + 8.dp)
                .fillMaxWidth(),
        )
    }
    if (confirmStrict) {
        RecentrAlertDialog(
            title = stringResource(R.string.focus_strict_confirm_title),
            body = stringResource(R.string.focus_strict_confirm_body),
            primaryText = stringResource(R.string.focus_strict_turn_on),
            onPrimary = {
                confirmStrict = false
                onUpdate { it.copy(strictMode = true) }
            },
            onDismissRequest = { confirmStrict = false },
            secondaryText = stringResource(R.string.focus_cancel),
        )
    }
    when (sheet) {
        SetupSheet.Sounds -> SoundsSheet(
            mix = config.sounds,
            muted = config.mutedSounds,
            onChange = { mix, muted -> onUpdate { it.copy(sounds = mix, mutedSounds = muted) } },
            onDismissRequest = { sheet = null },
        )
        SetupSheet.Block -> chooseApps(
            config.blockedApps,
            { apps ->
                onUpdate { it.copy(blockedApps = apps) }
                sheet = null
            },
            { sheet = null },
        )
        SetupSheet.Rounds -> RoundsSheet(config = config, onSave = { saved -> onUpdate { saved } }, onDismissRequest = { sheet = null })
        SetupSheet.Rename -> RenameSheet(label = config.label, onSave = { label -> onUpdate { it.copy(label = label) } }, onDismissRequest = { sheet = null })
        null -> Unit
    }
}

/** "Start focus", or "Back to focus · 24:18" (ticking) while a session runs. */
@Composable
private fun StartButton(run: FocusRun?, onStart: () -> Unit, onResume: () -> Unit, modifier: Modifier = Modifier) {
    if (run == null) {
        RecentrButton(text = stringResource(R.string.focus_start), onClick = onStart, style = RecentrButtonStyle.Brand, modifier = modifier)
        return
    }
    val now by produceState(System.currentTimeMillis(), run) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val left = run.remainingMillis(now)
    RecentrButton(
        text = if (left > 0) stringResource(R.string.focus_back_to_focus_left, countdown(left)) else stringResource(R.string.focus_back_to_focus),
        onClick = onResume,
        style = RecentrButtonStyle.Brand,
        icon = RecentrIcon.Hourglass,
        modifier = modifier,
    )
}

/** "Strict mode" row of the set-up card: shield sticker, label and its switch (the row toggles). */
@Composable
private fun StrictModeRow(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(Sticker.Shield, size = 32.dp)
        RowLabel(stringResource(R.string.focus_strict_mode))
        RecentrToggle(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Row of the set-up card: 32dp sticker, label, trailing value and an 18dp chevron. */
@Composable
private fun SetupRow(sticker: Sticker, label: String, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(sticker, size = 32.dp)
        RowLabel(label)
        trailing()
        RecentrIconImage(RecentrIcon.ChevronRight, tint = RecentrTheme.colors.textPrimary, size = 18.dp)
    }
}
