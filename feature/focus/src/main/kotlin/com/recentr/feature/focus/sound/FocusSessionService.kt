package com.recentr.feature.focus.sound

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.recentr.core.data.RecentrData
import com.recentr.core.model.FocusPhase
import com.recentr.core.model.FocusRun
import com.recentr.core.model.FocusSoundLayer
import com.recentr.feature.focus.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps a focus session going with Recentr in the background or the screen off: its phases change
 * on time, and its sounds play through each round and pause on breaks. A foreground service with an
 * ongoing notification, from the session's start to its end.
 */
class FocusSessionService : Service() {

    private val scope = MainScope()
    private lateinit var mixer: AmbientMixer

    /** What the mixer was last asked for, so the session's updates only change it when they should. */
    private var speaker: Speaker = Speaker.Off

    private sealed interface Speaker {
        data class Playing(val mix: List<FocusSoundLayer>) : Speaker
        data class Muted(val mix: List<FocusSoundLayer>) : Speaker
        data object Off : Speaker
    }

    override fun onCreate() {
        super.onCreate()
        mixer = AmbientMixer(this)
        createChannel()
        // The session's clock, for when none of its screens is up.
        scope.launch {
            while (isActive) {
                RecentrData.focus.advance(System.currentTimeMillis())
                delay(1_000)
            }
        }
        scope.launch {
            RecentrData.focus.run.collectLatest { run ->
                if (run == null) {
                    update(Speaker.Off)
                    // Once the sounds have faded out, unless another session starts first.
                    delay(1_000)
                    stopSelf()
                } else {
                    update(speakerFor(run))
                    if (canPostNotifications()) {
                        @Suppress("MissingPermission") // Checked by canPostNotifications.
                        NotificationManagerCompat.from(this@FocusSessionService).notify(NOTIFICATION_ID, notification(run))
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val run = RecentrData.focus.run.value
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(run), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        if (run == null) stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        mixer.release()
        super.onDestroy()
    }

    /** The round's sounds play; a break holds them in silence; a pause or a round waiting to start stops them. */
    private fun speakerFor(run: FocusRun): Speaker {
        val mix = run.config.sounds
        return when {
            mix.isEmpty() || run.isPaused -> Speaker.Off
            run.phase == FocusPhase.Focus -> Speaker.Playing(mix)
            else -> Speaker.Muted(mix)
        }
    }

    private fun update(wanted: Speaker) {
        if (wanted == speaker) return
        when (wanted) {
            is Speaker.Playing -> mixer.play(wanted.mix)
            is Speaker.Muted -> mixer.mute(wanted.mix)
            Speaker.Off -> mixer.stop()
        }
        speaker = wanted
    }

    /** "Deep work · Round 1 of 4", counting down to the end of the round or break. */
    private fun notification(run: FocusRun?): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.recentr.core.designsystem.R.drawable.ic_hourglass)
            .setContentTitle(run?.config?.label ?: getString(R.string.focus_notification_channel))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openRecentr())
        if (run != null) {
            val text = when {
                run.phase == FocusPhase.Break -> getString(R.string.focus_notification_break, run.round + 1)
                run.isPaused -> getString(R.string.focus_notification_paused, run.round, run.config.rounds)
                else -> getString(R.string.focus_notification_round, run.round, run.config.rounds)
            }
            builder.setContentText(text)
            val endsAt = run.phaseEndsAt
            if (endsAt != null) {
                builder.setWhen(endsAt).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
            } else {
                builder.setShowWhen(false)
            }
        }
        return builder.build()
    }

    /** Back to Recentr where it was left: the launcher entry, whichever icon is on. */
    private fun openRecentr(): PendingIntent? {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun canPostNotifications(): Boolean =
        NotificationManagerCompat.from(this).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.focus_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.focus_notification_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "focus_session"
        private const val NOTIFICATION_ID = 0xF0C5

        /** Keeps the session going on its own once a screen of it is up. Nothing when Android won't let it start. */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, FocusSessionService::class.java))
            } catch (_: IllegalStateException) {
                // In the background, Android 12+ doesn't start foreground services.
            }
        }
    }
}
