# Lifesaver — Visual Redesign Spec v2: "Liquid Glass Cockpit"

**This replaces the previous DESIGN.md entirely. Restyle instruction for Claude Code. Functionality stays untouched.**

**Target aesthetic: Apple WWDC25 Liquid Glass, dark, combined with an instrument-panel layout. The app should feel like the cockpit of the user's phone: translucent frosted-glass panels floating over a dark ambient background, real backdrop blur, hairline light edges that catch light like polished glass, one central ring instrument. Reference points: iOS 26 Liquid Glass, visionOS panels, Apple Watch activity rings. If a surface looks like a flat gray card, it is wrong. If it looks like Material Design, it is very wrong.**

---

## 1. Background System (the light source behind the glass)

Glass only reads as glass when something shines through it. Every screen sits on:

- **Base:** near-black `#0A0A0F`.
- **Ambient color field:** 2-3 large, extremely soft radial color blobs, heavily dimmed, blurred to formlessness (think 200dp+ blur radius): one warm orange `#FF9500` at ~12% opacity, one violet `#7B5CFF` at ~10%, one deep blue `#2E5CFF` at ~8%. Positioned off-center, different per screen so screens feel distinct.
- Blobs may drift very slowly (60s+ loop, subtle) on the dashboard only. Everywhere else static. Motion must never distract.
- Never a pure flat background, never a photo.

## 2. Glass Material (the one material of the app)

Every panel, card, dock, pill, and sheet is the same material:

- **Backdrop blur:** 24dp blur of everything behind the panel. Implementation: use the **Haze library (chrisbanes/haze)** for Compose backdrop blur. Requires Android 12+, which the target device has.
- **Tint:** white at 8% over the blurred backdrop (10% for smaller elements like pills).
- **Border:** 1dp stroke, white at 18%.
- **Top light edge:** additional 1dp inner highlight on the top edge only, white at 32%, fading out at the corners. This is the "polished glass catches light" signal and is mandatory on cards and sheets.
- **Corner radius:** 24dp for cards and sheets, 16dp for small tiles, fully rounded (pill) for badges, buttons, and the dock.
- **Shadow:** soft ambient shadow (black 35%, y-offset 8dp, blur 24dp) so panels float above the background.
- **Fallback mode** (performance or blur unavailable): replace blur with solid `#16161D` at 92% opacity, keep tint/border/light edge identical. Must be a single flag in the theme, not scattered conditionals.

## 3. Typography

- **Font: Inter** (bundle it), the closest free match to SF Pro. Weights 400 and 600 only.
- Large numerals are the soul of a cockpit: time values use Inter 600, tabular figures, tight letter spacing.
  - Hero number (central ring): 44sp
  - Tile numbers: 22sp
  - Body: 15sp regular
  - Labels/captions: 12sp regular, sentence case, NEVER all caps
- Text colors: white 100% (values), white 60% (labels), white 40% (captions/hints). Never pure gray hex values, always white with opacity so the glass tint shows through.

## 4. Color

- **Accent (instruments & data): orange `#FF9500`.** Used for: ring progress, active bars, risk highlights, streak flame. Sparingly — the cockpit is neutral glass, the data glows.
- Success/positive: green `#30D158` (streak intact, goals met).
- Danger: red-orange `#FF453A` (budget exhausted, block screen, integrity gaps).
- Never more than one accent color per panel. No gradients on UI elements themselves (gradients live only in the ambient background).

## 5. Components

- **Ring gauge (signature instrument):** circular progress ring, 8dp stroke, rounded caps, glass track (white 12%), accent fill, value + label centered inside. Sizes: hero 160dp (dashboard, intervention countdown), small 56dp (tiles).
- **Glass card:** material from §2, padding 20dp.
- **Status pill:** top of dashboard, pill glass, dot indicator + text: "All systems on" (green dot) / "Tracking gap today" (red dot, §9.2 integrity).
- **List rows** (from style F): inside a glass card, rows separated by hairline dividers (white 10%), label left, value right. Use for app budgets, settings, report line items.
- **Bar chart (risk hours):** slim rounded bars, white 14% default, accent for peak hours, no axes, no gridlines, caption below ("peak risk · 22:00").
- **Buttons:** glass pills. Primary action = accent-tinted glass (orange 22% tint, orange 40% border, white text). Secondary = neutral glass. Text sentence case, never all caps.
- **Floating dock:** bottom navigation as a detached glass pill (like the iOS 26 tab bar), 3 items: Cockpit, Patterns, Life. Icons only (outline style), active icon white 100% + tiny dot, inactive white 45%.
- **Sheets/dialogs:** glass sheet sliding from bottom, 24dp top corners, drag handle (white 25%).

