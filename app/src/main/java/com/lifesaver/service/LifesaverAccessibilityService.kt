package com.lifesaver.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.lifesaver.LifesaverApp
import com.lifesaver.data.LifesaverDatabase
import com.lifesaver.data.Settings
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.domain.DayKeys
import com.lifesaver.domain.FrictionLadder
import com.lifesaver.domain.ScheduleBlock
import com.lifesaver.intervention.BlockActivity
import com.lifesaver.intervention.InterventionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The interception engine (PRD §3.2–§3.4). Detects when a target app comes to the foreground,
 * accounts foreground time toward the daily budget (2x on Reels/Shorts), shows the block screen
 * when the budget is exhausted, and (M3) the intervention pause before the feed. All markers come
 * from [DetectionConfig]. During the 2-day baseline it observes only — no pauses, no blocks (§3.1).
 */
class LifesaverAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val main = Handler(Looper.getMainLooper())
    private lateinit var accountant: ForegroundAccountant
    private lateinit var notifications: Notifications
    private lateinit var db: LifesaverDatabase
    private lateinit var settingsRepo: com.lifesaver.data.SettingsRepository

    // Live config snapshot, refreshed from settings.
    @Volatile private var settings: Settings = Settings()

    // Current foreground session state.
    @Volatile private var currentApp: String? = null
    private var sessionStartMs = 0L
    private var lastFlushMs = 0L
    private var sessionFastMs = 0L
    @Volatile private var currentSurfaceFast = false // set by SurfaceDetector on content changes
    private var lastDetectMs = 0L
    /** True while our own intervention/block screen covers the target — don't accrue that time. */
    @Volatile private var ownUiForeground = false
    private var tickJob: Job? = null
    // Debounce enforcement so PiP/window churn can't re-launch the block/intervention in a loop.
    @Volatile private var lastEnforceApp: String? = null
    @Volatile private var lastEnforceAtMs = 0L
    @Volatile private var grayscaleActive = false
    // After a FULL/scheduled block we latch the app: while it stays foreground (incl. YouTube PiP),
    // we don't re-enforce. Cleared only when a different, non-self app comes to the front (real exit).
    @Volatile private var blockLatchApp: String? = null
    // Admission: the app the user has PASSED a breathe pause for (via Continue). While admitted, we
    // don't re-pause it. Cleared the moment they leave to another app/home — so dismissing the pause
    // (Back/Close) and re-opening always pauses again (no skip-the-wait bypass). Set by the
    // intervention screen through admit()/deny() below.
    @Volatile private var admittedApp: String? = null
    // Last time a breathe pause was shown/passed per app — drives the "every N minutes" reminder.
    private val lastBreatheAtMs = java.util.concurrent.ConcurrentHashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        self = this
        val container = LifesaverApp.instance.container
        db = container.database
        accountant = ForegroundAccountant(db)
        notifications = container.notifications
        settingsRepo = container.settings
        scope.launch { container.settings.settings.collect { settings = it } }
        startHeartbeat()
        // Safety: clear any leftover grayscale from a previous crash so the phone isn't stuck gray.
        Grayscale.setEnabled(this, false)
    }

    private fun updateGrayscale() {
        val want = currentSurfaceFast && settings.grayscaleOnReels && currentApp != null && !ownUiForeground
        if (want && !grayscaleActive) {
            Grayscale.setEnabled(this, true); grayscaleActive = true
        } else if (!want && grayscaleActive) {
            Grayscale.setEnabled(this, false); grayscaleActive = false
        }
    }

    private fun clearGrayscale() {
        if (grayscaleActive) { Grayscale.setEnabled(this, false); grayscaleActive = false }
    }

    /** Integrity layer (§9.2): on connect, if we were dark for >10min, log a tracking gap; then
     *  tick a heartbeat so the next dark period is measurable. */
    private fun startHeartbeat() {
        scope.launch {
            val last = settingsRepo.lastAlive()
            val now = System.currentTimeMillis()
            if (last in 1 until now && now - last > 10 * 60_000L) {
                db.trackingGapDao().insert(
                    com.lifesaver.data.TrackingGap(
                        startTs = last, endTs = now,
                        dayKey = DayKeys.dayKey(now), durationMs = now - last,
                    ),
                )
            }
            while (isActive) {
                settingsRepo.setLastAlive(System.currentTimeMillis())
                delay(60_000L)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == applicationContext.packageName) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> { onForegroundPackage(pkg); maybeScan(pkg, event) }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> maybeScan(pkg, event)
            else -> Unit
        }
    }

    /**
     * Scans a target app's node tree for the Reels/Shorts surface (§3.4). Runs on window-state and
     * content-changed events, keyed only on the target package (not the session bookkeeping) and
     * with an event.source fallback when rootInActiveWindow is unavailable — so detection and the
     * debug view-id list are as robust as possible. Enforcement only fires for the current session.
     */
    private fun maybeScan(pkg: String, event: AccessibilityEvent) {
        if (!DetectionConfig.isTarget(pkg) || ownUiForeground) return
        val target = DetectionConfig.targetFor(pkg) ?: return
        val now = System.currentTimeMillis()
        if (now - lastDetectMs < DETECT_DEBOUNCE_MS) return
        lastDetectMs = now

        // Only scan when the target app is the ACTUAL active window — otherwise a background event
        // during a transition (recents / home) makes us scan the wrong screen and overwrite the
        // real capture. This is why the debug list showed the app-switcher, not Instagram.
        val root = rootInActiveWindow
        if (root == null) { lastScanSummary = "no active window (waiting for $pkg)"; return }
        val rootPkg = root.packageName?.toString()
        if (rootPkg != pkg) { lastScanSummary = "active=$rootPkg (waiting for $pkg on top)"; return }
        val result = SurfaceDetector.detect(root, target)
        lastSeenViewIds = result.seenTokens
        lastSurfaceFast = result.isFast
        lastScanSummary = "$pkg · tokens=${result.seenTokens.size} · fast=${result.isFast}"

        if (pkg == currentApp) {
            val wasFast = currentSurfaceFast
            currentSurfaceFast = result.isFast
            updateGrayscale()
            // Just landed on Reels/Shorts and it's limited → check the sub-budget right away.
            if (result.isFast && !wasFast && enforcing() && settings.reelsLimited(pkg)) {
                val app = pkg
                scope.launch {
                    val reelsMs = accountant.reelsToday(app, DayKeys.todayKey())
                    if (reelsMs >= settings.reelsLimitMs(app)) triggerBlock(app, reels = true)
                }
            }
        }
    }

    private fun onForegroundPackage(pkg: String) {
        // A non-self app is on top, so our overlay is no longer covering anything.
        ownUiForeground = false
        // Block latch: while the just-blocked app stays up front (e.g. YouTube keeps playing in a
        // PiP window), do NOT re-enforce — that's what made the block screen re-pop in a loop. Only
        // a real exit (a different, non-self app to the front) clears it and re-arms enforcement.
        blockLatchApp?.let { latched ->
            if (pkg == latched) {
                // The blocked app is still up front. Keep suppressing ONLY while it's a small PiP
                // window (so a video replaying in Picture-in-Picture can't re-pop the block in a
                // loop). A full-screen re-open falls through below and re-blocks immediately — that
                // was the bypass: the old blanket latch let you re-open and use the app freely.
                if (isTargetInSmallWindow(pkg)) return
            }
            blockLatchApp = null
        }
        // Left the admitted app (anything other than it comes to the front) → revoke admission, so
        // re-entering pauses again. Our own pause screen is our package and returns earlier, so it
        // doesn't clear admission.
        if (admittedApp != null && pkg != admittedApp) admittedApp = null
        // Any window change means we may have left Reels — drop grayscale until re-detected.
        clearGrayscale()
        lastForegroundPackage = pkg
        // Any enabled app is budgeted/paused (custom user-added apps too). Reels/Shorts detection
        // still only runs for the known targets (Instagram/YouTube) — see maybeScan.
        val isTarget = pkg in settings.enabledApps
        lastForegroundIsTarget = isTarget
        if (pkg == currentApp) return

        val now = System.currentTimeMillis()
        endCurrentSession(now)

        // A target playing in a small floating window (Picture-in-Picture, e.g. YouTube minimised)
        // is not a fresh full-screen open — don't start a session or throw up the block/pause for it,
        // otherwise it re-fires every time the PiP window changes. We re-enforce when it goes full again.
        if (isTarget && !isTargetInSmallWindow(pkg)) {
            startSession(pkg, now)
            // Scheduled hard-block wins over everything (even an active emergency unlock).
            if (isScheduledBlockedNow(pkg)) {
                triggerBlock(pkg, scheduled = true)
            } else if (enforcing()) {
                decideOnEntry(pkg)
            }
        }
    }

    /** True if [app] is inside any of its configured daily hard-block windows right now (local time). */
    private fun isScheduledBlockedNow(app: String): Boolean {
        if (!settings.onboardingComplete) return false
        val windows = settings.windowsFor(app)
        if (windows.isEmpty()) return false
        val now = java.time.LocalTime.now()
        return ScheduleBlock.isBlockedAny(windows, now.hour * 60 + now.minute)
    }

    /** True if [pkg]'s window covers only a small part of the screen (PiP / floating). Best-effort. */
    private fun isTargetInSmallWindow(pkg: String): Boolean {
        return try {
            val dm = resources.displayMetrics
            val screenArea = dm.widthPixels.toLong() * dm.heightPixels.toLong()
            if (screenArea <= 0L) return false
            val rect = android.graphics.Rect()
            for (w in windows) {
                val root = w.root ?: continue
                val wPkg = root.packageName?.toString()
                @Suppress("DEPRECATION") root.recycle()
                if (wPkg != pkg) continue
                w.getBoundsInScreen(rect)
                val area = rect.width().toLong() * rect.height().toLong()
                if (area in 1 until (screenArea * 4 / 10)) return true // < 40% of screen ⇒ PiP-ish
            }
            false
        } catch (t: Throwable) {
            false
        }
    }

    private fun canEnforceNow(app: String): Boolean {
        val now = System.currentTimeMillis()
        return !(app == lastEnforceApp && now - lastEnforceAtMs < ENFORCE_COOLDOWN_MS)
    }

    private fun markEnforced(app: String) {
        lastEnforceApp = app
        lastEnforceAtMs = System.currentTimeMillis()
    }

    // --- Session lifecycle ---

    private fun startSession(pkg: String, now: Long) {
        currentApp = pkg
        sessionStartMs = now
        lastFlushMs = now
        sessionFastMs = 0
        currentSurfaceFast = false
        startTicker()
    }

    private fun endCurrentSession(now: Long) {
        val app = currentApp ?: return
        flush(now)
        val start = sessionStartMs
        val fast = sessionFastMs
        scope.launch { accountant.recordSession(app, start, now, fast) }
        notifications.cancelUsage(notifId(app))
        currentApp = null
        stopTicker()
    }

    private fun startTicker() {
        stopTicker()
        tickJob = scope.launch {
            while (isActive && currentApp != null) {
                delay(TICK_MS)
                flush(System.currentTimeMillis())
            }
        }
    }

    private fun stopTicker() {
        tickJob?.cancel()
        tickJob = null
    }

    /** Persist the slice since the last flush and react to budget state. */
    private fun flush(now: Long) {
        val app = currentApp ?: return
        // While our pause/block screen covers the app, the user isn't in it — don't burn budget.
        if (ownUiForeground) {
            lastFlushMs = now
            return
        }
        val delta = now - lastFlushMs
        if (delta <= 0) return
        val fast = if (currentSurfaceFast) delta else 0L
        sessionFastMs += fast
        lastFlushMs = now
        val dayKey = DayKeys.todayKey()
        val budget = settings.budgetMs(app)
        val enforce = enforcing()
        scope.launch {
            val a = accountant.accrue(app, dayKey, delta, fast, budget)
            // Scheduled hard-block applies even during a pause; check it first.
            if (isScheduledBlockedNow(app)) {
                triggerBlock(app, scheduled = true)
                return@launch
            }
            if (enforce) {
                // Reels/Shorts sub-budget: wall off the fast surface when its own limit is hit.
                if (currentSurfaceFast && settings.reelsLimited(app) && a.reelsMs >= settings.reelsLimitMs(app)) {
                    triggerBlock(app, reels = true)
                    return@launch
                }
                if (a.exhausted) {
                    triggerBlock(app)
                } else {
                    notifications.showUsage(
                        notifId(app),
                        notifications.buildUsageNotification(
                            appLabel(app),
                            (a.remainingMs / 60_000L).toInt(),
                        ),
                    )
                    // Periodic breathe reminder mid-session (user setting). The gap check inside
                    // triggerIntervention keeps it from stacking; here we just offer one on schedule.
                    if (settings.breatheReminderMin > 0 &&
                        System.currentTimeMillis() - (lastBreatheAtMs[app] ?: 0L) >= settings.breatheReminderMin * 60_000L
                    ) {
                        val openIndex = db.interventionDao().openCountToday(app, dayKey) + 1
                        val friction = FrictionLadder.pauseSeconds(openIndex, settings.strictness)
                        triggerIntervention(app, openIndex, friction, (a.remainingMs / 60_000L).toInt().coerceAtLeast(0))
                    }
                }
            }
        }
    }

    // --- Enforcement decisions ---

    private fun decideOnEntry(app: String) {
        val dayKey = DayKeys.todayKey()
        val budget = settings.budgetMs(app)
        scope.launch {
            val eff = accountant.effectiveToday(app, dayKey)
            if (budget in 1..eff) {
                triggerBlock(app)
            } else if (app == admittedApp) {
                // Already breathed for this app and hasn't left it — let them straight in.
            } else {
                val openIndex = db.interventionDao().openCountToday(app, dayKey) + 1
                val friction = FrictionLadder.pauseSeconds(openIndex, settings.strictness)
                val minutesLeft = ((budget - eff) / 60_000L).toInt().coerceAtLeast(0)
                triggerIntervention(app, openIndex, friction, minutesLeft)
            }
        }
    }

    private fun triggerIntervention(app: String, openIndex: Int, friction: Int, minutesLeft: Int) {
        // Anti-churn only: the 6s cooldown squashes duplicate triggers from IG story/reel sub-windows
        // briefly stealing focus. Back-to-back is otherwise prevented by admission (a passed pause
        // isn't re-shown) — and a *dismissed* pause deliberately re-pauses on the next open.
        if (!canEnforceNow(app)) return
        lastBreatheAtMs[app] = System.currentTimeMillis()
        markEnforced(app)
        ownUiForeground = true
        main.post {
            val label = appLabel(app)
            val intent = Intent(this, InterventionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(InterventionActivity.EXTRA_APP_ID, app)
                .putExtra(InterventionActivity.EXTRA_APP_LABEL, label)
                .putExtra(InterventionActivity.EXTRA_OPEN_INDEX, openIndex)
                .putExtra(InterventionActivity.EXTRA_FRICTION_SECONDS, friction)
                .putExtra(InterventionActivity.EXTRA_MINUTES_LEFT, minutesLeft)
            startActivity(intent)
        }
    }

    private fun triggerBlock(app: String, scheduled: Boolean = false, reels: Boolean = false) {
        if (!canEnforceNow(app)) return
        markEnforced(app)
        ownUiForeground = true
        val untilMin = if (scheduled) {
            val now = java.time.LocalTime.now()
            ScheduleBlock.activeWindow(settings.windowsFor(app), now.hour * 60 + now.minute)?.endMinute ?: -1
        } else -1
        main.post {
            // A reels block only walls off the fast surface — keep the session alive so the rest of
            // the app (posts/DMs) still accounts. A full/scheduled block ends the session so a PiP
            // window can't respawn it via the tick; re-opening full-screen re-blocks once.
            if (!reels) {
                stopTicker()
                if (currentApp == app) currentApp = null
                // Latch the blocked app: PiP / window churn on it won't re-trigger the block until
                // the user actually leaves to another app or home.
                blockLatchApp = app
            }
            val label = appLabel(app)
            val intent = Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(BlockActivity.EXTRA_APP_ID, app)
                .putExtra(BlockActivity.EXTRA_APP_LABEL, label)
                .putExtra(BlockActivity.EXTRA_SCHEDULED, scheduled)
                .putExtra(BlockActivity.EXTRA_UNTIL_MIN, untilMin)
                .putExtra(BlockActivity.EXTRA_REELS, reels)
                .putExtra(BlockActivity.EXTRA_SURFACE_NAME, DetectionConfig.targetFor(app)?.fastSurfaceName ?: "Reels")
            startActivity(intent)
        }
    }

    // --- Phase helpers ---

    // Enforcement is live as soon as onboarding completes — the baseline is seeded from history,
    // so there's no observation period to wait through.
    private fun enforcing(): Boolean =
        settings.onboardingComplete && !settings.isPaused(System.currentTimeMillis())

    /** Curated label for known targets, otherwise the installed-app label (custom apps). */
    private fun appLabel(app: String): String =
        DetectionConfig.targetFor(app)?.label ?: LifesaverApp.instance.container.installedApps.labelFor(app)

    private fun notifId(app: String) = Notifications.USAGE_NOTIFICATION_ID_BASE + app.hashCode()

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        isConnected = false
        if (self === this) self = null
        clearGrayscale()
        endCurrentSession(System.currentTimeMillis())
        scope.cancel()
        return super.onUnbind(intent)
    }

    /** The user passed the breathe pause (Continue) — admit the app until they leave it. */
    private fun admit(app: String) {
        admittedApp = app
        lastBreatheAtMs[app] = System.currentTimeMillis()
    }

    /** The user dismissed the pause (Back/Close/redirect) without passing. Force the next open to be
     *  a fresh entry that pauses again — don't rely on seeing a home/other-app event first (some
     *  launchers don't emit one on the way out). We clear admission, the enforce cooldown, our-UI
     *  flag, and the session itself, so onForegroundPackage no longer short-circuits on re-open. */
    private fun deny(app: String) {
        admittedApp = null
        if (lastEnforceApp == app) lastEnforceAtMs = 0L
        ownUiForeground = false
        if (currentApp == app) {
            currentApp = null
            stopTicker()
        }
    }

    /** The user closed/back'd the block screen. Drop the latch + cooldown so re-opening the app
     *  re-blocks right away instead of being briefly usable. */
    private fun blockDismissed(app: String) {
        if (blockLatchApp == app) blockLatchApp = null
        if (lastEnforceApp == app) lastEnforceAtMs = 0L
        ownUiForeground = false
    }

    companion object {
        private const val TICK_MS = 5_000L
        private const val DETECT_DEBOUNCE_MS = 500L
        private const val ENFORCE_COOLDOWN_MS = 6_000L

        // Live service instance so the intervention screen can report the user's choice.
        @Volatile private var self: LifesaverAccessibilityService? = null

        /** Called by the intervention screen when the user taps Continue — the app is admitted. */
        fun admit(app: String) { self?.admit(app) }

        /** Called when the user leaves the pause without passing (Back/Close/redirect). */
        fun deny(app: String) { self?.deny(app) }

        /** Called when the user closes/back's the block screen. */
        fun blockDismissed(app: String) { self?.blockDismissed(app) }

        @Volatile var isConnected: Boolean = false; private set
        @Volatile var lastForegroundPackage: String? = null; private set
        @Volatile var lastForegroundIsTarget: Boolean = false; private set
        @Volatile var lastSurfaceFast: Boolean = false; private set
        @Volatile var lastSeenViewIds: List<String> = emptyList(); private set
        @Volatile var lastScanSummary: String = "no scan yet"; private set
    }
}
