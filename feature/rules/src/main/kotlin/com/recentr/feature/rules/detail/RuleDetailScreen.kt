package com.recentr.feature.rules.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recentr.core.data.BlockingAccess
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.component.BlockingAccessSheet
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.OverlappingInstalledAppIcons
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrTextButton
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.format.clockHours
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrElevation
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.theme.recentrShadow
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.AppId
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleChange
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.core.model.latestWish
import com.recentr.feature.rules.R
import com.recentr.feature.rules.sheets.BlockRightAwayDialog
import com.recentr.feature.rules.sheets.RuleSheet
import com.recentr.feature.rules.sheets.RuleSheetsHost
import com.recentr.feature.rules.sheets.StartConfirmDialog
import com.recentr.feature.rules.sheets.StrictConfirmDialog
import com.recentr.feature.rules.ui.PendingKind
import com.recentr.feature.rules.ui.asSticker
import com.recentr.feature.rules.ui.clockLabel
import com.recentr.feature.rules.ui.compactDuration
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesToastHost
import com.recentr.feature.rules.ui.components.SettingsGroup
import com.recentr.feature.rules.ui.components.StrictToggleRow
import com.recentr.feature.rules.ui.components.TimeCard
import com.recentr.feature.rules.ui.components.UnlockMethodRow
import com.recentr.feature.rules.ui.countdown
import com.recentr.feature.rules.ui.daysLabel
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.label
import com.recentr.feature.rules.ui.pendingKind
import com.recentr.feature.rules.ui.pendingWhenText
import com.recentr.feature.rules.ui.rememberWallClock
import com.recentr.feature.rules.ui.reminderLabel
import com.recentr.feature.rules.ui.unlocksPerDayLabel
import com.recentr.feature.rules.ui.wallClock

/**
 * Rule detail destination (Figma 44) with its Pause (45), Edit (46) and Delete (47) sheets. Every
 * change goes through the edit policy: while the rule is engaged, what loosens it waits for the end
 * of the engagement (the page says so), and what blocks more apps right away asks first.
 */
