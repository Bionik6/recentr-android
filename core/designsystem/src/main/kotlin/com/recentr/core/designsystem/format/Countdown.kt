package com.recentr.core.designsystem.format

/**
 * How a countdown reads: "1h05m", "24m", to the second only in its last minute, "42s". The
 * seconds go up, so "0s" shows at the end only; the minutes then drop, as a clock's would.
 */
fun countdown(millis: Long): String {
    val seconds = ((millis + 999) / 1000).coerceAtLeast(0)
    val minutes = seconds / 60 % 60
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${minutes}m"
        else -> "%dh%02dm".format(seconds / 3600, minutes)
    }
}
