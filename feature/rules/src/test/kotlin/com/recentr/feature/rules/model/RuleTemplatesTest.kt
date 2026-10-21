package com.recentr.feature.rules.model

import com.recentr.core.model.AppCategory
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.core.model.UnlockMethod
import com.recentr.core.model.UsageApp
import com.recentr.feature.rules.model.BuiltInTemplate.DinnerTogether
import com.recentr.feature.rules.model.BuiltInTemplate.EveningCalm
import com.recentr.feature.rules.model.BuiltInTemplate.FamilyHour
import com.recentr.feature.rules.model.BuiltInTemplate.FocusSprint
import com.recentr.feature.rules.model.BuiltInTemplate.NightPause
import com.recentr.feature.rules.model.BuiltInTemplate.PauseBeforeOpening
import com.recentr.feature.rules.model.BuiltInTemplate.SlowMorning
import com.recentr.feature.rules.model.BuiltInTemplate.SocialBudget
import com.recentr.feature.rules.model.BuiltInTemplate.StudyHour
import com.recentr.feature.rules.model.BuiltInTemplate.TimeForMe
import com.recentr.feature.rules.model.BuiltInTemplate.WalkBeforeYouScroll
import com.recentr.feature.rules.model.BuiltInTemplate.WorkRhythm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The built-in templates: their sections, and the rule each one creates — of its own kind, with its
 * title, sticker and settings, the rest kept at the kind's defaults.
 */
class RuleTemplatesTest {

    /** Names a rule after its title resource, so the test can tell which one it got. */
    private val factory = RuleFactory(string = { id, _ -> "string-$id" }, newId = { "id" })

    private val social = UsageApp("app.social", "Social", AppCategory.Social)
    private val video = UsageApp("app.video", "Video", AppCategory.Entertainment)
    private val music = UsageApp("app.music", "Music", AppCategory.Music)
    private val game = UsageApp("app.game", "Game", AppCategory.Games)
    private val work = UsageApp("app.work", "Work", AppCategory.Productivity)
    private val other = UsageApp("app.other", "Other", AppCategory.Other)
    private val installed = listOf(game, other, music, video, work, social)
    private val distractions = listOf(social, video, game)

    /** A new rule of [type] as [template] makes it: its title, apps and sticker, then [settings]. */
    private fun expected(template: BuiltInTemplate, type: RuleType, apps: List<UsageApp>, sticker: RuleSticker, settings: (Rule) -> Rule): Rule =
        settings(factory.newRule(type).copy(name = "string-${template.title}", apps = apps, sticker = sticker))

    @Test
    fun `the catalog has 12 templates in 4 sections`() {
        assertEquals(
            listOf(
                FocusSprint, StudyHour, WorkRhythm,
                EveningCalm, NightPause, SlowMorning,
                DinnerTogether, FamilyHour, TimeForMe,
                WalkBeforeYouScroll, PauseBeforeOpening, SocialBudget,
            ),
            BuiltInTemplate.entries,
        )
        assertEquals(listOf(TemplateSection.Focus, TemplateSection.Unwind, TemplateSection.BePresent, TemplateSection.ScrollLess), TemplateSection.entries)
        assertEquals(listOf(FocusSprint, StudyHour, WorkRhythm), TemplateSection.Focus.templates)
        assertEquals(listOf(EveningCalm, NightPause, SlowMorning), TemplateSection.Unwind.templates)
        assertEquals(listOf(DinnerTogether, FamilyHour, TimeForMe), TemplateSection.BePresent.templates)
        assertEquals(listOf(WalkBeforeYouScroll, PauseBeforeOpening, SocialBudget), TemplateSection.ScrollLess.templates)
        BuiltInTemplate.entries.forEach { assertEquals(it, BuiltInTemplate.byId(it.id)) }
        assertEquals(12, BuiltInTemplate.entries.map { it.id }.distinct().size)
    }

