package com.recentr.app

import android.app.UiModeManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.getSystemService
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.ThemeMode

/** Settings › Appearance › Theme: Light, Dark, or Auto = the system's dark theme setting. */
@Composable
internal fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
    ThemeMode.Auto -> isSystemInDarkTheme()
}

/**
 * [RecentrTheme] in the palette of [theme], with the activity window following it: system bar
 * scrims (3-button navigation before Android 10) and the window background seen behind
 * transitions. The icon tone itself is set per screen by `SystemBarsAppearance`.
 */
@Composable
internal fun ComponentActivity.RecentrAppTheme(theme: ThemeMode, content: @Composable () -> Unit) {
    val dark = theme.isDark()
    RecentrTheme(darkTheme = dark) {
        val background = RecentrTheme.colors.bgPage.toArgb()
        DisposableEffect(dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { dark },
            )
            window.setBackgroundDrawable(ColorDrawable(background))
            onDispose { }
        }
        content()
    }
}

/**
 * From Android 12, the app's night mode follows [theme] too, so the splash screen of the next
 * launch is already in the chosen palette (Auto leaves it to the system). The activities handle
 * the `uiMode` change themselves, so this never recreates them.
 */
internal fun Context.applyNightMode(theme: ThemeMode) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    getSystemService<UiModeManager>()?.setApplicationNightMode(
        when (theme) {
            ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
            ThemeMode.Auto -> UiModeManager.MODE_NIGHT_AUTO
        },
    )
}

// enableEdgeToEdge()'s default navigation bar scrims.
private val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
