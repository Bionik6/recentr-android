package com.recentr.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Settings › Time format: true for the 24-hour clock ("21:00"), false for AM/PM ("9:00 PM").
 * MainActivity provides it (the device setting until the user picks one).
 */
val LocalUse24HourClock = staticCompositionLocalOf { false }

private val Clock12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val Hour12: DateTimeFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)
private val Digits12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.US)
private val Clock24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

private fun localTime(minuteOfDay: Int): LocalTime = LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60)

/** "9:00 PM" or "21:00". */
@Composable
@ReadOnlyComposable
fun clockTime(minuteOfDay: Int): String = clockTimeOf(minuteOfDay, LocalUse24HourClock.current)

/** [clockTime] on the given clock (Settings › Time format samples). */
fun clockTimeOf(minuteOfDay: Int, use24Hour: Boolean): String =
    localTime(minuteOfDay).format(if (use24Hour) Clock24 else Clock12)

/** "9 PM", "9:30 PM" or "21:00", "21:30": minutes only when needed on the AM/PM clock. */
@Composable
@ReadOnlyComposable
fun clockTimeShort(minuteOfDay: Int): String {
    val time = localTime(minuteOfDay)
    val formatter = when {
        LocalUse24HourClock.current -> Clock24
        time.minute == 0 -> Hour12
        else -> Clock12
    }
    return time.format(formatter)
}

/** "9:00" or "21:00": the digits alone (the AM/PM goes in a caption). */
@Composable
@ReadOnlyComposable
fun clockDigits(minuteOfDay: Int): String =
    localTime(minuteOfDay).format(if (LocalUse24HourClock.current) Clock24 else Digits12)

/** "9" or "21", "9:30" or "21:30": hours alone when on the hour (compact "9 – 5" ranges). */
@Composable
@ReadOnlyComposable
fun clockHours(minuteOfDay: Int): String {
    val time = localTime(minuteOfDay)
    val hour = if (LocalUse24HourClock.current) time.hour else (time.hour + 11) % 12 + 1
    return if (time.minute == 0) hour.toString() else "%d:%02d".format(hour, time.minute)
}

/** [clockTime] of an epoch time in the device zone. */
@Composable
@ReadOnlyComposable
fun wallClockTime(epochMillis: Long): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    return clockTime(time.hour * 60 + time.minute)
}
