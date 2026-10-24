package com.recentr.feature.rules

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import com.recentr.core.data.BlockingAccess
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.component.BlockingAccessSheet
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.allowed.AlwaysAllowedDestination
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
 * opened from Home ("Start blocking", over the Rules tab) and Insights as well as from the Rules
 * tab and the rule detail. A rule it creates then opens on its [RuleDetailRoute]. [apps]
 * preselects the apps of a new rule (comma-separated package names).
 */
@Serializable
internal data class RuleEditorRoute(
    val type: String,
    val ruleId: String? = null,
    val templateId: String? = null,
    val apps: String? = null,
)

/** Full-screen rule detail (drawn above the tab bar). */
@Serializable
internal data class RuleDetailRoute(val ruleId: String)

/** Settings › Always allowed (drawn above the tab bar), also opened from Home's card. */
@Serializable
internal data object AlwaysAllowedRoute

/**
 * Rules tab content (rules list). Drawn under the floating tab bar: keep [contentPadding]
 * (bottom) clear of interactive content. The header "+" opens the "New rule" sheet. [ruleToShow]
 * is the rule Home's next rule tile shows: the list scrolls to it, then [onRuleShown].
 */
@Composable
fun RulesTab(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    ruleToShow: String? = null,
    onRuleShown: () -> Unit = {},
) {
    val viewModel: RulesViewModel = viewModel(factory = RulesViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(ruleToShow) {
        ruleToShow ?: return@LaunchedEffect
        viewModel.showRule(ruleToShow)
        onRuleShown()
    }
    var newRule by rememberSaveable { mutableStateOf(false) }
    var explainBlocking by rememberSaveable { mutableStateOf(false) }
    RulesScreen(
        state = state,
        contentPadding = contentPadding,
        modifier = modifier,
        actions = RulesListActions(
            onShowTemplates = viewModel::showTemplates,
            onToggleGroup = viewModel::toggleGroup,
            onToggleTemplatesGroup = viewModel::toggleTemplatesGroup,
            onOpenRule = { navController.navigate(RuleDetailRoute(it)) },
            onCreate = { navController.navigateToNewRule(it) },
            onTemplate = { navController.navigateToTemplate(it) },
            onNewRule = { newRule = true },
            onTurnOnBlocking = { explainBlocking = true },
            onRuleShown = viewModel::ruleShown,
        ),
    )
    if (explainBlocking) {
        val context = LocalContext.current
        BlockingAccessSheet(onOpenSettings = { BlockingAccess.openSettings(context) }, onDismissRequest = { explainBlocking = false })
    }
    if (newRule) {
        NewRuleSheet(
            onCreate = { navController.navigateToNewRule(it) },
            onBrowseTemplates = { viewModel.showTemplates(true) },
            onDismiss = { newRule = false },
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
    composable<AlwaysAllowedRoute> { AlwaysAllowedDestination(onBack = { navController.popBackStack() }) }
    dialog<RuleEditorRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
        RuleEditorDestination(
            onClose = { navController.popBackStack() },
            // A new rule opens on its page, wherever the editor was opened from.
            onCreated = { id ->
                navController.navigate(RuleDetailRoute(id)) { popUpTo<RuleEditorRoute> { inclusive = true } }
            },
        )
    }
}

/** Opens a rule's page (a rule's notification), with [navOptions] for the screens it replaces. */
fun NavHostController.navigateToRule(ruleId: String, navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(RuleDetailRoute(ruleId), navOptions)
}

/** The rule whose page is showing, if one is. */
fun NavHostController.ruleShown(): String? =
    currentBackStackEntry?.takeIf { it.destination.hasRoute<RuleDetailRoute>() }?.toRoute<RuleDetailRoute>()?.ruleId

/** Opens Settings › Always allowed: the apps no rule and no focus session puts on hold. */
fun NavHostController.navigateToAlwaysAllowed() {
    navigate(AlwaysAllowedRoute) { launchSingleTop = true }
}

/** "Start blocking": opens the new Session rule flow. */
fun NavHostController.navigateToStartSession() {
    navigate(RuleEditorRoute(type = RuleType.Session.name)) { launchSingleTop = true }
}

/** Opens the editor of a new [type] rule, with [apps] already chosen (Insights "Set a limit"). */
fun NavHostController.navigateToNewRule(type: RuleType, apps: List<UsageApp> = emptyList()) {
    val chosen = apps.takeIf { it.isNotEmpty() }?.joinToString(",") { it.packageName }
    navigate(RuleEditorRoute(type = type.name, apps = chosen)) { launchSingleTop = true }
}

/** Opens the editor of an existing rule. */
fun NavHostController.navigateToRuleEditor(ruleId: String) {
    val type = RecentrData.rules.rule(ruleId)?.type ?: RuleType.Session
    navigate(RuleEditorRoute(type = type.name, ruleId = ruleId)) { launchSingleTop = true }
}

/** Opens the editor of a new rule made from a built-in template, of the template's kind. */
private fun NavHostController.navigateToTemplate(templateId: String) {
    val type = BuiltInTemplate.byId(templateId)?.type ?: RuleType.Session
    navigate(RuleEditorRoute(type = type.name, templateId = templateId)) { launchSingleTop = true }
}
