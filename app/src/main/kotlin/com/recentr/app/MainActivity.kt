package com.recentr.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.recentr.app.navigation.RecentrNavHost
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.component.RecentrTab
import com.recentr.core.designsystem.motion.ProvideRecentrMotion
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppIconStyle
import com.recentr.core.model.AppSettings
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels { MainActivityViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { viewModel.startState.value == StartState.Loading || viewModel.settings.value == null }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            RecentrData.settings.settings.map { it.appIcon }.distinctUntilChanged().collect { launcherIcon = it }
        }

        // Debug-only QA shortcuts:
        //   adb shell am start -n com.recentr.app/.MainActivity --es start main|onboarding --es tab rules
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val forcedStart = if (debuggable) intent.getStringExtra("start") else null
        val initialTab = if (debuggable) {
            RecentrTab.entries.firstOrNull { it.name.equals(intent.getStringExtra("tab"), ignoreCase = true) }
        } else {
            null
        } ?: RecentrTab.Home

        setContent {
            RecentrTheme {
                // Settings › Appearance: Reduce motion and Haptics apply to every screen. Screens wait
                // until they are read, so each opens with the right motion, also when recreated.
                val loadedSettings = viewModel.settings.collectAsStateWithLifecycle().value
                val settings = loadedSettings ?: AppSettings()
                ProvideRecentrMotion(reduceMotion = settings.reduceMotion, hapticsEnabled = settings.haptics) {
                    val start = viewModel.startState.collectAsStateWithLifecycle().value
                    if (start != StartState.Loading && loadedSettings != null) {
                        val showOnboarding = when (forcedStart) {
                            "main" -> false
                            "onboarding" -> true
                            else -> start == StartState.Onboarding
                        }
                        RecentrNavHost(showOnboarding = showOnboarding, initialTab = initialTab, debugTools = debuggable)
                    }
                }
            }
        }
    }

    /** Latest Settings › Appearance › App icon choice, applied when the app goes to the background. */
    private var launcherIcon: AppIconStyle? = null

    override fun onStop() {
        super.onStop()
        // Switching the launcher alias while the task is in front can close it on some launchers.
        launcherIcon?.let { LauncherIcons.apply(this, it) }
    }

    override fun onResume() {
        super.onResume()
        // Back from Settings › Usage access (onboarding "Connect"): switch demo ↔ device numbers.
        RecentrData.recheckUsageAccess()
    }
}
