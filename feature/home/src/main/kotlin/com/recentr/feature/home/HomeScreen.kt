package com.recentr.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.data.DemoScreenTimeRepository
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.home.ui.components.HeroPage
import com.recentr.feature.home.ui.components.HomeHero
import com.recentr.feature.home.ui.components.HomeTiles
import com.recentr.feature.home.ui.components.HomeTodayCard
import com.recentr.feature.home.ui.components.MostUsedSection
import com.recentr.feature.home.ui.components.SkeletonCard
import com.recentr.feature.home.ui.components.WeekCard
import com.recentr.feature.home.ui.formatDuration
import com.recentr.feature.home.ui.labelRes
import com.recentr.feature.home.ui.sheets.GoalSheet
import com.recentr.feature.home.ui.sheets.OfflineInfoSheet
import com.recentr.feature.home.ui.sheets.PeriodSheet
import com.recentr.feature.home.ui.sheets.ScreenTimeInfoSheet
import kotlin.math.absoluteValue

/** Sheets presented over Home (Figma 24 · 25 · 26 · 28). */
internal enum class HomeSheet { ScreenTimeInfo, OfflineInfo, Period, Goal }

/** Figma vertical rhythm of the first screen (402 × 874 frame). */
private object HomeMetrics {
    val headerTop = 8.dp
    val headerToHero = 48.dp
    val heroToCard = 83.dp
    val cardToTiles = 11.dp
    val tilesToCta = 17.dp
    val ctaToTabBar = 8.dp
    val tilesToMostUsed = 24.dp
    /** Mascot instance: 108.8 × 136, 6dp in from the card's right edge, 10dp over its top. */
    val mascotWidth = 108.8.dp
    val mascotEndInset = 6.dp
    val mascotOverlap = 10.dp
}

