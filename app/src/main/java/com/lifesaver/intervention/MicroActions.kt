package com.lifesaver.intervention

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.components.LifesaverCard
import com.lifesaver.ui.components.RaisedButton
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private enum class Micro { MENU, BREATHE, JOURNAL, GOALS }

/** In-app micro-actions (PRD §3.2): 60s breathing, one journal line, today's goals. Completing
 *  any of them counts as a substitution (the caller records action = micro_action). */
@Composable
fun MicroActionSheet(goals: List<String>, onComplete: () -> Unit) {
    var screen by remember { mutableStateOf(Micro.MENU) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (screen) {
            Micro.MENU -> Menu(onPick = { screen = it })
            Micro.BREATHE -> Breathe(onDone = onComplete)
            Micro.JOURNAL -> Journal(onDone = onComplete)
            Micro.GOALS -> Goals(goals, onDone = onComplete)
        }
    }
}

@Composable
private fun Menu(onPick: (Micro) -> Unit) {
    Text("Pick one", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
    MenuItem("Breathe for 60 seconds") { onPick(Micro.BREATHE) }
    Spacer(Modifier.height(8.dp))
    MenuItem("Write one line") { onPick(Micro.JOURNAL) }
    Spacer(Modifier.height(8.dp))
    MenuItem("See today's goals") { onPick(Micro.GOALS) }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    LifesaverCard {
        RaisedButton(label, onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Breathe(onDone: () -> Unit) {
    var remaining by remember { mutableStateOf(60) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    val transition = rememberInfiniteTransition(label = "breathe")
    val scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "scale",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(160.dp).scale(scale).clip(CircleShape).background(Accent),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (remaining > 0) "$remaining" else "Done",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        RaisedButton(
            text = if (remaining > 0) "Finish early" else "Done",
            onClick = onDone,
        )
    }
}

@Composable
private fun Journal(onDone: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Text("One honest line", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("What were you actually looking for?") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
    )
    Spacer(Modifier.height(16.dp))
    RaisedButton("Done", onClick = onDone, enabled = text.isNotBlank())
}

@Composable
private fun Goals(goals: List<String>, onDone: () -> Unit) {
    Text("Today's goals", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    if (goals.isEmpty()) {
        Text("Add if-then plans to see them here.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    } else {
        goals.forEach {
            LifesaverCard { Text(it, style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(8.dp))
        }
    }
    Spacer(Modifier.height(16.dp))
    RaisedButton("Done", onClick = onDone)
}
