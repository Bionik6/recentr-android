package com.recentr.feature.rules.ui.components

import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.recentr.core.designsystem.component.RecentrBottomSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Hides the sheet with its slide-down animation, then runs the follow-up action. */
@Stable
internal class SheetCloser @OptIn(ExperimentalMaterial3Api::class) constructor(
    private val state: SheetState,
    private val scope: CoroutineScope,
    private val onDismissed: () -> Unit,
) {
    @OptIn(ExperimentalMaterial3Api::class)
    fun close(then: () -> Unit = {}) {
        scope.launch { state.hide() }.invokeOnCompletion {
            onDismissed()
            then()
        }
    }
}

/**
 * [RecentrBottomSheet] whose content gets a [SheetCloser] to dismiss with animation.
 * [onDismissRequest] is called once the sheet is gone (drag, scrim, back or [SheetCloser.close]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RulesSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.(SheetCloser) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val closer = remember(state, scope, onDismissRequest) { SheetCloser(state, scope, onDismissRequest) }
    RecentrBottomSheet(onDismissRequest = onDismissRequest, sheetState = state) {
        content(closer)
    }
}

/**
 * Body layout of a rules sheet, reproducing the Figma sheet metrics on top of the
 * [RecentrBottomSheet] drag handle (which already takes 21dp):
 *
 * * [figmaHeight] — total sheet height in the design (including the 34pt home indicator), or
 *   null for the near full-height sheets (they fill the available height).
 * * [topGap] — Figma gap between the grabber and the first element.
 *
 * The [content] scrolls when it does not fit; [bottomBar] (the CTA) stays pinned 10dp above the
 * navigation bar, like the Figma gap above the home indicator.
 */
@Composable
internal fun SheetBody(
    figmaHeight: Dp?,
    topGap: Dp,
    modifier: Modifier = Modifier,
    wrapContent: Boolean = false,
    /** Full-height sheets only: extra space left above the sheet (stacked sheet look). */
    stackedOffset: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    bottomBar: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Full-height sheets stop 4dp below the status bar, like the Figma sheets (y = 58 under a
    // 54pt status bar). The drag handle takes 21dp, the navigation bar is padded below.
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bodyHeight = figmaHeight?.let { it - 45.dp } ?: (windowHeight - statusBar - 4.dp - stackedOffset - 21.dp - navigationBar)
    if (wrapContent) {
        Column(modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = (topGap - 6.dp).coerceAtLeast(0.dp), bottom = 24.dp),
                horizontalAlignment = horizontalAlignment,
                content = content,
            )
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 10.dp), content = bottomBar)
        }
        return
    }
    Column(modifier.fillMaxWidth().navigationBarsPadding()) {
        Column(Modifier.fillMaxWidth().height(bodyHeight)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = (topGap - 6.dp).coerceAtLeast(0.dp), bottom = 16.dp),
                horizontalAlignment = horizontalAlignment,
                content = content,
            )
            Column(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
                content = bottomBar,
            )
        }
    }
}

/**
 * Navigation `dialog` destinations host a [RecentrBottomSheet], which brings its own scrim:
 * clear the dialog window's default dim so the background is only dimmed once.
 */
@Composable
internal fun ClearDialogDim() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
        window?.setDimAmount(0f)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}