/**
 * Home tab (Figma "Home & Insight" 22–29). Scrollable: the sky, header, hero pager, usage card and
 * tiles fill the first screen above the pinned "Start blocking" CTA; "Most used" follows below.
 */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onSelectPeriod: (UsagePeriod) -> Unit,
    onRefresh: () -> Unit,
    onSaveGoal: (Int) -> Unit,
    onToggleWellSpent: (String) -> Unit,
    onSelectGrouping: (UsageGrouping) -> Unit,
    onStartBlocking: () -> Unit,
    onOpenRules: () -> Unit,
    modifier: Modifier = Modifier,
    initialSheet: HomeSheet? = null,
    pagerState: PagerState = rememberPagerState { HeroPage.entries.size },
) {
    val colors = RecentrTheme.colors
    var sheet by rememberSaveable { mutableStateOf(initialSheet) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    // The frosted status-bar strip fades in once the header has scrolled under it (Figma 23).
    val topBlurAlpha by remember {
        derivedStateOf {
            val start = with(density) { (HomeMetrics.headerTop + 40.dp).toPx() }
            val range = with(density) { 32.dp.toPx() }
            ((scrollState.value - start) / range).coerceIn(0f, 1f)
        }
    }
    val contentUnderStatusBar by remember { derivedStateOf { topBlurAlpha > 0.5f } }
    SystemBarsAppearance(lightIcons = sheet != null || !contentUnderStatusBar)

    val summary = state.summary
    val showSkeleton = state.showSkeleton
    // 0 on the Quality page, 1 one page away: drives the mascot fade and the focus bars.
    val qualityDistance by remember {
        derivedStateOf {
            ((pagerState.currentPage - HeroPage.Quality.ordinal) + pagerState.currentPageOffsetFraction)
                .absoluteValue.coerceAtMost(1f)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(colors.bgPage)) {
        val ctaReserve = contentPadding.calculateBottomPadding() + HomeMetrics.ctaToTabBar + RecentrTheme.sizes.button
        val firstScreenMinHeight = maxHeight - ctaReserve - HomeMetrics.tilesToCta
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .paint(
                    painter = painterResource(Backdrop.Sky.drawable),
                    sizeToIntrinsics = false,
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.FillWidth,
                ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = firstScreenMinHeight)
                    .statusBarsPadding()
                    .padding(top = HomeMetrics.headerTop),
            ) {
                HomeHeader(
                    period = state.period,
                    syncing = state.isSyncing,
                    onChoosePeriod = { sheet = HomeSheet.Period },
                    onRefresh = onRefresh,
                    onMore = { sheet = HomeSheet.Goal },
                    modifier = Modifier.padding(horizontal = RecentrTheme.spacing.screenGutter),
                )
                Spacer(Modifier.height(HomeMetrics.headerToHero))
                Spacer(Modifier.weight(1f))
                HomeHero(
                    pagerState = pagerState,
                    period = state.period,
                    summary = summary,
                    syncing = showSkeleton,
                    dailyGoalMinutes = state.dailyGoalMinutes,
                    onScreenTimeInfo = { sheet = HomeSheet.ScreenTimeInfo },
                    onOfflineInfo = { sheet = HomeSheet.OfflineInfo },
                    onEditGoal = { sheet = HomeSheet.Goal },
                )
                Spacer(Modifier.height(HomeMetrics.heroToCard))
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().padding(horizontal = RecentrTheme.spacing.screenGutter)) {
                    UsageCard(
                        state = state,
                        focusHighlight = 1f - qualityDistance,
                    )
                    Mascot(
                        mood = when {
                            showSkeleton -> MascotMood.Dozing
                            pagerState.currentPage == HeroPage.Offline.ordinal -> MascotMood.Dozing
                            summary != null && !summary.isMultiDay && summary.totalMinutes > state.dailyGoalMinutes -> MascotMood.Impatient
                            else -> MascotMood.Calm
                        },
                        width = HomeMetrics.mascotWidth,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(
                                x = -HomeMetrics.mascotEndInset,
                                y = -(HomeMetrics.mascotWidth * 1.25f) + HomeMetrics.mascotOverlap,
                            )
                            .graphicsLayer { alpha = qualityDistance },
                    )
                }
                Spacer(Modifier.height(HomeMetrics.cardToTiles))
                HomeTiles(
                    streakDays = summary?.streakDays,
                    nextRule = state.nextRule,
                    activeSession = state.activeSession,
                    loading = showSkeleton,
                    onOpenRules = onOpenRules,
                    modifier = Modifier.padding(horizontal = RecentrTheme.spacing.screenGutter),
                )
            }
            if (summary != null) {
                // Previous numbers stay readable but dimmed while a re-sync runs.
                val listAlpha by animateFloatAsState(if (showSkeleton) 0.4f else 1f, label = "mostUsedAlpha")
                Spacer(Modifier.height(HomeMetrics.tilesToMostUsed))
                MostUsedSection(
                    usage = summary.mostUsed,
                    grouping = state.grouping,
                    wellSpent = state.wellSpent,
                    onToggleWellSpent = onToggleWellSpent,
                    onSelectGrouping = onSelectGrouping,
                    modifier = Modifier
                        .padding(horizontal = RecentrTheme.spacing.screenGutter)
                        .graphicsLayer { alpha = listAlpha },
                )
            }
            Spacer(Modifier.height(ctaReserve + HomeMetrics.tilesToMostUsed))
        }

        // Figma 23 "Top blur": frosted strip behind the status bar once content scrolls under it.
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = topBlurAlpha }
                .background(colors.alphaWhite80)
                .statusBarsPadding()
                .height(6.dp),
        )

        // Scrolled content fades into the page grey in the gap above the pinned CTA and stays
        // hidden behind it and the floating tab bar (the first-screen tiles end above the fade).
        val fadeHeight = HomeMetrics.tilesToCta - 1.dp
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ctaReserve + fadeHeight)
                .background(
                    Brush.verticalGradient(
                        0f to colors.bgPage.copy(alpha = 0f),
                        fadeHeight / (ctaReserve + fadeHeight) to colors.bgPage,
                        1f to colors.bgPage,
                    ),
                ),
        )

        RecentrButton(
            text = stringResource(R.string.home_start_blocking),
            onClick = onStartBlocking,
            style = RecentrButtonStyle.Brand,
            icon = RecentrIcon.Plus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + HomeMetrics.ctaToTabBar)
                .padding(horizontal = RecentrTheme.spacing.screenGutter)
                .fillMaxWidth(),
        )
    }

    when (sheet) {
        HomeSheet.ScreenTimeInfo -> if (summary != null) {
            ScreenTimeInfoSheet(
                value = formatDuration(summary.dailyAverageMinutes),
                dailyAverage = summary.isMultiDay,
                onDismissRequest = { sheet = null },
            )
        }
        HomeSheet.OfflineInfo -> if (summary != null) {
            OfflineInfoSheet(
                value = formatDuration(summary.longestOfflineMinutes),
                period = summary.period,
                onDismissRequest = { sheet = null },
            )
        }
        HomeSheet.Period -> PeriodSheet(
            selected = state.period,
            onSelect = onSelectPeriod,
            onDismissRequest = { sheet = null },
        )
        HomeSheet.Goal -> GoalSheet(
            currentGoalMinutes = state.dailyGoalMinutes,
            onSave = onSaveGoal,
            onDismissRequest = { sheet = null },
        )
        null -> Unit
    }
}