@Composable
internal fun RuleDetailDestination(
    onBack: () -> Unit,
    onEditSettings: (String) -> Unit,
    viewModel: RuleDetailViewModel = viewModel(factory = RuleDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rule = state.rule
    LaunchedEffect(state.loaded, rule == null) {
        if (state.loaded && rule == null) onBack()
    }
    if (rule == null) {
        BackdropBox(Backdrop.Aurora)
        return
    }
    var sheet by rememberSaveable { mutableStateOf(RuleSheet.None) }
    var pause by rememberSaveable { mutableStateOf(false) }
    var delete by rememberSaveable { mutableStateOf(false) }
    var showApps by rememberSaveable { mutableStateOf(false) }
    var confirmStrict by rememberSaveable { mutableStateOf(false) }
    var explainBlocking by rememberSaveable { mutableStateOf(false) }
    /** Activate (a lock) or Start (a limit), waiting for "Turn on" / "Start". */
    var confirmStart by rememberSaveable { mutableStateOf(false) }
    /** A change that would block more apps right away, waiting for "Block". */
    var confirmBlock by remember { mutableStateOf<((Rule) -> Rule)?>(null) }
    val blockingOn by RecentrData.blockingEnabled.collectAsStateWithLifecycle()
    val clock = rememberWallClock()
    fun save(transform: (Rule) -> Rule) {
        if (viewModel.preview(transform)?.blocksMore == true) confirmBlock = transform else viewModel.update(clock, transform)
    }
    fun startOrActivate() {
        viewModel.start()
        // Started, but nothing is held until the app blocker is on: say how.
        if (!blockingOn) explainBlocking = true
    }
    /**
     * Starting a lock or a limit again asks first: once it's on, what makes it easier only applies
     * at midnight. A session starts right away.
     */
    fun askThenStart() {
        if (rule.type.asksBeforeStart) confirmStart = true else startOrActivate()
    }

    val allowed by RecentrData.alwaysAllowed.allowed.collectAsStateWithLifecycle()
    RuleDetailScreen(
        rule = rule,
        now = state.now,
        blockedApps = allowed.blocked(rule.apps, state.now),
        actions = DetailActions(
            onBack = onBack,
            onOpen = { target -> sheet = target },
            onStrictChange = { on ->
                when {
                    !on -> save { it.copy(strictMode = false) }
                    // Only emergency unlocks from now until it ends: ask first.
                    rule.isEnforcing(state.now) -> confirmStrict = true
                    else -> save { it.copy(strictMode = true) }
                }
            },
            onPause = { if (rule.isPaused(state.now)) viewModel.resume() else pause = true },
            onEditRule = { onEditSettings(rule.id) },
            onStart = ::askThenStart,
            onTurnOff = viewModel::turnOff,
            onEditSettings = { onEditSettings(rule.id) },
            onDelete = { delete = true },
            onDropPending = viewModel::dropPending,
            onOpenApps = { showApps = true },
        ),
    )
    RuleSheetsHost(
        sheet = sheet,
        // The sheets open on the rule as last wished, a change kept for later included.
        rule = rule.latestWish,
        now = state.now,
        onSheet = { sheet = it },
        onUpdate = ::save,
        onRename = { name, sticker -> viewModel.rename(clock, name, sticker) },
        onApps = { apps -> save { it.copy(apps = apps) } },
    )
    if (showApps) {
        // The app blocker's unlocks, live: an app whose unlock ends goes back with the blocked ones.
        val unlocks by RecentrData.holds.unlocks.collectAsStateWithLifecycle()
        RuleAppsSheet(rule = rule, now = state.now, unlocks = unlocks, alwaysAllowed = allowed, onDismiss = { showApps = false })
    }
    if (pause) {
        PauseRuleSheet(
            ruleName = rule.name,
            later = rule.isEnforcing(state.now),
            onPause = viewModel::pause,
            onDismiss = { pause = false },
        )
    }
    if (confirmStart) {
        StartConfirmDialog(
            rule = rule,
            onConfirm = {
                confirmStart = false
                startOrActivate()
            },
            onDismiss = { confirmStart = false },
        )
    }
    if (confirmStrict) {
        StrictConfirmDialog(
            onConfirm = {
                save { it.copy(strictMode = true) }
                confirmStrict = false
            },
            onDismiss = { confirmStrict = false },
        )
    }
    confirmBlock?.let { transform ->
        BlockRightAwayDialog(
            onConfirm = {
                viewModel.update(clock, transform)
                confirmBlock = null
            },
            onDismiss = { confirmBlock = null },
        )
    }
    if (explainBlocking) {
        val context = LocalContext.current
        BlockingAccessSheet(onOpenSettings = { BlockingAccess.openSettings(context) }, onDismissRequest = { explainBlocking = false })
    }
    if (delete) {
        val end = rule.engagementEnd(state.now)
        DeleteRuleSheet(
            ruleName = rule.name,
            streakDays = rule.streakDays,
            later = end?.let {
                pendingWhenText(
                    rule.type, RuleChange(it, null),
                    midnight = R.string.rules_delete_body_later_midnight,
                    sessionEnd = R.string.rules_delete_body_later_session,
                    at = R.string.rules_delete_body_later_at,
                )
            },
            onDelete = { viewModel.delete(clock) },
            onDismiss = { delete = false },
        )
    }
}

/** Activate (a lock) and Start (a limit) ask first ([StartConfirmDialog]); a session starts right away. */
internal val RuleType.asksBeforeStart: Boolean
    get() = this == RuleType.Lock || this == RuleType.Limit

/** A saved session not running: it can be started. */
internal fun Rule.startable(now: Long): Boolean = type == RuleType.Session && !isRunning(now)

internal class DetailActions(
    val onBack: () -> Unit = {},
    val onOpen: (RuleSheet) -> Unit = {},
    val onStrictChange: (Boolean) -> Unit = {},
    val onPause: () -> Unit = {},
    val onEditRule: () -> Unit = {},
    /** Start (sessions, limits) or Activate (locks). */
    val onStart: () -> Unit = {},
    /** A lock's "Turn off". */
    val onTurnOff: () -> Unit = {},
    val onEditSettings: () -> Unit = {},
    val onDelete: () -> Unit = {},
    /** "Drop changes", "Keep rule", "Keep it on", "Cancel" of the change kept for later. */
    val onDropPending: () -> Unit = {},
    /** The "Blocked" tile: the rule's apps, blocked or unlocked for now. */
    val onOpenApps: () -> Unit = {},
)

@Composable
internal fun RuleDetailScreen(
    rule: Rule,
    now: Long,
    actions: DetailActions,
    modifier: Modifier = Modifier,
    /** The Blocked tile's apps: the rule's, but for those always allowed. */
    blockedApps: List<UsageApp> = rule.apps,
) {
    SystemBarsAppearance(lightIcons = false)
    val colors = RecentrTheme.colors
    BackdropBox(Backdrop.Aurora, modifier) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                DetailNav(actions)
                Gap(14.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    StickerImage(rule.sticker.asSticker, size = 50.4.dp)
                    Column {
                        Text(
                            rule.name,
                            style = RecentrTheme.typography.displayLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(detailSubtitle(rule, now), style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
                    }
                }
                Gap(19.dp)
                DetailCollage(rule, now, blockedApps, actions.onOpenApps)
                Gap(24.dp)
                if (rule.pending != null) {
                    PendingCard(rule, actions.onDropPending)
                    Gap(12.dp)
                }
                // The settings as last wished: what waits for later is in the card above.
                DetailSettings(rule.latestWish, actions)
                Gap(16.dp)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DetailButtons(rule, now, actions)
            }
        }
        RulesToastHost(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp + 54.dp + 16.dp),
        )
    }
}

