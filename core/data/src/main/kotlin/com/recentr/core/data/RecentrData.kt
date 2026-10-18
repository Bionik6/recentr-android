package com.recentr.core.data

import android.content.Context

/**
 * Minimal service locator for the shared repositories. Initialised once from
 * `RecentrApplication.onCreate`; ViewModel factories read from it.
 */
object RecentrData {
    private lateinit var appContext: Context
    lateinit var userPreferences: UserPreferencesRepository
        private set
    val rules: RulesRepository by lazy { RulesRepository() }

    private val screenTimeDelegate = lazy {
        UsageAccessScreenTimeRepository(
            hasUsageAccess = { UsageAccess.isGranted(appContext) },
            device = UsageStatsScreenTimeRepository(appContext, userPreferences.profile),
            demo = DemoScreenTimeRepository(),
        )
    }

    /** Device screen time when usage access is granted, the Figma demo numbers otherwise. */
    val screenTime: ScreenTimeRepository by screenTimeDelegate

    fun init(context: Context) {
        if (!::appContext.isInitialized) appContext = context.applicationContext
        if (!::userPreferences.isInitialized) {
            userPreferences = UserPreferencesRepository(context.applicationContext)
        }
    }

    /** Call on every resume: usage access may have been granted or revoked in Settings. */
    fun recheckUsageAccess() {
        if (screenTimeDelegate.isInitialized()) screenTimeDelegate.value.recheckUsageAccess()
    }
}
