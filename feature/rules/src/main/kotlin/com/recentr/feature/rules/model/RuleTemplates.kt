package com.recentr.feature.rules.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.recentr.core.designsystem.component.TemplateColor
import com.recentr.core.model.AppId
import com.recentr.core.model.Rule
import com.recentr.core.model.RuleSchedule
import com.recentr.core.model.RuleSticker
import com.recentr.core.model.RuleType
import com.recentr.feature.rules.R
import com.recentr.feature.rules.ui.durationLabel

/** Template filters of the Templates view ("All" = null). */
internal enum class TemplateCategory(@param:StringRes val label: Int) {
    Focus(R.string.rules_filter_focus),
    Sleep(R.string.rules_filter_sleep),
    Family(R.string.rules_filter_family),
    Weekend(R.string.rules_filter_weekend),
}

/** Built-in rule templates (Figma Template Cards + session suggestions). */
internal enum class BuiltInTemplate(
    val id: String,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    /** Badge: a duration in minutes, or a fixed label. */
    val badgeMinutes: Int?,
    @param:StringRes val badgeLabel: Int?,
    val color: TemplateColor,
    val sticker: RuleSticker,
    val category: TemplateCategory,
    val type: RuleType,
) {
    DeepWork("tpl-deep-work", R.string.rules_tpl_deep_work, R.string.rules_tpl_deep_work_body, 30, null, TemplateColor.Sky, RuleSticker.Bolt, TemplateCategory.Focus, RuleType.Session),
    WindDown("tpl-wind-down", R.string.rules_tpl_wind_down, R.string.rules_tpl_wind_down_body, 60, null, TemplateColor.Night, RuleSticker.Moon, TemplateCategory.Sleep, RuleType.Bedtime),
    MorningCalm("tpl-morning-calm", R.string.rules_tpl_morning_calm, R.string.rules_tpl_morning_calm_body, null, R.string.rules_badge_daily, TemplateColor.Mint, RuleSticker.Leaf, TemplateCategory.Sleep, RuleType.Bedtime),
    Dinner("tpl-dinner", R.string.rules_tpl_dinner, R.string.rules_tpl_dinner_body, 45, null, TemplateColor.Sunset, RuleSticker.Note, TemplateCategory.Family, RuleType.Session),
    Study("tpl-study", R.string.rules_tpl_study, R.string.rules_tpl_study_body, 60, null, TemplateColor.Mint, RuleSticker.Note, TemplateCategory.Focus, RuleType.Session),
    MeTime("tpl-me-time", R.string.rules_tpl_me_time, R.string.rules_tpl_me_time_body, 45, null, TemplateColor.Sunset, RuleSticker.Leaf, TemplateCategory.Weekend, RuleType.Session),
    Weekend("tpl-weekend", R.string.rules_tpl_weekend, R.string.rules_tpl_weekend_body, null, R.string.rules_badge_weekend, TemplateColor.Night, RuleSticker.Lock, TemplateCategory.Weekend, RuleType.Lock),
    ;

    /** Rule prototype created from this template (id and name filled by the caller). */
    fun prototype(id: String, name: String): Rule {
        val social = listOf(AppId.Instagram, AppId.X, AppId.TikTok)
        return when (this) {
            DeepWork, Study -> Rule(id, RuleType.Session, name, apps = social + AppId.YouTube, durationMinutes = badgeMinutes, sticker = sticker)
            Dinner, MeTime -> Rule(id, RuleType.Session, name, apps = social, durationMinutes = badgeMinutes, sticker = sticker)
            WindDown -> Rule(
                id, RuleType.Bedtime, name,
                apps = social + AppId.YouTube,
                schedule = RuleSchedule(22 * 60, 23 * 60),
                sticker = sticker,
            )
            MorningCalm -> Rule(
                id, RuleType.Bedtime, name,
                schedule = RuleSchedule(6 * 60, 8 * 60),
                blockAllApps = true,
                allowedApps = listOf(AppId.Gmail),
                sticker = sticker,
            )
            Weekend -> Rule(
                id, RuleType.Lock, name,
                apps = social + AppId.Facebook,
                schedule = RuleSchedule(0, 0, setOf(6, 7)),
                sticker = sticker,
                unlocksPerDay = 1,
            )
        }
    }

    companion object {
        fun byId(id: String): BuiltInTemplate? = entries.firstOrNull { it.id == id }

        /** Session sheet "Suggestions" row. */
        val sessionSuggestions = listOf(DeepWork, MeTime, Study)
    }
}

/** A template as shown by a template card. */
@Immutable
internal data class TemplateCardUi(
    val id: String,
    val title: String,
    val body: String,
    val badge: String,
    val sticker: RuleSticker,
    val color: TemplateColor,
    val category: TemplateCategory?,
)

@Composable
internal fun BuiltInTemplate.toCard(): TemplateCardUi = TemplateCardUi(
    id = id,
    title = stringResource(title),
    body = stringResource(body),
    badge = badgeMinutes?.let { durationLabel(it) } ?: stringResource(badgeLabel!!),
    sticker = sticker,
    color = color,
    category = category,
)

/** Card for a template saved with "Save as template". */
@Composable
internal fun Rule.toSavedTemplateCard(): TemplateCardUi = TemplateCardUi(
    id = id,
    title = name,
    body = stringResource(R.string.rules_saved_template_body),
    badge = durationMinutes?.let { durationLabel(it) } ?: stringResource(R.string.rules_badge_daily),
    sticker = sticker,
    color = when (type) {
        RuleType.Session -> TemplateColor.Sky
        RuleType.Bedtime, RuleType.Lock -> TemplateColor.Night
        RuleType.Habit -> TemplateColor.Mint
        RuleType.Limit -> TemplateColor.Sunset
    },
    category = null,
)
