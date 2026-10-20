package com.recentr.app.blocking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.app.R
import com.recentr.core.data.EmergencyUnlocks
import com.recentr.core.data.Retype
import com.recentr.core.data.UnlockPass
import com.recentr.core.data.waitSecondsInUse
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.haptics.LocalRecentrHaptics
import com.recentr.core.designsystem.haptics.SensoryFeedback
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.MotionSpecs
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.motion.motionSpring
import com.recentr.core.designsystem.theme.CssLinearGradient
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrGradients
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.Rule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The unlock friction screens behind the shield's "Unlock anyway" (Figma › Unlock friction 48–55).
// They swap without a transition (navigation-style changes are the system's); only what's inside
// a screen animates.

/** 48 · Choose your unlock time: a countdown to sit through, then how long to open the app for. */
@Composable
internal fun WaitStep(rule: Rule, packageName: String, label: String, onNevermind: () -> Unit, onUnlock: (minutes: Int) -> Unit) {
    var secondsLeft by rememberSaveable { mutableIntStateOf(rule.waitSecondsInUse) }
    var minutes by rememberSaveable { mutableIntStateOf(rule.unlockDurationMinutes) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }
    val canUnlock = secondsLeft == 0
    SensoryFeedback(canUnlock) { if (canUnlock) success() }
    SensoryFeedback(minutes) { selection() }
    UnlockPage(
        actions = {
            DurationChips(selected = minutes, onSelect = { minutes = it })
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecentrButton(
                    text = stringResource(R.string.unlock_nevermind),
                    onClick = onNevermind,
                    style = RecentrButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                RecentrButton(
                    text = stringResource(R.string.unlock_unlock),
                    onClick = { onUnlock(minutes) },
                    style = if (canUnlock) RecentrButtonStyle.Primary else RecentrButtonStyle.Soft,
                    enabled = canUnlock,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Spacer(Modifier.height(50.dp))
        AppChip(packageName, stringResource(R.string.unlock_locked_by, label, rule.name))
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.unlock_wait_overline), style = RecentrTheme.typography.bodyMedium, color = RecentrTheme.colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Title(stringResource(R.string.unlock_wait_title))
        Spacer(Modifier.height(18.dp))
        Mascot(if (canUnlock) MascotMood.Calm else MascotMood.Impatient, width = 160.dp, modifier = Modifier.entrance(100))
        Spacer(Modifier.height(6.dp))
        CountdownCard(secondsLeft, Modifier.entrance(200, blur = false))
        Spacer(Modifier.height(8.dp))
    }
}

/** The tilted card that counts the wait down, then congratulates. */
@Composable
private fun CountdownCard(secondsLeft: Int, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val isDone = secondsLeft == 0
    val shape = RoundedCornerShape(44.dp)
    val glow by animateColorAsState(
        if (isDone) Color(0x5921C45E) else colors.brandSky.copy(alpha = 0.35f),
        MotionSpecs.smooth(0.4f),
        label = "countdownGlow",
    )
    Box(modifier.fillMaxWidth().height(230.dp)) {
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = (-7).dp, y = 10.dp)
                .rotate(4f)
                .size(width = 292.dp, height = 196.dp)
                .dropShadow(shape, Shadow(radius = 24.dp, color = glow, offset = DpOffset(0.dp, 10.dp)))
                .clip(shape)
                .background(if (isDone) SuccessCard else CountingCard)
                .innerShadow(shape, Shadow(radius = 8.dp, color = Color.White.copy(alpha = 0.45f), offset = DpOffset((-3).dp, 4.dp))),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = isDone,
                transitionSpec = {
                    (scaleIn(motionSpring(0.5f, 0.35f), initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
                },
                label = "countdown",
            ) { done ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (done) {
                        Box(Modifier.size(84.dp).clip(CircleShape).background(colors.alphaWhite24), contentAlignment = Alignment.Center) {
                            RecentrIconImage(RecentrIcon.Check, tint = colors.textOnDark, size = 52.8.dp)
                        }
                        Text(stringResource(R.string.unlock_wait_done), style = RecentrTheme.typography.bodyMedium, color = colors.textOnDarkMuted)
                    } else {
                        AnimatedContent(
                            targetState = secondsLeft,
                            transitionSpec = { (fadeIn() + scaleIn(initialScale = 1.2f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.8f)) },
                            label = "seconds",
                        ) { seconds ->
                            Text(seconds.toString(), style = RecentrTheme.typography.displayNumber, color = colors.textOnDark)
                        }
                        Text(stringResource(R.string.unlock_wait_breath), style = RecentrTheme.typography.bodyMedium, color = colors.textOnDarkMuted)
                    }
                }
            }
        }
        StickerImage(
            Sticker.Lock,
            size = 60.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-6).dp, y = (-13).dp)
                .rotate(-12f)
                .floating(amplitude = 4.dp),
        )
    }
}

