package com.recentr.app

import android.app.Application
import com.recentr.app.reminders.RuleReminderAlarms
import com.recentr.core.data.RecentrData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RecentrApplication : Application() {
    /** Work that lives as long as the process (rule reminder alarms). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        RecentrData.init(this)
        RuleReminderAlarms.start(this, appScope)
    }
}
