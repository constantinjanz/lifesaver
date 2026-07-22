package com.lifesaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.Danger
import com.lifesaver.ui.theme.SurfaceRaised
import com.lifesaver.ui.theme.Warning

// Linear determinate 4dp budget bar. Fill shifts accent -> amber (<25%) -> deep orange (0) (DESIGN.md §3).
@Composable
fun BudgetBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val fill = budgetColor(clamped)
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shape)
            .background(SurfaceRaised),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(shape)
                .background(fill),
        )
    }
}

// `fraction` is the share of budget REMAINING (1.0 full, 0.0 exhausted).
fun budgetColor(fraction: Float): Color = when {
    fraction <= 0f -> Danger
    fraction < 0.25f -> Warning
    else -> Accent
}
