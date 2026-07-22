package com.lifesaver.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        InterventionEvent::class,
        DailyUsage::class,
        DailyStatus::class,
        EmergencyUnlock::class,
        PendingChange::class,
        Checkin::class,
        Baseline::class,
        UsageSession::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LifesaverDatabase : RoomDatabase() {
    abstract fun interventionDao(): InterventionDao
    abstract fun usageDao(): UsageDao
    abstract fun statusDao(): StatusDao
    abstract fun unlockDao(): UnlockDao
    abstract fun pendingChangeDao(): PendingChangeDao
    abstract fun checkinDao(): CheckinDao
    abstract fun baselineDao(): BaselineDao

    companion object {
        @Volatile
        private var instance: LifesaverDatabase? = null

        fun get(context: Context): LifesaverDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifesaverDatabase::class.java,
                    "lifesaver.db",
                ).build().also { instance = it }
            }
    }
}
