package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration

class ViraApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        try {
            container.scheduleBackgroundSync()
        } catch (e: Exception) {
            Log.w("ViraApp", "WorkManager background schedule skipped: ${e.message}")
        }
    }
}
