package com.recentr.feature.rules.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppId
import com.recentr.core.model.HabitTask
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType

private val previewActions = EditorActions(
    open = {},
    update = {},
    onStrictChange = {},
    onSuggestion = {},
    onSaveTemplate = {},
    onCommit = {},
)

@Composable
private fun SheetPreview(content: @Composable ColumnScope.() -> Unit) {
    RecentrTheme {
        Column(
            Modifier.background(RecentrTheme.colors.bgSurface).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Preview(widthDp = 402)
@Composable
private fun SessionEditorPreview() = SheetPreview {
    SessionEditor(
        state = RuleEditorUiState(
            Rule("1", RuleType.Session, "Focus session", apps = listOf(AppId.Instagram, AppId.X, AppId.TikTok), durationMinutes = 30),
            isNew = true,
            customTitle = false,
        ),
        title = "Session",
        actions = previewActions,
    )
}

@Preview(widthDp = 402)
@Composable
private fun BedtimeEditorPreview() = SheetPreview {
    BedtimeEditor(
        state = RuleEditorUiState(
            Rule(
                "2", RuleType.Bedtime, "Bedtime",
                schedule = RuleSchedule(23 * 60, 7 * 60, (1..5).toSet()),
                blockAllApps = true,
                allowedApps = listOf(AppId.Gmail),
                strictMode = true,
            ),
            isNew = true,
            customTitle = false,
        ),
        title = "Bedtime",
        actions = previewActions,
    )
}

@Preview(widthDp = 402)
@Composable
private fun HabitEditorPreview() = SheetPreview {
    HabitEditor(
        state = RuleEditorUiState(
            Rule(
                "3", RuleType.Habit, "Before X opens",
                apps = listOf(AppId.X, AppId.TikTok),
                habitTasks = listOf(HabitTask("a", "Read 10 pages", done = true), HabitTask("b", "Drink a glass of water")),
            ),
            isNew = true,
            customTitle = false,
        ),
        title = "Habit",
        habitTitle = "Before X opens",
        actions = previewActions,
    )
}
