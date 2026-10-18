package com.recentr.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.recentr.core.model.AvatarColor
import com.recentr.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

/** Persists the [UserProfile] collected during onboarding and edited in Profile (DataStore). */
class UserPreferencesRepository internal constructor(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.userDataStore)

    val profile: Flow<UserProfile> = dataStore.data.map { it.toProfile() }

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toProfile())
            prefs[Keys.name] = next.name
            prefs[Keys.onboardingCompleted] = next.onboardingCompleted
            prefs[Keys.dailyGoal] = next.dailyGoalMinutes
            val bucket = next.reportedUsageBucket
            if (bucket != null) prefs[Keys.usageBucket] = bucket else prefs.remove(Keys.usageBucket)
            prefs[Keys.painPoints] = next.painPoints
            prefs[Keys.notifications] = next.notificationsEnabled
            prefs[Keys.email] = next.email
            prefs[Keys.avatarColor] = next.avatarColor.name
            val joined = next.joinedAtMillis
            if (joined != null) prefs[Keys.joinedAt] = joined else prefs.remove(Keys.joinedAt)
        }
    }

    suspend fun setDailyGoal(minutes: Int) = update { it.copy(dailyGoalMinutes = minutes) }

    suspend fun completeOnboarding() = update {
        it.copy(onboardingCompleted = true, joinedAtMillis = it.joinedAtMillis ?: System.currentTimeMillis())
    }

    suspend fun reset() = dataStore.edit { it.clear() }

    private fun Preferences.toProfile() = UserProfile(
        name = this[Keys.name].orEmpty(),
        onboardingCompleted = this[Keys.onboardingCompleted] ?: false,
        dailyGoalMinutes = this[Keys.dailyGoal] ?: UserProfile().dailyGoalMinutes,
        reportedUsageBucket = this[Keys.usageBucket],
        painPoints = this[Keys.painPoints].orEmpty(),
        notificationsEnabled = this[Keys.notifications] ?: false,
        email = this[Keys.email].orEmpty(),
        avatarColor = AvatarColor.entries.firstOrNull { it.name == this[Keys.avatarColor] } ?: AvatarColor.Sunset,
        joinedAtMillis = this[Keys.joinedAt],
    )

    private object Keys {
        val name = stringPreferencesKey("name")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val dailyGoal = intPreferencesKey("daily_goal_minutes")
        val usageBucket = stringPreferencesKey("usage_bucket")
        val painPoints = stringSetPreferencesKey("pain_points")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val email = stringPreferencesKey("email")
        val avatarColor = stringPreferencesKey("avatar_color")
        val joinedAt = longPreferencesKey("joined_at")
    }
}
