package com.recentr.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlinx.coroutines.launch

/** Preset goals of the Figma 28 sheet: 2h · 3h · 3h45 · 4h20 · 5h. */
private val GoalPresets = listOf(120, 180, 225, 260, 300)
private const val MinGoal = 15
private const val MaxGoal = 12 * 60

/** Tap: previous / next quarter hour. */
internal fun stepGoal(minutes: Int, increase: Boolean): Int {
    val stepped = if (increase) minutes - minutes % 15 + 15 else if (minutes % 15 == 0) minutes - 15 else minutes - minutes % 15
    return stepped.coerceIn(MinGoal, MaxGoal)
}

/** Long press: ±5 minutes. */
internal fun nudgeGoal(minutes: Int, increase: Boolean): Int =
    (minutes + if (increase) 5 else -5).coerceIn(MinGoal, MaxGoal)

/**
 * Figma 28 · "Daily goal" (Home and Insights) — −/+ stepper (±15 min, long-press ±5 min),
 * presets, "Save goal", which slides the sheet down. [formatMinutes] writes the goal ("3h45").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyGoalSheet(
    currentGoalMinutes: Int,
    onSave: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    formatMinutes: @Composable (Int) -> String,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    RecentrBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        GoalSheetContent(currentGoalMinutes, formatMinutes) { minutes ->
            onSave(minutes)
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
        }
    }
}

@Composable
private fun GoalSheetContent(currentGoalMinutes: Int, formatMinutes: @Composable (Int) -> String, onSave: (Int) -> Unit) {
    val colors = RecentrTheme.colors
    var goal by rememberSaveable { mutableIntStateOf(currentGoalMinutes) }
    val haptics = LocalHapticFeedback.current
    fun change(value: Int) {
        if (value != goal) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        goal = value
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = RecentrTheme.spacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.ds_goal_title), style = RecentrTheme.typography.title1, color = colors.textPrimary)
            Text(stringResource(R.string.ds_goal_subtitle), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
        }
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(
                icon = RecentrIcon.Minus,
                contentDescription = stringResource(R.string.ds_goal_decrease),
                onClick = { change(stepGoal(goal, increase = false)) },
                onLongClick = { change(nudgeGoal(goal, increase = false)) },
            )
            AnimatedContent(
                targetState = goal,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically(tween(220)) { if (up) it / 3 else -it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(180)) { if (up) -it / 3 else it / 3 } + fadeOut(tween(180)))
                },
                label = "goalValue",
            ) { minutes ->
                Text(
                    text = formatMinutes(minutes),
                    style = RecentrTheme.typography.displayHero,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
            }
            StepperButton(
                icon = RecentrIcon.Plus,
                contentDescription = stringResource(R.string.ds_goal_increase),
                onClick = { change(stepGoal(goal, increase = true)) },
                onLongClick = { change(nudgeGoal(goal, increase = true)) },
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GoalPresets.forEach { preset ->
                RecentrChip(
                    label = formatMinutes(preset),
                    style = if (preset == goal) RecentrChipStyle.Selected else RecentrChipStyle.Outline,
                    onClick = { change(preset) },
                    modifier = Modifier.height(32.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        RecentrButton(
            text = stringResource(R.string.ds_goal_save),
            onClick = { onSave(goal) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

/** 52dp sunken round button of the stepper (Icon Button · Sunken, scaled). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepperButton(
    icon: RecentrIcon,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RecentrShapes.full)
            .background(RecentrTheme.colors.bgSunken)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        RecentrIconImage(icon, tint = RecentrTheme.colors.textPrimary)
    }
}

@Preview(widthDp = 402)
@Composable
private fun GoalSheetPreview() {
    RecentrTheme {
        Column(Modifier.fillMaxWidth()) { GoalSheetContent(225, { "${it / 60}h${"%02d".format(it % 60)}" }, onSave = {}) }
    }
}