/**
 * The bottom bar. Session: Start, then nothing to stop: it ends on its own. Routine: Pause or
 * Resume. Lock: Activate, then Turn off. Limit: Start, for today. "Edit rule" is always there.
 */
@Composable
private fun RowScope.DetailButtons(rule: Rule, now: Long, actions: DetailActions) {
    @Composable
    fun edit(style: RecentrButtonStyle) = RecentrButton(
        text = stringResource(R.string.rules_edit_rule),
        onClick = actions.onEditRule,
        style = style,
        modifier = Modifier.weight(1f),
    )
    @Composable
    fun soft(text: Int, onClick: () -> Unit) = RecentrButton(
        text = stringResource(text),
        onClick = onClick,
        style = RecentrButtonStyle.Soft,
        modifier = Modifier.weight(1f),
    )
    @Composable
    fun primary(text: Int, onClick: () -> Unit) = RecentrButton(text = stringResource(text), onClick = onClick, modifier = Modifier.weight(1f))
    when (rule.type) {
        RuleType.Session -> if (rule.isRunning(now)) {
            edit(RecentrButtonStyle.Primary)
        } else {
            edit(RecentrButtonStyle.Soft)
            primary(R.string.rules_start, actions.onStart)
        }
        RuleType.Bedtime -> {
            soft(if (rule.isPaused(now)) R.string.rules_resume else R.string.rules_pause, actions.onPause)
            edit(RecentrButtonStyle.Primary)
        }
        RuleType.Lock -> if (rule.activeSince != null) {
            soft(R.string.rules_turn_off, actions.onTurnOff)
            edit(RecentrButtonStyle.Primary)
        } else {
            edit(RecentrButtonStyle.Soft)
            primary(R.string.rules_activate, actions.onStart)
        }
        RuleType.Limit -> if (rule.isEnforcing(now)) {
            edit(RecentrButtonStyle.Primary)
        } else {
            edit(RecentrButtonStyle.Soft)
            primary(R.string.rules_start, actions.onStart)
        }
    }
}

