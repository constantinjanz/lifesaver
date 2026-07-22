package com.lifesaver.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.components.RaisedButton
import com.lifesaver.ui.theme.Accent

/** 30-second Sunday check-in — 3 sliders 1–10 (PRD §3.7). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinScreen(onSubmit: (control: Int, satisfaction: Int, impulse: Int) -> Unit, onBack: () -> Unit) {
    var control by remember { mutableStateOf(5f) }
    var satisfaction by remember { mutableStateOf(5f) }
    var impulse by remember { mutableStateOf(5f) }

    com.lifesaver.ui.components.glass.GlassScreen(title = "Weekly check-in", onBack = onBack, seed = 5) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SliderRow("Sense of control over your phone use", control) { control = it }
            SliderRow("Satisfaction with how you spent your time", satisfaction) { satisfaction = it }
            SliderRow("Strength of the scroll impulse this week", impulse) { impulse = it }
            Spacer(Modifier.height(8.dp))
            RaisedButton(
                "Done",
                onClick = { onSubmit(control.toInt(), satisfaction.toInt(), impulse.toInt()); onBack() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${value.toInt()}", style = MaterialTheme.typography.bodyLarge, color = Accent)
        }
        Slider(value = value, onValueChange = onChange, valueRange = 1f..10f, steps = 8)
    }
}
