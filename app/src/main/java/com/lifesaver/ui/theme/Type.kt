package com.lifesaver.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Roboto (system default) — Material 1 type scale (DESIGN.md §2).
private val Roboto = FontFamily.Default

val LifesaverTypography = Typography(
    // Display — big dashboard numbers (34sp).
    displaySmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        color = TextPrimary,
    ),
    // Headline — in-content screen headers (24sp).
    headlineSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        color = TextPrimary,
    ),
    // Title — toolbar / card titles (Medium 20sp).
    titleLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        color = TextPrimary,
    ),
    // Subheading — list items, section labels (16sp).
    titleMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = TextPrimary,
    ),
    // Body 1 — default body (14sp, secondary).
    bodyMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = TextSecondary,
    ),
    // Body 2 — emphasized body (Medium 14sp).
    bodyLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = TextPrimary,
    ),
    // Caption — timestamps, footnotes (12sp, secondary).
    bodySmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = TextSecondary,
    ),
    // BUTTON — Medium 14sp, letterSpacing 0.05em. ALL CAPS enforced in components.
    labelLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.05.em,
    ),
)
