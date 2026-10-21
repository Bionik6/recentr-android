package com.recentr.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.recentr.core.data.DemoScreenTimeRepository
import com.recentr.core.designsystem.component.BlockingOffBanner
import com.recentr.core.designsystem.component.DailyGoalSheet
import com.recentr.core.designsystem.component.RecentrAvatar
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.UsageAccessCard
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.illustration.Backdrop
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.motion.pressScale
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.designsystem.util.SystemBarsAppearance
import com.recentr.core.model.AvatarColor
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.home.ui.components.HeroPage
import com.recentr.feature.home.ui.components.HomeHero
import com.recentr.feature.home.ui.components.HomeTodayCard
import com.recentr.feature.home.ui.components.MostUsedSection
import com.recentr.feature.home.ui.components.NextRuleTile
import com.recentr.feature.home.ui.components.SkeletonCard
import com.recentr.feature.home.ui.components.WeekCard
import com.recentr.feature.home.ui.formatDuration
import com.recentr.feature.home.ui.sheets.OfflineInfoSheet
import com.recentr.feature.home.ui.sheets.ScreenTimeInfoSheet
import com.recentr.feature.home.ui.style
import kotlin.math.absoluteValue

/** Sheets presented over Home (Figma 24 · 25 · 28). */
internal enum class HomeSheet { ScreenTimeInfo, OfflineInfo, Goal }

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
 * next rule tile fill the first screen above the pinned "Start blocking" CTA; "Most used" follows
 * below. Without usage access ([HomeUiState.needsUsageAccess]) the usage card asks for it
 * ([onConnectScreenTime]) and no number is shown.
 */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onSaveGoal: (Int) -> Unit,
    onSelectGrouping: (UsageGrouping) -> Unit,
    onStartBlocking: () -> Unit,
    onOpenRules: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenInsights: () -> Unit = {},
    onOpenStreak: () -> Unit = onOpenRules,
    onOpenProfile: () -> Unit = {},
    onOpenApp: (packageName: String) -> Unit = {},
    blockingOn: Boolean = true,
    onTurnOnBlocking: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onConnectScreenTime: () -> Unit = {},
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
    val needsUsageAccess = state.needsUsageAccess
    // 0 on the Quality page, 1 one page away: drives the mascot fade and the focus bars.
    val qualityDistance by remember {
        derivedStateOf {
            ((pagerState.currentPage - HeroPage.Quality.ordinal) + pagerState.currentPageOffsetFraction)
                .absoluteValue.coerceAtMost(1f)
        }
    }

    // Pull to refresh: the spinner drops in under the status bar, over the sky.
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier.fillMaxSize().background(colors.bgPage),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.isRefreshing,
                containerColor = colors.bgSurface,
                color = colors.brandSky,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
            )
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
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
                        streakDays = summary?.streakDays?.takeUnless { needsUsageAccess },
                        avatarInitial = state.avatarInitial,
                        avatarColor = state.avatarColor,
                        onOpenStreak = onOpenStreak,
                        onOpenProfile = onOpenProfile,
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
                        onConnect = onConnectScreenTime.takeIf { needsUsageAccess },
                        // In front of the mascot rising from the usage card: its chip stays whole.
                        modifier = Modifier.zIndex(1f),
                    )
                    Spacer(Modifier.height(HomeMetrics.heroToCard))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth().padding(horizontal = RecentrTheme.spacing.screenGutter)) {
                        UsageCard(
                            state = state,
                            focusHighlight = 1f - qualityDistance,
                            onClick = onOpenInsights,
                            onConnect = onConnectScreenTime,
                        )
                        if (state.showMascot) {
                            Mascot(
                                mood = when {
                                    showSkeleton || needsUsageAccess -> MascotMood.Dozing
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
                    }
                    Spacer(Modifier.height(HomeMetrics.cardToTiles))
                    // Until the app blocker is on no rule holds an app, so the next one doesn't matter:
                    // its slot shows the same "Blocking is off" card as Rules.
                    if (blockingOn) {
                        NextRuleTile(
                            nextRule = state.nextRule,
                            activeSession = state.activeSession,
                            loading = showSkeleton,
                            onOpenRules = onOpenRules,
                            modifier = Modifier.padding(horizontal = RecentrTheme.spacing.screenGutter),
                        )
                    } else {
                        BlockingOffBanner(
                            onClick = onTurnOnBlocking,
                            modifier = Modifier.padding(horizontal = RecentrTheme.spacing.screenGutter),
                        )
                    }
                }
                if (summary != null && !needsUsageAccess) {
                    // Previous numbers stay readable but dimmed while a re-sync runs.
                    val listAlpha by animateFloatAsState(if (showSkeleton) 0.4f else 1f, label = "mostUsedAlpha")
                    Spacer(Modifier.height(HomeMetrics.tilesToMostUsed))
                    MostUsedSection(
                        usage = summary.mostUsed,
                        grouping = state.grouping,
                        onSelectGrouping = onSelectGrouping,
                        onOpenApp = onOpenApp,
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
        HomeSheet.Goal -> DailyGoalSheet(
            currentGoalMinutes = state.dailyGoalMinutes,
            onSave = onSaveGoal,
            onDismissRequest = { sheet = null },
            formatMinutes = { formatDuration(it) },
        )
        null -> Unit
    }
}

/** Wordmark · streak · avatar (Figma "Header", 40dp high). */
@Composable
private fun HomeHeader(
    streakDays: Int?,
    avatarInitial: String,
    avatarColor: AvatarColor,
    onOpenStreak: () -> Unit,
    onOpenProfile: () -> Unit,
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
        StreakButton(days = streakDays, onClick = onOpenStreak)
        RecentrAvatar(
            initial = avatarInitial,
            style = avatarColor.style,
            size = RecentrTheme.sizes.iconButton,
            ringWidth = 2.dp,
            ringColor = colors.textOnDark,
            textStyle = RecentrTheme.typography.headline,
            onClick = onOpenProfile,
            onClickLabel = stringResource(R.string.home_open_profile),
        )
    }
}

/** Streak flame and its length on the sky glass ("—" until the first sync); opens the streak. */
@Composable
private fun StreakButton(days: Int?, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .pressScale(interactionSource, pressedScale = 0.92f)
            .height(RecentrTheme.sizes.iconButton)
            .clip(RecentrShapes.full)
            .background(colors.alphaWhite24)
            .border(1.dp, colors.alphaWhite24, RecentrShapes.full)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 8.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerImage(Sticker.Flame, size = 26.dp, contentDescription = stringResource(R.string.home_tile_streak))
        Text(
            text = days?.toString() ?: stringResource(R.string.home_tile_placeholder),
            style = RecentrTheme.typography.headline,
            color = colors.textOnDark,
            maxLines = 1,
        )
    }
}

