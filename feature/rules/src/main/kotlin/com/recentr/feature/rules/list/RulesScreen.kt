package com.recentr.feature.rules.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.AddCard
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.TemplateCard
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.BackdropBox
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.AppId
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleType
import com.recentr.feature.rules.R
import com.recentr.feature.rules.model.BuiltInTemplate
import com.recentr.feature.rules.model.TemplateCardUi
import com.recentr.feature.rules.model.TemplateCategory
import com.recentr.feature.rules.model.toCard
import com.recentr.feature.rules.model.toSavedTemplateCard
import com.recentr.feature.rules.ui.asSticker
import com.recentr.feature.rules.ui.components.RuleGroupHeader
import com.recentr.feature.rules.ui.components.RulesToastHost

/** Callbacks of the rules list. */
internal class RulesListActions(
    val onShowTemplates: (Boolean) -> Unit = {},
    val onToggleGroup: (RuleGroup) -> Unit = {},
    val onToggleTemplatesGroup: () -> Unit = {},
    val onFilter: (TemplateCategory?) -> Unit = {},
    val onOpenRule: (String) -> Unit = {},
    val onCreate: (RuleType) -> Unit = {},
    val onTemplate: (String) -> Unit = {},
    val onStartBlocking: () -> Unit = {},
    val onHelp: () -> Unit = {},
    val onMore: () -> Unit = {},
)

/**
 * Rules tab (Figma 30 / 34 / 43): grouped rule cards or the templates grid, a compact header once
 * scrolled, the bottom fade with the "Start blocking" CTA and the confirmation toast.
 */
