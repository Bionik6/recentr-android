package com.recentr.feature.onboarding.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.recentr.feature.onboarding.R
import kotlin.math.roundToInt

/** Compact duration as written in the design: "4h20", "6h", "45m". */
@Composable
@ReadOnlyComposable
fun compactDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> stringResource(R.string.onb_duration_hm, h, m)
        h > 0 -> stringResource(R.string.onb_duration_h, h)
        else -> stringResource(R.string.onb_duration_m, m)
    }
}

/**
 * Spelled-out duration rounded for prose: whole hours from one hour up ("2 hours"), otherwise
 * minutes rounded to 5 ("45 minutes").
 */
@Composable
@ReadOnlyComposable
fun spokenDuration(minutes: Int): String = if (minutes >= 60) {
    val hours = (minutes / 60f).roundToInt()
    pluralStringResource(R.plurals.onb_hours, hours, hours)
} else {
    val rounded = ((minutes / 5f).roundToInt() * 5).coerceAtLeast(5)
    pluralStringResource(R.plurals.onb_minutes, rounded, rounded)
}
