package com.recentr.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.AddCard
import com.recentr.core.designsystem.component.AppRow
import com.recentr.core.designsystem.component.HourlyUsageChart
import com.recentr.core.designsystem.component.MiniTile
import com.recentr.core.designsystem.component.OnboardingHeader
import com.recentr.core.designsystem.component.OptionRow
import com.recentr.core.designsystem.component.OptionTrailing
import com.recentr.core.designsystem.component.RecentrAlertDialog
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.RecentrButtonStyle
import com.recentr.core.designsystem.component.RecentrCheckbox
import com.recentr.core.designsystem.component.RecentrChip
import com.recentr.core.designsystem.component.RecentrChipStyle
import com.recentr.core.designsystem.component.RecentrIconButton
import com.recentr.core.designsystem.component.RecentrIconButtonStyle
import com.recentr.core.designsystem.component.RecentrRadioButton
import com.recentr.core.designsystem.component.RecentrSegmented
import com.recentr.core.designsystem.component.RecentrTab
import com.recentr.core.designsystem.component.RecentrTabBar
import com.recentr.core.designsystem.component.RecentrTextField
import com.recentr.core.designsystem.component.RecentrToggle
import com.recentr.core.designsystem.component.ReviewCard
import com.recentr.core.designsystem.component.OverlappingAppIcons
import com.recentr.core.designsystem.component.RuleCard
import com.recentr.core.designsystem.component.RuleTile
import com.recentr.core.designsystem.component.SettingRow
import com.recentr.core.designsystem.component.SettingToggleRow
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.component.TemplateCard
import com.recentr.core.designsystem.component.TemplateColor
import com.recentr.core.designsystem.component.TodayCard
import com.recentr.core.designsystem.component.TodayStat
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.illustration.AppIcon
import com.recentr.core.designsystem.illustration.DemoApp
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.illustration.Sticker
import com.recentr.core.designsystem.illustration.StickerImage
import com.recentr.core.designsystem.theme.RecentrGradients
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme

