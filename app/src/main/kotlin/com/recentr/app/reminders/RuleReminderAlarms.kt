package com.recentr.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.recentr.app.R
import com.recentr.core.data.RecentrData
import com.recentr.core.data.nextRuleReminder
import com.recentr.core.data.reminderMinutesLeft
import com.recentr.core.designsystem.format.clockTimeOf
import com.recentr.core.model.Rule
import com.recentr.core.model.TimeFormat
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * "Starts in N min" notifications of the rules' reminders (Rule detail › Reminders), gated by
 * Settings › Notifications › "Before a routine". One alarm at a time, for the next reminder due:
 * set again whenever the rules or that toggle change, after each reminder, after a reboot and
 * when the clock or the time zone changes.
 */
object RuleReminderAlarms {

    private const val CHANNEL_ID = "rule_reminders"
    private const val EXTRA_RULE_IDS = "ruleIds"
    private const val EXTRA_STARTS = "starts"

    /** Keeps the alarm in step with the rules for as long as the process lives. */
    fun start(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        createChannel(app)
        scope.launch {
            combine(RecentrData.rules.rules, RecentrData.settings.settings.map { it.notifyBeforeRoutine }) { rules, on ->
                if (on) rules else emptyList()
            }.collect { schedule(app, it) }
        }
    }

    /** Sets the alarm of the next reminder among [rules], or cancels it when there is none. */
    fun schedule(context: Context, rules: List<Rule>, now: Long = System.currentTimeMillis()) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val next = nextRuleReminder(rules, now)
        if (next == null) {
            alarms.cancel(alarmIntent(context, emptyMap()))
            return
        }
        val intent = alarmIntent(context, next.starts)
        // Exact when the user allows it (Android 12+), else as close as the system lets us.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
        }
    }

    /** Sets the next alarm from the saved rules and settings (receivers). */
    internal suspend fun reschedule(context: Context) {
        val on = RecentrData.settings.settings.first().notifyBeforeRoutine
        schedule(context, if (on) RecentrData.rules.rules.value else emptyList())
    }

    /** Posts the reminder of each rule of the alarm that still has one. */
    internal suspend fun notifyDue(context: Context, intent: Intent) {
        val ids = intent.getStringArrayExtra(EXTRA_RULE_IDS) ?: return
        val startTimes = intent.getLongArrayExtra(EXTRA_STARTS) ?: return
        val settings = RecentrData.settings.settings.first()
        if (!settings.notifyBeforeRoutine || !canPostNotifications(context)) return
        val use24Hour = settings.timeFormat?.let { it == TimeFormat.TwentyFourHour } ?: DateFormat.is24HourFormat(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val now = System.currentTimeMillis()
        ids.zip(startTimes.toList()).forEach { (id, start) ->
            val rule = RecentrData.rules.rule(id) ?: return@forEach
            if (rule.reminderMinutesBefore == null || !rule.enabled || start <= now) return@forEach
            // The minutes actually left: an approximate alarm (no "Alarms & reminders") can be late.
            val minutes = reminderMinutesLeft(start, now)
            val time = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.recentr.core.designsystem.R.drawable.ic_bell)
                .setContentTitle(context.getString(R.string.reminder_title, rule.name, minutes))
                .setContentText(context.getString(R.string.reminder_text, clockTimeOf(time.hour * 60 + time.minute, use24Hour)))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            @Suppress("MissingPermission") // Checked by canPostNotifications above.
            NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        }
    }

    private fun canPostNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun alarmIntent(context: Context, starts: Map<String, Long>): PendingIntent {
        val ids = starts.keys.toTypedArray()
        val intent = Intent(context, RuleReminderReceiver::class.java)
            .putExtra(EXTRA_RULE_IDS, ids)
            .putExtra(EXTRA_STARTS, ids.map { starts.getValue(it) }.toLongArray())
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** A reminder is due: posts it, then sets the next one. */
class RuleReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                RuleReminderAlarms.notifyDue(context, intent)
                RuleReminderAlarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Reboot, app update, clock or time zone change: alarms are gone or off, set the next one again. */
class RuleReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                RuleReminderAlarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
