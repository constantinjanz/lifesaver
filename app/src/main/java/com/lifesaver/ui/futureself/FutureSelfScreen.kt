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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.core.content.ContextCompat
import com.lifesaver.service.FutureSelf
import com.lifesaver.service.VoiceRecorder
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.components.glass.GlassScreen
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.Danger
import com.lifesaver.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun FutureSelfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var hasNote by remember { mutableStateOf(FutureSelf.exists(context)) }

    fun startRec() { if (recorder.start()) recording = true }
    fun stopRec() { recorder.stop(); recording = false; hasNote = FutureSelf.exists(context) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRec()
    }

    // Cap the note at 20 seconds.
    LaunchedEffect(recording) {
        if (recording) { delay(20_000); if (recording) stopRec() }
    }

    GlassScreen(title = "Future-self note", onBack = onBack, seed = 5) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassPanel {
                Text("A message to the you who reaches for the feed", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                Text(
                    "Record up to 20 seconds — in your own voice. It plays on the pause screen, right when you're about to scroll. Say what your future self would want to hear.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                GlassPill(
                    text = if (recording) "Stop recording" else if (hasNote) "Re-record" else "Record",
                    onClick = {
                        if (recording) {
                            stopRec()
                        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startRec()
                        } else {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (recording) {
                    Spacer(Modifier.height(8.dp))
                    Text("Recording…", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Danger)
                }
            }

            if (hasNote && !recording) {
                GlassPanel {
                    Text("Your note", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassPill("Play", onClick = { FutureSelf.play(context) }, modifier = Modifier.weight(1f))
                        GlassPill("Delete", onClick = { FutureSelf.delete(context); hasNote = false }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("It'll play on every pause. You can re-record or delete anytime.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Accent)
                }
            }
        }
    }
}
