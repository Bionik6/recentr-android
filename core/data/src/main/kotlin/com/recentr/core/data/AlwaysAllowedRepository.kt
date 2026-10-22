package com.recentr.core.data

import com.recentr.core.model.AlwaysAllowed
import com.recentr.core.model.UsageApp
import java.io.File
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * The apps always allowed (Settings › Always allowed), none at first. Kept in memory for the app
 * blocker and the screens, and saved to [file] as JSON after every change.
 *
 * A new list goes through the edit policy ([AlwaysAllowed.choosing]) against the [rules] and the
 * [focus] session of the moment: an app whose rule is engaged waits, and [settle] lets it join once
 * that's over. Removing an app applies now.
 */
class AlwaysAllowedRepository(
    /** Where the list is saved, or null to keep it in memory only (previews, tests). */
    private val file: File? = null,
    private val rules: RulesRepository,
    private val focus: FocusRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    writeDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _allowed = MutableStateFlow(file?.let(::read) ?: AlwaysAllowed())
    val allowed: StateFlow<AlwaysAllowed> = _allowed.asStateFlow()

    /** One write at a time, each with the latest state. */
    private val writes = CoroutineScope(SupervisorJob() + writeDispatcher)

    /** [apps] chosen at [now] (the picker's "Allow"): what it adds may wait ([AlwaysAllowed.choosing]). */
    fun choose(apps: List<UsageApp>, now: Long = clock()): AlwaysAllowed {
        val chosen = _allowed.updateAndGet { it.choosing(apps, rules.rules.value, focus.run.value, now, zone()) }
        persist()
        return chosen
    }

    /**
     * [packageName] no longer allowed, now, whether it's allowed or waits to join: the list as last
     * chosen without it, through the edit policy, so what still waits joins when it can.
     */
    fun remove(packageName: String, now: Long = clock()): AlwaysAllowed =
        choose(_allowed.value.latestWish.filterNot { it.packageName == packageName }, now)

    /** "Drop changes": the apps waiting to join don't, unless their time has come by [now]. */
    fun dropPending(now: Long = clock()) {
        _allowed.update { AlwaysAllowed(it.appsAt(now)) }
        persist()
    }

    /** The apps waiting to join join once their time has come. Call periodically, like [RulesRepository.settle]. */
    fun settle(now: Long) {
        val current = _allowed.value
        val settled = current.settled(now)
        if (settled != current && _allowed.compareAndSet(current, settled)) persist()
    }

    /** Allows nothing again ("Delete account"). */
    fun reset() {
        _allowed.value = AlwaysAllowed()
        persist()
    }

    private fun persist() {
        val target = file ?: return
        writes.launch { target.writeAtomically(json.encodeToString(_allowed.value)) }
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /** The saved list, or null when there's none yet (or the file can't be read). */
        fun read(file: File): AlwaysAllowed? = runCatching {
            if (file.exists()) json.decodeFromString<AlwaysAllowed>(file.readText()) else null
        }.getOrNull()
    }
}
