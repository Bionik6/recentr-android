package com.recentr.app.blocking

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.recentr.core.data.RecentrData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The app blocker. Android tells it which activity comes to the front (window state changes,
 * nothing else); when that app is on hold ([RecentrData.holds]) it opens the [HoldActivity] over
 * it. It also re-checks the app in front when the rules, the focus session or the apps opened
 * anyway change, and every few seconds (schedules starting, "Open anyway" running out, limits
 * used up).
 */
class HoldService : AccessibilityService() {

    private val scope = MainScope()

    /** The app in front, as far as the last activity events tell. */
    private var foreground: String? = null

    /** Whether an (app, class) pair is an activity — dialogs, keyboards and System UI windows aren't. */
    private val activities = HashMap<String, Boolean>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        RecentrData.recheckBlockingAccess()
        val holds = RecentrData.holds
        scope.launch { RecentrData.installedApps.load() }
        scope.launch {
            combine(RecentrData.rules.rules, RecentrData.focus.run, holds.unlocks) { _, _, _ -> }.collect { check() }
        }
        scope.launch {
            var usageReadAt = 0L
            while (isActive) {
                val now = System.currentTimeMillis()
                RecentrData.rules.settle(now)
                RecentrData.focus.advance(now)
                if (now - usageReadAt >= USAGE_REFRESH_MILLIS) {
                    usageReadAt = now
                    runCatching { holds.refreshUsage() }
                }
                check()
                delay(TICK_MILLIS)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val app = event.packageName?.toString() ?: return
        val className = event.className?.toString()
        // The hold screen covers the held app: that app is still the one "in front".
        if (app == packageName && className == HoldActivity::class.java.name) return
        if (!isActivity(app, className)) return
        foreground = app
        check()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        RecentrData.recheckBlockingAccess()
        super.onDestroy()
    }

    /** Puts the app in front on hold when it should be (once: the hold screen may already show it). */
    private fun check() {
        val app = foreground ?: return
        if (app == packageName || HoldActivity.showing == app) return
        if (RecentrData.holds.reason(app, System.currentTimeMillis()) == null) return
        startActivity(HoldActivity.intent(this, app))
    }

    private fun isActivity(app: String, className: String?): Boolean {
        if (className.isNullOrEmpty()) return false
        return activities.getOrPut("$app/$className") {
            val component = ComponentName(app, className)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getActivityInfo(component, PackageManager.ComponentInfoFlags.of(0))
                } else {
                    packageManager.getActivityInfo(component, 0)
                }
            }.isSuccess
        }
    }

    private companion object {
        const val TICK_MILLIS = 5_000L
        const val USAGE_REFRESH_MILLIS = 60_000L
    }
}
