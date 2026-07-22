package com.lifesaver.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.lifesaver.service.InstalledApps
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.Surface as SurfaceColor
import com.lifesaver.ui.theme.SurfaceRaised
import com.lifesaver.ui.theme.TextDisabled

@Composable
fun AppIcon(drawable: Drawable?, size: Dp = 40.dp) {
    if (drawable == null) {
        Box(modifier = Modifier.size(size).clip(CircleShape).background(SurfaceRaised))
        return
    }
    val bitmap = remember(drawable) {
        drawable.toBitmap(width = 96, height = 96).asImageBitmap()
    }
    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size).clip(CircleShape))
}

/** Multi-select picker over installed apps, capped at [maxSelect] (PRD §3.1: 1–3 redirects). */
@Composable
fun AppPickerDialog(
    apps: List<InstalledApps.Entry>,
    selected: Set<String>,
    maxSelect: Int,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = SurfaceColor, shape = MaterialTheme.shapes.medium, shadowElevation = 24.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Pick redirect apps", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Up to $maxSelect apps to jump to instead of scrolling.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(8.dp))
                Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    apps.forEach { app ->
                        val isSelected = app.packageName in selected
                        val atLimit = selected.size >= maxSelect && !isSelected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !atLimit) { onToggle(app.packageName) }
                                .padding(vertical = 8.dp),
                        ) {
                            AppIcon(app.icon)
                            Spacer(Modifier.size(12.dp))
                            Text(
                                app.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (atLimit) TextDisabled else MaterialTheme.typography.titleMedium.color,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = Accent)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FlatButton("Done", onClick = onConfirm)
                }
            }
        }
    }
}
