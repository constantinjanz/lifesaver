package com.lifesaver.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionDao {
    @Insert
    suspend fun insert(event: InterventionEvent): Long

    @Query("SELECT * FROM intervention_events ORDER BY ts DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<InterventionEvent>>

    @Query("SELECT COUNT(*) FROM intervention_events WHERE action != 'blocked'")
    fun totalInterventions(): Flow<Int>

    @Query("SELECT COUNT(*) FROM intervention_events WHERE action IN ('redirect','micro_action')")
    fun substitutionCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM intervention_events " +
            "WHERE appId = :appId AND dayKey = :dayKey AND action != 'blocked'",
    )
    suspend fun openCountToday(appId: String, dayKey: String): Int
}

@Dao
interface UsageDao {
    @Upsert
    suspend fun upsert(usage: DailyUsage)

    @Query("SELECT * FROM daily_usage WHERE dayKey = :dayKey")
    fun forDay(dayKey: String): Flow<List<DailyUsage>>

    @Query("SELECT * FROM daily_usage WHERE dayKey = :dayKey AND appId = :appId")
    suspend fun get(dayKey: String, appId: String): DailyUsage?

    @Query("SELECT * FROM daily_usage WHERE appId = :appId ORDER BY dayKey DESC LIMIT :limit")
    suspend fun history(appId: String, limit: Int): List<DailyUsage>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: UsageSession): Long

    @Query("SELECT * FROM usage_sessions WHERE dayKey = :dayKey ORDER BY startTs")
    suspend fun sessionsForDay(dayKey: String): List<UsageSession>

    @Query("SELECT * FROM usage_sessions WHERE startTs >= :fromMs ORDER BY startTs")
    suspend fun sessionsSince(fromMs: Long): List<UsageSession>

    @Query("SELECT * FROM daily_usage WHERE dayKey >= :fromDayKey")
    suspend fun usageSince(fromDayKey: String): List<DailyUsage>
}

@Dao
interface LifeDao {
    @Insert
    suspend fun insert(log: LifeLog): Long

    @Query("SELECT * FROM life_logs WHERE dayKey >= :fromDayKey ORDER BY ts DESC")
    fun since(fromDayKey: String): Flow<List<LifeLog>>

    @Query("SELECT COALESCE(SUM(minutes),0) FROM life_logs WHERE dayKey >= :fromDayKey")
    suspend fun totalMinutesSince(fromDayKey: String): Int

    @Query("SELECT * FROM life_logs WHERE dayKey >= :fromDayKey")
    suspend fun listSince(fromDayKey: String): List<LifeLog>
}

@Dao
interface TrackingGapDao {
    @Insert
    suspend fun insert(gap: TrackingGap): Long

    @Query("SELECT COALESCE(SUM(durationMs),0) FROM tracking_gaps WHERE dayKey = :dayKey")
    suspend fun gapMsForDay(dayKey: String): Long

    @Query("SELECT * FROM tracking_gaps WHERE dayKey >= :fromDayKey ORDER BY startTs DESC")
    suspend fun since(fromDayKey: String): List<TrackingGap>
}

@Dao
interface StatusDao {
    @Upsert
    suspend fun upsert(status: DailyStatus)

    @Query("SELECT * FROM daily_status ORDER BY dayKey DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<DailyStatus>

    @Query("SELECT * FROM daily_status ORDER BY dayKey DESC")
    fun all(): Flow<List<DailyStatus>>
}

@Dao
interface UnlockDao {
    @Insert
    suspend fun insert(unlock: EmergencyUnlock): Long

    @Query("SELECT COUNT(*) FROM emergency_unlocks WHERE weekKey = :weekKey")
    suspend fun usedThisWeek(weekKey: String): Int

    @Query("SELECT COUNT(*) FROM emergency_unlocks WHERE weekKey = :weekKey")
    fun usedThisWeekFlow(weekKey: String): Flow<Int>

    @Query("SELECT * FROM emergency_unlocks WHERE expiresTs > :nowMs ORDER BY expiresTs DESC LIMIT 1")
    suspend fun activeUnlock(nowMs: Long): EmergencyUnlock?

    @Query("SELECT * FROM emergency_unlocks WHERE weekKey = :weekKey ORDER BY ts")
    suspend fun forWeek(weekKey: String): List<EmergencyUnlock>
}

@Dao
interface PendingChangeDao {
    @Insert
    suspend fun insert(change: PendingChange): Long

    @Query("SELECT * FROM pending_changes WHERE applied = 0 AND cancelled = 0 ORDER BY effectiveTs")
    fun pending(): Flow<List<PendingChange>>

    @Query("SELECT * FROM pending_changes WHERE applied = 0 AND cancelled = 0 AND effectiveTs <= :nowMs")
    suspend fun due(nowMs: Long): List<PendingChange>

    @Query("UPDATE pending_changes SET applied = 1 WHERE id = :id")
    suspend fun markApplied(id: Long)

    @Query("UPDATE pending_changes SET cancelled = 1 WHERE id = :id")
    suspend fun cancel(id: Long)
}

@Dao
interface CheckinDao {
    @Upsert
    suspend fun upsert(checkin: Checkin)

    @Query("SELECT * FROM checkins ORDER BY weekKey DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<Checkin>>
}

@Dao
interface BaselineDao {
    @Upsert
    suspend fun upsert(baseline: Baseline)

    @Query("SELECT * FROM baseline WHERE appId = :appId AND weekdayGroup = :group")
    suspend fun get(appId: String, group: String): Baseline?

    @Query("SELECT * FROM baseline")
    suspend fun all(): List<Baseline>
}
