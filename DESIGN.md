# Lifesaver — Visual Redesign Spec: "2016 Material" Theme

**This is a complete restyle instruction for Claude Code. Functionality stays untouched. Read the whole file before writing code, then apply the theme app-wide. The target aesthetic is peak-2016 Android Material Design (Material 1), dark variant. Think Google apps and Instagram circa 2016: flat bold surfaces, real drop shadows, ink ripples, ALL CAPS buttons, sharp 2dp corners. No Material You, no dynamic color, no pill shapes, no tonal elevation. If a component looks like 2024 Material 3, it is wrong.**

---

## 1. Color System (fixed palette, no dynamic color)

Dark theme only. Disable Material 3 dynamic color entirely.

| Token | Value | Usage |
|---|---|---|
| `background` | `#212121` (Grey 900) | Window background |
| `surface` | `#303030` (Grey 850) | Cards, sheets, dialogs |
| `surfaceRaised` | `#424242` (Grey 800) | Menus, raised elements |
| `appBar` | `#212121` | Toolbar, same as background, separated by elevation shadow only |
| `primary` / accent | `#1DE9B6` (Teal A400) | FAB, active states, progress, switches, links, countdown ring |
| `primaryDark` | `#00BFA5` (Teal A700) | Pressed states, dark accents |
| `warning` | `#FFC400` (Amber A400) | Budget below 25%, streak-at-risk states |
| `danger` | `#FF3D00` (Deep Orange A400) | Budget exhausted, block screen accents, destructive actions |
| `textPrimary` | `#FFFFFF` at 100% | Headlines, key numbers |
| `textSecondary` | `#FFFFFF` at 70% | Body text |
| `textDisabled` | `#FFFFFF` at 38% | Disabled, hints |
| `divider` | `#FFFFFF` at 12% | 1dp hairline dividers |

Rules:
- Accent color is used sparingly and precisely: one FAB, active tab indicator, progress fills, the breathing ring. Never as large background fills except the intervention countdown ring.
- Status bar: solid `#1B1B1B`, no translucency games. Navigation bar `#212121`.
- No gradients anywhere. Flat fills only. 2016 Material is flat color + shadow, never gradient.

## 2. Typography (Roboto, Material 1 type scale)

Font: Roboto only (system default is fine). No custom fonts, no variable font tricks.

| Style | Spec | Usage |
|---|---|---|
| Display | Roboto Regular 34sp, textPrimary | Big dashboard numbers (time saved, streak count) |
| Headline | Roboto Regular 24sp | Screen headers inside content |
| Title | Roboto Medium 20sp | Toolbar title, card titles |
| Subheading | Roboto Regular 16sp | List items, section labels |
| Body 1 | Roboto Regular 14sp, textSecondary | Default body |
| Body 2 | Roboto Medium 14sp | Emphasized body |
| Caption | Roboto Regular 12sp, textSecondary | Timestamps, footnotes |
| BUTTON | Roboto Medium 14sp, ALL CAPS, letterSpacing 0.05em | Every button label |

ALL CAPS button text is mandatory and non-negotiable. It is the single strongest 2016 signal.

## 3. Shape, Elevation, Components

- **Corner radius: 2dp everywhere.** Cards, buttons, dialogs, sheets. Never more. FAB is the only circle.
- **Real shadows, not tonal elevation:** cards rest at 2dp elevation, raised buttons 2dp resting / 8dp pressed, FAB 6dp resting / 12dp pressed, dialogs 24dp, app bar 4dp. In Compose use `shadowElevation`, keep surfaces the same color regardless of elevation (no tonalElevation, set it to 0).
- **App bar:** classic 56dp toolbar, title left-aligned 16dp inset, Title style, overflow menu with three vertical dots. Casts a 4dp shadow onto scrolling content.
- **FAB:** 56dp circle, accent background, dark icon (`#212121`), bottom-right, 16dp margins. Use on the dashboard for "Edit plans".
- **Buttons:** raised buttons (accent bg, dark text, 2dp radius, shadow) for primary actions; flat/text buttons (accent text, no bg) for dialogs and secondary actions. Dialog buttons are ALWAYS flat, right-aligned, ALL CAPS ("CANCEL  CONFIRM").
- **Ink ripples:** every tappable surface shows a ripple from the touch point. White 12% on dark surfaces, dark on accent surfaces.
- **Switches/sliders:** Material 1 style, accent when on, grey when off.
- **Progress:** linear determinate 4dp bars for budgets (accent fill → amber under 25% → deep orange at 0), grey 800 track. Circular indeterminate spinner in accent for loading.
- **Dividers:** 1dp hairlines between list items. 2016 lists love dividers, use them.
- **Snackbars:** dark grey `#323232`, white text, accent flat action button, bottom, 2dp radius. Use for confirmations ("SETTING QUEUED FOR 24H — UNDO").
- **Lists:** 72dp two-line list items with 40dp circular icon/avatar left, 16dp keylines. Settings screen is a classic preference list with category subheaders in accent Caption style.
- **Empty states:** centered flat illustration-free layout, Grey icon 56dp, Subheading text, flat accent button.

