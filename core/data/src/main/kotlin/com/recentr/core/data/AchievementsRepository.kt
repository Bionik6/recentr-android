package com.recentr.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.recentr.core.model.Achievement
import com.recentr.core.model.AchievementProgress
import com.recentr.core.model.CalmLevel
import com.recentr.core.model.DayStats
import com.recentr.core.model.UsageApp
import com.recentr.core.model.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

private val Context.achievementsDataStore: DataStore<Preferences> by preferencesDataStore(name = "achievements")

/** Everything the Achievements screens show, from one evaluation. */
data class AchievementsSnapshot(
    /** The catalogue, in its order. */
    val badges: List<AchievementProgress>,
    val level: CalmLevel,
    val streak: StreakState,
    /** Longest streak in the history (the current one included). */
    val bestStreak: Int,
    /** Earned badges not celebrated yet, newest first. */
    val uncelebrated: List<Achievement>,
    /** Usage history, oldest first, ending today. */
    val days: List<DayStats>,
    val goalMinutes: Int,
    /** When the user started ("Since you started · 23 days"). */
    val startedOn: LocalDate,
    /** Apps of the first blocking rule ("You blocked Instagram & X for the first time"). */
    val firstLockApps: List<UsageApp>,
)

/**
 * Badges, calm points and streak savers (Figma 105–110). Badges are evaluated by
 * [AchievementEngine] against the screen-time history, the focus log and the rules. With device
 * numbers, earned days are kept (DataStore), so a badge stays earned once its days leave the
 * system's retained history; without usage access the (empty) numbers are evaluated on the fly. Which badges were
 * celebrated ("New badge" screen) is kept for both.
 */
class AchievementsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val screenTime: ScreenTimeRepository,
    private val profile: Flow<UserProfile>,
    private val focus: FocusRepository,
    private val rules: RulesRepository,
    private val installedAtMillis: () -> Long,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    constructor(
        context: Context,
        screenTime: ScreenTimeRepository,
        profile: Flow<UserProfile>,
        focus: FocusRepository,
        rules: RulesRepository,
        installedAtMillis: () -> Long,
    ) : this(context.applicationContext.achievementsDataStore, screenTime, profile, focus, rules, installedAtMillis)

    /** Evaluates the badges against the latest numbers and records the newly earned ones. */
    suspend fun load(): AchievementsSnapshot {
        val now = clock()
        val today = now.toLocalDate()
        val user = profile.first()
        val days = screenTime.dailyStats(today.minusDays(HISTORY_DAYS), today)
        val startedOn = Instant.ofEpochMilli(user.joinedAtMillis ?: installedAtMillis()).atZone(zone()).toLocalDate()
        val ruleList = rules.rules.value
        val evaluated = AchievementEngine.evaluate(
            AchievementInputs(
                now = now,
                days = days,
                goalMinutes = user.dailyGoalMinutes,
                baselineDailyMinutes = user.reportedDailyMinutes,
                focusSessions = focus.history.value,
                rules = ruleList,
                startedOn = startedOn,
                zone = zone(),
            ),
        )

        val device = screenTime.source.value == ScreenTimeSource.Device
        val prefs = dataStore.data.first()
        val stored = if (device) prefs[Keys.earned].orEmpty().mapNotNull(::parseEarned).toMap() else emptyMap()
        val badges = evaluated.map { progress ->
            val earnedOn = listOfNotNull(stored[progress.achievement], progress.earnedOn).minOrNull()
            progress.copy(
                earnedOn = earnedOn,
                progress = if (earnedOn != null) progress.achievement.target else progress.progress,
            )
        }
        if (device) {
            val earned = badges.mapNotNull { badge -> badge.earnedOn?.let { "${badge.achievement.name}:${it.toEpochDay()}" } }.toSet()
            if (earned != prefs[Keys.earned]) dataStore.edit { it[Keys.earned] = earned }
        }
        val celebrated = prefs[celebratedKey(device)].orEmpty()

        val newestFirst = days.asReversed().asSequence().map { StreakDay(it.date, it.hasData, it.minutes) }
        val streak = Streaks.current(newestFirst, user.dailyGoalMinutes)
        return AchievementsSnapshot(
            badges = badges,
            level = CalmLevel.of(badges.filter { it.earned }.sumOf { it.achievement.points }),
            streak = streak,
            bestStreak = maxOf(streak.days, Streaks.best(days.map { StreakDay(it.date, it.hasData, it.minutes) }, user.dailyGoalMinutes)),
            uncelebrated = badges
                .filter { it.earned && it.achievement.name !in celebrated }
                .sortedByDescending { it.earnedOn }
                .map { it.achievement },
            days = days,
            goalMinutes = user.dailyGoalMinutes,
            startedOn = startedOn,
            firstLockApps = ruleList.firstOrNull { it.apps.isNotEmpty() }?.apps.orEmpty(),
        )
    }

    /** The "New badge" screen was shown for [achievements] (or they were all seen at once). */
    suspend fun markCelebrated(achievements: Collection<Achievement>) {
        val key = celebratedKey(screenTime.source.value == ScreenTimeSource.Device)
        dataStore.edit { prefs -> prefs[key] = prefs[key].orEmpty() + achievements.map { it.name } }
    }

    /** "Delete account": forgets the earned badges. */
    suspend fun reset() = dataStore.edit { it.clear() }

    private fun parseEarned(entry: String): Pair<Achievement, LocalDate>? {
        val (name, day) = entry.split(':').takeIf { it.size == 2 } ?: return null
        val achievement = Achievement.entries.firstOrNull { it.name == name } ?: return null
        return achievement to LocalDate.ofEpochDay(day.toLongOrNull() ?: return null)
    }

    private fun celebratedKey(device: Boolean) = if (device) Keys.celebrated else Keys.celebratedDemo

    private object Keys {
        val earned = stringSetPreferencesKey("earned")
        val celebrated = stringSetPreferencesKey("celebrated")
        val celebratedDemo = stringSetPreferencesKey("celebrated_demo")
    }

    companion object {
        /** Days of history evaluated before today (this month and the previous one). */
        const val HISTORY_DAYS = 62L
    }
}