@Composable
internal fun RulesScreen(
    state: RulesUiState,
    contentPadding: PaddingValues,
    actions: RulesListActions,
    modifier: Modifier = Modifier,
) {
    SystemBarsAppearance(lightIcons = false)
    val rulesList = rememberLazyListState()
    val templatesList = rememberLazyListState()
    val bottomInset = contentPadding.calculateBottomPadding()
    // Room for the CTA (8 + 54) and a breathing gap above it.
    val listBottom = bottomInset + 8.dp + 54.dp + 28.dp
    BackdropBox(Backdrop.Aurora, modifier) {
        Crossfade(targetState = state.showTemplates, label = "rulesTemplates") { templates ->
            if (templates) {
                TemplatesList(state, templatesList, listBottom, actions)
            } else {
                RulesList(state, rulesList, listBottom, actions)
            }
        }
        val listState = if (state.showTemplates) templatesList else rulesList
        CompactHeader(
            title = stringResource(if (state.showTemplates) R.string.rules_templates_title else R.string.rules_title),
            listState = listState,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomInset + 96.dp)
                .background(
                    Brush.verticalGradient(
                        0f to RecentrTheme.colors.bgPage.copy(alpha = 0f),
                        0.45f to RecentrTheme.colors.bgPage.copy(alpha = 0.92f),
                        1f to RecentrTheme.colors.bgPage,
                    ),
                ),
        )
        RulesToastHost(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 8.dp + 54.dp + 12.dp),
        )
        RecentrButton(
            text = stringResource(R.string.rules_start_blocking),
            onClick = actions.onStartBlocking,
            style = RecentrButtonStyle.Brand,
            icon = RecentrIcon.Plus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = bottomInset + 8.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun RulesList(state: RulesUiState, listState: LazyListState, bottom: androidx.compose.ui.unit.Dp, actions: RulesListActions) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = statusBar + 12.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            RulesHeader(
                showTemplates = false,
                subtitle = if (state.pausedCount > 0) {
                    stringResource(R.string.rules_subtitle_active_paused, state.activeCount, state.pausedCount)
                } else {
                    stringResource(R.string.rules_subtitle_active, state.activeCount)
                },
                actions = actions,
                modifier = Modifier.padding(bottom = 9.dp),
            )
        }
        state.sections.forEach { section ->
            val expanded = section.group !in state.collapsed
            item(key = "group-${section.group}") {
                RuleGroupHeader(
                    label = stringResource(section.group.label),
                    count = section.rules.size.takeIf { it > 0 },
                    expanded = expanded,
                    onToggle = { actions.onToggleGroup(section.group) },
                    modifier = Modifier.padding(horizontal = 24.dp).animateItem(),
                )
            }
            if (expanded) {
                items(section.rules, key = { it.id }) { rule ->
                    RuleListCard(
                        rule = rule,
                        now = state.now,
                        onClick = { actions.onOpenRule(rule.id) },
                        modifier = Modifier.padding(horizontal = 24.dp).animateItem(),
                    )
                }
                if (section.group.addTitle != null && section.group.addHint != null) {
                    item(key = "add-${section.group}") {
                        AddCard(
                            title = stringResource(section.group.addTitle),
                            hint = stringResource(section.group.addHint),
                            onClick = { actions.onCreate(section.group.type) },
                            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().animateItem(),
                        )
                    }
                }
            }
        }
        item(key = "templates-header") {
            RuleGroupHeader(
                label = stringResource(R.string.rules_group_templates),
                count = null,
                expanded = !state.templatesCollapsed,
                onToggle = actions.onToggleTemplatesGroup,
                modifier = Modifier.padding(horizontal = 24.dp).animateItem(),
            )
        }
        if (!state.templatesCollapsed) {
            item(key = "templates-row") {
                val cards = allTemplateCards(state.savedTemplates)
                LazyRow(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.animateItem(),
                ) {
                    items(cards, key = { it.id }) { card ->
                        TemplateCard(
                            title = card.title,
                            body = card.body,
                            duration = card.badge,
                            sticker = card.sticker.asSticker,
                            color = card.color,
                            onClick = { actions.onTemplate(card.id) },
                            modifier = Modifier.size(width = 165.dp, height = 210.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatesList(state: RulesUiState, listState: LazyListState, bottom: androidx.compose.ui.unit.Dp, actions: RulesListActions) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cards = allTemplateCards(state.savedTemplates).filter { state.filter == null || it.category == state.filter }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = statusBar + 12.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            RulesHeader(
                showTemplates = true,
                subtitle = stringResource(R.string.rules_templates_subtitle),
                actions = actions,
                modifier = Modifier.padding(bottom = 11.dp),
            )
        }
        item(key = "filters") {
            TemplateFilters(
                selected = state.filter,
                onSelect = actions.onFilter,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        if (cards.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.rules_templates_empty),
                    style = RecentrTheme.typography.bodyRegular,
                    color = RecentrTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                )
            }
        }
        items(cards.chunked(2), key = { row -> row.first().id }) { row ->
            Row(
                Modifier.padding(horizontal = 24.dp).fillMaxWidth().animateItem(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                row.forEach { card ->
                    TemplateCard(
                        title = card.title,
                        body = card.body,
                        duration = card.badge,
                        sticker = card.sticker.asSticker,
                        color = card.color,
                        onClick = { actions.onTemplate(card.id) },
                        modifier = Modifier.weight(1f).height(230.dp),
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun allTemplateCards(saved: List<Rule>): List<TemplateCardUi> =
    BuiltInTemplate.entries.map { it.toCard() } + saved.map { it.toSavedTemplateCard() }

@Composable
private fun TemplateFilters(selected: TemplateCategory?, onSelect: (TemplateCategory?) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf<TemplateCategory?>(null) + TemplateCategory.entries
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            com.recentr.core.designsystem.component.RecentrChip(
                label = stringResource(option?.label ?: R.string.rules_filter_all),
                style = if (option == selected) {
                    com.recentr.core.designsystem.component.RecentrChipStyle.Selected
                } else {
                    com.recentr.core.designsystem.component.RecentrChipStyle.Outline
                },
                onClick = { onSelect(option) },
            )
        }
    }
}

/** "Rules  Templates" switch title with help / more buttons (Figma Header). */
@Composable
private fun RulesHeader(showTemplates: Boolean, subtitle: String, actions: RulesListActions, modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val rulesColor by animateColorAsState(if (showTemplates) colors.textTertiary else colors.textPrimary, label = "rulesTitle")
    val templatesColor by animateColorAsState(if (showTemplates) colors.textPrimary else colors.textTertiary, label = "templatesTitle")
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.semantics { heading() }) {
                Text(
                    stringResource(R.string.rules_title),
                    style = RecentrTheme.typography.displayLarge,
                    color = rulesColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clip(RecentrShapes.xs).clickable(role = Role.Tab) { actions.onShowTemplates(false) },
                )
                Text("  ", style = RecentrTheme.typography.displayLarge, maxLines = 1)
                Text(
                    stringResource(R.string.rules_templates_title),
                    style = RecentrTheme.typography.displayLarge,
                    color = templatesColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.clip(RecentrShapes.xs).clickable(role = Role.Tab) { actions.onShowTemplates(true) },
                )
            }
            Text(subtitle, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary, maxLines = 1)
        }
        RecentrIconButton(RecentrIcon.Help, contentDescription = stringResource(R.string.rules_help), onClick = actions.onHelp)
        RecentrIconButton(RecentrIcon.More, contentDescription = stringResource(R.string.rules_more), onClick = actions.onMore)
    }
}

/** Blurred-looking compact title bar shown once the large header scrolled away (Figma 30). */
@Composable
private fun CompactHeader(title: String, listState: LazyListState) {
    val visible by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 56 }
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(RecentrTheme.colors.bgSurface.copy(alpha = 0.96f))
                .statusBarsPadding()
                .height(50.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                title,
                style = RecentrTheme.typography.headline,
                color = RecentrTheme.colors.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun RulesScreenPreview() {
    RecentrTheme {
        RulesScreen(
            state = RulesUiState(
                sections = listOf(
                    RuleSection(
                        RuleGroup.Locks,
                        listOf(Rule("1", RuleType.Lock, "Instagram & X", apps = listOf(AppId.Instagram, AppId.X), unlocksUsedToday = 2)),
                    ),
                    RuleSection(
                        RuleGroup.Sessions,
                        listOf(
                            Rule(
                                "2", RuleType.Session, "Work Hours",
                                apps = listOf(AppId.X, AppId.Instagram, AppId.TikTok),
                                schedule = RuleSchedule(540, 1020, (1..5).toSet()),
                                streakDays = 3,
                            ),
                        ),
                    ),
                ),
                activeCount = 2,
            ),
            contentPadding = PaddingValues(bottom = 96.dp),
            actions = RulesListActions(),
        )
    }
}
