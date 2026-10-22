package com.recentr.feature.insights

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.recentr.core.data.UsageAccess
import com.recentr.core.model.Achievement
import com.recentr.core.model.RangeUnit
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UsagePeriod
import com.recentr.core.model.UsageRange
import com.recentr.feature.insights.achievements.AchievementsScreen
import com.recentr.feature.insights.achievements.AchievementsViewModel
import com.recentr.feature.insights.achievements.BadgeSheet
import com.recentr.feature.insights.achievements.BadgeUnlockedScreen
import com.recentr.feature.insights.achievements.MilestonesScreen
import com.recentr.feature.insights.achievements.StreakScreen
import com.recentr.feature.insights.app.AppDetailScreen
import com.recentr.feature.insights.app.AppDetailViewModel
import com.recentr.feature.insights.goal.DailyGoalScreen
import com.recentr.feature.insights.goal.DailyGoalViewModel
import com.recentr.feature.insights.overview.InsightsOverviewScreen
import com.recentr.feature.insights.overview.InsightsOverviewViewModel
import com.recentr.feature.insights.overview.OverviewActions
import com.recentr.feature.insights.pickups.PickupsScreen
import com.recentr.feature.insights.pickups.PickupsViewModel
import com.recentr.feature.insights.report.MonthlyReportScreen
import com.recentr.feature.insights.report.MonthlyReportViewModel
import com.recentr.feature.insights.share.ShareScreen
import com.recentr.feature.insights.share.ShareSubject
import java.time.LocalDate
import kotlinx.serialization.Serializable

/** 100 · App detail over a day, week or month: a [RangeUnit] name and its first day (ISO). */
@Serializable internal data class AppDetailRoute(val packageName: String, val unit: String, val start: String) {
    constructor(packageName: String, range: UsageRange) : this(packageName, range.unit.name, range.start.toString())

    val range: UsageRange get() = UsageRange.of(RangeUnit.valueOf(unit), LocalDate.parse(start))
}

/** 101 · Pickups over a day, week or month. */
@Serializable internal data class PickupsRoute(val unit: String, val start: String) {
    constructor(range: UsageRange) : this(range.unit.name, range.start.toString())

    val range: UsageRange get() = UsageRange.of(RangeUnit.valueOf(unit), LocalDate.parse(start))
}

/** 104 · Daily goal. */
@Serializable internal data object DailyGoalRoute

/** 103 · Monthly report. */
@Serializable internal data object MonthlyReportRoute

/** 105 · Achievements. */
@Serializable internal data object AchievementsRoute

/** 106 · Unlocked: [fresh] right after it was earned ("New badge"), else from the grid. */
@Serializable internal data class BadgeUnlockedRoute(val achievement: String, val fresh: Boolean)

/** 108 · Streak. */
@Serializable internal data object StreakRoute

/** 109 · Milestones. */
@Serializable internal data object MilestonesRoute

/** 110 · Share: the badge [achievement], or the monthly report when null. */
@Serializable internal data class ShareRoute(val achievement: String? = null)

/**
 * Insights tab content (Figma 99 / 102 under a tab header), on today's Day tab at first. Drawn
 * under the floating tab bar: [contentPadding] (bottom) is kept clear for it. [openPeriod] (Home's
 * usage card) switches to that period, then [onPeriodOpened] clears it.
 */
