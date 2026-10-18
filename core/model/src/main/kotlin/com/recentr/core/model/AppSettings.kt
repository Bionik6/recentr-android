package com.recentr.core.model

/** Theme choice of Settings › Appearance. */
enum class ThemeMode { Light, Dark, Auto }

/** Launcher icon colourway of Settings › Appearance › App icon. */
enum class AppIconStyle { Sky, Night, Mint, Sunset }

/** Languages offered by Settings › Language (BCP 47 tag + endonym). */
enum class AppLanguage(val tag: String, val endonym: String) {
    English("en", "English"),
    Spanish("es", "Español"),
    French("fr", "Français"),
    German("de", "Deutsch"),
    Portuguese("pt", "Português"),
    Vietnamese("vi", "Tiếng Việt"),
    Japanese("ja", "日本語"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
    }
}

/** Recentr Plus billing period (Profile › Recentr Plus). */
enum class PlusPlan { Yearly, Monthly }

/** App preferences (Profile & Settings flow). */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.Light,
    val appIcon: AppIconStyle = AppIconStyle.Sky,
    val showMascotOnHome: Boolean = true,
    val reduceMotion: Boolean = false,
    /** Null = follow the system language. */
    val language: AppLanguage? = null,
    // Notifications
    val notifyBeforeRoutine: Boolean = true,
    val notifyLimitAlmostReached: Boolean = true,
    val notifyDailyGoal: Boolean = false,
    val notifyStreaks: Boolean = true,
    val notifyWeeklyRecap: Boolean = true,
    /** Quiet hours, minutes from midnight (10 PM – 7 AM in the design). */
    val quietHoursStartMinute: Int = 22 * 60,
    val quietHoursEndMinute: Int = 7 * 60,
    // Protection
    /** Settings › Strict mode: new sessions and routines start strict. */
    val strictByDefault: Boolean = false,
    val emergencyUnlocksLeft: Int = 2,
    val unlockPassPrinted: Boolean = true,
    // Recentr Plus (no billing backend: kept on this device)
    val plusPlan: PlusPlan? = null,
    val plusRenewsAtMillis: Long? = null,
) {
    val anyNotification: Boolean
        get() = notifyBeforeRoutine || notifyLimitAlmostReached || notifyDailyGoal || notifyStreaks || notifyWeeklyRecap
}
