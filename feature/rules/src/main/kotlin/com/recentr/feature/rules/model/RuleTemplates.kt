package com.recentr.feature.rules.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.component.TemplateColor
import com.recentr.core.model.AppCategory
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.compactTimeRangeLabel
import com.recentr.feature.rules.ui.durationLabel
import com.recentr.feature.rules.ui.unlockLabel
import java.time.Instant
import java.time.ZoneId

/** Sections of the Templates tab, in display order: a title, a line about them, then their templates. */
internal enum class TemplateSection(@param:StringRes val title: Int, @param:StringRes val description: Int) {
    Focus(R.string.rules_section_focus, R.string.rules_section_focus_body),
    Unwind(R.string.rules_section_unwind, R.string.rules_section_unwind_body),
    BePresent(R.string.rules_section_be_present, R.string.rules_section_be_present_body),
    ScrollLess(R.string.rules_section_scroll_less, R.string.rules_section_scroll_less_body),
    ;

    /** Its templates, in catalog order. */
    val templates: List<BuiltInTemplate> get() = BuiltInTemplate.entries.filter { it.section == this }
}

/** Moments of the day, which the session templates suit (session suggestions). */
internal enum class DayMoment {
    /** 5:00–12:00. */
    Morning,
    /** 12:00–18:00. */
    Afternoon,
    /** 18:00–22:00. */
    Evening,
    /** 22:00–5:00. */
    Night,
    ;

    companion object {
        fun at(epochMillis: Long, zone: ZoneId): DayMoment = when (Instant.ofEpochMilli(epochMillis).atZone(zone).hour) {
            in 5 until 12 -> Morning
            in 12 until 18 -> Afternoon
            in 18 until 22 -> Evening
            else -> Night
        }
    }
}

/** What a template sets up: the kind of rule it creates ([type]) and the settings it gives it. */
internal sealed interface TemplateSetup {
    val type: RuleType

    /** A session of [minutes], suggested first at the moments of the day it [suits]. */
    data class Session(val minutes: Int, val suits: Set<DayMoment>) : TemplateSetup {
        override val type: RuleType get() = RuleType.Session
    }

    /** A routine blocking during [schedule]. */
    data class Routine(val schedule: RuleSchedule) : TemplateSetup {
        override val type: RuleType get() = RuleType.Bedtime
    }

    /** A lock opened with [unlockMethod] ([waitSeconds] for a wait), [unlocksPerDay] unlocks of [unlockMinutes] a day. */
    data class Lock(
        val unlockMethod: UnlockMethod,
        val waitSeconds: Int,
        val unlocksPerDay: Int,
        val unlockMinutes: Int,
    ) : TemplateSetup {
        override val type: RuleType get() = RuleType.Lock
    }

    /** A limit: a budget of [minutes] for today, shared by its apps. */
    data class Limit(val minutes: Int) : TemplateSetup {
        override val type: RuleType get() = RuleType.Limit
    }
}

/** The installed apps a template preselects, category by category. The user can change them in the editor. */
internal enum class TemplateApps(private val categories: List<AppCategory>) {
    /** Social, video and games. */
    Distractions(listOf(AppCategory.Social, AppCategory.Entertainment, AppCategory.Games)),
    /** Social and video. */
    SocialAndVideo(listOf(AppCategory.Social, AppCategory.Entertainment)),
    Social(listOf(AppCategory.Social)),
    ;

    /** Those of [installed]. */
    fun pick(installed: List<UsageApp>): List<UsageApp> =
        categories.flatMap { category -> installed.filter { it.category == category } }
}

private val Weekdays = (1..5).toSet()

/**
 * Built-in rule templates (Templates tab, the row under the rules, session suggestions), in catalog
 * order. Each creates a rule of its own kind with its [setup], [title] and [sticker], blocking the
 * installed [apps] ([RuleFactory.fromTemplate]).
 */