/** Living catalogue of the design system — every token and component on one scrollable page. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignSystemCatalog(modifier: Modifier = Modifier) {
    val colors = RecentrTheme.colors
    val type = RecentrTheme.typography
    var tab by remember { mutableStateOf(RecentrTab.Home) }
    var toggle by remember { mutableStateOf(true) }
    var checked by remember { mutableStateOf(true) }
    var segment by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var option by remember { mutableStateOf(true) }
    var alert by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(colors.bgPage)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Recentr", style = type.brandWordmark, color = colors.textPrimary)
            Text("Foundations", style = type.displayLarge)
            Section("Colour")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    colors.bgPage, colors.bgSurface, colors.bgSunken, colors.textPrimary, colors.textSecondary,
                    colors.textTertiary, colors.borderSubtle, colors.borderDashed, colors.brandSky, colors.brandSkyLight,
                    colors.brandSkyDeep, colors.accentNote, colors.accentTangerine, colors.accentNoteBlue,
                    colors.accentNoteMint, colors.accentNotePink, colors.feedbackSuccess, colors.feedbackDanger,
                    colors.auroraMint, colors.auroraBlue, colors.auroraPink, colors.fillSelection, colors.fillGrabber,
                ).forEach {
                    Box(Modifier.size(36.dp).clip(RecentrShapes.sm).background(it))
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Brush>(
                    RecentrGradients.brandCta, RecentrGradients.brandDeep, RecentrGradients.success,
                    RecentrGradients.strict, RecentrGradients.shield,
                ).forEach { Box(Modifier.size(width = 96.dp, height = 56.dp).clip(RecentrShapes.xl).background(it)) }
            }
            Section("Typography")
            Text("Focused", style = type.displayHero)
            Text("4h20", style = type.displayNumber)
            Text("Display Large", style = type.displayLarge)
            Text("Title 1", style = type.title1)
            Text("Title 2", style = type.title2)
            Text("Headline", style = type.headline)
            Text("Body regular — Offly adds a pause before the apps that pull you in.", style = type.bodyRegular)
            Text("Body medium", style = type.bodyMedium)
            Text("Caption regular", style = type.captionRegular)
            Text("Caption medium", style = type.captionMedium)
            Text("LABEL SMALL", style = type.labelSmall)
            Section("Icons")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RecentrIcon.entries.forEach { RecentrIconImage(it) }
            }
            Section("Stickers & mascot")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Sticker.entries.forEach { StickerImage(it, size = 64.dp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MascotMood.entries.forEach { Mascot(it, width = 90.dp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DemoApp.entries.forEach { AppIcon(it) } }

            Section("Core")
            RecentrButtonStyle.entries.forEach {
                RecentrButton("Continue", onClick = {}, style = it, modifier = Modifier.fillMaxWidth())
            }
            RecentrButton("Start blocking", onClick = { alert = true }, style = RecentrButtonStyle.Brand, icon = RecentrIcon.Plus, modifier = Modifier.fillMaxWidth())
            Row(
                Modifier.clip(RecentrShapes.xl).background(RecentrGradients.brandCta).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecentrIconButtonStyle.entries.forEach { RecentrIconButton(RecentrIcon.More, null, onClick = {}, style = it) }
            }
            Row(
                Modifier.clip(RecentrShapes.xl).background(RecentrGradients.brandCta).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RecentrChipStyle.entries.forEach { RecentrChip("All day", style = it) }
            }

            Section("App")
            RecentrTabBar(selected = tab, onSelect = { tab = it })
            RuleCard(
                title = "Work Hours",
                sticker = Sticker.Bolt,
                streak = 3,
                apps = { OverlappingAppIcons(listOf(DemoApp.Instagram, DemoApp.TikTok, DemoApp.X)) },
                value = "0s / 2h15",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            ) {
                RecentrChip("Upcoming")
                RecentrChip("9 AM – 5 PM · Weekdays", style = RecentrChipStyle.Outline)
            }
            TodayCard(
                stats = listOf(
                    TodayStat("Decrease", "▾ 6h48", colors.feedbackSuccess),
                    TodayStat("Pickups", "4"),
                    TodayStat("Notifications", "5"),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                HourlyUsageChart(listOf(0, 0, 0, 0, 0, 0, 0, 8, 20, 12, 28, 60, 24, 44, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0))
            }
            AddCard("Add a session", "E.g. “Block apps for the next 30 min”", onClick = {}, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniTile("Streak", "3 days", Sticker.Flame, Modifier.weight(1f))
                MiniTile("Next · 9:00 AM", "Work Hours", Sticker.Bolt, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RuleTile("Lock", Sticker.Lock, onClick = {}, modifier = Modifier.width(108.dp))
                StickyNote("Why I’m blocking", "Finish the thesis chapter before Friday. Phone stays in the drawer.", Modifier.width(220.dp))
            }

            Section("Onboarding")
            OnboardingHeader(progress = 0.35f, onBack = {}, onSkip = {})
            OptionRow("It affects my sleep", selected = false, onClick = {}, icon = RecentrIcon.Moon, modifier = Modifier.fillMaxWidth())
            OptionRow("It affects my sleep", selected = option, onClick = { option = !option }, icon = RecentrIcon.Moon, modifier = Modifier.fillMaxWidth())
            OptionRow("It affects my sleep", selected = option, onClick = { option = !option }, icon = RecentrIcon.Moon, trailing = OptionTrailing.Checkbox, modifier = Modifier.fillMaxWidth())
            OptionRow("5 – 7 hours", selected = option, onClick = { option = !option }, trailing = OptionTrailing.Radio, modifier = Modifier.fillMaxWidth())
            RecentrTextField(name, { name = it }, placeholder = "Your first name", modifier = Modifier.fillMaxWidth())
            ReviewCard("Got my evenings back", "I went from 7 hours a day to under 3. The little pause before opening X is genius.", "Jonah R.", "J", Modifier.width(250.dp))

            Section("Rules")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RecentrToggle(toggle, { toggle = it })
                RecentrCheckbox(checked, { checked = it })
                RecentrCheckbox(!checked, { checked = !it })
                RecentrRadioButton(checked, { checked = true })
                RecentrRadioButton(!checked, { checked = false })
            }
            SettingRow("Block", "3 apps", onClick = {}, icon = RecentrIcon.Apps, modifier = Modifier.fillMaxWidth())
            SettingToggleRow("Block", toggle, { toggle = it }, icon = RecentrIcon.Apps, modifier = Modifier.fillMaxWidth())
            RecentrSegmented(listOf("Before", "During", "Always"), segment, { segment = it }, Modifier.fillMaxWidth())
            AppRow(DemoApp.X, checked, { checked = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TemplateColor.entries.forEach {
                    TemplateCard("Deep Work", "Silence everything for focused time.", "All day", Sticker.Bolt, it, onClick = {}, modifier = Modifier.size(width = 165.dp, height = 210.dp))
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }
    if (alert) {
        RecentrAlertDialog(
            title = "“Recentr” would like to access Screen Time",
            body = "Recentr uses this to set limits and see your usage. Your data never leaves this phone.",
            primaryText = "Continue",
            secondaryText = "Don’t allow",
            onPrimary = { alert = false },
            onDismissRequest = { alert = false },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(title.uppercase(), style = RecentrTheme.typography.captionMedium, color = RecentrTheme.colors.textTertiary, modifier = Modifier.padding(top = 12.dp))
}
