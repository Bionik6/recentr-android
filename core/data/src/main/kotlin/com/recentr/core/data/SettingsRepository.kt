package com.recentr.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.recentr.core.model.AppLanguage
import com.recentr.core.model.AppSettings
import com.recentr.core.model.PlusPlan
import com.recentr.core.model.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** App preferences of the Profile & Settings flow (DataStore). */
class SettingsRepository internal constructor(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    suspend fun reset() = dataStore.edit { it.clear() }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = enumOr(this[Keys.theme], defaults.theme),
            appIcon = enumOr(this[Keys.appIcon], defaults.appIcon),
            showMascotOnHome = this[Keys.showMascot] ?: defaults.showMascotOnHome,
            reduceMotion = this[Keys.reduceMotion] ?: defaults.reduceMotion,
            haptics = this[Keys.haptics] ?: defaults.haptics,
            language = AppLanguage.fromTag(this[Keys.language]),
            timeFormat = TimeFormat.entries.firstOrNull { it.name == this[Keys.timeFormat] },
            notifyBeforeRoutine = this[Keys.notifyRoutine] ?: defaults.notifyBeforeRoutine,
            notifyLimitAlmostReached = this[Keys.notifyLimit] ?: defaults.notifyLimitAlmostReached,
            notifyDailyGoal = this[Keys.notifyGoal] ?: defaults.notifyDailyGoal,
            notifyStreaks = this[Keys.notifyStreaks] ?: defaults.notifyStreaks,
            notifyWeeklyRecap = this[Keys.notifyRecap] ?: defaults.notifyWeeklyRecap,
            quietHoursStartMinute = this[Keys.quietStart] ?: defaults.quietHoursStartMinute,
            quietHoursEndMinute = this[Keys.quietEnd] ?: defaults.quietHoursEndMinute,
            strictByDefault = this[Keys.strictByDefault] ?: defaults.strictByDefault,
            emergencyUnlocksLeft = this[Keys.emergencyUnlocks] ?: defaults.emergencyUnlocksLeft,
            unlockPassPrinted = this[Keys.unlockPassPrinted] ?: defaults.unlockPassPrinted,
            plusPlan = PlusPlan.entries.firstOrNull { it.name == this[Keys.plusPlan] },
            plusRenewsAtMillis = this[Keys.plusRenewsAt],
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[Keys.theme] = s.theme.name
        this[Keys.appIcon] = s.appIcon.name
        this[Keys.showMascot] = s.showMascotOnHome
        this[Keys.reduceMotion] = s.reduceMotion
        this[Keys.haptics] = s.haptics
        s.language?.let { this[Keys.language] = it.tag } ?: remove(Keys.language)
        s.timeFormat?.let { this[Keys.timeFormat] = it.name } ?: remove(Keys.timeFormat)
        this[Keys.notifyRoutine] = s.notifyBeforeRoutine
        this[Keys.notifyLimit] = s.notifyLimitAlmostReached
        this[Keys.notifyGoal] = s.notifyDailyGoal
        this[Keys.notifyStreaks] = s.notifyStreaks
        this[Keys.notifyRecap] = s.notifyWeeklyRecap
        this[Keys.quietStart] = s.quietHoursStartMinute
        this[Keys.quietEnd] = s.quietHoursEndMinute
        this[Keys.strictByDefault] = s.strictByDefault
        this[Keys.emergencyUnlocks] = s.emergencyUnlocksLeft
        this[Keys.unlockPassPrinted] = s.unlockPassPrinted
        s.plusPlan?.let { this[Keys.plusPlan] = it.name } ?: remove(Keys.plusPlan)
        s.plusRenewsAtMillis?.let { this[Keys.plusRenewsAt] = it } ?: remove(Keys.plusRenewsAt)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val appIcon = stringPreferencesKey("app_icon")
        val showMascot = booleanPreferencesKey("show_mascot_on_home")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val haptics = booleanPreferencesKey("haptics")
        val language = stringPreferencesKey("language")
        val timeFormat = stringPreferencesKey("time_format")
        val notifyRoutine = booleanPreferencesKey("notify_before_routine")
        val notifyLimit = booleanPreferencesKey("notify_limit")
        val notifyGoal = booleanPreferencesKey("notify_daily_goal")
        val notifyStreaks = booleanPreferencesKey("notify_streaks")
        val notifyRecap = booleanPreferencesKey("notify_weekly_recap")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val strictByDefault = booleanPreferencesKey("strict_by_default")
        val emergencyUnlocks = intPreferencesKey("emergency_unlocks_left")
        val unlockPassPrinted = booleanPreferencesKey("unlock_pass_printed")
        val plusPlan = stringPreferencesKey("plus_plan")
        val plusRenewsAt = longPreferencesKey("plus_renews_at")
    }
}
