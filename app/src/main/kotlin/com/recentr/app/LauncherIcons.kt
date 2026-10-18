package com.recentr.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.recentr.core.model.AppIconStyle

/**
 * Switches the launcher icon (Settings › Appearance › App icon) by enabling one of the
 * `activity-alias` entries of the manifest and disabling the others.
 */
object LauncherIcons {
    /** Aliases are declared relative to the app namespace (com.recentr.app), like MainActivity. */
    private fun alias(style: AppIconStyle): String =
        MainActivity::class.java.name.substringBeforeLast('.') + ".Launcher" + style.name

    fun apply(context: Context, style: AppIconStyle) {
        val pm = context.packageManager
        // Enable the new entry first so the app never disappears from the launcher.
        val ordered = AppIconStyle.entries.sortedBy { it != style }
        ordered.forEach { candidate ->
            val component = ComponentName(context.packageName, alias(candidate))
            val wanted = if (candidate == style) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (isEnabled(pm, component, default = candidate == AppIconStyle.Sky) != (candidate == style)) {
                pm.setComponentEnabledSetting(component, wanted, PackageManager.DONT_KILL_APP)
            }
        }
    }

    private fun isEnabled(pm: PackageManager, component: ComponentName, default: Boolean): Boolean =
        when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> default
            else -> false
        }
}
