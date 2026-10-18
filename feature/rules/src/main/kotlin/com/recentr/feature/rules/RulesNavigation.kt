package com.recentr.feature.rules

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.model.RuleType
import com.recentr.feature.rules.detail.RuleDetailDestination
import com.recentr.feature.rules.editor.RuleEditorDestination
import com.recentr.feature.rules.list.NewRuleSheet
import com.recentr.feature.rules.list.RulesListActions
import com.recentr.feature.rules.list.RulesScreen
import com.recentr.feature.rules.list.RulesViewModel
import com.recentr.feature.rules.model.BuiltInTemplate
import kotlinx.serialization.Serializable

/**
 * Rule editor sheet (create / edit). A `dialog` destination hosting a bottom sheet, so it can be
 * opened from Home ("Start blocking") as well as from the Rules tab and the rule detail.
 */
@Serializable
internal data class RuleEditorRoute(val type: String, val ruleId: String? = null, val templateId: String? = null)

/** Full-screen rule detail (drawn above the tab bar). */
@Serializable
internal data class RuleDetailRoute(val ruleId: String)

/**
 * Rules tab content (rules list). Drawn under the floating tab bar: keep [contentPadding]
 * (bottom) clear of interactive content. [onOpenFocus] opens the focus timer ("New rule" sheet).
 */
@Composable
fun RulesTab(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onOpenFocus: () -> Unit = {},
) {
    val viewModel: RulesViewModel = viewModel(factory = RulesViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var newRule by rememberSaveable { mutableStateOf(false) }
    var help by rememberSaveable { mutableStateOf(false) }
    RulesScreen(
        state = state,
        contentPadding = contentPadding,
        modifier = modifier,
        actions = RulesListActions(
            onShowTemplates = viewModel::showTemplates,
            onToggleGroup = viewModel::toggleGroup,
            onToggleTemplatesGroup = viewModel::toggleTemplatesGroup,
            onFilter = viewModel::setFilter,
            onOpenRule = { navController.navigate(RuleDetailRoute(it)) },
            onCreate = { navController.navigateToNewRule(it) },
            onTemplate = { navController.navigateToTemplate(it) },
            onStartBlocking = { navController.navigateToStartSession() },
            onHelp = { help = true },
            onMore = { newRule = true },
        ),
    )
    if (newRule) {
        NewRuleSheet(
            onCreate = { navController.navigateToNewRule(it) },
            onBrowseTemplates = { viewModel.showTemplates(true) },
            onOpenFocus = onOpenFocus,
            onDismiss = { newRule = false },
        )
    }
    if (help) {
        RecentrAlertDialog(
            title = stringResource(R.string.rules_help_title),
            body = stringResource(R.string.rules_help_body),
            primaryText = stringResource(R.string.rules_help_ok),
            onPrimary = { help = false },
            onDismissRequest = { help = false },
        )
    }
}

/** Full-screen rule destinations (editors, detail…) pushed above the tab bar. */
fun NavGraphBuilder.rulesDestinations(navController: NavHostController) {
    composable<RuleDetailRoute> {
        RuleDetailDestination(
            onBack = { navController.popBackStack() },
            onEditSettings = { id -> navController.navigate(RuleEditorRoute(type = RuleType.Session.name, ruleId = id)) },
        )
    }
    dialog<RuleEditorRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
        RuleEditorDestination(onClose = { navController.popBackStack() })
    }
}

/** Entry used by Home's "Start blocking" CTA: opens the new Session rule flow. */
fun NavHostController.navigateToStartSession() = navigateToNewRule(RuleType.Session)

private fun NavHostController.navigateToNewRule(type: RuleType) {
    navigate(RuleEditorRoute(type = type.name)) { launchSingleTop = true }
}

private fun NavHostController.navigateToTemplate(templateId: String) {
    val type = BuiltInTemplate.byId(templateId)?.type
        ?: RecentrData.rules.savedTemplate(templateId)?.type
        ?: RuleType.Session
    navigate(RuleEditorRoute(type = type.name, templateId = templateId)) { launchSingleTop = true }
}
