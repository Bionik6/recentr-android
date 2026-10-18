package com.recentr.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors

/**
 * Recentr theme. Exposes the Figma tokens through [RecentrTheme] and maps them onto
 * Material 3 so platform components (sheets, dialogs, menus) inherit Figtree and the palette.
 * The design is light-only.
 */
@Composable
fun RecentrTheme(content: @Composable () -> Unit) {
    val colors = RecentrColors()
    val typography = RecentrTypography()

    val colorScheme = lightColorScheme(
        primary = colors.actionPrimary,
        onPrimary = colors.textOnAction,
        secondary = colors.brandSky,
        onSecondary = colors.textOnDark,
        tertiary = colors.accentTangerine,
        background = colors.bgPage,
        onBackground = colors.textPrimary,
        surface = colors.bgSurface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.bgSunken,
        onSurfaceVariant = colors.textSecondary,
        surfaceContainerLow = colors.bgSurface,
        surfaceContainer = colors.bgSurface,
        surfaceContainerHigh = colors.bgSurface,
        surfaceContainerHighest = colors.bgSunken,
        outline = colors.borderDashed,
        outlineVariant = colors.borderSubtle,
        error = colors.feedbackDanger,
        scrim = colors.overlayScrim,
    )

    val materialTypography = Typography(
        displayLarge = typography.displayHero,
        displayMedium = typography.displayLarge,
        displaySmall = typography.title1,
        headlineLarge = typography.title1,
        headlineMedium = typography.title2,
        headlineSmall = typography.title2,
        titleLarge = typography.title2,
        titleMedium = typography.headline,
        titleSmall = typography.captionMedium,
        bodyLarge = typography.bodyRegular,
        bodyMedium = typography.bodyRegular,
        bodySmall = typography.captionRegular,
        labelLarge = typography.headline,
        labelMedium = typography.captionMedium,
        labelSmall = typography.labelSmall,
    )

    CompositionLocalProvider(
        LocalRecentrColors provides colors,
        LocalRecentrTypography provides typography,
        LocalRecentrSpacing provides RecentrSpacing(),
        LocalRecentrRadius provides RecentrRadius(),
        LocalRecentrSizes provides RecentrSizes(),
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = materialTypography) {
            CompositionLocalProvider(
                LocalContentColor provides colors.textPrimary,
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = colors.brandSky,
                    backgroundColor = colors.alphaSky12,
                ),
                content = content,
            )
        }
    }
}

/** Entry point to the design tokens: `RecentrTheme.colors.brandSky`, `RecentrTheme.typography.headline`… */
object RecentrTheme {
    val colors: RecentrColors
        @Composable @ReadOnlyComposable get() = LocalRecentrColors.current
    val typography: RecentrTypography
        @Composable @ReadOnlyComposable get() = LocalRecentrTypography.current
    val spacing: RecentrSpacing
        @Composable @ReadOnlyComposable get() = LocalRecentrSpacing.current
    val radius: RecentrRadius
        @Composable @ReadOnlyComposable get() = LocalRecentrRadius.current
    val sizes: RecentrSizes
        @Composable @ReadOnlyComposable get() = LocalRecentrSizes.current
}
