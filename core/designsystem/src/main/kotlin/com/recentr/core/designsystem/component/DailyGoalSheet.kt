package com.recentr.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlinx.coroutines.launch

/** Preset goals of the Figma 28 sheet: 2h · 3h · 3h45 · 4h20 · 5h. */
private val GoalPresets = listOf(120, 180, 225, 260, 300)
private const val GoalStep = 5
private val GoalRange = 15..12 * 60

/**
 * Figma 28 · "Daily goal" (Home and Insights) — a ruler in five minutes, presets, "Save goal",
 * which slides the sheet down. [formatMinutes] writes the goal ("3h45").
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
    Column(
        Modifier.fillMaxWidth().padding(horizontal = RecentrTheme.spacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.ds_goal_title), style = RecentrTheme.typography.title1, color = colors.textPrimary)
            Text(stringResource(R.string.ds_goal_subtitle), style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
        }
        Spacer(Modifier.height(24.dp))
        DurationPicker(
            value = goal,
            onValueChange = { goal = it },
            range = GoalRange,
            contentDescription = stringResource(R.string.ds_goal_title),
            step = GoalStep,
            labelEvery = 30,
            presets = GoalPresets,
            format = formatMinutes,
        )
        Spacer(Modifier.height(24.dp))
        RecentrButton(
            text = stringResource(R.string.ds_goal_save),
            onClick = { onSave(goal) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Preview(widthDp = 402)
@Composable
private fun GoalSheetPreview() {
    RecentrTheme {
        Column(Modifier.fillMaxWidth()) { GoalSheetContent(225, { "${it / 60}h${"%02d".format(it % 60)}" }, onSave = {}) }
    }
}
