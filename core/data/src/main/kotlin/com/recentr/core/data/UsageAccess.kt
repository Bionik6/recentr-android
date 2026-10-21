package com.recentr.core.data

import android.Manifest
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Screen time on Android = the "Usage access" special app-op (`PACKAGE_USAGE_STATS`), granted by
 * the user in Settings — there is no runtime dialog for it.
 */
object UsageAccess {

    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Deprecated in SDK 37 for a flagged checkOpNoThrow overload; still the API for 29–36.
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /**
     * Opens Settings › Usage access, on this app's page when the device supports it (`package:` uri),
     * otherwise on the list. Returns false when no settings screen could be opened.
     */
    fun openSettings(context: Context): Boolean {
        val withPackage = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
        val generic = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        for (intent in listOf(withPackage, generic)) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // Try the next variant.
            } catch (_: SecurityException) {
                // Some OEM builds protect the per-app page; fall back to the list.
            }
        }
        return false
    }
}
