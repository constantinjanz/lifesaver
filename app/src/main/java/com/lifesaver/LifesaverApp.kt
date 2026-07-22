package com.lifesaver

import android.app.Application

// Application singleton. Will own the Room database, DataStore settings, and
// WorkManager scheduling as later milestones land.
class LifesaverApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LifesaverApp
            private set
    }
}
