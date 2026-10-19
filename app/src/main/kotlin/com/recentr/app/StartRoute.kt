package com.recentr.app

import com.recentr.core.designsystem.component.RecentrTab

/**
 * Debug-only QA shortcuts of the launch intent:
 * `adb shell am start -n com.recentr.app/.MainActivity --es start main|onboarding --es tab rules`.
 *
 * They only apply to the launch that carries them ([freshLaunch]). When the system brings the
 * task back after its process died (the next morning, say) or from Recents, the activity is
 * recreated with the task's first intent, extras included: honouring them again sent a user who
 * had finished the onboarding back to its first screen.
 */
internal data class QaShortcuts(val start: String? = null, val tab: RecentrTab? = null) {
    companion object {
        fun of(debuggable: Boolean, freshLaunch: Boolean, start: String?, tab: String?): QaShortcuts {
            if (!debuggable || !freshLaunch) return QaShortcuts()
            return QaShortcuts(
                start = start?.takeIf { it == "main" || it == "onboarding" },
                tab = RecentrTab.entries.firstOrNull { it.name.equals(tab, ignoreCase = true) },
            )
        }
    }
}

/**
 * Whether the app opens on the onboarding: the QA shortcut when there is one, else whether the
 * stored profile finished it.
 */
internal fun showsOnboarding(stored: StartState, forcedStart: String?): Boolean = when (forcedStart) {
    "main" -> false
    "onboarding" -> true
    else -> stored == StartState.Onboarding
}

/**
 * Whether a screen restored on the onboarding should go to Home instead: the app opened on Home
 * ([showsOnboarding] false: the stored profile finished it, no QA shortcut asked for it), so the
 * onboarding it was left on is stale. Never while the onboarding is meant to run.
 */
internal fun leavesRestoredOnboarding(showsOnboarding: Boolean, restoredOnOnboarding: Boolean): Boolean =
    !showsOnboarding && restoredOnOnboarding
