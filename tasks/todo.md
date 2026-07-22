# Lifesaver v1 — Build Progress

Stack: Kotlin + Jetpack Compose + Room + DataStore, minSdk 26 / target 36.
Toolchain: Gradle 9.6.1, AGP 9.2.1, Kotlin 2.3.10. Build with JBR (JDK 21) + local SDK.
Plan: `~/.claude/plans/declarative-doodling-sparrow.md`. Source of truth: `PRD.md`, theme: `DESIGN.md`.

## Foundation (shared, lands in M1)  ✅ DONE — builds, 19 unit tests pass
- [x] Gradle project (wrapper 9.6.1, AGP 9.2.1, Kotlin 2.3.10), version matrix pinned to cached deps
- [x] `LifesaverTheme` — 2016 Material dark palette, Roboto type scale, 2dp shapes, flat surfaces
- [x] Reusable components: RaisedButton, FlatButton (ALL CAPS enforced), LifesaverCard, BudgetBar, CountdownRing
- [x] Launcher icon, manifest, Application class
- [x] **Validate: `assembleDebug` produces app-debug.apk**
- [x] Room DB + entities + DAOs (8 tables, v1.1-forward-compatible)
- [x] DataStore SettingsRepository
- [x] `DetectionConfig` (single source of IG/YT markers)
- [x] Domain logic + unit tests: BudgetEngine, FrictionLadder, Streak, TimeSaved, SelfBinding, Baseline, DayKeys

## M1 — Scaffold & permissions  ✅ DONE — builds
- [x] Onboarding viewpager shell (concept pages + budget sliders)
- [x] Guided permissions flow: Usage Access, Accessibility, Overlay, Battery-opt — explanation + deep link + live status
- [x] Dashboard skeleton reading UsageStatsManager (today's IG/YT usage) + baseline banner
- [x] Nav graph (dashboard / onboarding / settings / plans / debug)

## M2 — Baseline & budget engine  ✅ DONE — builds
- [x] 2-day observation mode + baseline banner
- [x] AccessibilityService foreground detection + ForegroundAccountant (mutex-serialized accrual)
- [x] UsageStatsReader (today + reconciliation source)
- [x] Persistent low-priority usage notification
- [x] BudgetEngine + BlockActivity (hard block at 0, lifts at local midnight) + MidnightResetWorker

## M3 — Intervention screen  ✅ DONE — builds
- [x] InterventionActivity (Compose), escalating 5/15/30s countdown, breathing pulse
- [x] Context-matched if-then plan display (PlanMatcher), gated CONTINUE ("n MIN LEFT")
- [x] Overlay handling: accrual paused while our UI covers the app; currentApp invariant prevents re-fire

## M4 — Substitution  ✅ DONE — builds
- [x] If-then plan onboarding + standalone editor (3 contexts)
- [x] Redirect app picker (installed apps) + intent launch (intervention + block screens)
- [x] Micro-actions: 60s breathing, one-line journal, today's goals
- [x] Substitution-rate recording (per intervention exit)

## M5 — Reels/Shorts detection  ✅ DONE — builds
- [x] SurfaceDetector (bounded node-tree scan, 500ms debounce) + 2x multiplier
- [x] Debug screen: live view IDs, current surface, service status

## M6 — Rewards & binding  ✅ DONE — builds
- [x] StreakCalculator (current + longest)
- [x] TimeSaved tangibles (pages/workouts) — weekly dashboard card
- [x] Weekly check-in (3 sliders) + Sunday notification (WeeklyCheckinWorker)
- [x] Emergency unlocks (2/week, 15 min, typed reason, day excluded from streak, pause honored by service)
- [x] SelfBinding 24h queue (loosen delayed w/ banner + cancel, tighten immediate) + ApplyQueuedChangesWorker

## M7 — Hardening  ✅ DONE — builds
- [x] Battery-opt flow (onboarding), BOOT_COMPLETED receiver reschedules workers
- [x] Midnight/timezone via local DayKeys; budgets clamp ≥ 0 (no negatives)
- [x] Empty states (zeros render), polish
- [x] §7 acceptance-criteria manual-QA checklist doc (docs/manual-qa.md — device-only checks)

## Review

**v1 (M1–M7) complete.** Clean build from scratch: BUILD SUCCESSFUL; 19 unit tests pass;
`app-debug.apk` (~20 MB) produced. Committed in 5 milestone commits.

**Verified here (build machine):** compiles clean, domain logic unit-tested (budget 1x/2x,
friction ladder, streak, time-saved, self-binding, baseline, day-keys).

**Not verifiable here — needs the Samsung + real IG/YT apps** (see `docs/manual-qa.md`):
intervention appears <1s with no feed flash; 1x vs 2x stopwatch on Reels/Shorts; block at zero /
lift at midnight; redirect launches; reboot + overnight battery-opt survival; data survives reboot.

**Known follow-ups / honest gaps:**
- Circular-reveal transition on the intervention (DESIGN.md §5) not implemented — countdown ring +
  breathing pulse are in; the reveal animation is polish left for a later pass.
- IG/YT surface view-ID markers in `DetectionConfig` are best-effort guesses; confirm against the
  live apps via the Debug screen and adjust that one file.
- v1.1 (§9) not started (session profiling, integrity/tracking-gaps, browser blocking, life layer,
  weekly report, streak insurance, backup/restore) — schema + detection config are ready for it.
