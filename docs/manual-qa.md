# Lifesaver — Manual QA Checklist (on-device)

These are the acceptance criteria (PRD §7) that can only be verified on the physical Samsung
device with the real Instagram/YouTube apps. The build machine can prove the app compiles,
installs, and passes unit tests, but not these runtime behaviours. Run through this after each
sideload.

## Install
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## M1 — Scaffold & permissions
- [ ] App launches to onboarding on first run; concept pages swipe; dots track the page.
- [ ] Budget sliders move 10–120 min and persist after finishing.
- [ ] Each of the 4 permission rows deep-links to the correct settings page.
- [ ] Granting a permission and returning shows a live green checkmark (no app restart).
- [ ] After finishing onboarding, dashboard shows "Baseline mode: day 1 of 2".
- [ ] Dashboard "TODAY" shows real minutes for Instagram/YouTube from UsageStatsManager.

## M2 — Baseline & budget engine
- [ ] During baseline (2 days) no pause/block appears; usage still recorded.
- [ ] After baseline, opening a watched app past its budget shows the block screen.
- [ ] Persistent silent notification shows correct "N min left" while a watched app is open.
- [ ] Block lifts at local midnight (verify by changing device clock across midnight).

## M3 — Intervention screen
- [ ] Opening Instagram triggers the pause screen in under ~1s with **no feed flash**.
- [ ] Friction escalates 5s → 15s → 30s across the 1st/2nd/3rd+ opens that day; resets next day.
- [ ] "Continue" is disabled until the countdown ends, then enabled.
- [ ] The pause quotes the correct if-then plan for the time of day.

## M4 — Substitution
- [ ] Redirect buttons launch the chosen apps.
- [ ] Micro-actions (60s breathing / one-line journal / today's goals) work.
- [ ] Substitution rate on the dashboard rises after a redirect/micro-action, not after Continue.

## M5 — Reels/Shorts detection
- [ ] Stopwatch check: 1 min in Reels burns ~2 min of budget; 1 min in DMs burns ~1 min.
- [ ] Same for YouTube Shorts vs a normal video.
- [ ] Debug screen lists live view IDs and the current detected surface.

## M6 — Rewards & binding
- [ ] Streak increments on a within-budget, no-unlock day; shows current + longest.
- [ ] Emergency unlock (2/week) lifts restrictions for 15 min, requires a typed reason, and
      excludes that day from streak success.
- [ ] Loosening a setting (raise budget) shows a 24h pending banner; tightening applies instantly.
- [ ] Sunday check-in (3 sliders) records and plots.

## M7 — Hardening
- [ ] After a reboot, the accessibility service still intercepts (open Instagram).
- [ ] After 24h idle + overnight deep sleep, interception still fires (battery-opt exclusion held).
- [ ] Streak / time-saved / check-in data survive a reboot.
- [ ] Timezone change does not produce negative budgets or a broken day.
```
