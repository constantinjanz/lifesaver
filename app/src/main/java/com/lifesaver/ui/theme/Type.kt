@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.lifesaver.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lifesaver.R

// Inter (bundled variable font) — weights 400 and 600 only (DESIGN v2 §3).
private val interRegular = Font(
    R.font.inter_variable,
    weight = FontWeight.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(400)),
)
private val interSemiBold = Font(
    R.font.inter_variable,
    weight = FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600)),
)
val Inter = FontFamily(interRegular, interSemiBold)

// Tabular figures for the cockpit numerals.
private const val TABULAR = "tnum"

val LifesaverTypography = Typography(
    // Hero numeral (central ring) — 44sp, 600, tabular, tight.
    displaySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 44.sp,
        letterSpacing = (-0.5).sp, fontFeatureSettings = TABULAR, color = TextPrimary,
    ),
    // Screen headers inside content — 24sp 600.
    headlineSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = TextPrimary,
    ),
    // Tile numbers / toolbar title — 22sp 600 tabular.
    titleLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
        fontFeatureSettings = TABULAR, color = TextPrimary,
    ),
    // List item / section label — 15sp 400.
    titleMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = TextPrimary,
    ),
    // Emphasized body — 15sp 600.
    bodyLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary,
    ),
    // Body — 15sp 400, secondary.
    bodyMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = TextSecondary,
    ),
    // Caption/hint — 12sp 400, sentence case (never caps).
    bodySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = TextCaption,
    ),
    // Button label — 15sp 600, sentence case (GlassPill does NOT uppercase).
    labelLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary,
    ),
)
