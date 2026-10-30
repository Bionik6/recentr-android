package com.recentr.feature.rules.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.data.AppUnlocks
import com.recentr.core.data.RuleApps
import com.recentr.core.designsystem.format.countdown
import com.recentr.core.designsystem.illustration.InstalledAppIcon
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.AppId
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleType
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.components.Gap
import com.recentr.feature.rules.ui.components.RulesSheet
import com.recentr.feature.rules.ui.components.SheetBody

/**
 * "Apps" (the rule page's "Blocked" tile): the rule's apps, the ones it keeps closed, the ones
 * unlocked past it for now with the time their unlock has left, and the ones always allowed,
 * which it never holds. [now] ticks every second and [unlocks] is the app blocker's live state, so
 * an app whose unlock ends goes back with the others.
 */
@Composable
internal fun RuleAppsSheet(rule: Rule, now: Long, unlocks: AppUnlocks, alwaysAllowed: AlwaysAllowed, onDismiss: () -> Unit) {
    RulesSheet(onDismissRequest = onDismiss) {
        SheetBody(figmaHeight = null, topGap = 18.dp, wrapContent = true, horizontalAlignment = Alignment.Start, bottomBar = {}) {
            val holding = rule.isHolding(now)
            RuleAppsContent(
                subtitle = stringResource(if (holding) R.string.rules_apps_blocking else R.string.rules_apps_not_blocking),
                blockedTitle = stringResource(if (holding) R.string.rules_apps_blocked else R.string.rules_apps_blocked_when_on),
                apps = unlocks.appsOf(rule, now, alwaysAllowed),
                now = now,
            )
        }
    }
}

/**
 * The same sheet for a focus session's apps on hold (Figma 92): every one of them, and those
 * always allowed apart. An unlock doesn't get past a session, so there's no "Unlocked for now".
 */
@Composable
internal fun FocusAppsSheet(apps: List<UsageApp>, now: Long, alwaysAllowed: AlwaysAllowed, onDismiss: () -> Unit) {
    RulesSheet(onDismissRequest = onDismiss) {
        SheetBody(figmaHeight = null, topGap = 18.dp, wrapContent = true, horizontalAlignment = Alignment.Start, bottomBar = {}) {
            val (allowed, held) = apps.partition { alwaysAllowed.allows(it.packageName, now) }
            RuleAppsContent(
                subtitle = stringResource(R.string.focus_apps_subtitle),
                blockedTitle = stringResource(R.string.focus_apps_on_hold),
                apps = RuleApps(blocked = held, unlocked = emptyList(), allowed = allowed),
                now = now,
                showsUnlocked = false,
            )
        }
    }
}

@Composable
private fun ColumnScope.RuleAppsContent(subtitle: String, blockedTitle: String, apps: RuleApps, now: Long, showsUnlocked: Boolean = true) {
    val colors = RecentrTheme.colors
    Text(
        stringResource(R.string.rules_apps_title),
        style = RecentrTheme.typography.title1,
        color = colors.textPrimary,
        modifier = Modifier.semantics { heading() },
    )
    Gap(2.dp)
    Text(subtitle, style = RecentrTheme.typography.bodyRegular, color = colors.textSecondary)
    Gap(16.dp)
    // Scrolls when the rule has many apps; the sheet otherwise hugs them.
    Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
        SectionHeader(blockedTitle, apps.blocked.size)
        AppsCard(empty = stringResource(R.string.rules_apps_none_blocked).takeIf { apps.blocked.isEmpty() }) {
            apps.blocked.forEach { app -> key(app.packageName) { AppRow(app, trailing = null) } }
        }
        if (showsUnlocked) {
            Gap(18.dp)
            SectionHeader(stringResource(R.string.rules_apps_unlocked), apps.unlocked.size)
            AppsCard(empty = stringResource(R.string.rules_apps_none_unlocked).takeIf { apps.unlocked.isEmpty() }) {
                apps.unlocked.forEach { (app, until) ->
                    key(app.packageName) { AppRow(app, trailing = stringResource(R.string.rules_value_left, countdown(until - now))) }
                }
            }
        }
        if (apps.allowed.isNotEmpty()) {
            Gap(18.dp)
            SectionHeader(stringResource(R.string.rules_apps_always_allowed), apps.allowed.size)
            AppsCard(empty = null) {
                apps.allowed.forEach { app -> key(app.packageName) { AppRow(app, trailing = null) } }
            }
        }
    }
}

/** "Blocked · 9". */
@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(Modifier.padding(start = 4.dp, bottom = 8.dp).semantics(mergeDescendants = true) { heading() }) {
        Text(title, style = RecentrTheme.typography.headline, color = RecentrTheme.colors.textSecondary)
        Text(stringResource(R.string.rules_apps_section_count, count), style = RecentrTheme.typography.headline, color = RecentrTheme.colors.textTertiary)
    }
}

/** The sunken group the rows sit in, or [empty] when there are none. */
@Composable
private fun AppsCard(empty: String?, rows: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(RecentrTheme.colors.bgSunken)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (empty != null) {
            Text(
                empty,
                style = RecentrTheme.typography.bodyRegular,
                color = RecentrTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            )
        } else {
            rows()
        }
    }
}

/** An app: its icon and name, and how long its unlock has left ([trailing]). */
@Composable
private fun AppRow(app: UsageApp, trailing: String?) {
    val colors = RecentrTheme.colors
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstalledAppIcon(app.packageName, size = 32.dp)
        Text(
            app.label,
            style = RecentrTheme.typography.bodyMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, style = RecentrTheme.typography.captionMedium.copy(fontFeatureSettings = "tnum"), color = colors.textSecondary, maxLines = 1)
        }
    }
}

@Preview(widthDp = 402)
@Composable
private fun RuleAppsPreview() {
    val apps = listOf(AppId.Instagram, AppId.TikTok, AppId.X, AppId.YouTube).map { UsageApp.of(it) }
    val rule = Rule("l", RuleType.Lock, "Socials", apps = apps, activeSince = 0L)
    val unlocks = AppUnlocks().open(apps[2].packageName, listOf(rule), 272_000L)
    RecentrTheme {
        Column(Modifier.background(RecentrTheme.colors.bgSurface).padding(24.dp)) {
            RuleAppsContent(
                subtitle = stringResource(R.string.rules_apps_blocking),
                blockedTitle = stringResource(R.string.rules_apps_blocked),
                apps = unlocks.appsOf(rule, 0L, AlwaysAllowed(listOf(apps[3]))),
                now = 0L,
            )
        }
    }
}
