package com.recentr.feature.home.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.component.RecentrBottomSheet
import com.recentr.core.designsystem.component.RecentrButton
import com.recentr.core.designsystem.component.StickyNote
import com.recentr.core.designsystem.illustration.Mascot
import com.recentr.core.designsystem.illustration.MascotMood
import com.recentr.core.designsystem.motion.entrance
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.UsagePeriod
import com.recentr.feature.home.R
import kotlinx.coroutines.launch

/**
 * [RecentrBottomSheet] whose content can close it with the slide-down animation (`hide()`),
 * like tapping "Got it" on iOS. The design-system drag handle takes 21dp (8 + 5 + 8): sheets add
 * the remaining Figma gap themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.(hide: () -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hide: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
    }
    RecentrBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        content(hide)
    }
}

/** Bottom of every Home sheet: Figma 8 + 4 gap above the home indicator. */
@Composable
private fun ColumnScope.SheetBottomGap() {
    Spacer(Modifier.height(12.dp))
}

/** Figma 24 · "1h12 spent glued to your screen". */
@Composable
internal fun ScreenTimeInfoSheet(
    value: String,
    dailyAverage: Boolean,
    onDismissRequest: () -> Unit,
) {
    HomeBottomSheet(onDismissRequest) { hide ->
        ScreenTimeInfoContent(value, dailyAverage, onGotIt = hide)
    }
}

@Composable
private fun ScreenTimeInfoContent(value: String, dailyAverage: Boolean, onGotIt: () -> Unit) {
    InfoSheetContent(
        mood = MascotMood.Impatient,
        onGotIt = onGotIt,
        note = stringResource(R.string.home_info_glued_hints),
    ) {
        Text(
            text = stringResource(if (dailyAverage) R.string.home_info_glued_average else R.string.home_info_glued, value),
            style = RecentrTheme.typography.title1,
            color = RecentrTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Figma 25 · "8h offline". */
@Composable
internal fun OfflineInfoSheet(
    value: String,
    period: UsagePeriod,
    onDismissRequest: () -> Unit,
) {
    HomeBottomSheet(onDismissRequest) { hide ->
        OfflineInfoContent(value, period, onGotIt = hide)
    }
}

@Composable
private fun OfflineInfoContent(value: String, period: UsagePeriod, onGotIt: () -> Unit) {
    InfoSheetContent(
        mood = MascotMood.Dozing,
        onGotIt = onGotIt,
        note = stringResource(R.string.home_info_offline_hints),
    ) {
        Text(
            text = stringResource(R.string.home_info_offline, value),
            style = RecentrTheme.typography.displayLarge,
            color = RecentrTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                when (period) {
                    UsagePeriod.Today -> R.string.home_info_offline_today
                    UsagePeriod.Yesterday -> R.string.home_info_offline_yesterday
                    UsagePeriod.ThisWeek -> R.string.home_info_offline_week
                    UsagePeriod.ThisMonth -> R.string.home_info_offline_month
                },
            ),
            style = RecentrTheme.typography.bodyRegular,
            color = RecentrTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Shared layout of the info sheets: mascot, title, "Hints to reflect" note, "Got it". The mascot and note rise in. */
@Composable
private fun InfoSheetContent(
    mood: MascotMood,
    note: String,
    onGotIt: () -> Unit,
    title: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = RecentrTheme.spacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Mascot(mood, width = 128.dp, modifier = Modifier.entrance(from = DpOffset(0.dp, 30.dp)))
        Spacer(Modifier.height(10.dp))
        title()
        Spacer(Modifier.height(16.dp))
        StickyNote(
            title = stringResource(R.string.home_info_hints_title),
            body = note,
            modifier = Modifier
                .fillMaxWidth()
                .entrance(150, blur = false),
        )
        Spacer(Modifier.height(20.dp))
        RecentrButton(
            text = stringResource(R.string.home_info_got_it),
            onClick = onGotIt,
            modifier = Modifier.fillMaxWidth(),
        )
        SheetBottomGap()
    }
}

@Preview(widthDp = 402)
@Composable
private fun InfoSheetPreview() {
    RecentrTheme {
        Column(Modifier.fillMaxWidth()) { ScreenTimeInfoContent("1h12", dailyAverage = false, onGotIt = {}) }
    }
}

@Preview(widthDp = 402)
@Composable
private fun OfflineSheetPreview() {
    RecentrTheme {
        Column(Modifier.fillMaxWidth()) { OfflineInfoContent("8h", UsagePeriod.Today, onGotIt = {}) }
    }
}
