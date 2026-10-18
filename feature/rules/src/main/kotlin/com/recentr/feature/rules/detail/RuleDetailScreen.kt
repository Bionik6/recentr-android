package com.recentr.feature.rules.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.OverlappingAppIcons
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.icon.RecentrIcon
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
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.feature.rules.R
import com.recentr.feature.rules.sheets.RuleSheet
import com.recentr.feature.rules.sheets.RuleSheetsHost
import com.recentr.feature.rules.sheets.StrictConfirmDialog
import com.recentr.feature.rules.ui.ESSENTIAL_APPS
import com.recentr.feature.rules.ui.allowedAppsLabel
import com.recentr.feature.rules.ui.asDemoApp
import com.recentr.feature.rules.ui.asSticker
import com.recentr.feature.rules.ui.blockedApps
import com.recentr.feature.rules.ui.clockLabel
import com.recentr.feature.rules.ui.clockShort
import com.recentr.feature.rules.ui.compactDuration
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesToastHost
import com.recentr.feature.rules.ui.components.SettingsGroup
import com.recentr.feature.rules.ui.components.StrictToggleRow
import com.recentr.feature.rules.ui.components.TimeCard
import com.recentr.feature.rules.ui.countdown
import com.recentr.feature.rules.ui.daysLabel
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.habitRepeatLabel
import com.recentr.feature.rules.ui.label
import com.recentr.feature.rules.ui.limitActionLabel
import com.recentr.feature.rules.ui.limitResetLabel
import com.recentr.feature.rules.ui.reminderLabel
import com.recentr.feature.rules.ui.unlockLabel
import com.recentr.feature.rules.ui.unlocksPerDayLabel
import com.recentr.feature.rules.ui.wallClock

/** Rule detail destination (Figma 44) with its Pause (45), Edit (46) and Delete (47) sheets. */
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
    var confirmStrict by rememberSaveable { mutableStateOf(false) }
    val locked = rule.isStrictLocked(state.now)
    fun guarded(action: () -> Unit) = if (locked) viewModel.strictLockedNotice() else action()

    RuleDetailScreen(
        rule = rule,
        now = state.now,
        actions = DetailActions(
            onBack = onBack,
            onOpen = { target -> guarded { sheet = target } },
            onStrictChange = { on ->
                when {
                    !on -> guarded { viewModel.update { it.copy(strictMode = false) } }
                    // It would lock right away, for the rest of the session: ask first.
                    rule.isEnforcing(state.now) -> confirmStrict = true
                    else -> viewModel.update { it.copy(strictMode = true) }
                }
            },
            onPause = {
                if (rule.isPaused(state.now)) viewModel.resume() else guarded { pause = true }
            },
            onEditRule = { guarded { sheet = RuleSheet.Name } },
            onEditSettings = { guarded { onEditSettings(rule.id) } },
            onDelete = { guarded { delete = true } },
        ),
    )
    RuleSheetsHost(
        sheet = sheet,
        rule = rule,
        now = state.now,
        onSheet = { sheet = it },
        onUpdate = viewModel::update,
        onRename = viewModel::rename,
        onApps = viewModel::setApps,
    )
    if (pause) {
        PauseRuleSheet(ruleName = rule.name, onPause = viewModel::pause, onDismiss = { pause = false })
    }
    if (confirmStrict) {
        StrictConfirmDialog(
            onConfirm = {
                viewModel.update { it.copy(strictMode = true) }
                confirmStrict = false
            },
            onDismiss = { confirmStrict = false },
        )
    }
    if (delete) {
        DeleteRuleSheet(
            ruleName = rule.name,
            streakDays = rule.streakDays,
            onDelete = viewModel::delete,
            onDismiss = { delete = false },
        )
    }
}

