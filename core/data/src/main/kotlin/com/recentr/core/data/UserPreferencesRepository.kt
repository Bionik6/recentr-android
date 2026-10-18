package com.recentr.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

/** Persists the [UserProfile] collected during onboarding (DataStore). */
class UserPreferencesRepository internal constructor(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.userDataStore)

    val profile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.name].orEmpty(),
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false,
            dailyGoalMinutes = prefs[Keys.dailyGoal] ?: UserProfile().dailyGoalMinutes,
            reportedUsageBucket = prefs[Keys.usageBucket],
            painPoints = prefs[Keys.painPoints].orEmpty(),
            notificationsEnabled = prefs[Keys.notifications] ?: false,
        )
    }

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        dataStore.edit { prefs ->
            val current = UserProfile(
                name = prefs[Keys.name].orEmpty(),
                onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false,
                dailyGoalMinutes = prefs[Keys.dailyGoal] ?: UserProfile().dailyGoalMinutes,
                reportedUsageBucket = prefs[Keys.usageBucket],
                painPoints = prefs[Keys.painPoints].orEmpty(),
                notificationsEnabled = prefs[Keys.notifications] ?: false,
            )
            val next = transform(current)
            prefs[Keys.name] = next.name
            prefs[Keys.onboardingCompleted] = next.onboardingCompleted
            prefs[Keys.dailyGoal] = next.dailyGoalMinutes
            val bucket = next.reportedUsageBucket
            if (bucket != null) prefs[Keys.usageBucket] = bucket else prefs.remove(Keys.usageBucket)
            prefs[Keys.painPoints] = next.painPoints
            prefs[Keys.notifications] = next.notificationsEnabled
        }
    }

    suspend fun setDailyGoal(minutes: Int) = update { it.copy(dailyGoalMinutes = minutes) }

    suspend fun completeOnboarding() = update { it.copy(onboardingCompleted = true) }

    suspend fun reset() = dataStore.edit { it.clear() }

    private object Keys {
        val name = stringPreferencesKey("name")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val dailyGoal = intPreferencesKey("daily_goal_minutes")
        val usageBucket = stringPreferencesKey("usage_bucket")
        val painPoints = stringSetPreferencesKey("pain_points")
        val notifications = booleanPreferencesKey("notifications_enabled")
    }
}
