package com.lifesaver.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * All timestamps are epoch millis (UTC). "dayKey" / "weekKey" are LOCAL calendar keys
 * (see DayKeys) so daily/weekly rollups reset at local midnight / Monday (PRD §2, §3.5).
 */

/** One recorded intervention outcome. Feeds substitution rate (§3.7). */
@Entity(
    tableName = "intervention_events",
    indices = [Index("appId"), Index("dayKey"), Index("ts")],
)
data class InterventionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val dayKey: String,
    val appId: String,
    val openIndex: Int,
    val frictionSeconds: Int,
    /** dismissed | continued | redirect | micro_action | blocked */
    val action: String,
    val redirectTarget: String? = null,
    val latencyMs: Long? = null,
    /** feed | reels_shorts | unknown — surface at the moment of the event. */
    val surface: String = "unknown",
)

/** Per-app per-day usage accounting. reelsShortsMs already reflects raw seconds on the
 *  2x surface; budget math applies the multiplier from these two fields (§3.3). */
@Entity(tableName = "daily_usage", primaryKeys = ["dayKey", "appId"])
data class DailyUsage(
    val dayKey: String,
    val appId: String,
    val foregroundMs: Long = 0,
    val reelsShortsMs: Long = 0,
    val budgetMs: Long = 0,
    val blocked: Boolean = false,
)

/** Per-day success verdict for streaks (§3.7, §9.2). */
@Entity(tableName = "daily_status", primaryKeys = ["dayKey"])
data class DailyStatus(
    val dayKey: String,
    val success: Boolean,
    /** ok | over_budget | unlock_used | tracking_gap | baseline */
    val reason: String,
    val unlocksUsed: Int = 0,
    val freezeConsumed: Boolean = false,
)

/** Emergency unlock usage (§3.5). Two per calendar week, 15 min each. */
@Entity(
    tableName = "emergency_unlocks",
    indices = [Index("weekKey"), Index("ts")],
)
data class EmergencyUnlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val weekKey: String,
    val dayKey: String,
    val reason: String,
    val expiresTs: Long,
)

/** A queued settings change under the 24h self-binding rule (§3.6). Loosening changes
 *  are applied only after effectiveTs; tightening changes are applied immediately (no row). */
@Entity(tableName = "pending_changes", indices = [Index("effectiveTs")])
data class PendingChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val settingKey: String,
    val newValue: String,
    val requestedTs: Long,
    val effectiveTs: Long,
    /** loosen | tighten (tighten rows are rare; kept for the audit trail). */
    val direction: String,
    val applied: Boolean = false,
    val cancelled: Boolean = false,
)

/** Sunday weekly check-in, 3 sliders 1-10 (§3.7). */
@Entity(tableName = "checkins", primaryKeys = ["weekKey"])
data class Checkin(
    val weekKey: String,
    val ts: Long,
    val control: Int,
    val satisfaction: Int,
    val impulse: Int,
)

/** Self-refining usage baseline, split weekday/weekend (§3.1, §3.7). */
@Entity(tableName = "baseline", primaryKeys = ["appId", "weekdayGroup"])
data class Baseline(
    val appId: String,
    /** weekday | weekend */
    val weekdayGroup: String,
    val avgMs: Long,
    val sampleDays: Int,
)

/** Append-only session log. v1 fills the core fields; the nullable columns are the
 *  v1.1 §9.1 session-profiling depth, populated later without a schema change. */
@Entity(
    tableName = "usage_sessions",
    indices = [Index("appId"), Index("dayKey"), Index("startTs")],
)
data class UsageSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appId: String,
    val dayKey: String,
    val startTs: Long,
    val endTs: Long,
    val durationMs: Long,
    val reelsShortsMs: Long = 0,
    // v1.1 (§9.1): surface breakdown seconds, preceding app, trigger class.
    val feedMs: Long? = null,
    val otherSurfaceMs: Long? = null,
    val precedingApp: String? = null,
    val triggerClass: String? = null,
)
