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

## M2 — Baseline & budget engine
- [ ] 2-day observation mode + baseline banner
- [ ] AccessibilityService foreground detection + ForegroundAccountant
- [ ] UsageStatsReconciler (daily correction)
- [ ] Persistent low-priority usage notification
- [ ] BudgetEngine + BlockActivity (hard block at 0, lifts at local midnight) + MidnightResetWorker

## M3 — Intervention screen
- [ ] InterventionActivity (Compose, circular reveal), escalating 5/15/30s countdown, breathing pulse
- [ ] Context-matched if-then plan display, gated CONTINUE ("n MIN LEFT")
- [ ] Overlay race hardening

## M4 — Substitution
- [ ] If-then plan onboarding + editor (min 2 contexts)
- [ ] Redirect app picker (installed apps) + intent launch
- [ ] Micro-actions: 60s breathing, one-line journal, today's goals
- [ ] Substitution-rate recording

## M5 — Reels/Shorts detection
- [ ] SurfaceDetector (node-tree scan, ≤500ms debounce) + 2x multiplier
- [ ] Debug screen: live view IDs, current surface, service status

## M6 — Rewards & binding
- [ ] StreakCalculator (current + longest)
- [ ] TimeSaved tangibles (pages/workouts/study)
- [ ] Weekly check-in (3 sliders, Sunday)
- [ ] Emergency unlocks (2/week, 15 min, typed reason)
- [ ] SelfBinding 24h queue (loosen delayed, tighten immediate) + ApplyQueuedChangesWorker

## M7 — Hardening
- [ ] Samsung battery-opt flow, BOOT_COMPLETED persistence
- [ ] Midnight/timezone edge cases (no negative budgets)
- [ ] Empty states, polish
- [ ] §7 acceptance-criteria pass + manual-QA checklist doc

## Review notes
(filled per milestone)
