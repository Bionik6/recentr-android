package com.recentr.app.blocking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recentr.app.R
import com.recentr.core.data.EmergencyUnlocks
import com.recentr.core.data.HoldReason
import com.recentr.core.data.RecentrData
import com.recentr.core.data.UnlockStep
import com.recentr.core.data.heldUntil
import com.recentr.core.data.unlockStep
import com.recentr.core.data.unlocksLeft
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.format.wallClockTime
import com.recentr.core.designsystem.haptics.SensoryFeedback
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.motion.floating
import com.recentr.core.designsystem.theme.RecentrGradients
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.AppSettings
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The hold screen of [packageName]: the shield (Figma 56 "App blocked shield") over an app a rule
 * or the focus session keeps closed, then — when the rule allows it — the unlock friction behind
 * "Unlock anyway" (48–54) and the Unlocked card (55). [onReleased] runs once the app may be used
 * again; "Open <app>" on the Unlocked card is that moment too.
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
    val unlocks by holds.unlocks.collectAsStateWithLifecycle()
    val settings by RecentrData.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val reason = remember(packageName, now, rules, run, unlocks) { holds.reason(packageName, now) }
    val label = remember(packageName) { RecentrData.installedApps.app(packageName)?.label ?: packageName }
    val zone = ZoneId.systemDefault()
    val emergencyLeft = EmergencyUnlocks.left(settings, now, zone)
    val scope = rememberCoroutineScope()

    var step by rememberSaveable(packageName) { mutableStateOf<UnlockStep?>(null) }
    /** On the scan step: typing the pass's code instead. */
    var typingPasscode by rememberSaveable(packageName) { mutableStateOf(false) }
    /** Until when the app was just unlocked, while the Unlocked card shows. */
    var unlockedUntil by rememberSaveable(packageName) { mutableStateOf<Long?>(null) }
    /** How long the app was unlocked for by walking: Walk complete shows instead of Unlocked. */
    var walkedMinutes by rememberSaveable(packageName) { mutableStateOf<Int?>(null) }
    /** An emergency unlock being spent: one at a time. */
    var spendingEmergency by remember { mutableStateOf(false) }

    val unlocked = unlockedUntil?.let { now < it } == true
    SensoryFeedback(unlocked) { if (unlocked) success() }

    if (unlocked) {
        val lockAgain = {
            holds.lockAgain(packageName, System.currentTimeMillis())
            unlockedUntil = null
            walkedMinutes = null
            onClose()
        }
        val walked = walkedMinutes
        if (walked != null) {
            WalkCompleteStep(packageName, label, minutes = walked, onOpen = onReleased, onLockAgain = lockAgain)
        } else {
            UnlockedStep(
                packageName = packageName,
                label = label,
                until = unlockedUntil ?: now,
                now = now,
                onOpen = onReleased,
                onLockAgain = lockAgain,
            )
        }
        return
    }
    if (reason == null) {
        LaunchedEffect(Unit) { onReleased() }
        return
    }
    val rule = (reason as? HoldReason.ByRule)?.rule
    val currentStep = step.takeIf { rule != null && it != null && rule.unlockStep(now, emergencyLeft) == it }
    BackHandler(enabled = currentStep != null) {
        if (typingPasscode) typingPasscode = false else step = null
    }

    fun unlock(through: Rule, minutes: Int, walked: Boolean = false) {
        val at = System.currentTimeMillis()
        holds.openAnyway(through, packageName, at, minutes)
        unlockedUntil = at + minutes * 60_000L
        walkedMinutes = minutes.takeIf { walked }
        step = null
        typingPasscode = false
    }

    if (rule == null || currentStep == null) {
        Shield(reason, packageName, label, now, emergencyLeft, onClose, onOpenRecentr, onUnlock = { step = it })
        return
    }
    when (currentStep) {
        UnlockStep.Wait -> WaitStep(rule, packageName, label, onNevermind = onClose, onUnlock = { minutes -> unlock(rule, minutes) })
        UnlockStep.Retype -> RetypeStep(rule, packageName, label, onUnlock = { unlock(rule, rule.unlockDurationMinutes) })
        UnlockStep.Scan -> if (typingPasscode) {
            PasscodeStep(rule, packageName, label, onUnlock = { unlock(rule, rule.unlockDurationMinutes) })
        } else {
            ScanStep(
                rule = rule,
                onClose = onClose,
                onUsePasscode = { typingPasscode = true },
                onUnlock = { unlock(rule, rule.unlockDurationMinutes) },
            )
        }
        UnlockStep.Walk -> WalkStep(
            rule = rule,
            packageName = packageName,
            label = label,
            onCancel = onClose,
            onUnlock = { unlock(rule, rule.unlockDurationMinutes, walked = true) },
        )
        UnlockStep.Emergency -> EmergencyStep(
            packageName = packageName,
            label = label,
            // A limit has no unlocks at all; Strict mode leaves none but these.
            limit = if (rule.type == RuleType.Limit) rule.name else null,
            unlocksLeft = emergencyLeft,
            onCancel = onClose,
            onUnlock = {
                if (!spendingEmergency) {
                    spendingEmergency = true
                    scope.launch {
                        val at = System.currentTimeMillis()
                        if (holds.emergencyUnlock(packageName, at)) {
                            unlockedUntil = at + EmergencyUnlocks.MINUTES * 60_000L
                            walkedMinutes = null
                            step = null
                        }
                        spendingEmergency = false
                    }
                }
            },
        )
    }
}

