package com.recentr.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.recentr.core.designsystem.R

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
    Font(R.font.figtree_extrabold, FontWeight.ExtraBold),
    Font(R.font.figtree_black, FontWeight.Black),
)

/**
 * Builds a Figma text style. Figma letter-spacing is expressed in percent of the font size,
 * line-height as a multiplier — both are converted 1:1 so text boxes match the design.
 */
private fun figmaStyle(
    weight: FontWeight,
    size: Float,
    lineHeight: Float,
    letterSpacingPercent: Float,
) = TextStyle(
    fontFamily = Figtree,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = (letterSpacingPercent / 100f).em,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    // Fixed: the line box is exactly `lineHeight`, like Figma, even when it is smaller than the
    // font's natural ascent + descent (Display styles use a 1.0 line height).
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
        mode = LineHeightStyle.Mode.Fixed,
    ),
)

/** Text styles from "Foundations · 02 Typography". */
@Immutable
data class RecentrTypography(
    val displayHero: TextStyle = figmaStyle(FontWeight.ExtraBold, 72f, 1f, -3f),
    val displayNumber: TextStyle = figmaStyle(FontWeight.ExtraBold, 104f, 1f, -4f),
    val displayLarge: TextStyle = figmaStyle(FontWeight.Bold, 34f, 1.15f, -1.5f),
    val title1: TextStyle = figmaStyle(FontWeight.Bold, 28f, 1.18f, -1f),
    val title2: TextStyle = figmaStyle(FontWeight.Bold, 22f, 1.22f, -0.6f),
    val headline: TextStyle = figmaStyle(FontWeight.SemiBold, 17f, 1.3f, -0.3f),
    val bodyRegular: TextStyle = figmaStyle(FontWeight.Normal, 15f, 1.4f, -0.1f),
    val bodyMedium: TextStyle = figmaStyle(FontWeight.Medium, 15f, 1.4f, -0.1f),
    val captionRegular: TextStyle = figmaStyle(FontWeight.Normal, 13f, 1.35f, 0f),
    val captionMedium: TextStyle = figmaStyle(FontWeight.SemiBold, 13f, 1.35f, 0f),
    val labelSmall: TextStyle = figmaStyle(FontWeight.SemiBold, 11f, 1.3f, 0.2f),
    val brandWordmark: TextStyle = figmaStyle(FontWeight.Black, 26f, 1f, -1f),
    val noteHandwritten: TextStyle = figmaStyle(FontWeight.Medium, 15f, 1.4f, -0.1f),
)

val LocalRecentrTypography = staticCompositionLocalOf { RecentrTypography() }
