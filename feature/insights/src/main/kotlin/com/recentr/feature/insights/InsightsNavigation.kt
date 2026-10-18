package com.recentr.feature.insights

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.recentr.core.model.Achievement
import com.recentr.core.model.AppId
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsagePeriod
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
import com.recentr.feature.insights.overview.InsightsTab
import com.recentr.feature.insights.overview.OverviewActions
import com.recentr.feature.insights.pickups.PickupsScreen
import com.recentr.feature.insights.pickups.PickupsViewModel
import com.recentr.feature.insights.report.MonthlyReportScreen
import com.recentr.feature.insights.report.MonthlyReportViewModel
import com.recentr.feature.insights.share.ShareScreen
import com.recentr.feature.insights.share.ShareSubject
import kotlinx.serialization.Serializable

/** 99 / 102 · Overview on [tab]; [day] is the day of the Day tab (Today or Yesterday). */
@Serializable internal data class InsightsRoute(val tab: String, val day: String = UsagePeriod.Today.name)

/** 100 · App detail over [period] (a [UsagePeriod] name). */
@Serializable internal data class AppDetailRoute(val packageName: String, val period: String)

/** 101 · Pickups over [period]. */
@Serializable internal data class PickupsRoute(val period: String)

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
 * Opens Insights on the tab matching a Home period: Today / Yesterday → Day, this week → Week,
 * this month → Month.
 */
fun NavHostController.navigateToInsights(period: UsagePeriod) {
    val tab = when (period) {
        UsagePeriod.Today, UsagePeriod.Yesterday -> InsightsTab.Day
        UsagePeriod.ThisWeek -> InsightsTab.Week
        UsagePeriod.ThisMonth -> InsightsTab.Month
    }
    val day = if (period == UsagePeriod.Yesterday) UsagePeriod.Yesterday else UsagePeriod.Today
    navigate(InsightsRoute(tab.name, day.name)) { launchSingleTop = true }
}

/** Opens one app's insights over [period] (Home "Most used"). */
fun NavHostController.navigateToAppInsights(packageName: String, period: UsagePeriod) {
    navigate(AppDetailRoute(packageName, period.name)) { launchSingleTop = true }
}

/** Opens the streak screen (Home streak tile). */
fun NavHostController.navigateToStreak() {
    navigate(StreakRoute) { launchSingleTop = true }
}

/**
 * Insights & Achievements destinations (Figma 99–110), drawn above the tab bar.
 *
 * @param onCreateRule opens the rule editor for a new rule of a type, with apps preselected.
 * @param onEditRule opens the editor of an existing rule.
 */
fun NavGraphBuilder.insightsDestinations(
    navController: NavHostController,
    onCreateRule: (RuleType, List<AppId>) -> Unit,
    onEditRule: (ruleId: String) -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    fun open(route: Any) = navController.navigate(route) { launchSingleTop = true }

    composable<InsightsRoute> { entry ->
        val route = entry.toRoute<InsightsRoute>()
        val viewModel: InsightsOverviewViewModel = viewModel(
            factory = InsightsOverviewViewModel.factory(InsightsTab.valueOf(route.tab), UsagePeriod.valueOf(route.day)),
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        InsightsOverviewScreen(
            state = state,
            actions = OverviewActions(
                onBack = back,
                onSelectTab = viewModel::selectTab,
                onOpenApp = { packageName, period -> open(AppDetailRoute(packageName, period.name)) },
                onOpenDailyGoal = { open(DailyGoalRoute) },
                onOpenPickups = { open(PickupsRoute(it.name)) },
                onOpenStreak = { open(StreakRoute) },
                onOpenAchievements = { open(AchievementsRoute) },
                onOpenReport = { open(MonthlyReportRoute) },
            ),
        )
    }
    composable<AppDetailRoute> { entry ->
        val route = entry.toRoute<AppDetailRoute>()
        val viewModel: AppDetailViewModel = viewModel(factory = AppDetailViewModel.factory(route.packageName, UsagePeriod.valueOf(route.period)))
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
        val viewModel: PickupsViewModel = viewModel(factory = PickupsViewModel.factory(UsagePeriod.valueOf(route.period)))
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