/** 1, 2, 5, 10 or 15 minutes. */
@Composable
private fun DurationChips(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DurationPresets.forEach { minutes ->
            RecentrChip(
                label = stringResource(R.string.unlock_minutes_chip, minutes),
                style = if (minutes == selected) RecentrChipStyle.Selected else RecentrChipStyle.Outline,
                onClick = { onSelect(minutes) },
            )
        }
    }
}

/** 50 · Type this to unlock, and 51 · the typo that sends it back to the start. */
@Composable
internal fun RetypeStep(rule: Rule, packageName: String, label: String, onUnlock: () -> Unit) {
    val colors = RecentrTheme.colors
    val target = stringResource(R.string.unlock_retype_text)
    var retype by remember { mutableStateOf(Retype()) }
    var field by remember { mutableStateOf(TextFieldValue()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(retype.hasTypo) { if (!retype.hasTypo) focus.requestFocus() }
    SensoryFeedback(retype.hasTypo) { if (retype.hasTypo) error() }
    val isDone = retype.isDone(target)
    UnlockPage(
        actions = {
            RecentrButton(
                text = stringResource(R.string.unlock_unlock),
                onClick = onUnlock,
                style = if (isDone) RecentrButtonStyle.Primary else RecentrButtonStyle.Soft,
                enabled = isDone,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Spacer(Modifier.height(8.dp))
        AppChip(packageName, stringResource(R.string.unlock_locked_by, label, rule.name))
        Spacer(Modifier.height(12.dp))
        Title(stringResource(R.string.unlock_retype_title))
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.unlock_quote, target),
            style = RecentrTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.bgSunken)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        )
        Spacer(Modifier.height(36.dp))
        val fieldShape = RoundedCornerShape(22.dp)
        val fieldModifier = Modifier
            .fillMaxWidth()
            .clip(fieldShape)
            .background(colors.bgSurface)
            .border(2.dp, if (retype.hasTypo) colors.feedbackDanger else colors.brandSky, fieldShape)
            .padding(horizontal = 18.dp, vertical = 16.dp)
        if (retype.hasTypo) {
            val matching = retype.matching(target)
            Text(
                buildAnnotatedString {
                    append(retype.typed.take(matching))
                    withStyle(SpanStyle(color = colors.feedbackDanger)) { append(retype.typed.drop(matching)) }
                },
                style = RecentrTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
                modifier = fieldModifier,
            )
        } else {
            BasicTextField(
                value = field,
                onValueChange = {
                    field = it
                    retype = retype.typed(it.text, target)
                },
                singleLine = true,
                textStyle = RecentrTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.brandSky),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                ),
                modifier = fieldModifier.focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (field.text.isEmpty()) {
                            Text(stringResource(R.string.unlock_retype_hint), style = RecentrTheme.typography.bodyMedium, color = colors.textTertiary)
                        }
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (retype.hasTypo) {
                Text(
                    stringResource(R.string.unlock_retype_typo),
                    style = RecentrTheme.typography.captionMedium,
                    color = colors.feedbackDanger,
                    modifier = Modifier.weight(1f),
                )
                val startOver = stringResource(R.string.unlock_retype_start_over)
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) {
                            retype = retype.startOver()
                            field = TextFieldValue()
                        }
                        .semantics { contentDescription = startOver },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    RecentrIconImage(RecentrIcon.Refresh, tint = colors.feedbackDanger, size = 18.dp)
                }
            } else {
                Text(
                    stringResource(R.string.unlock_retype_count, retype.typed.length, target.length),
                    style = RecentrTheme.typography.captionMedium,
                    color = colors.textTertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The pass's four digits, typed instead of scanning it. */
@Composable
internal fun PasscodeStep(rule: Rule, packageName: String, label: String, onUnlock: () -> Unit) {
    val colors = RecentrTheme.colors
    var code by rememberSaveable { mutableStateOf("") }
    var isWrong by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    SensoryFeedback(isWrong) { if (isWrong) error() }
    val canUnlock = code.count(Char::isDigit) >= 4
    UnlockPage(
        actions = {
            RecentrButton(
                text = stringResource(R.string.unlock_unlock),
                onClick = { if (UnlockPass.opens(rule, code)) onUnlock() else isWrong = true },
                style = if (canUnlock) RecentrButtonStyle.Primary else RecentrButtonStyle.Soft,
                enabled = canUnlock,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Spacer(Modifier.height(8.dp))
        AppChip(packageName, stringResource(R.string.unlock_locked_by, label, rule.name))
        Spacer(Modifier.height(12.dp))
        Title(stringResource(R.string.unlock_passcode_title))
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.unlock_passcode_subtitle),
            style = RecentrTheme.typography.bodyRegular,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        val shape = RoundedCornerShape(22.dp)
        BasicTextField(
            value = code,
            onValueChange = { value ->
                code = value.filter { it.isDigit() || it == '#' }.take(5)
                isWrong = false
            },
            singleLine = true,
            textStyle = RecentrTheme.typography.displayLarge.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(colors.brandSky),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(shape)
                .background(colors.bgSurface)
                .border(2.dp, if (isWrong) colors.feedbackDanger else colors.brandSky, shape)
                .focusRequester(focus),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (code.isEmpty()) {
                        Text(
                            stringResource(R.string.unlock_passcode_hint),
                            style = RecentrTheme.typography.displayLarge,
                            color = colors.textTertiary,
                        )
                    }
                    inner()
                }
            },
        )
        if (isWrong) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.unlock_passcode_wrong), style = RecentrTheme.typography.captionMedium, color = colors.feedbackDanger)
        }
    }
}

