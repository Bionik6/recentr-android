package com.recentr.app

import android.app.Application
import com.recentr.core.data.RecentrData

class RecentrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RecentrData.init(this)
    }
}