@Composable
fun InsightsOverviewTab(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    openPeriod: UsagePeriod? = null,
    onPeriodOpened: () -> Unit = {},
) {
    val viewModel: InsightsOverviewViewModel = viewModel(
        factory = InsightsOverviewViewModel.factory(UsageRange.of(RangeUnit.Day, LocalDate.now())),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(openPeriod) {
        if (openPeriod != null) {
            viewModel.show(openPeriod)
            onPeriodOpened()
        }
    }
    val open: (Any) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
    val context = LocalContext.current
    InsightsOverviewScreen(
        state = state,
        actions = OverviewActions(
            onSelectTab = viewModel::selectTab,
            onPrevious = viewModel::previous,
            onNext = viewModel::next,
            onOpenApp = { packageName, range -> open(AppDetailRoute(packageName, range)) },
            onSeeMoreApps = viewModel::seeMoreApps,
            onOpenDailyGoal = { open(DailyGoalRoute) },
            onOpenPickups = { open(PickupsRoute(it)) },
            onOpenStreak = { open(StreakRoute) },
            onOpenAchievements = { open(AchievementsRoute) },
            onOpenReport = { open(MonthlyReportRoute) },
            onConnectScreenTime = { UsageAccess.openSettings(context) },
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

/** Opens one app's insights over [period] (Home "Most used"). */
fun NavHostController.navigateToAppInsights(packageName: String, period: UsagePeriod) {
    navigate(AppDetailRoute(packageName, UsageRange.of(period, LocalDate.now()))) { launchSingleTop = true }
}

/** Opens the streak screen (Home's streak flame). */
fun NavHostController.navigateToStreak() {
    navigate(StreakRoute) { launchSingleTop = true }
}

/**
 * Insights & Achievements destinations (Figma 100–110), drawn above the tab bar.
 *
 * @param onCreateRule opens the rule editor for a new rule of a type, with apps preselected.
 * @param onEditRule opens the editor of an existing rule.
 */
fun NavGraphBuilder.insightsDestinations(
    navController: NavHostController,
    onCreateRule: (RuleType, List<UsageApp>) -> Unit,
    onEditRule: (ruleId: String) -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    fun open(route: Any) = navController.navigate(route) { launchSingleTop = true }

    composable<AppDetailRoute> { entry ->
        val route = entry.toRoute<AppDetailRoute>()
        val viewModel: AppDetailViewModel = viewModel(factory = AppDetailViewModel.factory(route.packageName, route.range))
        val state by viewModel.state.collectAsStateWithLifecycle()
        AppDetailScreen(
            state = state,
            onBack = back,
            onLimit = { app, ruleId -> if (ruleId != null) onEditRule(ruleId) else onCreateRule(RuleType.Limit, listOf(app)) },
            onBlock = { onCreateRule(RuleType.Lock, listOf(it)) },
        )
    }
    composable<PickupsRoute> { entry ->
        val route = entry.toRoute<PickupsRoute>()
        val viewModel: PickupsViewModel = viewModel(factory = PickupsViewModel.factory(route.range))
        val state by viewModel.state.collectAsStateWithLifecycle()
        PickupsScreen(state = state, onBack = back, onStartBedtime = { onCreateRule(RuleType.Bedtime, emptyList()) })
    }
    composable<DailyGoalRoute> {
        val viewModel: DailyGoalViewModel = viewModel(factory = DailyGoalViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        DailyGoalScreen(state = state, onBack = back, onSaveGoal = viewModel::saveGoal)
    }
    composable<MonthlyReportRoute> {
        val viewModel: MonthlyReportViewModel = viewModel(factory = MonthlyReportViewModel.Factory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        MonthlyReportScreen(state = state, onClose = back, onShare = { open(ShareRoute()) })
    }
    composable<AchievementsRoute> { AchievementsDestination(navController) }
    composable<BadgeUnlockedRoute> { entry ->
        val route = entry.toRoute<BadgeUnlockedRoute>()
        val achievement = Achievement.valueOf(route.achievement)
        val viewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory)
        val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
        BadgeUnlockedScreen(
            achievement = achievement,
            earnedOn = snapshot?.badges?.firstOrNull { it.achievement == achievement }?.earnedOn,
            fresh = route.fresh,
            onNice = back,
            onShare = { open(ShareRoute(achievement.name)) },
        )
    }
    composable<StreakRoute> {
        val viewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory)
        val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
        StreakScreen(snapshot = snapshot, onBack = back)
    }
    composable<MilestonesRoute> {
        val viewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory)
        val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
        MilestonesScreen(snapshot = snapshot, onBack = back)
    }
    composable<ShareRoute> { entry ->
        val route = entry.toRoute<ShareRoute>()
        val badge = route.achievement?.let(Achievement::valueOf)
        val subject = if (badge == null) {
            val viewModel: MonthlyReportViewModel = viewModel(factory = MonthlyReportViewModel.Factory)
            val state by viewModel.state.collectAsStateWithLifecycle()
            state.report?.let { ShareSubject.Month(it) }
        } else {
            val viewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory)
            val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
            snapshot?.let { ShareSubject.Badge(badge, it.level) }
        }
        ShareScreen(subject = subject, onClose = back)
    }
}

/**
 * 105 · Achievements, with 107's badge sheet over it. Badges earned since the last visit are
 * celebrated first (106, the newest one), then all count as seen.
 */
@Composable
private fun AchievementsDestination(navController: NavHostController) {
    val viewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory)
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var sheetBadge by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(snapshot?.uncelebrated) {
        val fresh = snapshot?.uncelebrated.orEmpty()
        if (fresh.isNotEmpty()) {
            viewModel.markCelebrated(fresh)
            navController.navigate(BadgeUnlockedRoute(fresh.first().name, fresh = true)) { launchSingleTop = true }
        }
    }
    AchievementsScreen(
        snapshot = snapshot,
        onBack = { navController.popBackStack() },
        onOpenBadge = { badge ->
            if (badge.earned) {
                navController.navigate(BadgeUnlockedRoute(badge.achievement.name, fresh = false)) { launchSingleTop = true }
            } else {
                sheetBadge = badge.achievement.name
            }
        },
        onOpenMilestones = { navController.navigate(MilestonesRoute) { launchSingleTop = true } },
    )
    val selected = sheetBadge?.let { name -> snapshot?.badges?.firstOrNull { it.achievement.name == name } }
    if (selected != null) {
        BadgeSheet(badge = selected, onDismissRequest = { sheetBadge = null })
    }
}
