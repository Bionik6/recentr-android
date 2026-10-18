package com.recentr.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recentr.core.designsystem.theme.RecentrShapes
import com.recentr.core.designsystem.theme.RecentrTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Design-system bottom sheet: white surface, 36dp top corners (`radius/sheet`), grey grabber
 * and the `overlay/scrim` dim. Built on Material 3 [ModalBottomSheet] so it gets Android's drag,
 * back-gesture and accessibility behaviour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentrBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RecentrShapes.sheet,
        containerColor = RecentrTheme.colors.bgSurface,
        contentColor = RecentrTheme.colors.textPrimary,
        scrimColor = RecentrTheme.colors.overlayScrim,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                SheetGrabber()
            }
        },
        content = content,
    )
}

/** Hides a [RecentrSheet] with its slide-down animation, then runs a follow-up action. */
@Stable
class SheetController @OptIn(ExperimentalMaterial3Api::class) internal constructor(
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
 * [RecentrBottomSheet] whose content can close it with the slide-down animation through a
 * [SheetController]. [onDismissRequest] runs once the sheet is gone (drag, scrim, back or
 * [SheetController.close]). The content is padded above the navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentrSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(SheetController) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val controller = remember(state, scope, onDismissRequest) { SheetController(state, scope, onDismissRequest) }
    RecentrBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = state) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) { content(controller) }
    }
}
