package com.recentr.core.model

import java.util.Locale

/** Theme choice of Settings › Appearance. */
enum class ThemeMode { Light, Dark, Auto }

/** Launcher icon colourway of Settings › Appearance › App icon. */
enum class AppIconStyle { Sky, Night, Mint, Sunset }

/**
 * Languages Recentr is translated into, offered by Settings › Language (BCP 47 tag + endonym), in
 * the order of their English names.
 */
enum class AppLanguage(val tag: String, val endonym: String) {
    Arabic("ar", "العربية"),
    Bulgarian("bg", "Български"),
    Chinese("zh-Hans", "简体中文"),
    Czech("cs", "Čeština"),
    English("en", "English (US)"),
    French("fr", "Français"),
    German("de", "Deutsch"),
    Hebrew("he", "עברית"),
    Hindi("hi", "हिन्दी"),
    Indonesian("id", "Bahasa Indonesia"),
    Italian("it", "Italiano"),
    Japanese("ja", "日本語"),
    Korean("ko", "한국어"),
    Polish("pl", "Polski"),
    Portuguese("pt-BR", "Português (Brasil)"),
    Russian("ru", "Русский"),
    Spanish("es", "Español"),
    Swedish("sv", "Svenska"),
    Turkish("tr", "Türkçe"),
    Ukrainian("uk", "Українська"),
    ;

    companion object {
        /** The language of a stored or system tag: "fr", "pt" (kept by earlier versions), "iw". */
        fun fromTag(tag: String?): AppLanguage? = tag?.let { forLocale(Locale.forLanguageTag(it)) }

        /**
         * The translation Android picks for [locale]: same language, whatever the region, except
         * Chinese, which is only translated in simplified characters.
         */
        fun forLocale(locale: Locale): AppLanguage? {
            // Android still reports Hebrew and Indonesian by their former codes.
            val language = when (locale.language) {
                "iw" -> "he"
                "in" -> "id"
                else -> locale.language
            }
            if (language == "zh") {
                val simplified = locale.script == "Hans" || (locale.script.isEmpty() && locale.country in setOf("", "CN", "SG"))
                return if (simplified) Chinese else null
            }
            return entries.firstOrNull { it.tag.substringBefore('-') == language }
        }
    }
}

/** Clock of Settings › Time format: "9:00 PM" or "21:00". */
enum class TimeFormat { AmPm, TwentyFourHour }

/** Recentr Plus billing period (Profile › Recentr Plus). */
enum class PlusPlan { Yearly, Monthly }

/** App preferences (Profile & Settings flow). */
data class AppSettings(
    /** Follows the system's dark theme until the user picks Light or Dark. */
    val theme: ThemeMode = ThemeMode.Auto,
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
    /** Home's "Always allowed?" card was closed: it doesn't come back. */
    val alwaysAllowedCardDismissed: Boolean = false,
    // Recentr Plus (no billing backend: kept on this device)
    val plusPlan: PlusPlan? = null,
    val plusRenewsAtMillis: Long? = null,
) {
    val anyNotification: Boolean
        get() = notifyBeforeRoutine || notifyLimitAlmostReached || notifyDailyGoal || notifyStreaks || notifyWeeklyRecap
}