/**
 * 54 · Emergency unlock: hold to spend one of the month's three, past Strict mode or past a
 * [limit] (its name), which has no other unlocks.
 */
@Composable
internal fun EmergencyStep(
    packageName: String,
    label: String,
    limit: String?,
    unlocksLeft: Int,
    onCancel: () -> Unit,
    onUnlock: () -> Unit,
) {
    val colors = RecentrTheme.colors
    UnlockPage(
        actions = {
            HoldToUnlock(enabled = unlocksLeft > 0, onComplete = onUnlock)
            Spacer(Modifier.height(12.dp))
            RecentrButton(
                text = stringResource(R.string.unlock_cancel),
                onClick = onCancel,
                style = RecentrButtonStyle.Soft,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            )
        },
    ) {
        Spacer(Modifier.height(8.dp))
        AppChip(packageName, if (limit != null) stringResource(R.string.unlock_locked_by, label, limit) else stringResource(R.string.unlock_strict_on, label))
        Spacer(Modifier.height(40.dp))
        Mascot(MascotMood.Impatient, width = 160.dp, modifier = Modifier.entrance(100))
        Spacer(Modifier.height(32.dp))
        Title(stringResource(R.string.unlock_emergency_title))
        Spacer(Modifier.height(16.dp))
        Text(
            when {
                limit != null && unlocksLeft > 0 -> stringResource(R.string.unlock_emergency_limit_body, unlocksLeft)
                limit != null -> stringResource(R.string.unlock_emergency_limit_spent)
                unlocksLeft > 0 -> stringResource(R.string.unlock_emergency_body, unlocksLeft)
                else -> stringResource(R.string.unlock_emergency_spent)
            },
            style = RecentrTheme.typography.bodyRegular,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(38.dp))
        val tokensLabel = pluralStringResource(R.plurals.unlock_emergency_left, unlocksLeft, unlocksLeft)
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = tokensLabel },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(EmergencyUnlocks.PER_MONTH) { index ->
                val isLeft = index < unlocksLeft
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(if (isLeft) colors.alphaTangerine14 else colors.bgSunken),
                    contentAlignment = Alignment.Center,
                ) {
                    RecentrIconImage(RecentrIcon.Shield, tint = if (isLeft) colors.accentTangerine else colors.textTertiary, size = 18.dp)
                }
            }
        }
    }
}

