package com.recentr.core.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val rules: RulesRepository by lazy { RulesRepository(File(appContext.filesDir, "rules.json")) }
    val focus: FocusRepository by lazy { FocusRepository(File(appContext.filesDir, "focus_sounds.json")) }

    /** The device's launcher apps, offered by the Rules and Focus app pickers. */
    val installedApps: InstalledAppsRepository by lazy { InstalledAppsRepository(appContext) }

    private val screenTimeDelegate = lazy {
        UsageAccessScreenTimeRepository(
            hasUsageAccess = { UsageAccess.isGranted(appContext) },
            device = UsageStatsScreenTimeRepository(appContext, userPreferences.profile),
            noAccess = EmptyScreenTimeRepository(),
        )
    }

    /** Device screen time when usage access is granted, empty numbers otherwise. */
    val screenTime: ScreenTimeRepository by screenTimeDelegate

    /** The apps no rule and no focus session puts on hold (Settings › Always allowed). */
    val alwaysAllowed: AlwaysAllowedRepository by lazy {
        AlwaysAllowedRepository(File(appContext.filesDir, "always_allowed.json"), rules, focus)
    }

    /** What the app blocker puts on hold, and the apps opened anyway. */
    val holds: AppHoldRepository by lazy { AppHoldRepository(rules, focus, installedApps, screenTime, settings, alwaysAllowed) }

    private val _blockingEnabled = MutableStateFlow(false)

    /** Whether Recentr's accessibility service (the app blocker) is on. */
    val blockingEnabled: StateFlow<Boolean> = _blockingEnabled.asStateFlow()

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
        recheckBlockingAccess()
    }

    /** Call on every resume: usage access may have been granted or revoked in Settings. */
    fun recheckUsageAccess() {
        if (screenTimeDelegate.isInitialized()) screenTimeDelegate.value.recheckUsageAccess()
    }

    /** Call on every resume, and when the service starts or stops: blocking may have been turned on or off. */
    fun recheckBlockingAccess() {
        _blockingEnabled.value = BlockingAccess.isEnabled(appContext)
    }

    /** "Delete account": erases the profile, preferences, rules, apps always allowed, focus log and badges on this device. */
    suspend fun eraseAll() {
        userPreferences.reset()
        settings.reset()
        rules.reset()
        alwaysAllowed.reset()
        focus.reset()
        achievements.reset()
    }
}
