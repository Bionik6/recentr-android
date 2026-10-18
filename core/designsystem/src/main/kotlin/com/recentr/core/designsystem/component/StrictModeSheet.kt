package com.recentr.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.R
import com.recentr.core.designsystem.icon.RecentrIcon
import com.recentr.core.designsystem.icon.RecentrIconImage
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme

/** A choice of the Strict mode sheet (Figma 32 "Option"): its name and what it locks. */
@Immutable
data class StrictOption(val title: String, val description: String)

/**
 * Figma 32 · "Strict mode" (rules and Settings): the tangerine shield badge, Title/1, [subtitle],
 * one option row per choice (name, description, radio), the chosen option's description, then
 * the CTA. [confirmText] / [confirmStyle] give the CTA for the selected option (the tangerine
 * "Turn on Strict mode" by default); [onConfirm] runs once the sheet has slid down.
 */
@Composable
fun StrictModeSheet(
    subtitle: String,
    options: List<StrictOption>,
    initialIndex: Int,
    confirmText: @Composable (index: Int) -> String,
    onConfirm: (index: Int) -> Unit,
    onDismissRequest: () -> Unit,
    confirmStyle: (index: Int) -> RecentrButtonStyle = { RecentrButtonStyle.Strict },
) {
    RecentrSheet(onDismissRequest = onDismissRequest) { controller ->
        StrictModeContent(
            subtitle = subtitle,
            options = options,
            initialIndex = initialIndex,
            confirmText = confirmText,
            confirmStyle = confirmStyle,
            onConfirm = { index -> controller.close { onConfirm(index) } },
        )
    }
}

@Composable
private fun StrictModeContent(
    subtitle: String,
    options: List<StrictOption>,
    initialIndex: Int,
    confirmText: @Composable (index: Int) -> String,
    confirmStyle: (index: Int) -> RecentrButtonStyle,
    onConfirm: (index: Int) -> Unit,
) {
    val colors = RecentrTheme.colors
    var selected by rememberSaveable { mutableIntStateOf(initialIndex.coerceIn(0, options.lastIndex)) }
    Column(Modifier.fillMaxWidth()) {
        // The drag handle takes 21dp; Figma has 33dp from the sheet top to the badge.
        Column(
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(56.dp).clip(RecentrShapes.full).background(colors.alphaTangerine14), contentAlignment = Alignment.Center) {
                RecentrIconImage(RecentrIcon.Shield, tint = colors.accentTangerine, size = 31.2.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.ds_strict_title), style = RecentrTheme.typography.title1, color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = RecentrTheme.typography.bodyRegular,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, option ->
                    StrictOptionRow(option, selected = index == selected, onClick = { selected = index })
                }
            }
            Spacer(Modifier.height(14.dp))
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "strictSummary",
            ) { index ->
                Text(
                    options[index].description,
                    style = RecentrTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        RecentrButton(
            text = confirmText(selected),
            onClick = { onConfirm(selected) },
            style = confirmStyle(selected),
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
        )
    }
}

/** Figma 32 "Option": white row, 1dp subtle border, name + description and the radio. */
@Composable
private fun StrictOptionRow(option: StrictOption, selected: Boolean, onClick: () -> Unit) {
    val colors = RecentrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 15.dp, end = 17.dp, top = 13.dp, bottom = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(option.title, style = RecentrTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(option.description, style = RecentrTheme.typography.captionRegular, color = colors.textSecondary)
        }
        StrictRadio(selected)
    }
}

/** 24dp radio: a 2dp `border/subtle` ring, or a sky ring around a sky dot when selected. */
@Composable
private fun StrictRadio(selected: Boolean) {
    val colors = RecentrTheme.colors
    Canvas(Modifier.size(24.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(
            color = if (selected) colors.brandSky else colors.borderSubtle,
            radius = 11.dp.toPx(),
            center = center,
            style = Stroke(2.dp.toPx()),
        )
        if (selected) drawCircle(colors.brandSky, radius = 6.dp.toPx(), center = center)
    }
}

@Preview(widthDp = 402)
@Composable
private fun StrictModeSheetPreview() {
    RecentrTheme {
        Column(Modifier.background(RecentrTheme.colors.bgSurface)) {
            StrictModeContent(
                subtitle = "Lock this rule so you can’t change it on impulse.",
                options = listOf(
                    StrictOption("Start time", "Locks only when you start the session."),
                    StrictOption("During", "No edits, no deletes and no unlocks while the session runs."),
                    StrictOption("Always", "Keeps this rule locked until you manually turn it off."),
                ),
                initialIndex = 1,
                confirmText = { "Turn on Strict mode" },
                confirmStyle = { RecentrButtonStyle.Strict },
                onConfirm = {},
            )
        }
    }
}