/**
 * Today card (single day), Week card (week / month) or the syncing skeleton; tapping opens Insights.
 * Without usage access, the card asking for it ([onConnect]).
 */
@Composable
private fun UsageCard(state: HomeUiState, focusHighlight: Float, onClick: () -> Unit, onConnect: () -> Unit) {
    val summary = state.summary
    val kind = when {
        state.showSkeleton || summary == null -> 0
        state.needsUsageAccess -> 3
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
            target == 3 -> UsageAccessCard(onConnect, Modifier.fillMaxWidth())
            target == 2 -> WeekCard(summary, todayIndex = state.todayIndex, modifier = Modifier.fillMaxWidth(), onClick = onClick)
            else -> HomeTodayCard(summary, focusHighlight, Modifier.fillMaxWidth(), onClick = onClick)
        }
    }
}

private val PreviewRepository = DemoScreenTimeRepository()

private fun previewState(period: UsagePeriod = UsagePeriod.Today, syncing: Boolean = false) = HomeUiState(
    period = period,
    summary = PreviewRepository.snapshot(period),
    isSyncing = syncing,
    dailyGoalMinutes = 4 * 60,
    nextRule = NextRuleUi("work-hours", "Work Hours", RuleType.Bedtime, 9 * 60, null),
)

@Composable
private fun HomePreviewHost(state: HomeUiState, page: HeroPage = HeroPage.ScreenTime) {
    RecentrTheme {
        HomeScreen(
            state = state,
            contentPadding = PaddingValues(bottom = 96.dp),
            onSaveGoal = {},
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

@Preview(widthDp = 411, heightDp = 923)
@Composable
private fun HomeNoUsageAccessPreview() = HomePreviewHost(previewState().copy(usageAccess = false))
