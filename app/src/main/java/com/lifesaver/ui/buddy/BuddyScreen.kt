package com.lifesaver.ui.buddy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifesaver.buddy.RequestResult
import com.lifesaver.detection.DetectionConfig
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.components.glass.GlassScreen
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.Success
import com.lifesaver.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BuddyScreen(
    paired: Boolean,
    buddyLabel: String?,
    onPair: suspend (String) -> String?,
    onRequest: suspend (String, String, Int, String) -> RequestResult?,
    onPoll: suspend (String) -> String,
    onApproved: (Int) -> Unit,
    onUnpair: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingReq by remember { mutableStateOf<String?>(null) }
    var pendingMinutes by remember { mutableIntStateOf(0) }

    // Poll the pending request until the buddy decides.
    LaunchedEffect(pendingReq) {
        val req = pendingReq ?: return@LaunchedEffect
        while (true) {
            delay(3000)
            when (onPoll(req)) {
                "approved" -> {
                    onApproved(pendingMinutes)
                    message = "Approved! +$pendingMinutes min unlocked."
                    pendingReq = null
                    return@LaunchedEffect
                }
                "denied" -> {
                    message = "Your buddy denied it."
                    pendingReq = null
                    return@LaunchedEffect
                }
            }
        }
    }

    GlassScreen(title = "Buddy unlock", onBack = onBack, seed = 4) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!paired) {
                PairSection(busy) { label ->
                    busy = true; message = null
                    scope.launch {
                        val url = onPair(label)
                        busy = false
                        if (url != null) {
                            openWhatsApp(context, "Be my Lifesaver gatekeeper. Set a secret PIN here — and please DON'T tell me the PIN, that's the whole point: $url")
                            message = "Setup link sent. Once your buddy sets their PIN, you can ask for time."
                        } else {
                            message = "Couldn't reach the server. Check your connection."
                        }
                    }
                }
            } else {
                RequestSection(
                    buddyLabel = buddyLabel ?: "your buddy",
                    busy = busy,
                    waiting = pendingReq != null,
                    onAsk = { appId, appLabel, minutes, reason ->
                        busy = true; message = null
                        scope.launch {
                            val res = onRequest(appId, appLabel, minutes, reason)
                            busy = false
                            if (res != null) {
                                pendingMinutes = minutes
                                pendingReq = res.requestId
                                openWhatsApp(context, "Lifesaver: can I get +$minutes min of $appLabel? Approve with your PIN here: ${res.approveUrl}")
                            } else {
                                message = "Couldn't create the request. Has your buddy set their PIN yet?"
                            }
                        }
                    },
                    onUnpair = onUnpair,
                )
            }

            message?.let {
                GlassPanel {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = if (it.startsWith("Approved")) Success else TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PairSection(busy: Boolean, onPair: (String) -> Unit) {
    var label by remember { mutableStateOf("") }
    GlassPanel {
        Text("Pick your gatekeeper", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Choose someone you trust. They set a secret PIN (you never see it). From then on, more time needs their approval on WhatsApp.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = label, onValueChange = { label = it },
            placeholder = { Text("Their name (e.g. Max)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        if (busy) CircularProgressIndicator() else
            GlassPill("Send setup link on WhatsApp", onClick = { onPair(label.ifBlank { "your buddy" }) }, primary = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RequestSection(
    buddyLabel: String,
    busy: Boolean,
    waiting: Boolean,
    onAsk: (String, String, Int, String) -> Unit,
    onUnpair: () -> Unit,
) {
    var appId by remember { mutableStateOf(DetectionConfig.targets.first().appId) }
    var minutes by remember { mutableIntStateOf(15) }
    var reason by remember { mutableStateOf("") }

    GlassPanel {
        Text("Ask $buddyLabel for more time", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetectionConfig.targets.forEach { t ->
                GlassPill(t.label, onClick = { appId = t.appId }, primary = t.appId == appId)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("+$minutes min", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("−5", onClick = { minutes = (minutes - 5).coerceAtLeast(5) })
                GlassPill("+5", onClick = { minutes = (minutes + 5).coerceAtMost(120) })
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = reason, onValueChange = { reason = it },
            placeholder = { Text("Why? (optional, your buddy sees this)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        when {
            waiting -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.height(22.dp))
                Text("Waiting for $buddyLabel to approve…", style = MaterialTheme.typography.bodyMedium, color = Accent)
            }
            busy -> CircularProgressIndicator()
            else -> {
                val label = DetectionConfig.targetFor(appId)?.label ?: "app"
                GlassPill("Ask on WhatsApp", onClick = { onAsk(appId, label, minutes, reason) }, primary = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    GlassPill("Unpair buddy", onClick = onUnpair, modifier = Modifier.fillMaxWidth())
}

private fun openWhatsApp(context: android.content.Context, text: String) {
    val uri = Uri.parse("https://wa.me/?text=" + Uri.encode(text))
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
