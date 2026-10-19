package com.recentr.app.blocking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recentr.core.data.RecentrData
import com.recentr.core.designsystem.format.LocalUse24HourClock
import com.recentr.core.designsystem.motion.ProvideRecentrMotion
import com.recentr.core.designsystem.theme.RecentrTheme
import com.recentr.core.model.AppSettings
import com.recentr.core.model.TimeFormat

/**
 * The shield ([HoldScreen]): opened by [HoldService] over an app blocked by a rule or the focus
 * session, with the unlock friction behind "Unlock anyway". It closes itself once the app may be
 * used again (session over, unlocked…); "I've got this" goes back to the home screen, never to
 * the held app.
 */
class HoldActivity : ComponentActivity() {

    private var held by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        held = intent.getStringExtra(EXTRA_PACKAGE)
        onBackPressedDispatcher.addCallback(this) { goHome() }
        setContent {
            RecentrTheme {
                val settings = RecentrData.settings.settings.collectAsStateWithLifecycle(AppSettings()).value
                ProvideRecentrMotion(reduceMotion = settings.reduceMotion, hapticsEnabled = settings.haptics) {
                    val use24Hour = settings.timeFormat?.let { it == TimeFormat.TwentyFourHour }
                        ?: DateFormat.is24HourFormat(LocalContext.current)
                    CompositionLocalProvider(LocalUse24HourClock provides use24Hour) {
                        held?.let { app ->
                            HoldScreen(
                                packageName = app,
                                onClose = ::goHome,
                                onOpenRecentr = ::openRecentr,
                                onReleased = ::finish,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        held = intent.getStringExtra(EXTRA_PACKAGE)
        showing = held
    }

    override fun onStart() {
        super.onStart()
        showing = held
    }

    // Not onPause: a system dialog over the hold screen (the camera permission) pauses it while it
    // stays on screen, and the blocker would put the app on hold again over the dialog.
    override fun onStop() {
        super.onStop()
        if (showing == held) showing = null
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    /** Back to Recentr where the user left it (the running focus session, the habit's tasks…). */
    private fun openRecentr() {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"

        /** The app the hold screen is showing, or null when it isn't on screen. */
        @Volatile
        var showing: String? = null
            private set

        /**
         * Opens the hold screen over [packageName]. Not a user action: the held app isn't told the
         * user is leaving it, so a playing video doesn't go picture-in-picture over the shield.
         */
        fun intent(context: Context, packageName: String): Intent =
            Intent(context, HoldActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