## 6. Motion

- Physics-based springs (Compose `spring()`, medium damping), never linear. Press states scale to 0.97.
- Panels enter with fade + 12dp upward drift, 350ms. The intervention screen enters as a soft blur-in: background app dims and blurs first, then the glass panel fades over it. This replaces the old circular reveal.
- Ring progress animates with spring on screen entry.
- No bounce excess, no parallax gimmicks. visionOS calm, not Android launcher flash.

## 7. Screen-by-Screen

### Cockpit (dashboard)
Top: status pill (integrity + service state). Center: hero ring = today's reclaimed time vs baseline, caption "reclaimed today". Below: two glass tiles side by side (Instagram / YouTube) each with small ring or slim bar for budget left. Then: risk-hours bar chart card. Then: "Life vs feed" card, two horizontal bars comparing logged life-hours vs target-app hours this week. Streak as a compact row card: flame icon, "7 days", freezes banked shown as small snowflake dots. Floating dock at bottom.

### Intervention screen
Background: the blurred, dimmed target app (the blur IS the intervention: the feed becomes unreadable). Center: hero ring as countdown, breathing scale animation (4s cycle), "Breathe" caption. Below the ring: the current if-then plan quoted in Inter 600 white, max 3 lines. Cycling intentions: rotate through the user's plans for the current context, one per intervention, least-recently-shown first. Bottom: glass dock with redirect app icons (44dp, real app icons), a "Micro-action" glass pill, and the gated "Continue · 12 min left" pill which stays at 30% opacity until the countdown completes.

### Block screen
Same layout, ring static in danger red at 100%, lock glyph inside. Title: "Instagram is done for today". Below: reclaimed time + streak as reassurance, redirect dock stays. Emergency unlock as small text button at the very bottom ("Use emergency unlock · 1 left"), opens a glass sheet asking for the typed reason.

### Patterns
Hour x weekday heatmap as a grid of rounded cells (white opacity scale, accent for top cells). Cards for: Reels/Shorts share, post-midnight minutes, first-touch flag, detected recurring patterns with their template suggestions ("Weekdays ~17:30, 20+ min YouTube. Commute? Try a podcast.") — each pattern card has a dismiss ("Not true") that suppresses that pattern.

### Weekly report
One scrollable glass sheet, section per §9.5 PRD, every number in instrument style. Check-in sliders at the end as glass sliders. Dense, factual, zero cheerleading.

### Life log
1-tap logging: four glass tiles (Projects, Sport, People, Learning), tap = logged with spring pulse + optional minutes stepper. Weekly focus shown as a ring (2 of 3 evenings).

### Settings & onboarding
Settings: glass card list rows, pending 24h changes as amber-tinted rows with countdown. Onboarding: one glass panel per page over the ambient background, page dots, permission pages with live status checkmarks.

### Debug screen
Exempt from the design system. Plain utilitarian list, monospace allowed.

## 8. Implementation Notes (Compose)

- One `LifesaverTheme` + one `GlassPanel` composable implementing §2 exactly (blur via Haze, tint, border, top light edge, radius, shadow, fallback flag). EVERY surface in the app uses `GlassPanel` or its pill/tile variants. No raw `Card`, no `Surface` with Material defaults anywhere.
- `RingGauge`, `GlassPill`, `GlassRow`, `RiskBars`, `AmbientBackground` as the other core components. Build all six first, then restyle screens using only these.
- Kill all Material color/elevation defaults: no dynamicColorScheme, tonalElevation 0 everywhere, Material components only as behavior shells, never for their looks.
- Performance: blur is expensive. Reuse one Haze state per screen, never nest blurred panels inside blurred panels, test scrolling at 60fps on the device. If frames drop, the fallback mode (§2) must be togglable in the debug screen for A/B checking.
- Verification: build, screenshot every screen (cockpit, intervention, block, patterns, life log, report, settings, onboarding p1), check each against this file before declaring done. Specifically check: light edge visible on top of cards, background blobs shine through panels, no flat gray surfaces anywhere, no all-caps labels.