/** 56 · App blocked shield: why the app is closed, "I've got this", and the way past it, if any. */
@Composable
private fun Shield(
    reason: HoldReason,
    packageName: String,
    label: String,
    now: Long,
    emergencyLeft: Int,
    onClose: () -> Unit,
    onOpenRecentr: () -> Unit,
    onUnlock: (UnlockStep) -> Unit,
) {
    val title: String
    val subtitle: String
    val stat: String?
    val primary: String
    val onPrimary: () -> Unit
    val links = mutableListOf<Pair<String, () -> Unit>>()
    when (reason) {
        is HoldReason.Focus -> {
            val focusRun = reason.run
            val focused = (focusRun.focusedMillis(now) / 60_000L).toInt()
            title = stringResource(R.string.shield_on_hold, label)
            subtitle = stringResource(R.string.shield_until, focusRun.config.label, wallClockTime(now + focusRun.remainingMillis(now)))
            stat = if (focused < 1) stringResource(R.string.shield_focus_started) else stringResource(R.string.shield_focus_minutes, focused)
            primary = stringResource(R.string.shield_back_to_focus)
            onPrimary = {
                RecentrData.focus.resistUrge()
                onOpenRecentr()
            }
            // A strict session can't be ended early: it only runs out.
            if (!focusRun.isStrict) {
                links += stringResource(R.string.shield_end_focus) to { RecentrData.focus.end(System.currentTimeMillis()) }
            }
        }
        is HoldReason.ByRule -> {
            val rule = reason.rule
            val zone = ZoneId.systemDefault()
            title = stringResource(R.string.shield_resting, label)
            subtitle = ruleSubtitle(rule, now, zone)
            stat = RecentrData.holds.stayedOffSince(rule, packageName, now)
                ?.let { now - it }
                ?.takeIf { it >= 60_000L }
                ?.let { stringResource(R.string.shield_stayed_off, stayedOff(it)) }
            primary = stringResource(R.string.shield_got_this)
            onPrimary = onClose
            rule.unlockStep(now, emergencyLeft)?.let { step ->
                val text = when {
                    step == UnlockStep.Emergency -> stringResource(R.string.shield_emergency)
                    rule.unlocksPerDay != null ->
                        stringResource(R.string.shield_unlock_anyway_left, rule.unlocksLeft ?: 0, rule.unlocksPerDay ?: 0)
                    else -> stringResource(R.string.shield_unlock_anyway)
                }
                links += text to { onUnlock(step) }
            }
        }
    }

    val colors = RecentrTheme.colors
    SystemBarsAppearance(lightIcons = true)
    Box(Modifier.fillMaxSize().background(RecentrGradients.shieldScreen)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(126.dp))
                StickerImage(Sticker.Lock, size = 168.dp, modifier = Modifier.entrance(blur = false).floating())
                Spacer(Modifier.height(23.dp))
                Text(
                    title,
                    style = RecentrTheme.typography.title1,
                    color = colors.textOnDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.entrance(80),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = RecentrTheme.typography.bodyMedium,
                    color = colors.textOnDarkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.entrance(140),
                )
                if (stat != null) {
                    Spacer(Modifier.height(29.dp))
                    Row(
                        Modifier
                            .entrance(200)
                            .clip(RecentrShapes.full)
                            .background(colors.alphaWhite24)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RecentrIconImage(RecentrIcon.Flame, tint = colors.textOnDark)
                        Text(stat, style = RecentrTheme.typography.captionMedium, color = colors.textOnDark)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            RecentrButton(
                text = primary,
                onClick = onPrimary,
                style = RecentrButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth().entrance(260, blur = false),
            )
            links.forEach { (text, onClick) ->
                Text(
                    text,
                    style = RecentrTheme.typography.bodyMedium,
                    color = colors.textOnDarkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clip(RecentrShapes.full)
                        .clickable(role = Role.Button, onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(if (links.isEmpty()) 14.dp else 20.dp))
        }
    }
}

/** "Night Routine · until 7:00 AM", "Social time · today’s 1h used"… */
@Composable
private fun ruleSubtitle(rule: Rule, now: Long, zone: ZoneId): String {
    if (rule.type == RuleType.Limit) return stringResource(R.string.shield_limit, rule.name, duration(rule.limitMinutes ?: 0))
    val until = rule.heldUntil(now, zone) ?: return rule.name
    val sameDay = Instant.ofEpochMilli(until).atZone(zone).toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return stringResource(if (sameDay) R.string.shield_until else R.string.shield_until_tomorrow, rule.name, wallClockTime(until))
}

/** "2h 15m", "45m", "2h". */
@Composable
private fun duration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.duration_m, m)
        m == 0 -> stringResource(R.string.duration_h, h)
        else -> stringResource(R.string.duration_h_m, h, m)
    }
}

/** "6h15", "38m", "2h". */
@Composable
private fun stayedOff(millis: Long): String {
    val minutes = (millis / 60_000L).toInt()
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.duration_m, m)
        m == 0 -> stringResource(R.string.duration_h, h)
        else -> stringResource(R.string.stayed_off_h_m, h, m)
    }
}