@Composable
private fun DetailNav(actions: DetailActions) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentrIconButton(RecentrIcon.ChevronLeft, contentDescription = stringResource(R.string.rules_back), onClick = actions.onBack)
        Box {
            RecentrIconButton(RecentrIcon.More, contentDescription = stringResource(R.string.rules_more), onClick = { menu = true })
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                containerColor = RecentrTheme.colors.bgSurface,
                shape = RoundedCornerShape(16.dp),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rules_menu_edit_settings), style = RecentrTheme.typography.bodyMedium) },
                    onClick = {
                        menu = false
                        actions.onEditSettings()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.rules_menu_delete),
                            style = RecentrTheme.typography.bodyMedium,
                            color = RecentrTheme.colors.feedbackDanger,
                        )
                    },
                    onClick = {
                        menu = false
                        actions.onDelete()
                    },
                )
            }
        }
    }
}

/**
 * "Routine · Active weekdays", "Session · Running now", "Lock · On", "Limit · Until midnight",
 * "… · Paused".
 */
@Composable
private fun detailSubtitle(rule: Rule, now: Long): String {
    val state = when {
        rule.isPaused(now) -> if (rule.pausedUntil == Long.MAX_VALUE) {
            stringResource(R.string.rules_detail_paused)
        } else {
            stringResource(R.string.rules_detail_paused_until, wallClock(rule.pausedUntil!!))
        }
        else -> when (rule.type) {
            RuleType.Session -> if (rule.isRunning(now)) stringResource(R.string.rules_detail_running) else durationLabel(rule.durationMinutes ?: 30)
            RuleType.Lock -> stringResource(if (rule.activeSince != null) R.string.rules_detail_on else R.string.rules_detail_off)
            RuleType.Limit -> stringResource(if (rule.isEnforcing(now)) R.string.rules_until_midnight else R.string.rules_budget_today)
            RuleType.Bedtime -> {
                val days = rule.schedule?.days ?: (1..7).toSet()
                val label = daysLabel(days)
                // "Active weekdays" / "Active every day", but keep "Active Mon, Wed" capitalised.
                val named = days == (1..7).toSet() || days == (1..5).toSet() || days == setOf(6, 7)
                stringResource(R.string.rules_detail_active_days, if (named) label.lowercase() else label)
            }
        }
    }
    return stringResource(R.string.rules_detail_sub, rule.type.label(), state)
}

/**
 * What waits for the end of the engagement, and the way to drop it: "Your changes apply at
 * midnight · Drop changes", "Deleted at midnight · Keep rule", "Turns off at midnight · Keep it
 * on", "Pauses when this window ends · Cancel".
 */
@Composable
private fun PendingCard(rule: Rule, onDrop: () -> Unit) {
    val change = rule.pending ?: return
    val colors = RecentrTheme.colors
    val (text, action) = when (rule.pendingKind) {
        PendingKind.Delete -> pendingWhenText(
            rule.type, change,
            midnight = R.string.rules_pending_delete_midnight,
            sessionEnd = R.string.rules_pending_delete_session,
            at = R.string.rules_pending_delete_at,
        ) to R.string.rules_pending_keep_rule
        PendingKind.TurnOff -> stringResource(R.string.rules_pending_turn_off) to R.string.rules_pending_keep_on
        PendingKind.Pause -> stringResource(R.string.rules_pending_pause) to R.string.rules_pending_dont_pause
        else -> pendingWhenText(
            rule.type, change,
            midnight = R.string.rules_pending_changes_midnight,
            sessionEnd = R.string.rules_pending_changes_session,
            at = R.string.rules_pending_changes_at,
        ) to R.string.rules_pending_drop
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.alphaSky12)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.bgSurface), contentAlignment = Alignment.Center) {
            RecentrIconImage(RecentrIcon.Clock, tint = colors.brandSky, size = 18.dp)
        }
        Text(text, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
        RecentrTextButton(text = stringResource(action), onClick = onDrop, color = colors.brandSky)
    }
}

