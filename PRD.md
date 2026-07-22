# Lifesaver — Product Requirements & Build Spec

**Version:** 1.0 (Ideation complete, ready for implementation)
**Owner:** Constantin
**Target device:** Samsung Android phone (personal use, sideloaded APK, no Play Store)
**App UI language:** English
**This document is the single source of truth for Claude Code. Build exactly this, in the milestone order at the bottom.**

---

## 1. Mission

Lifesaver is a personal Android app that intercepts the reflexive grab for Instagram and YouTube and redirects it into pre-defined replacement activities. It is not primarily a blocker. It is a redirection machine.

Design philosophy (grounded in research):
- Friction beats hard blocks. The PNAS field study on the app "one sec" (280 participants, 6 weeks) showed a 57% reduction in target app openings through short friction pauses alone; 36% of intercepted attempts were abandoned on the spot.
- Hard blocks trigger reactance and uninstalls. Autonomy must be preserved, but impulsivity must be made expensive.
- Scrolling is a symptom that feeds a need (boredom relief, emotion regulation, variable reward). Blocking alone leaves the need unserved. Substitution via implementation intentions ("if-then plans", medium-to-large effects in a 94-study meta-analysis) is the core differentiator of this app.

## 2. Core Loop

1. User opens Instagram or YouTube → Lifesaver intercepts with a full-screen **Intervention Screen** before the feed appears.
2. The screen shows: an escalating breathing pause (5s → 15s → 30s per opening, resets daily), the user's own if-then plan for the current context, and two exits:
   - **1-tap redirect** into a pre-chosen alternative app (e.g. Kindle, podcast app, Anki), launched via intent.
   - **Micro-action** inside Lifesaver (60s breathing exercise, one journal line, view today's goals).
3. If the user proceeds anyway, the **daily budget** for that app starts burning (default 30 min/app/day, configurable in onboarding).
   - Time spent in Reels (Instagram) or Shorts (YouTube) burns budget at **2x speed**. The rest of the app (feed, DMs, search, normal videos) burns at 1x.
4. Budget empty → **hard block** for that app until midnight (local time).
5. **Emergency unlocks:** 2 per week, 15 minutes each, all restrictions lifted. Using one costs visibly: that day cannot count as a streak success day, and the unlock is logged and mirrored back in the weekly report.
6. **Self-binding:** loosening any rule (higher budget, fewer friction seconds, disabling a block) takes effect only after a 24h delay. Tightening takes effect immediately.

## 3. Feature Spec v1

### 3.1 Onboarding
- Explain the concept in 3-4 short screens (why friction, why substitution).
- **Baseline phase (updated in v1.1):** first 2 days are observation-only. No blocking, no friction. The app records a full behavioral profile (see §9.1) to establish the personal baseline. Dashboard shows "Baseline mode: day X of 2". After day 2 enforcement activates, but the baseline keeps refining silently over the first 14 days, split by weekday vs weekend, so "time saved" is not distorted by which two days happened to be observed.
- **If-then plans:** user defines hyper-specific implementation intentions per context. Minimum 2, suggested contexts: "evening (after 21:00)", "waiting/bored (daytime)", "just woke up". Format: "If I want to scroll [context], then I will [specific action]". Free text + suggestions.
- **Redirect apps:** user picks 1-3 installed apps as redirect targets (app picker over installed apps). Stored per if-then context if the user wants.
- **Budgets:** set daily budget per target app. Default suggestion 30 min. Slider 10-120 min.
- **Permissions flow:** guided setup for (a) Usage Access, (b) Accessibility Service, (c) Display over other apps, (d) exclude from battery optimization (critical on Samsung, see §5). Each with a short explanation screen and a deep link into the right settings page, plus a live checkmark when granted.

### 3.2 Intervention Screen
- Triggered when a target app moves to foreground (detected via AccessibilityService window state events, NOT by polling).
- Full-screen overlay (SYSTEM_ALERT_WINDOW) rendered instantly, before the feed can load.
- Content: countdown ring with breathing animation, the if-then plan matching the current context (time-of-day based), buttons: [redirect app icon(s)] / [micro-action] / [Continue to Instagram (n min left today)]. "Continue" is only enabled after the countdown.
- Escalation per app per day: 1st opening 5s, 2nd 15s, 3rd+ 30s. Resets at midnight.
- Tone of copy: calm, direct, never preachy. It quotes the user's own plan back at them.

### 3.3 Budget Engine
- Tracks foreground time per target app per day (accessibility events as primary signal, UsageStatsManager as reconciliation).
- Reels/Shorts surface detection (see 3.4) applies a 2x burn multiplier while active.
- Persistent notification while a target app is open: "Instagram: 12 min left today" (low-priority, silent).
- At 0 min: hard block. Opening the app shows a block screen (same visual language as intervention screen) with: time saved today, streak, redirect buttons, and the emergency unlock option. Block ends at midnight.

### 3.4 Reels/Shorts Detection (2x multiplier)
- AccessibilityService inspects the active window's node tree for known surface markers:
  - Instagram (`com.instagram.android`): view IDs containing `clips_` (e.g. `clips_viewer_view_pager`), Reels tab selected state.
  - YouTube (`com.google.android.youtube`): view IDs containing `reel_` (e.g. `reel_recycler`, `reel_player_page_container`), Shorts player markers.
- **Maintainability requirement:** detection markers MUST live in one central config object (a single Kotlin file or local JSON), never scattered through service code. Instagram and YouTube change their UI regularly; updating detection must be a one-file change.
- Debounce: evaluate at most every 500ms; never loop-fire actions.
- If detection confidence is unclear, default to 1x (never punish false positives).

### 3.5 Emergency Unlocks
- 2 per calendar week (resets Monday 00:00), 15 minutes each, lifts all friction/blocks.
- Activation requires typing a short free-text reason (logged, shown in weekly report).
- Consequence: current day is excluded from streak success. No other punishment.
- Remaining unlocks always visible on dashboard.

### 3.6 Self-Binding (24h rule)
- Any loosening change (budget up, friction down, disabling detection, removing a target app) is queued and applied 24h after confirmation, with a visible pending banner ("Budget increase to 45 min becomes active tomorrow 18:32. Cancel?").
- Tightening applies immediately.
- Uninstall/disable protection is explicitly OUT of scope for v1 (no Device Admin API). The 24h rule covers in-app settings only.

### 3.7 Rewards & Feedback
- **Streak:** a day counts as success if total target-app time stayed within budget AND no emergency unlock was used. Streak = consecutive success days. Show current + longest.
- **Time saved:** baseline (from the observation phase per §3.1, self-refining over the first 14 days, then fixed; editable only via the 24h rule) minus actual usage, accumulated daily/weekly/all-time. Translate into tangibles: book pages (~1.5 min/page), workouts (45 min), university study blocks (90 min). Show "This week you saved 6h 20m ≈ 4 workouts".
- **Substitution rate:** % of interventions that ended in a redirect or micro-action instead of "Continue". This is a primary success metric, show it prominently.
- **Weekly check-in:** every Sunday evening, a 30-second prompt with 3 sliders (1-10): perceived control, satisfaction with phone use, strength of scroll impulse. Stored locally, plotted over weeks.
- NO points/level system in v1 (deliberate decision: self-granted points that gate nothing decay fast; streaks + loss aversion + tangible time carry the motivation).

### 3.8 Dashboard (main screen)
- Today: screen time per target app vs budget (progress bars), time saved, current streak, remaining emergency unlocks.
- This week: substitution rate, saved-time tangibles, check-in trend sparkline.
- Quick access: settings (with 24h-rule), if-then plan editor.

## 4. Non-Goals v1
- No backend, no accounts, no cloud sync. All data local (Room + DataStore).
- No support for apps beyond Instagram + YouTube (architecture should keep the target-app list extensible, but UI exposes only these two).
- No uninstall protection / Device Admin.
- No Play Store release, no analytics SDKs, no tracking of any kind.

## 5. Technical Guidance
- **Stack:** Kotlin, Jetpack Compose, Material 3, minSdk 26, target latest stable SDK. Room for events/metrics, DataStore for settings. No third-party SDKs beyond AndroidX/Room.
- **Key mechanisms:**
  - `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED` + `TYPE_WINDOW_CONTENT_CHANGED`, filtered to the two target packages) for foreground detection, surface detection, and time accounting.
  - `UsageStatsManager` for baseline measurement and daily reconciliation of accessibility-based accounting.
  - `SYSTEM_ALERT_WINDOW` overlay for intervention/block screens (an overlay Activity is acceptable if it reliably wins the race against the target app's first frame).
- **Samsung-specific risk (important):** aggressive battery management kills background services. Onboarding must guide the user to exclude Lifesaver from battery optimization and enable "allow background activity". The AccessibilityService itself is fairly resilient, but test after phone reboots and overnight deep sleep.
- **Race condition:** the intervention screen must appear before the feed renders. Optimize the accessibility event path; if there is visible feed flash, consider showing the overlay on `TYPE_WINDOW_STATE_CHANGED` immediately and lazy-loading the screen content.
- **Midnight/timezone:** all daily resets at local midnight; handle timezone changes gracefully (no negative budgets).
- **Detection fragility:** treat §3.4 markers as best-effort heuristics that WILL break with app updates. Central config, defensive defaults, and a debug screen listing currently-seen view IDs (hidden dev option) to make repairs trivial.

## 6. Build Milestones (implement in this order, each independently testable)
1. **M1 — Scaffold & permissions:** project setup, onboarding shell, guided permissions flow with live status, dashboard skeleton reading UsageStatsManager (today's usage for both apps).
2. **M2 — Baseline & budget engine:** 3-day observation mode, budget accounting via AccessibilityService with UsageStats reconciliation, persistent notification, hard block screen at budget zero.
3. **M3 — Intervention screen:** escalating friction countdown, context-matched if-then plan display, "Continue" gating. Overlay race-condition hardening.
4. **M4 — Substitution:** if-then plan onboarding/editor, redirect app picker + intent launch, micro-actions (breathing 60s, one-line journal, today's goals).
5. **M5 — Reels/Shorts detection:** central marker config, 2x burn multiplier, dev debug screen for view IDs.
6. **M6 — Rewards & binding:** streaks, time-saved tangibles, substitution rate, weekly check-in, emergency unlocks, 24h self-binding queue.
7. **M7 — Hardening:** Samsung battery-optimization flow, reboot persistence, midnight edge cases, empty states, polish.

## 7. Acceptance Criteria (v1 done means)
- Opening Instagram triggers the intervention screen in under ~1s with no feed flash on the test device.
- Budget burns correctly at 1x/2x (verify with a stopwatch against Reels vs DMs), hard block engages at zero and lifts at midnight.
- Redirect buttons launch the chosen apps; substitution rate is recorded.
- Streak, time saved, and check-in data survive reboots.
- Loosening a setting visibly queues for 24h; tightening is instant.
- After 24h idle + reboot, the AccessibilityService still intercepts (battery optimization survived).

## 8. v2 Ideas (do NOT build now)
- Arbitrary target apps (TikTok, X, Reddit) with generic budget engine.
- Context-aware interventions (location, calendar: harder rules during university blocks).
- Points economy only if points gate something real (e.g. unlock cosmetics, charity donations).
- **Habit analysis & just-in-time interventions (JITAI), researched roadmap:**
  - Stage 1, rule-based (build first, no ML needed): learn personal risk windows from the app's own logged data (openings + budget burn per hour-of-day and day-of-week). Surface them ("your risk hours: 22:00-00:30") and automatically harden interventions inside these windows (longer friction, budget warnings earlier). Research shows rule-based JITAIs are a legitimate first stage before any ML.
  - Stage 2, intention gap: before a session starts, ask a 1-tap intention ("What are you here for? DM / search / just scrolling"). Key finding from regret-prediction research: the gap between intended and actual use predicts regret far better than session duration. Track intention vs. actual surface used and mirror it back weekly.
  - Stage 3, context triggers: harden interventions right after productivity-app use ends and at night, the two contexts where sessions most often displace a valued alternative (sleep, work) and are most regretted.
  - Stage 4, ML-based prediction: only if stages 1-3 prove insufficient. Pre-session contextual features (time, prior app, notification) generalize across people; physiological signals only add person-specific lift and need a wearable. Do not start here.
- Scaling to other users: Play Store compliance for AccessibilityService use is a significant hurdle; revisit only if personal use proves value for 60+ days.

---

## 9. v1.1 Spec (approved — build this next, in the order given)

v1.1 has one theme: the app stops being only a fence and becomes a mirror. It must know its own blind spots, close the biggest bypass, and make the freed-up time visible as real life. Where §9 conflicts with earlier sections, §9 wins.

### 9.1 Detailed Behavioral Baseline & Session Profiling
- Observation phase shortened to 2 days (see updated §3.1), but recording depth increases massively. Session-level logging (continues forever, not just during baseline):
  - timestamp start/end, duration
  - surface breakdown within the session (Reels / feed / DMs / search for Instagram; Shorts / normal video / other for YouTube), seconds per surface
  - preceding foreground app (what was I doing right before I opened it)
  - trigger class where detectable: opened from notification vs. self-initiated
- Derived metrics, computed continuously and shown on a new "Patterns" screen:
  - hour-of-day x day-of-week heatmap of usage
  - personal risk windows (top 2-3 recurring high-usage slots) — this feeds JITAI stage 1 (§8)
  - Reels/Shorts share of total time
  - post-midnight usage minutes
  - first-touch flag: was Instagram/YouTube among the first 3 apps after waking (first unlock of the day)
- Storage: Room, local only. Sessions table is append-only; derived metrics are recomputed, never hand-edited.

### 9.2 Integrity Layer (anti-bypass visibility, NOT prevention)
- Detect when the AccessibilityService is disabled or dies: on next app start / service reconnect, compare expected vs. actual coverage and log a "tracking gap" with start/end time.
- Any day containing a tracking gap of > 10 minutes cannot count as a streak success day.
- Gaps are reported factually, never moralizing: "Tuesday 22:41: tracking was off for 2h 3m."
- Detect uninstall-reinstall via a persisted marker in app-external storage (or backup timestamp mismatch) where feasible; log it the same way.

### 9.3 Browser Blocking
- The AccessibilityService reads the URL bar of Chrome (`com.android.chrome`) and Samsung Internet (`com.sec.android.app.sbrowser`).
- instagram.com counts toward the Instagram budget; youtube.com toward YouTube; youtube.com/shorts and instagram.com/reels burn at 2x, same rules as the apps.
- Same intervention screen on first navigation to these domains. URL markers live in the same central detection config as §3.4.

### 9.4 Life Layer (the actual point of the app)
- **Four life areas,** defined in onboarding: Projects (building own things), Sport & body, People & experiences, Learning & reading.
- **One weekly focus:** each week the user picks ONE area and one concrete target (e.g. "3 project evenings", "2 gym sessions"). The other areas remain loggable but are never pushed. The app must not nag about four goals in parallel.
- **1-tap activity logging:** a single-screen log: tap area, optional minutes, done. Plus: after a redirect from an intervention, the app asks ONCE ~30 min later via one notification: "Did the redirect stick? [Yes / No]" — this measures true substitution, not just button clicks.
- **Dashboard addition — "Life vs. Feed" card:** this week's logged life-hours vs. target-app hours, side by side, two bars. The single most honest visualization in the app.
- **Weekly focus check** integrated into the Sunday report (§9.5), not as separate notifications.
- Proactivity rule for v1.1: the app speaks ONLY at the scroll moment (interventions) and Sunday evening (report + check-in). No other notifications. A single daily impulse inside the learned risk window is planned for v1.2, once 2-3 weeks of pattern data exist.

### 9.5 Weekly Report (Sunday evening, the retention ritual)
- One screen, generated locally every Sunday ~20:00 (single notification):
  - screen time trend vs. baseline (per app, with Reels/Shorts share)
  - time saved, translated into tangibles
  - substitution rate: interventions → redirect/micro-action, and confirmed sticks (§9.4)
  - intention vs. actual (once JITAI stage 2 exists; placeholder section until then)
  - risk-window heatmap delta vs. last week
  - integrity status: tracking gaps, emergency unlocks with the typed reasons
  - life layer: weekly focus result ("2 of 3 project evenings"), logged hours per area
  - weekly check-in (3 sliders, §3.7) embedded at the end
- Tone: factual, dense, zero cheerleading. The numbers are the motivation.

### 9.6 Streak Insurance
- Every 7 consecutive success days earn 1 streak freeze (max 2 banked).
- A failed day automatically consumes a freeze (streak survives, freeze visibly spent in report). No freeze available → streak breaks.
- Rationale: the most common uninstall moment is "streak broken, screw it". This is loss-aversion protection, not softness.

### 9.7 Backup & Restore
- Automatic daily local backup (JSON export of Room DB + settings) to app-external storage the user can reach (e.g. Documents/Lifesaver/), keep last 7.
- Manual export/import in settings. Reinstall must never zero the streak and history — and a restore after reinstall is logged by the integrity layer (§9.2).

### 9.8 Deferred to v1.2 (decided, do not build yet)
- Single daily impulse in the learned risk window (needs §9.1 data maturity).
- Health Connect integration (Samsung Health): auto-import steps/workouts into the Sport & body area. Separate technical work package; 1-tap logging covers the need until then.

### 9.9 v1.1 Build Order
1. §9.1 session profiling + Patterns screen (everything else feeds on this data)
2. §9.2 integrity layer
3. §9.3 browser blocking
4. §9.4 life layer
5. §9.5 weekly report
6. §9.6 streak insurance + §9.7 backup
7. Regression pass: all v1 acceptance criteria (§7) still hold, plus: a tracking gap marks the day, browser Instagram burns budget, reinstall restores history.