## 4. Iconography

- Material icons, filled style (2016 had no outlined/rounded variants). 24dp, white 70% inactive, accent or white 100% active.
- No emoji in UI. No custom illustrations. Icons carry the personality.

## 5. Motion

- Standard 2016 curves: FastOutSlowIn for on-screen movement, LinearOutSlowIn for entering, FastOutLinearIn for exiting. Durations 195ms (small), 225ms (medium), 375ms (large/full-screen).
- **Circular reveal** for the intervention screen: it expands from screen center as a circle when it intercepts an app open. This is THE signature 2016 transition, invest here.
- Screen-to-screen: vertical slide-up + fade for detail screens, standard fade-through for tabs.
- FAB presses scale 1.0 → 1.06 with shadow lift.
- No spring/bounce physics anywhere. 2016 motion is confident and damped, never bouncy.

## 6. Screen-by-Screen Art Direction

### Dashboard
- Toolbar "Lifesaver", overflow menu (Settings, Debug).
- Content: vertical scroll of 2dp-radius cards on `#212121`, 8dp gaps, 16dp side margins:
  1. **Today card:** two budget rows (Instagram, YouTube), each: app icon 40dp, app name Subheading, linear progress bar, right-aligned "12 MIN LEFT" in Body 2. Bar color shifts teal → amber → deep orange.
  2. **Streak card:** Display-size number centered ("7"), Caption "DAY STREAK" beneath, small flame icon in accent. Longest streak as Caption bottom-right.
  3. **Time saved card:** Display number ("6H 20M"), Caption "SAVED THIS WEEK", below it the tangible translation as Body 1 ("≈ 4 WORKOUTS").
  4. **Substitution card:** percentage + thin progress bar, Caption "OF INTERVENTIONS REDIRECTED".
  5. **Check-in sparkline card:** simple 2dp accent line chart, no axis decoration.
- FAB bottom-right: edit icon → if-then plan editor.

### Intervention Screen
- Full-bleed `#212121`, no toolbar, enters via circular reveal.
- Center: 180dp countdown ring, 4dp accent stroke, sweeping clockwise; breathing hint text inside ("BREATHE") pulsing opacity 70%→100% synced to a 4s cycle.
- Below ring: the user's if-then plan, Headline style, white, max 3 lines, quoted exactly as typed.
- Bottom sheet area (surface `#303030`, 2dp top corners): row of redirect app icons (40dp, circular, labeled with Caption), a flat "MICRO-ACTION" button, and the gated raised button "CONTINUE — 12 MIN LEFT" which stays textDisabled until the countdown completes, then fills accent.

### Block Screen (budget exhausted)
- Same layout language as intervention, but ring is static Deep Orange A400, icon lock inside.
- Headline: "INSTAGRAM IS DONE FOR TODAY". Body: time saved today + streak.
- Redirect icons stay. Emergency unlock as a flat deep-orange text button at the very bottom, small: "USE EMERGENCY UNLOCK (1 LEFT)" → confirmation dialog with flat buttons.

### Onboarding
- Classic 2016 viewpager: full-screen colored feel but keep dark surfaces, one concept per page, Headline + Body 1, dot page indicator (accent active dot), flat "SKIP" top-right, raised "NEXT" bottom-right.
- Permission pages: 56dp filled icon in accent, live status as a checkmark chip, raised "GRANT" button per permission.

### Settings
- Classic preference list: category subheaders ("BUDGETS", "FRICTION", "BINDING") in accent Caption ALL CAPS, two-line items, dividers, switches right-aligned.
- Pending 24h changes: amber-tinted list item with clock icon and Caption countdown ("ACTIVE TOMORROW 18:32 — CANCEL").

### Weekly Check-in
- Dialog-style card, 24dp elevation, three labeled sliders, flat "DONE" button. Under 30 seconds to complete, no decoration.

### Debug Screen
- Deliberately unstyled utilitarian list, monospace values allowed. This screen is exempt from the design system.

## 7. Implementation Notes (Compose)

- Build one `LifesaverTheme` wrapping `MaterialTheme` with the custom dark `ColorScheme`, custom `Typography` mapping the table above, and `Shapes(small/medium/large = RoundedCornerShape(2.dp))`.
- Set `tonalElevation = 0.dp` on all surfaces; express depth exclusively via `shadowElevation`.
- Kill dynamic color: never call `dynamicDarkColorScheme`.
- Create small reusable components first: `RaisedButton`, `FlatButton` (ALL CAPS enforced in the component, callers pass normal text), `LifesaverCard`, `BudgetBar`, `CountdownRing`. Then restyle screens using only these.
- Every button label passes through the components so ALL CAPS can never be forgotten.
- Verify: build the APK, then screenshot each screen (dashboard, intervention, block, settings, onboarding page 1) and check against this spec before declaring done.
