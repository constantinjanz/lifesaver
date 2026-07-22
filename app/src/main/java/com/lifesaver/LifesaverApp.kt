package com.lifesaver

import android.app.Application
import com.lifesaver.data.LifesaverDatabase
import com.lifesaver.data.SettingsRepository
import com.lifesaver.service.UsageStatsReader

/**
 * Application singleton and lightweight service locator. No DI framework (PRD §4: keep deps
 * minimal) — screens and workers reach dependencies via [container].
 */
class LifesaverApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        container.notifications.ensureChannels()
        // Anchor daily rollover to local midnight (streak finalize + baseline refine).
        com.lifesaver.work.MidnightResetWorker.scheduleNext(this)
    }

    companion object {
        lateinit var instance: LifesaverApp
            private set
    }
}

/** Owns the singletons: Room DB, settings, usage reader, notifications. */
class AppContainer(app: Application) {
    val database: LifesaverDatabase by lazy { LifesaverDatabase.get(app) }
    val settings: SettingsRepository by lazy { SettingsRepository(app) }
    val usageReader: UsageStatsReader by lazy { UsageStatsReader(app) }
    val notifications: com.lifesaver.service.Notifications by lazy {
        com.lifesaver.service.Notifications(app)
    }

    /** Long-lived scope for fire-and-forget writes that must survive an Activity finishing. */
    val appScope: kotlinx.coroutines.CoroutineScope =
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default,
        )
}