/** Wordmark · period chip · refresh · more (Figma "Header", 40dp high). */
@Composable
private fun HomeHeader(
    period: UsagePeriod,
    syncing: Boolean,
    onChoosePeriod: () -> Unit,
    onRefresh: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RecentrTheme.colors
    Row(
        modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_wordmark),
            style = RecentrTheme.typography.brandWordmark,
            color = colors.textOnDark,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        RecentrChip(
            label = stringResource(period.labelRes),
            style = RecentrChipStyle.OnSky,
            icon = RecentrIcon.ChevronDown,
            onClick = onChoosePeriod,
        )
        val rotation = if (syncing) {
            val transition = rememberInfiniteTransition(label = "sync")
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
                label = "syncRotation",
            )
        } else {
            null
        }
        RecentrIconButton(
            icon = RecentrIcon.Refresh,
            contentDescription = stringResource(R.string.home_refresh),
            onClick = onRefresh,
            style = RecentrIconButtonStyle.OnSky,
            enabled = !syncing,
            modifier = Modifier.graphicsLayer { rotationZ = rotation?.value ?: 0f },
        )
        RecentrIconButton(
            icon = RecentrIcon.More,
            contentDescription = stringResource(R.string.home_more),
            onClick = onMore,
            style = RecentrIconButtonStyle.OnSky,
        )
    }
}

/** Today card (single day), Week card (week / month) or the syncing skeleton. */
@Composable
private fun UsageCard(state: HomeUiState, focusHighlight: Float) {
    val summary = state.summary
    val kind = when {
        state.showSkeleton || summary == null -> 0
        summary.isMultiDay -> 2
        else -> 1
    }
    AnimatedContent(
        targetState = kind,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
        label = "usageCard",
    ) { target ->
        when {
            target == 0 || summary == null -> SkeletonCard(Modifier.fillMaxWidth())
            target == 2 -> WeekCard(summary, todayIndex = state.todayIndex, modifier = Modifier.fillMaxWidth())
            else -> HomeTodayCard(summary, focusHighlight, Modifier.fillMaxWidth())
        }
    }
}

private val PreviewRepository = DemoScreenTimeRepository()

private fun previewState(period: UsagePeriod = UsagePeriod.Today, syncing: Boolean = false) = HomeUiState(
    period = period,
    summary = PreviewRepository.snapshot(period),
    isSyncing = syncing,
    dailyGoalMinutes = 4 * 60,
    nextRule = NextRuleUi("work-hours", "Work Hours", RuleType.Session, 9 * 60, null),
)

@Composable
private fun HomePreviewHost(state: HomeUiState, page: HeroPage = HeroPage.ScreenTime) {
    RecentrTheme {
        HomeScreen(
            state = state,
            contentPadding = PaddingValues(bottom = 96.dp),
            onSelectPeriod = {},
            onRefresh = {},
            onSaveGoal = {},
            onToggleWellSpent = {},
            onSelectGrouping = {},
            onStartBlocking = {},
            onOpenRules = {},
            pagerState = rememberPagerState(initialPage = page.ordinal) { HeroPage.entries.size },
        )
    }
}

@Preview(widthDp = 411, heightDp = 923)
@Composable
private fun HomeScreenTimePreview() = HomePreviewHost(previewState())

@Preview(widthDp = 411, heightDp = 923)
@Composable
private fun HomeQualityPreview() = HomePreviewHost(previewState(), HeroPage.Quality)

@Preview(widthDp = 411, heightDp = 923)
@Composable
private fun HomeWeekPreview() = HomePreviewHost(previewState(UsagePeriod.ThisWeek))

@Preview(widthDp = 411, heightDp = 923)
@Composable
private fun HomeSyncingPreview() = HomePreviewHost(previewState(syncing = true))
