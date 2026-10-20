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
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp

private val previewActions = EditorActions(
    open = {},
    update = {},
    onStrictChange = {},
    onSuggestion = {},
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
            Rule("1", RuleType.Session, "Focus session", apps = listOf(AppId.Instagram, AppId.X, AppId.TikTok).map { UsageApp.of(it) }, durationMinutes = 30),
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
                apps = listOf(AppId.Instagram, AppId.TikTok).map { UsageApp.of(it) },
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
private fun LimitEditorPreview() = SheetPreview {
    LimitEditor(
        state = RuleEditorUiState(
            Rule("3", RuleType.Limit, "Social time", apps = listOf(AppId.Instagram, AppId.TikTok).map { UsageApp.of(it) }, limitMinutes = 2 * 60 + 15),
            isNew = true,
            customTitle = false,
        ),
        title = "Limit",
        actions = previewActions,
    )
}
