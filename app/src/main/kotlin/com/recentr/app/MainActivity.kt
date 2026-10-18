package com.recentr.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recentr.app.navigation.RecentrNavHost
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.component.RecentrTab
import com.recentr.core.designsystem.theme.RecentrTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels { MainActivityViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { viewModel.startState.value == StartState.Loading }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
                val start = viewModel.startState.collectAsStateWithLifecycle().value
                if (start != StartState.Loading) {
                    val showOnboarding = when (forcedStart) {
                        "main" -> false
                        "onboarding" -> true
                        else -> start == StartState.Onboarding
                    }
                    RecentrNavHost(showOnboarding = showOnboarding, initialTab = initialTab)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Back from Settings › Usage access (onboarding "Connect"): switch demo ↔ device numbers.
        RecentrData.recheckUsageAccess()
    }
}
