package com.recentr.app.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recentr.app.R
import com.recentr.core.data.HoldReason
import com.recentr.core.data.RecentrData
import com.recentr.core.data.canOpenAnyway
import com.recentr.core.data.heldUntil
import com.recentr.core.data.unlocksLeft
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.format.wallClockTime
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * The hold screen of [packageName] (Figma 96 "App on hold": impatient mascot and the held app,
 * tilted): why it is on hold, "Close", and — when the rule allows it — "Open anyway" after its
 * unlock (wait, reason or retype). [onReleased] runs once the app may be used again.
 */
@Composable
internal fun HoldScreen(
    packageName: String,
    onClose: () -> Unit,
    onOpenRecentr: () -> Unit,
    onReleased: () -> Unit,
) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    val holds = RecentrData.holds
    val rules by RecentrData.rules.rules.collectAsStateWithLifecycle()
    val run by RecentrData.focus.run.collectAsStateWithLifecycle()
    val openUntil by holds.openUntil.collectAsStateWithLifecycle()
    val reason = remember(packageName, now, rules, run, openUntil) { holds.reason(packageName, now) }
    if (reason == null) {
        LaunchedEffect(Unit) { onReleased() }
        return
    }
    val label = remember(packageName) { RecentrData.installedApps.app(packageName)?.label ?: packageName }
    HoldCard(packageName) {
        when (reason) {
            is HoldReason.Focus -> {
                val focusRun = reason.run
                val endsAt = now + focusRun.remainingMillis(now)
                Heading(
                    title = stringResource(R.string.hold_focus_title, (focusRun.focusedMillis(now) / 60_000L).toInt()),
                    body = stringResource(R.string.hold_focus_body, label, wallClockTime(endsAt)),
                )
                RecentrButton(
                    text = stringResource(R.string.hold_back_to_focus),
                    onClick = {
                        RecentrData.focus.resistUrge()
                        onOpenRecentr()
                    },
                    style = RecentrButtonStyle.Brand,
                    modifier = Modifier.fillMaxWidth(),
                )
                RecentrButton(
                    text = stringResource(R.string.hold_end_focus),
                    onClick = { RecentrData.focus.end(System.currentTimeMillis()) },
                    style = RecentrButtonStyle.Soft,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is HoldReason.ByRule -> RuleHold(reason.rule, packageName, label, now, onClose, onOpenRecentr)
        }
    }
}

@Composable
private fun ColumnScope.RuleHold(
    rule: Rule,
    packageName: String,
    label: String,
    now: Long,
    onClose: () -> Unit,
    onOpenRecentr: () -> Unit,
) {
    val until = rule.heldUntil(now, ZoneId.systemDefault())
    Heading(
        title = stringResource(R.string.hold_rule_title, label),
        body = when {
            rule.type == RuleType.Limit -> stringResource(R.string.hold_limit_body, duration(rule.limitMinutes ?: 0), rule.name)
            rule.type == RuleType.Habit -> stringResource(R.string.hold_habit_body, rule.name)
            until != null -> stringResource(R.string.hold_rule_until, rule.name, wallClockTime(until))
            else -> stringResource(R.string.hold_rule_for_now, rule.name)
        },
    )
    rule.note?.takeIf { it.isNotBlank() }?.let { note ->
        Text(
            stringResource(R.string.hold_note, note),
            style = RecentrTheme.typography.captionRegular,
            color = RecentrTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
    RecentrButton(text = stringResource(R.string.hold_close, label), onClick = onClose, modifier = Modifier.fillMaxWidth())
    if (rule.type == RuleType.Habit) {
        RecentrButton(
            text = stringResource(R.string.hold_open_recentr),
            onClick = onOpenRecentr,
            style = RecentrButtonStyle.Soft,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (rule.canOpenAnyway(now)) {
        OpenAnyway(rule, label) { RecentrData.holds.openAnyway(rule, packageName, System.currentTimeMillis()) }
    }
}

/** "Open anyway", then the rule's unlock: wait the countdown, give a reason or retype a sentence. */
@Composable
private fun ColumnScope.OpenAnyway(rule: Rule, label: String, onOpen: () -> Unit) {
    var unlocking by rememberSaveable { mutableStateOf(false) }
    val openText = stringResource(R.string.hold_open_for, rule.unlockDurationMinutes)
    if (!unlocking) {
        RecentrButton(
            text = stringResource(R.string.hold_open_anyway),
            onClick = { unlocking = true },
            style = RecentrButtonStyle.Soft,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        when (rule.unlockMethod) {
            UnlockMethod.Reason -> {
                var reason by rememberSaveable { mutableStateOf("") }
                RecentrTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = stringResource(R.string.hold_reason_hint),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                RecentrButton(
                    text = openText,
                    onClick = onOpen,
                    style = RecentrButtonStyle.Soft,
                    enabled = reason.trim().length >= MIN_REASON_LENGTH,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            UnlockMethod.Retype -> {
                val sentence = stringResource(R.string.hold_retype_phrase, label)
                var typed by rememberSaveable { mutableStateOf("") }
                Text(
                    stringResource(R.string.hold_retype_prompt, sentence),
                    style = RecentrTheme.typography.captionMedium,
                    color = RecentrTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                RecentrTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    placeholder = stringResource(R.string.hold_retype_hint),
                    modifier = Modifier.fillMaxWidth(),
                )
                RecentrButton(
                    text = openText,
                    onClick = onOpen,
                    style = RecentrButtonStyle.Soft,
                    enabled = typed.trim().equals(sentence, ignoreCase = true),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                var left by rememberSaveable { mutableIntStateOf(rule.waitSeconds) }
                LaunchedEffect(Unit) {
                    while (left > 0) {
                        delay(1_000)
                        left--
                    }
                }
                RecentrButton(
                    text = if (left > 0) stringResource(R.string.hold_open_in, left) else openText,
                    onClick = onOpen,
                    style = RecentrButtonStyle.Soft,
                    enabled = left == 0,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    rule.unlocksLeft?.let { left ->
        Text(
            pluralStringResource(R.plurals.hold_unlocks_left, left, left),
            style = RecentrTheme.typography.captionRegular,
            color = RecentrTheme.colors.textTertiary,
        )
    }
}

/** The page and its card: mascot with the held app, then [content]. */
@Composable
private fun HoldCard(packageName: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(32.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bgPage)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 31.dp, vertical = 24.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .recentrShadow(RecentrElevation.card, shape)
                .clip(shape)
                .background(colors.bgSurface)
                .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(width = 160.dp, height = 110.dp)) {
                Mascot(MascotMood.Impatient, width = 88.dp, modifier = Modifier.offset(x = 20.dp))
                InstalledAppIcon(
                    packageName,
                    size = 52.dp,
                    modifier = Modifier
                        .offset(x = 95.dp, y = 56.dp)
                        .graphicsLayer { rotationZ = 10f },
                )
            }
            content()
        }
    }
}

@Composable
private fun ColumnScope.Heading(title: String, body: String) {
    Text(title, style = RecentrTheme.typography.title2, color = RecentrTheme.colors.textPrimary, textAlign = TextAlign.Center)
    Text(body, style = RecentrTheme.typography.bodyRegular, color = RecentrTheme.colors.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(4.dp))
}

/** "2h 15m", "45m", "2h". */
@Composable
private fun duration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.hold_duration_m, m)
        m == 0 -> stringResource(R.string.hold_duration_h, h)
        else -> stringResource(R.string.hold_duration_h_m, h, m)
    }
}

private const val MIN_REASON_LENGTH = 3
