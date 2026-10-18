package com.recentr.core.data

import android.content.Context
import kotlinx.coroutines.flow.map

/**
 * Minimal service locator for the shared repositories. Initialised once from
 * `RecentrApplication.onCreate`; ViewModel factories read from it.
 */
object RecentrData {
    private lateinit var appContext: Context
    lateinit var userPreferences: UserPreferencesRepository
        private set
    lateinit var settings: SettingsRepository
        private set
    val rules: RulesRepository by lazy { RulesRepository() }
    val focus: FocusRepository by lazy { FocusRepository() }

    private val screenTimeDelegate = lazy {
        UsageAccessScreenTimeRepository(
            hasUsageAccess = { UsageAccess.isGranted(appContext) },
            device = UsageStatsScreenTimeRepository(appContext, userPreferences.profile),
            demo = DemoScreenTimeRepository(goalMinutes = userPreferences.profile.map { it.dailyGoalMinutes }),
        )
    }

    /** Device screen time when usage access is granted, the Figma demo numbers otherwise. */
    val screenTime: ScreenTimeRepository by screenTimeDelegate

    /** Badges, calm points and streak savers of the Achievements flow. */
    val achievements: AchievementsRepository by lazy {
        AchievementsRepository(appContext, screenTime, userPreferences.profile, focus, rules) { installedAtMillis }
    }

    /** When the app was first installed: fallback for "Recentr-ing for N days". */
    val installedAtMillis: Long by lazy {
        runCatching { appContext.packageManager.getPackageInfo(appContext.packageName, 0).firstInstallTime }
            .getOrDefault(System.currentTimeMillis())
    }

    fun init(context: Context) {
        if (!::appContext.isInitialized) appContext = context.applicationContext
        if (!::userPreferences.isInitialized) {
            userPreferences = UserPreferencesRepository(context.applicationContext)
        }
        if (!::settings.isInitialized) settings = SettingsRepository(context.applicationContext)
    }

    /** Call on every resume: usage access may have been granted or revoked in Settings. */
    fun recheckUsageAccess() {
        if (screenTimeDelegate.isInitialized()) screenTimeDelegate.value.recheckUsageAccess()
    }

    /** "Delete account": erases the profile, preferences, rules, focus log and badges on this device. */
    suspend fun eraseAll() {
        userPreferences.reset()
        settings.reset()
        rules.reset()
        focus.reset()
        achievements.reset()
    }
}
