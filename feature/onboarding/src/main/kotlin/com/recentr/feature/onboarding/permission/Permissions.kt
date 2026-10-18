package com.recentr.feature.onboarding.permission

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.recentr.core.data.UsageAccess

/** Usage access (`PACKAGE_USAGE_STATS`), granted by the user in Settings — see [UsageAccess]. */
internal fun Context.hasUsageAccess(): Boolean = UsageAccess.isGranted(this)

/**
 * Opens Settings › Usage access, on this app's page when the device supports it (`package:` uri),
 * otherwise on the list. Returns false when no settings screen could be opened.
 */
internal fun Context.openUsageAccessSettings(): Boolean {
    val withPackage = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:$packageName".toUri())
    val generic = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    for (intent in listOf(withPackage, generic)) {
        try {
            startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the next variant.
        } catch (_: SecurityException) {
            // Some OEM builds protect the per-app page; fall back to the list.
        }
    }
    return false
}

/** True when the app may post notifications (runtime permission on API 33+, app setting before). */
internal fun Context.canPostNotifications(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(this).areNotificationsEnabled()
    }
