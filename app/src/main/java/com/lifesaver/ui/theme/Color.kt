package com.lifesaver.ui.theme

import androidx.compose.ui.graphics.Color

// "Liquid Glass Cockpit" palette (DESIGN v2). Text is always white-with-opacity so the glass
// tint shows through — never pure gray hex.

// Background system (§1)
val Base = Color(0xFF0A0A0F) // near-black window base
val BlobOrange = Color(0xFFFF9500)
val BlobViolet = Color(0xFF7B5CFF)
val BlobBlue = Color(0xFF2E5CFF)

// Glass material (§2)
val GlassTint = Color(0x14FFFFFF) // white 8%
val GlassTintStrong = Color(0x1AFFFFFF) // white 10% (smaller elements/pills)
val GlassBorder = Color(0x2EFFFFFF) // white 18%
val GlassLightEdge = Color(0x52FFFFFF) // white 32% top inner highlight
val GlassTrack = Color(0x1FFFFFFF) // white 12% ring/track
val GlassFallback = Color(0xEB16161D) // solid #16161D @ 92% when blur unavailable

// Data / accents (§4) — one accent per panel, data glows on neutral glass.
val Accent = Color(0xFFFF9500) // orange — rings, active bars, risk, flame
val Success = Color(0xFF30D158) // green — streak intact, goals met
val Danger = Color(0xFFFF453A) // red-orange — budget out, block, integrity gaps

// Text (§3)
val TextPrimary = Color(0xFFFFFFFF) // 100% — values
val TextSecondary = Color(0x99FFFFFF) // 60% — labels
val TextCaption = Color(0x66FFFFFF) // 40% — captions/hints

// Bars / dividers
val BarDefault = Color(0x24FFFFFF) // white 14%
val HairlineDivider = Color(0x1AFFFFFF) // white 10%

// Content color on accent-tinted glass
val OnAccent = Color(0xFFFFFFFF)

// --- Transitional aliases (old v1 names) so screens keep compiling during the glass migration.
// These are progressively removed as each screen is restyled onto the glass components.
val Background = Base
val Surface = GlassFallback
val SurfaceRaised = Color(0x24FFFFFF)
val AppBar = Base
val AccentDark = Accent
val Warning = Accent
val TextDisabled = TextCaption
val Divider = HairlineDivider
val StatusBar = Base
