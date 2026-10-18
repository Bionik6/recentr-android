package com.recentr.feature.rules.model

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A confirmation toast ("Session started · 30 min"). */
internal data class RulesToast(val text: String, val postedAt: Long = SystemClock.elapsedRealtime()) {
    /** Toasts are only worth showing right after the action that posted them. */
    val isFresh: Boolean get() = SystemClock.elapsedRealtime() - postedAt < FRESH_MS

    private companion object {
        const val FRESH_MS = 4_000L
    }
}

/**
 * Feature-wide toast channel: editors and sheets post, the visible rules screen (list or detail)
 * shows it. Kept out of the ViewModels because the editor sheet closes right after posting.
 */
internal object RulesMessages {
    private val _toast = MutableStateFlow<RulesToast?>(null)
    val toast: StateFlow<RulesToast?> = _toast.asStateFlow()

    fun show(text: String) {
        _toast.value = RulesToast(text)
    }

    fun consume(toast: RulesToast) {
        _toast.compareAndSet(toast, null)
    }
}
