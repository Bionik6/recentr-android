package com.recentr.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens from the Figma "Foundations · 01 Colour" collection.
 * Names mirror the Figma variables (`bg/page` → [bgPage], `text/on-dark-muted` → [textOnDarkMuted]…).
 */
@Immutable
data class RecentrColors(
    // bg
    val bgPage: Color = Color(0xFFF6F7FB),
    val bgSurface: Color = Color(0xFFFFFFFF),
    val bgSunken: Color = Color(0xFFEEF1F6),
    // text
    val textPrimary: Color = Color(0xFF0E1220),
    val textSecondary: Color = Color(0xFF5B6475),
    val textTertiary: Color = Color(0xFF9AA3B2),
    val textOnDark: Color = Color(0xFFFFFFFF),
    val textOnDarkMuted: Color = Color(0xB8FFFFFF),
    val textOnAction: Color = Color(0xFFFFFFFF),
    // border
    val borderSubtle: Color = Color(0xFFE6E9F0),
    val borderDashed: Color = Color(0xFFC3CAD8),
    // brand
    val brandSky: Color = Color(0xFF2F7BF6),
    val brandSkyLight: Color = Color(0xFF7CB8FF),
    val brandSkyDeep: Color = Color(0xFF1452D4),
    // action
    val actionPrimary: Color = Color(0xFF0E1220),
    // accent
    val accentNote: Color = Color(0xFFFFE27A),
    val accentNoteInk: Color = Color(0xFF4A3A00),
    val accentTangerine: Color = Color(0xFFFF7A1A),
    val accentNoteBlue: Color = Color(0xFFDDEBFF),
    val accentNoteMint: Color = Color(0xFFD9F7E6),
    val accentNotePink: Color = Color(0xFFFFE0EE),
    // feedback
    val feedbackSuccess: Color = Color(0xFF22C55E),
    val feedbackDanger: Color = Color(0xFFFF3B30),
    // aurora
    val auroraMint: Color = Color(0xFFBFF2DA),
    val auroraBlue: Color = Color(0xFFCFE2FF),
    val auroraPink: Color = Color(0xFFFFD9EE),
    // fill
    val fillSelection: Color = Color(0xFFE9EDF4),
    val fillGrabber: Color = Color(0xFFD3D8E2),
    // overlay
    val overlayScrim: Color = Color(0x5C0E1220),
    // alpha
    val alphaWhite80: Color = Color(0xCCFFFFFF),
    val alphaWhite24: Color = Color(0x3DFFFFFF),
    val alphaInk06: Color = Color(0x0F0E1220),
    val alphaTangerine14: Color = Color(0x24FF7A1A),
    val alphaSky12: Color = Color(0x1F2F7BF6),
    // hero
    val heroSky1: Color = Color(0xFF1E5BDC),
    val heroSky2: Color = Color(0xFF3C86FA),
    val heroSky3: Color = Color(0xFF8DC2FF),
    val heroSky4: Color = Color(0xFFDCEBFF),
    val heroCloud: Color = Color(0xFFFFFFFF),
)

val LocalRecentrColors = staticCompositionLocalOf { RecentrColors() }