/**
 * Tilted collage: time card, blocked apps, "Why" note and streak (positions from Figma 44). The
 * pieces drop into place one after the other when the page opens. Read-only: "Edit rule" opens
 * the editor.
 */
@Composable
private fun DetailCollage(rule: Rule, now: Long, blockedApps: List<UsageApp>, onOpenApps: () -> Unit) {
    val (label, value, caption) = timeTile(rule, now)
    Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
        // Fixed widths, the time card on the left edge and the apps tile on the right: a countdown
        // doesn't make them grow and shrink, and a long value ("14:56:36") shrinks to fit.
        TimeCard(
            label = label,
            value = value,
            caption = caption,
            rotation = 4f,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = (-54.7).dp)
                .width(164.dp)
                .entrance(50, from = DpOffset((-20).dp, 10.dp), blur = false),
        )
        AppsTile(
            apps = blockedApps,
            onClick = onOpenApps,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-12).dp, y = (-69.16).dp)
                .width(148.dp)
                .entrance(120, from = DpOffset(20.dp, 10.dp), blur = false)
                .graphicsLayer { rotationZ = -5f },
        )
        // Pinned by its top just under the apps tile (where Figma's two-line note starts) and
        // capped at three lines, so a long reason grows downwards instead of over the apps.
        StickyNote(
            title = stringResource(R.string.rules_why),
            body = rule.note?.takeIf { it.isNotBlank() } ?: stringResource(R.string.rules_why_empty),
            maxBodyLines = 3,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 86.dp, y = 126.dp)
                .entrance(200, blur = false)
                .width(180.dp)
                .graphicsLayer { rotationZ = 3f },
        )
        MiniTile(
            label = stringResource(R.string.rules_streak),
            value = pluralStringResource(R.plurals.rules_streak_days, rule.streakDays, rule.streakDays),
            sticker = Sticker.Flame,
            modifier = Modifier
                .offset(x = (-80.38).dp, y = 71.6.dp)
                .entrance(280, blur = false)
                .width(171.dp)
                .graphicsLayer { rotationZ = -2f },
        )
    }
}

@Composable
private fun timeTile(rule: Rule, now: Long): Triple<String, String, String> = when (rule.type) {
    RuleType.Session -> {
        val end = rule.sessionEndsAt
        if (end != null && rule.isRunning(now)) {
            Triple(
                stringResource(R.string.rules_tile_now),
                countdown(end - now),
                stringResource(R.string.rules_tile_left_of, durationLabel(((end - (rule.activeSince ?: end)) / 60_000L).toInt())),
            )
        } else {
            Triple(
                stringResource(R.string.rules_tile_ready),
                durationLabel(rule.durationMinutes ?: 0),
                stringResource(R.string.rules_session_title),
            )
        }
    }
    RuleType.Limit -> Triple(
        stringResource(R.string.rules_tile_today),
        compactDuration(rule.limitMinutes ?: 0),
        if (rule.isEnforcing(now)) {
            stringResource(R.string.rules_tile_used, compactDuration(rule.usedMinutes))
        } else {
            stringResource(R.string.rules_tile_not_started)
        },
    )
    RuleType.Lock -> Triple(
        stringResource(R.string.rules_tile_unlocks_today),
        rule.unlocksPerDay?.let { stringResource(R.string.rules_tile_unlocks_of, rule.unlocksUsedToday, it) } ?: rule.unlocksUsedToday.toString(),
        stringResource(if (rule.activeSince != null) R.string.rules_value_locked else R.string.rules_detail_off),
    )
    RuleType.Bedtime -> {
        val schedule = rule.schedule
        if (schedule == null || schedule.allDay) {
            Triple(
                daysLabel(schedule?.days ?: (1..7).toSet()).uppercase(),
                stringResource(R.string.rules_all_day),
                stringResource(R.string.rules_tile_all_day_caption, unlocksPerDayLabel(rule.unlocksPerDay)),
            )
        } else {
            Triple(
                daysLabel(schedule.days).uppercase(),
                stringResource(R.string.rules_time_range, clockHours(schedule.startMinute), clockHours(schedule.endMinute)),
                stringResource(R.string.rules_tile_to, clockLabel(schedule.startMinute), clockLabel(schedule.endMinute)),
            )
        }
    }
}

