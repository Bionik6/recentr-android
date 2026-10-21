package com.recentr.feature.onboarding.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.recentr.core.data.UsageAccess

/** Usage access (`PACKAGE_USAGE_STATS`), granted by the user in Settings — see [UsageAccess]. */
internal fun Context.hasUsageAccess(): Boolean = UsageAccess.isGranted(this)

/** Settings › Usage access — see [UsageAccess.openSettings]. */
internal fun Context.openUsageAccessSettings(): Boolean = UsageAccess.openSettings(this)

/** True when the app may post notifications (runtime permission on API 33+, app setting before). */
internal fun Context.canPostNotifications(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(this).areNotificationsEnabled()
    }
