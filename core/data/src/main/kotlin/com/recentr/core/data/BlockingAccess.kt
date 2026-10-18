package com.recentr.core.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Whether Recentr's accessibility service is on: it is what puts blocked apps on hold. The user
 * turns it on in Settings › Accessibility (after the in-app explanation).
 */
object BlockingAccess {

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it)?.packageName == context.packageName }
    }

    /**
     * Opens Recentr's page in Settings › Accessibility when the system allows it, the
     * Accessibility list otherwise.
     */
    fun openSettings(context: Context) {
        val details = service(context)?.let { component ->
            Intent(ACTION_ACCESSIBILITY_DETAILS)
                .putExtra(Intent.EXTRA_COMPONENT_NAME, component.flattenToString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val list = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = details != null && runCatching { context.startActivity(details) }.isSuccess
        if (!opened) runCatching { context.startActivity(list) }
    }

    /** `Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS` (not in the public SDK; Settings may fall back to the list). */
    private const val ACTION_ACCESSIBILITY_DETAILS = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"

    /** Recentr's accessibility service, as installed. */
    private fun service(context: Context): ComponentName? =
        context.getSystemService(AccessibilityManager::class.java)
            ?.installedAccessibilityServiceList
            ?.map(AccessibilityServiceInfo::getResolveInfo)
            ?.firstOrNull { it.serviceInfo.packageName == context.packageName }
            ?.let { ComponentName(it.serviceInfo.packageName, it.serviceInfo.name) }
}
