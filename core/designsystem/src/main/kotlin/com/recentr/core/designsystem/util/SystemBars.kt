package com.recentr.core.designsystem.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Ordered requests for the system-bar icon tone of one window. The most recently *entered*
 * request wins, so both navigation (the new screen enters before the old one leaves) and overlays
 * (a sheet on top of a screen, then dismissed) resolve to the right tone.
 */
private object SystemBarsRequests {
    class Request(var lightIcons: Boolean)

    private val stacks = mutableMapOf<Window, MutableList<Request>>()

    fun add(window: Window, view: View, request: Request) {
        stacks.getOrPut(window) { mutableListOf() }.add(request)
        apply(window, view)
    }

    fun remove(window: Window, view: View, request: Request) {
        val stack = stacks[window] ?: return
        stack.remove(request)
        if (stack.isEmpty()) stacks.remove(window) else apply(window, view)
    }

    fun apply(window: Window, view: View) {
        val top = stacks[window]?.lastOrNull() ?: return
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !top.lightIcons
            isAppearanceLightNavigationBars = !top.lightIcons
        }
    }
}

/**
 * Sets the status / navigation bar icon colour while this composable is on screen — the Android
 * counterpart of the Figma "Status Bar · Tone" property. Use `lightIcons = true` on the sky
 * backdrop and over dimmed sheets, `false` on page-grey screens. Nested / successive calls are
 * resolved in entry order (the latest screen or overlay wins).
 */
@Composable
fun SystemBarsAppearance(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = remember(view) { view.context.findActivity()?.window } ?: return
    val request = remember { SystemBarsRequests.Request(lightIcons) }
    DisposableEffect(window) {
        SystemBarsRequests.add(window, view, request)
        onDispose { SystemBarsRequests.remove(window, view, request) }
    }
    SideEffect {
        if (request.lightIcons != lightIcons) {
            request.lightIcons = lightIcons
            SystemBarsRequests.apply(window, view)
        }
    }
}