/** Hold to unlock: a bar that fills while pressed, and empties if let go too soon. */
@Composable
private fun HoldToUnlock(enabled: Boolean, onComplete: () -> Unit) {
    val colors = RecentrTheme.colors
    val haptics = LocalRecentrHaptics.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var pressing by remember { mutableStateOf(false) }
    /** Held to the end once: later presses do nothing. */
    var completed by remember { mutableStateOf(false) }
    val seconds = (EmergencyUnlocks.HOLD_MILLIS / 1_000L).toInt()
    val secondsLeft = if (pressing) maxOf(1, kotlin.math.ceil(seconds * (1f - progress.value)).toInt()) else seconds
    val text = if (enabled) stringResource(R.string.unlock_hold, secondsLeft) else stringResource(R.string.unlock_hold_none)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RecentrShapes.full)
            .background(colors.alphaTangerine14)
            .semantics {
                role = Role.Button
                if (enabled) {
                    onClick {
                        onComplete()
                        true
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        if (completed) return@detectTapGestures
                        pressing = true
                        haptics.mediumImpact()
                        val filling = scope.launch {
                            val left = (EmergencyUnlocks.HOLD_MILLIS * (1f - progress.value)).toInt()
                            progress.animateTo(1f, tween(left, easing = LinearEasing))
                            pressing = false
                            completed = true
                            onComplete()
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            // Let go too soon, or the gesture was cancelled: empty the bar again.
                            if (progress.value < 1f) {
                                filling.cancel()
                                pressing = false
                                haptics.mediumImpact()
                                scope.launch { progress.animateTo(0f, motionSpring(0.4f)) }
                            }
                        }
                    },
                )
            },
    ) {
        val fill = progress.value
        Box(
            Modifier
                .fillMaxHeight()
                .width(maxOf(64.dp, maxWidth * fill))
                .alpha(if (fill > 0f) 1f else 0f)
                .background(RecentrGradients.strict),
        )
        Text(
            text,
            style = RecentrTheme.typography.headline,
            color = if (enabled) colors.textPrimary else colors.textTertiary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** 55 · Unlocked: the app is open until the timer ends, or until "Lock it again". */
@Composable
internal fun UnlockedStep(packageName: String, label: String, until: Long, now: Long, onOpen: () -> Unit, onLockAgain: () -> Unit) {
    val colors = RecentrTheme.colors
    val secondsLeft = ((until - now + 999) / 1_000L).coerceAtLeast(0)
    val countdown = "%d:%02d".format(secondsLeft / 60, secondsLeft % 60)
    UnlockPage(
        actions = {
            RecentrButton(
                text = stringResource(R.string.unlock_open_app, label),
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            RecentrButton(
                text = stringResource(R.string.unlock_lock_again),
                onClick = onLockAgain,
                style = RecentrButtonStyle.Soft,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Spacer(Modifier.height(96.dp))
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            val shape = RoundedCornerShape(44.dp)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 6.dp, y = 2.dp)
                    .entrance(from = DpOffset(0.dp, 40.dp), blur = false)
                    .rotate(-3f)
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
                    .recentrShadow(RecentrElevation.float, shape)
                    .clip(shape)
                    .background(colors.bgSurface)
                    .padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InstalledAppIcon(packageName, size = 64.dp)
                Text(
                    stringResource(R.string.unlock_open_title, label),
                    style = RecentrTheme.typography.title1,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                RecentrChip(stringResource(R.string.unlock_time_left, countdown), style = RecentrChipStyle.Success, icon = RecentrIcon.Clock)
                Text(
                    stringResource(R.string.unlock_open_caption),
                    style = RecentrTheme.typography.captionRegular,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            StickerImage(
                Sticker.Lock,
                size = 60.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = (-28).dp)
                    .entrance(150, blur = false)
                    .rotate(-18f)
                    .floating(amplitude = 4.dp),
            )
            Mascot(
                MascotMood.Calm,
                width = 114.4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 239.dp)
                    .entrance(200),
            )
        }
    }
}

/** "YouTube · locked by Night Routine": the app being unlocked, and what keeps it closed. */
@Composable
private fun AppChip(packageName: String, text: String) {
    Row(
        Modifier
            .recentrShadow(RecentrElevation.card, RecentrShapes.full)
            .clip(RecentrShapes.full)
            .background(RecentrTheme.colors.bgSurface)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstalledAppIcon(packageName, size = 25.6.dp)
        Text(
            text,
            style = RecentrTheme.typography.captionMedium,
            color = RecentrTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = RecentrTheme.typography.title1, color = RecentrTheme.colors.textPrimary, textAlign = TextAlign.Center)
}

/**
 * The Aurora page of the unlock steps: [content] scrolls under the status bar, [actions] stay at
 * the bottom, above the keyboard when it's open.
 */
@Composable
private fun UnlockPage(actions: @Composable ColumnScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    SystemBarsAppearance(lightIcons = false)
    BackdropBox(Backdrop.Aurora) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
            Spacer(Modifier.height(16.dp))
            actions()
            Spacer(Modifier.height(14.dp))
        }
    }
}

private val DurationPresets = listOf(1, 2, 5, 10, 15)

/** The countdown card's sky, then its success green (CSS 153° over the 292 × 196 card). */
private val CountingCard = CssLinearGradient(153.28f, listOf(0f to Color(0xFF7CB8FF), 0.71429f to Color(0xFF1452D4)))
private val SuccessCard = CssLinearGradient(153.28f, listOf(0f to Color(0xFF78E59E), 0.71429f to Color(0xFF17A859)))
