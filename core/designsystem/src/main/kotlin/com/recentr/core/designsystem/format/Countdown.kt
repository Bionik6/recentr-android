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

/**
 * A countdown on a clock, to the second: "24:38", or "03:24:57" from an hour on. The seconds go
 * up, so "00:00" shows at the end only.
 */
fun clockCountdown(millis: Long): String {
    val seconds = ((millis + 999) / 1000).coerceAtLeast(0)
    val (hours, minutes, rest) = Triple(seconds / 3600, seconds / 60 % 60, seconds % 60)
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, rest) else "%02d:%02d".format(minutes, rest)
}