internal enum class BuiltInTemplate(
    val id: String,
    val section: TemplateSection,
    @param:StringRes val title: Int,
    /** The line under the title on its card (not copied into the rule). */
    @param:StringRes val body: Int,
    val setup: TemplateSetup,
    val sticker: RuleSticker,
    val color: TemplateColor,
    val apps: TemplateApps,
) {
    FocusSprint(
        "tpl-focus-sprint", TemplateSection.Focus, R.string.rules_tpl_focus_sprint, R.string.rules_tpl_focus_sprint_body,
        TemplateSetup.Session(45, setOf(DayMoment.Morning, DayMoment.Afternoon)),
        RuleSticker.Bolt, TemplateColor.Sky, TemplateApps.Distractions,
    ),
    StudyHour(
        "tpl-study-hour", TemplateSection.Focus, R.string.rules_tpl_study_hour, R.string.rules_tpl_study_hour_body,
        TemplateSetup.Session(60, setOf(DayMoment.Afternoon, DayMoment.Evening)),
        RuleSticker.Note, TemplateColor.Mint, TemplateApps.Distractions,
    ),
    WorkRhythm(
        "tpl-work-rhythm", TemplateSection.Focus, R.string.rules_tpl_work_rhythm, R.string.rules_tpl_work_rhythm_body,
        TemplateSetup.Routine(RuleSchedule(9 * 60, 12 * 60, Weekdays)),
        RuleSticker.Flame, TemplateColor.Sunset, TemplateApps.Distractions,
    ),
    EveningCalm(
        "tpl-evening-calm", TemplateSection.Unwind, R.string.rules_tpl_evening_calm, R.string.rules_tpl_evening_calm_body,
        TemplateSetup.Routine(RuleSchedule(22 * 60, 23 * 60)),
        RuleSticker.Moon, TemplateColor.Sunset, TemplateApps.SocialAndVideo,
    ),
    NightPause(
        "tpl-night-pause", TemplateSection.Unwind, R.string.rules_tpl_night_pause, R.string.rules_tpl_night_pause_body,
        TemplateSetup.Routine(RuleSchedule(23 * 60, 7 * 60)),
        RuleSticker.Moon, TemplateColor.Night, TemplateApps.Distractions,
    ),
    SlowMorning(
        "tpl-slow-morning", TemplateSection.Unwind, R.string.rules_tpl_slow_morning, R.string.rules_tpl_slow_morning_body,
        TemplateSetup.Routine(RuleSchedule(7 * 60, 8 * 60)),
        RuleSticker.Leaf, TemplateColor.Sky, TemplateApps.Social,
    ),
    DinnerTogether(
        "tpl-dinner-together", TemplateSection.BePresent, R.string.rules_tpl_dinner_together, R.string.rules_tpl_dinner_together_body,
        TemplateSetup.Routine(RuleSchedule(19 * 60 + 30, 20 * 60 + 15)),
        RuleSticker.Flame, TemplateColor.Sunset, TemplateApps.Distractions,
    ),
    FamilyHour(
        "tpl-family-hour", TemplateSection.BePresent, R.string.rules_tpl_family_hour, R.string.rules_tpl_family_hour_body,
        TemplateSetup.Routine(RuleSchedule(16 * 60, 17 * 60, setOf(7))),
        RuleSticker.Note, TemplateColor.Sky, TemplateApps.Distractions,
    ),
    TimeForMe(
        "tpl-time-for-me", TemplateSection.BePresent, R.string.rules_tpl_time_for_me, R.string.rules_tpl_time_for_me_body,
        TemplateSetup.Session(30, setOf(DayMoment.Evening, DayMoment.Night)),
        RuleSticker.Leaf, TemplateColor.Mint, TemplateApps.Distractions,
    ),
    WalkBeforeYouScroll(
        "tpl-walk-before-you-scroll", TemplateSection.ScrollLess, R.string.rules_tpl_walk_before_you_scroll, R.string.rules_tpl_walk_before_you_scroll_body,
        TemplateSetup.Lock(UnlockMethod.Walk, waitSeconds = 10, unlocksPerDay = 3, unlockMinutes = 5),
        RuleSticker.Lock, TemplateColor.Mint, TemplateApps.Social,
    ),
    PauseBeforeOpening(
        "tpl-pause-before-opening", TemplateSection.ScrollLess, R.string.rules_tpl_pause_before_opening, R.string.rules_tpl_pause_before_opening_body,
        TemplateSetup.Lock(UnlockMethod.Wait, waitSeconds = 10, unlocksPerDay = 3, unlockMinutes = 5),
        RuleSticker.Lock, TemplateColor.Night, TemplateApps.Social,
    ),
    SocialBudget(
        "tpl-social-budget", TemplateSection.ScrollLess, R.string.rules_tpl_social_budget, R.string.rules_tpl_social_budget_body,
        TemplateSetup.Limit(30),
        RuleSticker.Timer, TemplateColor.Sunset, TemplateApps.Social,
    ),
    ;

    /** The kind of rule it creates. */
    val type: RuleType get() = setup.type

    /**
     * This template as a session suggestion at [now] ([moment] of the day), with its tier: 0 for a
     * routine whose window holds now, for the minutes left in it (rounded up to 5); 1 for a routine
     * starting within the hour, for its window's length; 2 for a session suited to [moment], 3 for
     * the other sessions, for their own length. Null for the other routines, the locks and limits.
     */
    private fun suggestionAt(now: Long, zone: ZoneId, moment: DayMoment): Pair<Int, SessionSuggestion>? {
        val (tier, minutes) = when (val setup = setup) {
            is TemplateSetup.Session -> (if (moment in setup.suits) 2 else 3) to setup.minutes
            is TemplateSetup.Routine -> {
                val schedule = setup.schedule
                val end = schedule.windowEndAt(now, zone)
                val start = schedule.nextStartAfter(now, zone)
                when {
                    end != null -> 0 to ((end - now + FIVE_MINUTES - 1) / FIVE_MINUTES * 5).toInt()
                    start != null && start - now <= HOUR -> 1 to schedule.lengthMinutes
                    else -> return null
                }
            }
            is TemplateSetup.Lock, is TemplateSetup.Limit -> return null
        }
        return tier to SessionSuggestion(this, minutes.coerceIn(SESSION_MIN_MINUTES, SESSION_MAX_MINUTES))
    }

    companion object {
        private const val SUGGESTIONS = 3
        private const val FIVE_MINUTES = 5 * 60_000L
        private const val HOUR = 60 * 60_000L

        fun byId(id: String): BuiltInTemplate? = entries.firstOrNull { it.id == id }

        /**
         * The new session's 3 "Suggestions" for [now], from the session and routine templates:
         * by tier ([suggestionAt]: what holds now, then what starts within the hour, then the
         * sessions suited to the moment of the day, then the others), catalog order within a tier.
         * Lengths are clamped to the session lengths the editor allows.
         */
        fun sessionSuggestions(now: Long, zone: ZoneId): List<SessionSuggestion> {
            val moment = DayMoment.at(now, zone)
            return entries
                .mapNotNull { it.suggestionAt(now, zone, moment) }
                .sortedBy { (tier, _) -> tier }
                .take(SUGGESTIONS)
                .map { (_, suggestion) -> suggestion }
        }
    }
}

/** A "Suggestions" tile of a new session: [template]'s title and sticker, for [minutes]. */
@Immutable
internal data class SessionSuggestion(val template: BuiltInTemplate, val minutes: Int)

/** A template as shown by a template card. */
@Immutable
internal data class TemplateCardUi(
    val id: String,
    val title: String,
    val body: String,
    /** Its chip: a session's or a limit's length, a routine's hours, a lock's unlock method. */
    val badge: String,
    val sticker: RuleSticker,
    val color: TemplateColor,
)

@Composable
internal fun BuiltInTemplate.toCard(): TemplateCardUi = TemplateCardUi(
    id = id,
    title = stringResource(title),
    body = stringResource(body),
    badge = when (val setup = setup) {
        is TemplateSetup.Session -> durationLabel(setup.minutes)
        is TemplateSetup.Routine -> compactTimeRangeLabel(setup.schedule)
        is TemplateSetup.Lock -> unlockLabel(setup.unlockMethod, setup.waitSeconds)
        is TemplateSetup.Limit -> durationLabel(setup.minutes)
    },
    sticker = sticker,
    color = color,
)