/** The rule's [apps] it blocks: those always allowed aren't, even when they were picked for it. */
@Composable
private fun AppsTile(apps: List<UsageApp>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.float, shape)
            .clip(shape)
            .background(colors.bgSurface)
            // Opens the "Apps" sheet: which are blocked, which are unlocked for now.
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.rules_blocked), style = RecentrTheme.typography.captionMedium, color = colors.textTertiary)
        OverlappingInstalledAppIcons(apps.take(3).map { it.packageName }, size = 40.dp, overlap = 8.dp)
        Text(
            pluralStringResource(R.plurals.rules_apps_count, apps.size, apps.size),
            style = RecentrTheme.typography.headline,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The rule's settings, as last wished ([rule] is its [latestWish]). */
@Composable
private fun DetailSettings(rule: Rule, actions: DetailActions) {
    @Composable
    fun row(label: Int, value: String, icon: RecentrIcon, target: RuleSheet?) = SettingRow(
        label = stringResource(label),
        value = value,
        onClick = { if (target != null) actions.onOpen(target) else actions.onEditSettings() },
        icon = icon,
        modifier = Modifier.fillMaxWidth(),
    )
    SettingsGroup {
        when (rule.type) {
            RuleType.Session -> {
                UnlockMethodRow(rule, onClick = { actions.onOpen(RuleSheet.Unlock) })
                row(R.string.rules_row_active, durationLabel(rule.durationMinutes ?: 0), RecentrIcon.Clock, RuleSheet.Duration)
            }
            RuleType.Lock -> {
                UnlockMethodRow(rule, onClick = { actions.onOpen(RuleSheet.Unlock) })
                row(R.string.rules_row_unlocks_per_day, unlocksPerDayLabel(rule.unlocksPerDay), RecentrIcon.Refresh, RuleSheet.UnlocksPerDay)
            }
            // The budget's stepper is in the editor.
            RuleType.Limit -> row(R.string.rules_budget_today, compactDuration(rule.limitMinutes ?: 0), RecentrIcon.Clock, null)
            RuleType.Bedtime -> {
                UnlockMethodRow(rule, onClick = { actions.onOpen(RuleSheet.Unlock) })
                row(R.string.rules_row_reminders, reminderLabel(rule.reminderMinutesBefore), RecentrIcon.Bell, RuleSheet.Reminders)
            }
        }
        if (rule.canBeStrict) StrictToggleRow(rule.strictMode, actions.onStrictChange)
    }
}

@Preview(heightDp = 874)
@Composable
private fun RuleDetailPreview() {
    RecentrTheme {
        RuleDetailScreen(
            rule = Rule(
                id = "work-hours",
                type = RuleType.Bedtime,
                name = "Work Hours",
                apps = listOf(AppId.X, AppId.Instagram, AppId.TikTok).map { UsageApp.of(it) },
                schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()),
                sticker = RuleSticker.Bolt,
                reminderMinutesBefore = 10,
                note = "Deep work beats busy work.",
                streakDays = 3,
            ),
            now = 0L,
            actions = DetailActions(),
        )
    }
}

@Preview(heightDp = 874)
@Composable
private fun LockTurningOffPreview() {
    val lock = Rule(
        id = "socials",
        type = RuleType.Lock,
        name = "Instagram & X",
        apps = listOf(AppId.X, AppId.Instagram).map { UsageApp.of(it) },
        activeSince = 0L,
        unlocksUsedToday = 2,
    )
    RecentrTheme {
        RuleDetailScreen(
            rule = lock.copy(pending = RuleChange(24 * 3_600_000L, lock.copy(activeSince = null))),
            now = 0L,
            actions = DetailActions(),
        )
    }
}