internal class DetailActions(
    val onBack: () -> Unit = {},
    val onOpen: (RuleSheet) -> Unit = {},
    val onStrictChange: (Boolean) -> Unit = {},
    val onPause: () -> Unit = {},
    val onEditRule: () -> Unit = {},
    val onEditSettings: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
internal fun RuleDetailScreen(rule: Rule, now: Long, actions: DetailActions, modifier: Modifier = Modifier) {
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
                DetailCollage(rule, now, actions)
                Gap(24.dp)
                DetailSettings(rule, now, actions)
                Gap(16.dp)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RecentrButton(
                    text = stringResource(if (rule.isPaused(now)) R.string.rules_resume else R.string.rules_pause),
                    onClick = actions.onPause,
                    style = RecentrButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                RecentrButton(
                    text = stringResource(R.string.rules_edit_rule),
                    onClick = actions.onEditRule,
                    modifier = Modifier.weight(1f),
                )
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

/** "Session · Active weekdays", "Lock · All day", "Session · Running now", "… · Paused". */
@Composable
private fun detailSubtitle(rule: Rule, now: Long): String {
    val state = when {
        rule.isPaused(now) -> if (rule.pausedUntil == Long.MAX_VALUE) {
            stringResource(R.string.rules_detail_paused)
        } else {
            stringResource(R.string.rules_detail_paused_until, wallClock(rule.pausedUntil!!))
        }
        rule.isRunning(now) -> stringResource(R.string.rules_detail_running)
        rule.type == RuleType.Limit -> stringResource(R.string.rules_detail_daily)
        rule.type == RuleType.Habit -> habitRepeatLabel(rule.habitRepeat)
        rule.schedule == null -> stringResource(R.string.rules_all_day)
        else -> {
            val days = rule.schedule!!.days
            val label = daysLabel(days)
            // "Active weekdays" / "Active every day", but keep "Active Mon, Wed" capitalised.
            val named = days == (1..7).toSet() || days == (1..5).toSet() || days == setOf(6, 7)
            stringResource(R.string.rules_detail_active_days, if (named) label.lowercase() else label)
        }
    }
    return stringResource(R.string.rules_detail_sub, rule.type.label(), state)
}

/**
 * Tilted collage: time card, blocked apps, "Why" note and streak (positions from Figma 44). The
 * pieces drop into place one after the other when the page opens.
 */
@Composable
private fun DetailCollage(rule: Rule, now: Long, actions: DetailActions) {
    val schedule = rule.schedule
    val (label, value, caption) = timeTile(rule, now)
    val timeTarget = when {
        rule.isRunning(now) || (rule.type == RuleType.Session && schedule == null) -> RuleSheet.Duration
        rule.type == RuleType.Session || rule.type == RuleType.Lock -> RuleSheet.Schedule
        rule.type == RuleType.Bedtime -> RuleSheet.BedtimeStart
        rule.type == RuleType.Habit -> RuleSheet.Repeats
        else -> null
    }
    Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
        TimeCard(
            label = label,
            value = value,
            caption = caption,
            rotation = 4f,
            onClick = if (timeTarget != null) ({ actions.onOpen(timeTarget) }) else actions.onEditSettings,
            modifier = Modifier
                .offset(x = (-120.3).dp, y = (-54.7).dp)
                .entrance(50, from = DpOffset((-20).dp, 10.dp), blur = false),
        )
        AppsTile(
            rule = rule,
            onClick = { actions.onOpen(if (rule.type == RuleType.Bedtime && rule.blockAllApps) RuleSheet.AllowedApps else RuleSheet.Apps) },
            modifier = Modifier
                .offset(x = 92.15.dp, y = (-69.16).dp)
                .entrance(120, from = DpOffset(20.dp, 10.dp), blur = false)
                .graphicsLayer { rotationZ = -5f },
        )
        StickyNote(
            title = stringResource(R.string.rules_why),
            body = rule.note?.takeIf { it.isNotBlank() } ?: stringResource(R.string.rules_why_placeholder),
            onClick = { actions.onOpen(RuleSheet.Note) },
            modifier = Modifier
                .offset(x = 86.dp, y = 56.6.dp)
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
private fun timeTile(rule: Rule, now: Long): Triple<String, String, String> {
    val schedule = rule.schedule
    return when {
        rule.isRunning(now) -> Triple(
            stringResource(R.string.rules_tile_now),
            countdown((rule.sessionEndsAt ?: now) - now),
            stringResource(R.string.rules_tile_left_of, durationLabel(rule.durationMinutes ?: 0)),
        )
        rule.type == RuleType.Limit -> Triple(
            stringResource(R.string.rules_tile_daily),
            compactDuration(rule.limitMinutes ?: 0),
            stringResource(R.string.rules_tile_used, compactDuration(rule.usedMinutes)),
        )
        rule.type == RuleType.Habit -> Triple(
            habitRepeatLabel(rule.habitRepeat).uppercase(),
            "${rule.habitTasks.count { it.done }}/${rule.habitTasks.size}",
            stringResource(R.string.rules_tile_tasks_done),
        )
        rule.type == RuleType.Session && schedule == null -> Triple(
            stringResource(R.string.rules_tile_now),
            durationLabel(rule.durationMinutes ?: 0),
            stringResource(R.string.rules_session_title),
        )
        schedule == null || schedule.lengthMinutes >= RuleSchedule.MINUTES_PER_DAY -> Triple(
            daysLabel(schedule?.days ?: (1..7).toSet()).uppercase(),
            stringResource(R.string.rules_all_day),
            stringResource(R.string.rules_tile_all_day_caption, unlocksPerDayLabel(rule.unlocksPerDay)),
        )
        else -> Triple(
            daysLabel(schedule.days).uppercase(),
            stringResource(R.string.rules_time_range, clockShort(schedule.startMinute), clockShort(schedule.endMinute)),
            stringResource(R.string.rules_tile_to, clockLabel(schedule.startMinute), clockLabel(schedule.endMinute)),
        )
    }
}

@Composable
private fun AppsTile(rule: Rule, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val apps = rule.blockedApps()
    Column(
        modifier = modifier
            .recentrShadow(RecentrElevation.float, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(if (rule.type == RuleType.Habit) R.string.rules_unlocks_label else R.string.rules_blocked),
            style = RecentrTheme.typography.captionMedium,
            color = colors.textTertiary,
        )
        OverlappingAppIcons(apps.take(3).map(AppId::asDemoApp), size = 40.dp, overlap = 8.dp)
        Text(
            if (rule.type == RuleType.Bedtime && rule.blockAllApps) {
                stringResource(R.string.rules_all_except, ESSENTIAL_APPS + rule.allowedApps.size)
            } else {
                pluralStringResource(R.plurals.rules_apps_count, apps.size, apps.size)
            },
            style = RecentrTheme.typography.headline,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun DetailSettings(rule: Rule, now: Long, actions: DetailActions) {
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
                row(R.string.rules_row_unlock_method, unlockLabel(rule), RecentrIcon.Hourglass, RuleSheet.Unlock)
                if (rule.schedule != null) {
                    row(R.string.rules_row_reminders, reminderLabel(rule.reminderMinutesBefore), RecentrIcon.Bell, RuleSheet.Reminders)
                } else {
                    row(R.string.rules_row_active, durationLabel(rule.durationMinutes ?: 0), RecentrIcon.Clock, RuleSheet.Duration)
                }
            }
            RuleType.Lock -> {
                row(R.string.rules_row_unlock_method, unlockLabel(rule), RecentrIcon.Hourglass, RuleSheet.Unlock)
                row(R.string.rules_row_unlocks_per_day, unlocksPerDayLabel(rule.unlocksPerDay), RecentrIcon.Refresh, RuleSheet.UnlocksPerDay)
            }
            RuleType.Limit -> {
                row(R.string.rules_row_resets, limitResetLabel(rule.limitReset), RecentrIcon.Refresh, RuleSheet.Resets)
                row(R.string.rules_row_when_time_up, limitActionLabel(rule.limitAction), RecentrIcon.Lock, RuleSheet.TimeUp)
            }
            RuleType.Bedtime -> {
                row(R.string.rules_row_always_allowed, allowedAppsLabel(rule), RecentrIcon.Check, RuleSheet.AllowedApps)
                row(R.string.rules_row_reminders, reminderLabel(rule.reminderMinutesBefore), RecentrIcon.Bell, RuleSheet.Reminders)
            }
            RuleType.Habit -> {
                row(R.string.rules_row_repeats, habitRepeatLabel(rule.habitRepeat), RecentrIcon.Refresh, RuleSheet.Repeats)
                row(
                    R.string.rules_row_tasks,
                    stringResource(R.string.rules_status_tasks, rule.habitTasks.count { it.done }, rule.habitTasks.size),
                    RecentrIcon.Check,
                    null,
                )
            }
        }
        if (rule.canBeStrict || rule.strictMode) {
            StrictToggleRow(rule.strictMode, actions.onStrictChange, locked = rule.isStrictLocked(now))
        }
    }
}

@Preview(heightDp = 874)
@Composable
private fun RuleDetailPreview() {
    RecentrTheme {
        RuleDetailScreen(
            rule = Rule(
                id = "work-hours",
                type = RuleType.Session,
                name = "Work Hours",
                apps = listOf(AppId.X, AppId.Instagram, AppId.TikTok),
                schedule = RuleSchedule(9 * 60, 17 * 60, (1..5).toSet()),
                reminderMinutesBefore = 10,
                note = "Deep work beats busy work.",
                streakDays = 3,
            ),
            now = 0L,
            actions = DetailActions(),
        )
    }
}
