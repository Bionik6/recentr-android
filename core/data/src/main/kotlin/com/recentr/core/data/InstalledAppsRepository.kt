package com.recentr.core.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import com.recentr.core.model.AppCategory
import com.recentr.core.model.AppId
import com.recentr.core.model.UsageApp
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * The apps installed on this device that have a launcher entry — what the Rules and Focus app
 * pickers offer and what "all apps" blocks. Read from `PackageManager` (visible through the
 * `<queries>` of this module's manifest), without the [protectedPackages].
 */
class InstalledAppsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    private val _apps = MutableStateFlow<List<UsageApp>?>(null)

    /** Launcher apps sorted by label, or null until the first [refresh] is done. */
    val apps: StateFlow<List<UsageApp>?> = _apps.asStateFlow()

    /** Re-reads the launcher apps (installs and uninstalls since the last read). */
    suspend fun refresh(): List<UsageApp> = withContext(Dispatchers.IO) { launcherApps() }.also { _apps.value = it }

    /** The apps, read once and then kept until the next [refresh]. */
    suspend fun load(): List<UsageApp> = _apps.value ?: refresh()

    /** The app published under [packageName], or null when it is not installed (or not visible). */
    fun app(packageName: String): UsageApp? = packageManager.describeApp(packageName)

    /**
     * Never put on hold, and not offered by the pickers: Recentr, the home screens, System UI and
     * Settings (so blocking can always be turned off).
     */
    val protectedPackages: Set<String> by lazy {
        buildSet {
            add(appContext.packageName)
            add(SYSTEM_UI)
            addAll(packagesFor(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)))
            addAll(packagesFor(Intent(Settings.ACTION_SETTINGS)))
        }
    }

    private fun packagesFor(intent: Intent): List<String> = queryActivities(intent).mapNotNull { it.activityInfo?.packageName }

    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }

    private fun launcherApps(): List<UsageApp> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return packagesFor(launcher)
            .distinct()
            .filter { it !in protectedPackages }
            .mapNotNull { packageManager.describeApp(it) }
            .sortedWith(compareBy(collator) { it.label })
    }
}

private const val SYSTEM_UI = "com.android.systemui"

/**
 * [packageName] as an app of the usage lists and pickers: catalogue packages map to their [AppId]
 * (Figma name and category), other apps keep their own label and declared category. Null when the
 * package is not installed, not visible to Recentr (no launcher entry) or has no label.
 */
internal fun PackageManager.describeApp(packageName: String): UsageApp? {
    AppId.fromPackageName(packageName)?.let { return UsageApp.of(it, packageName) }
    val info = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            getApplicationInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    }
    val label = info.loadLabel(this).toString().takeIf { it.isNotBlank() } ?: return null
    return UsageApp(packageName, label, info.usageCategory)
}

/** Category declared by the app (`android:appCategory`). */
internal val ApplicationInfo.usageCategory: AppCategory
    get() = when (category) {
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
        ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_NEWS -> AppCategory.Entertainment
        ApplicationInfo.CATEGORY_AUDIO -> AppCategory.Music
        ApplicationInfo.CATEGORY_GAME -> AppCategory.Games
        ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_MAPS -> AppCategory.Productivity
        else -> AppCategory.Other
    }
