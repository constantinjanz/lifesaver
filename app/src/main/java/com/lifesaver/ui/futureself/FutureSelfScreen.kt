package com.lifesaver.ui.futureself

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lifesaver.service.FutureSelf
import com.lifesaver.service.VoiceRecorder
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.components.glass.GlassScreen
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.Danger
import com.lifesaver.ui.theme.Success
import com.lifesaver.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private val RUNGS = listOf(
    Triple(1, "First open", "~5 second pause"),
    Triple(2, "Second open", "~15 second pause"),
    Triple(3, "Third+ open", "~30 second pause"),
)

@Composable
fun FutureSelfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var recordingRung by remember { mutableStateOf<Int?>(null) }
    var pendingRung by remember { mutableStateOf<Int?>(null) }
    var version by remember { mutableIntStateOf(0) } // bump to re-check files

    fun startRec(rung: Int) {
        if (recorder.start(FutureSelf.file(context, rung))) recordingRung = rung
    }
    fun stopRec() { recorder.stop(); recordingRung = null; version++ }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val r = pendingRung
        pendingRung = null
        if (granted && r != null) startRec(r)
    }

    fun requestAndStart(rung: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRec(rung)
        } else {
            pendingRung = rung
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-stop each note at 20s.
    LaunchedEffect(recordingRung) {
        val r = recordingRung
        if (r != null) { delay(20_000); if (recordingRung == r) stopRec() }
    }

    GlassScreen(title = "Future-self note", onBack = onBack, seed = 5) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassPanel {
                Text("A message to the you who reaches for the feed", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Record in your own voice — it plays on the pause screen and stops when the wait is over. The pause gets longer each time you open the app, so record one note per length.",
                    style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                )
            }

            RUNGS.forEach { (rung, title, sub) ->
                val exists = remember(version) { FutureSelf.exists(context, rung) }
                GlassPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        if (exists) Text("Saved ✓", style = MaterialTheme.typography.bodySmall, color = Success)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (recordingRung == rung) {
                        GlassPill("Stop recording", onClick = { stopRec() }, primary = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        Text("Recording…", style = MaterialTheme.typography.bodySmall, color = Danger)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPill(if (exists) "Re-record" else "Record", onClick = { requestAndStart(rung) }, primary = !exists, modifier = Modifier.weight(1f))
                            if (exists) {
                                GlassPill("Play", onClick = { FutureSelf.play(context, rung) }, modifier = Modifier.weight(1f))
                                GlassPill("Delete", onClick = { FutureSelf.delete(context, rung); version++ }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Text(
                "Tip: keep each note a touch shorter than its pause so it finishes naturally.",
                style = MaterialTheme.typography.bodySmall, color = Accent,
            )
        }
    }
}