    @Test
    fun `session templates make sessions of their length`() {
        assertEquals(
            expected(FocusSprint, RuleType.Session, distractions, RuleSticker.Bolt) { it.copy(durationMinutes = 45) },
            factory.fromTemplate(FocusSprint, installed),
        )
        assertEquals(
            expected(StudyHour, RuleType.Session, distractions, RuleSticker.Note) { it.copy(durationMinutes = 60) },
            factory.fromTemplate(StudyHour, installed),
        )
        assertEquals(
            expected(TimeForMe, RuleType.Session, distractions, RuleSticker.Leaf) { it.copy(durationMinutes = 30) },
            factory.fromTemplate(TimeForMe, installed),
        )
    }

    @Test
    fun `routine templates make routines on their schedule`() {
        assertEquals(
            expected(WorkRhythm, RuleType.Bedtime, distractions, RuleSticker.Flame) { it.copy(schedule = RuleSchedule(9 * 60, 12 * 60, (1..5).toSet())) },
            factory.fromTemplate(WorkRhythm, installed),
        )
        assertEquals(
            expected(EveningCalm, RuleType.Bedtime, listOf(social, video), RuleSticker.Moon) { it.copy(schedule = RuleSchedule(22 * 60, 23 * 60)) },
            factory.fromTemplate(EveningCalm, installed),
        )
        assertEquals(
            expected(NightPause, RuleType.Bedtime, distractions, RuleSticker.Moon) { it.copy(schedule = RuleSchedule(23 * 60, 7 * 60)) },
            factory.fromTemplate(NightPause, installed),
        )
        assertEquals(
            expected(SlowMorning, RuleType.Bedtime, listOf(social), RuleSticker.Leaf) { it.copy(schedule = RuleSchedule(7 * 60, 8 * 60)) },
            factory.fromTemplate(SlowMorning, installed),
        )
        assertEquals(
            expected(DinnerTogether, RuleType.Bedtime, distractions, RuleSticker.Flame) { it.copy(schedule = RuleSchedule(19 * 60 + 30, 20 * 60 + 15)) },
            factory.fromTemplate(DinnerTogether, installed),
        )
        assertEquals(
            expected(FamilyHour, RuleType.Bedtime, distractions, RuleSticker.Note) { it.copy(schedule = RuleSchedule(16 * 60, 17 * 60, setOf(7))) },
            factory.fromTemplate(FamilyHour, installed),
        )
        // Every day, and overnight.
        assertEquals((1..7).toSet(), factory.fromTemplate(NightPause, installed).schedule?.days)
    }

    @Test
    fun `lock templates make locks with 3 unlocks of 5 minutes a day`() {
        assertEquals(
            expected(WalkBeforeYouScroll, RuleType.Lock, listOf(social), RuleSticker.Lock) {
                it.copy(unlockMethod = UnlockMethod.Walk, unlocksPerDay = 3, unlockDurationMinutes = 5)
            },
            factory.fromTemplate(WalkBeforeYouScroll, installed),
        )
        assertEquals(
            expected(PauseBeforeOpening, RuleType.Lock, listOf(social), RuleSticker.Lock) {
                it.copy(unlockMethod = UnlockMethod.Wait, waitSeconds = 10, unlocksPerDay = 3, unlockDurationMinutes = 5)
            },
            factory.fromTemplate(PauseBeforeOpening, installed),
        )
    }

    @Test
    fun `social budget is a 30-minute limit for the social apps, named after the template`() {
        val limit = factory.fromTemplate(SocialBudget, installed)
        assertEquals(expected(SocialBudget, RuleType.Limit, listOf(social), RuleSticker.Timer) { it.copy(limitMinutes = 30) }, limit)
        assertEquals("string-${SocialBudget.title}", limit.name)
    }

    @Test
    fun `a template rule starts off, not strict, without a note`() {
        BuiltInTemplate.entries.forEach { template ->
            val rule = factory.fromTemplate(template, installed)
            assertEquals(template.type, rule.type)
            assertNull(rule.activeSince)
            assertNull(rule.note)
            // Settings › Strict mode decides, in the editor.
            assertFalse(rule.strictMode)
        }
    }

    @Test
    fun `without matching apps installed, a template preselects none`() {
        BuiltInTemplate.entries.forEach { template ->
            assertEquals(emptyList<UsageApp>(), factory.fromTemplate(template, listOf(music, work, other)).apps)
        }
    }
}
