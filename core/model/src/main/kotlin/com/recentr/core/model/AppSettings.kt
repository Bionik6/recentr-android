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

/** Clock of Settings › Time format: "9:00 PM" or "21:00". */
enum class TimeFormat { AmPm, TwentyFourHour }

/** Recentr Plus billing period (Profile › Recentr Plus). */
enum class PlusPlan { Yearly, Monthly }

/** App preferences (Profile & Settings flow). */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.Light,
    val appIcon: AppIconStyle = AppIconStyle.Sky,
    val showMascotOnHome: Boolean = true,
    val reduceMotion: Boolean = false,
    /** Haptic feedback on choices, toggles and celebrations (Settings › Appearance). */
    val haptics: Boolean = true,
    /** Null = follow the system language. */
    val language: AppLanguage? = null,
    /** Null = follow the device's 24-hour setting. */
    val timeFormat: TimeFormat? = null,
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
    /** Emergency unlocks spent in [emergencyUnlocksMonth] (they come back every month). */
    val emergencyUnlocksUsed: Int = 0,
    /** The month of [emergencyUnlocksUsed], as `year * 12 + month - 1`, or null when none were spent. */
    val emergencyUnlocksMonth: Int? = null,
    // Recentr Plus (no billing backend: kept on this device)
    val plusPlan: PlusPlan? = null,
    val plusRenewsAtMillis: Long? = null,
) {
    val anyNotification: Boolean
        get() = notifyBeforeRoutine || notifyLimitAlmostReached || notifyDailyGoal || notifyStreaks || notifyWeeklyRecap
}
