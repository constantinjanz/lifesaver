package com.lifesaver.intervention

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifesaver.LifesaverApp
import com.lifesaver.data.InterventionEvent
import com.lifesaver.data.RedirectApp
import com.lifesaver.domain.DayKeys
import com.lifesaver.ui.components.AppIcon
import com.lifesaver.ui.components.glass.GlassBackground
import com.lifesaver.ui.components.glass.GlassPanel
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.components.glass.RingGauge
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.LifesaverTheme
import com.lifesaver.ui.theme.TextPrimary
import com.lifesaver.ui.theme.TextSecondary
import com.lifesaver.ui.theme.clickableNoRipple
import kotlinx.coroutines.launch

/**
 * The pre-feed intervention (PRD §3.2, DESIGN v2 §7). A glass cockpit panel fades in over the
 * (hidden) target app: hero ring countdown with breathing scale, the user's if-then plan quoted
 * in Inter 600, and a glass dock of redirect apps + micro-action + a gated Continue pill.
 */
class InterventionActivity : ComponentActivity() {

    private var appId: String = ""
    private var openIndex: Int = 1
    private var frictionSeconds: Int = 5
    private var triggeredAt: Long = 0
    private var recorded = false
    @Volatile private var selectedIntention: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appId = intent.getStringExtra(EXTRA_APP_ID) ?: ""
        val label = intent.getStringExtra(EXTRA_APP_LABEL) ?: appId
        openIndex = intent.getIntExtra(EXTRA_OPEN_INDEX, 1)
        frictionSeconds = intent.getIntExtra(EXTRA_FRICTION_SECONDS, 5)
        val minutesLeft = intent.getIntExtra(EXTRA_MINUTES_LEFT, 0)
        triggeredAt = System.currentTimeMillis()

        setContent {
            LifesaverTheme {
                GlassBackground(seed = 1, drift = false) {
                    InterventionContent(
                        label = label,
                        openIndex = openIndex,
                        frictionSeconds = frictionSeconds,
                        minutesLeft = minutesLeft,
                        intentions = com.lifesaver.detection.DetectionConfig.intentionsFor(appId),
                        onPickIntention = { selectedIntention = it },
                        onContinue = { record("continued"); finish() },
                        onDismiss = { record("dismissed"); goHome() },
                        onRedirect = ::redirectTo,
                    )
                }
            }
        }
    }

    private fun redirectTo(app: RedirectApp) {
        record("redirect", redirectTarget = app.appId)
        LifesaverApp.instance.container.installedApps.launchIntent(app.appId)?.let { startActivity(it) }
        finish()
    }

    private fun record(action: String, redirectTarget: String? = null) {
        if (recorded) return
        recorded = true
        val event = InterventionEvent(
            ts = triggeredAt,
            dayKey = DayKeys.dayKey(triggeredAt),
            appId = appId,
            openIndex = openIndex,
            frictionSeconds = frictionSeconds,
            action = action,
            redirectTarget = redirectTarget,
            latencyMs = System.currentTimeMillis() - triggeredAt,
            intention = selectedIntention,
        )
        val container = LifesaverApp.instance.container
        container.appScope.launch { container.database.interventionDao().insert(event) }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    override fun onBackPressed() {
        record("dismissed"); goHome()
    }

    companion object {
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_APP_LABEL = "app_label"
        const val EXTRA_OPEN_INDEX = "open_index"
        const val EXTRA_FRICTION_SECONDS = "friction_seconds"
        const val EXTRA_MINUTES_LEFT = "minutes_left"
    }
}

private data class RedirectEntry(val app: RedirectApp, val icon: Drawable?)

@Composable
private fun InterventionContent(
    label: String,
    openIndex: Int,
    frictionSeconds: Int,
    minutesLeft: Int,
    intentions: List<String>,
    onPickIntention: (String?) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    onRedirect: (RedirectApp) -> Unit,
) {
    var pickedIntention by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val rung = openIndex.coerceIn(1, 3)
    val hasNote = remember { com.lifesaver.service.FutureSelf.noteFor(context, rung) != null }
    val settings by produceState(initialValue = null as com.lifesaver.data.Settings?) {
        value = LifesaverApp.instance.container.settings.current()
    }

    // Fade + rise-in entrance (§6).
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val enter by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "enter")

    // Countdown is driven by wall-clock from a saved start, so rotation / any recreation resumes
    // where it was instead of resetting.
    val startMs = androidx.compose.runtime.saveable.rememberSaveable { System.currentTimeMillis() }
    var elapsed by remember { mutableStateOf(0f) }
    LaunchedEffect(startMs) {
        val total = frictionSeconds.coerceAtLeast(1) * 1000L
        while (true) {
            elapsed = ((System.currentTimeMillis() - startMs).toFloat() / total).coerceIn(0f, 1f)
            if (elapsed >= 1f) break
            kotlinx.coroutines.delay(50)
        }
    }
    val done = elapsed >= 1f

    // Future-self voice: play the note for this pause length on entry, and cut it off the moment
    // the countdown ends (so it never bleeds past the wait).
    val player = remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    LaunchedEffect(Unit) { if (hasNote) player.value = com.lifesaver.service.FutureSelf.play(context, rung) }
    LaunchedEffect(done) {
        if (done) { player.value?.let { runCatching { it.stop(); it.release() } }; player.value = null }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { player.value?.let { runCatching { it.stop(); it.release() } } }
    }

    // Mission = a random one of YOUR missions for the current time of day (falls back to any).
    val planText = remember(settings) {
        val plans = settings?.ifThenPlans ?: emptyList()
        val bucket = com.lifesaver.domain.MissionTime.bucketForHour(java.time.LocalTime.now().hour)
        val pool = plans.filter { it.context == bucket }.ifEmpty { plans }
        if (pool.isEmpty()) null else pool[kotlin.random.Random.nextInt(pool.size)].text
    }

    val redirects by produceState(initialValue = emptyList<RedirectEntry>(), settings) {
        val s = settings ?: return@produceState
        if (s.redirectApps.isEmpty()) { value = emptyList(); return@produceState }
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val all = LifesaverApp.instance.container.installedApps.launchable()
            s.redirectApps.map { r -> RedirectEntry(r, all.firstOrNull { it.packageName == r.appId }?.icon) }
        }
    }

    // Breathing scale on the ring, 4s cycle (§7).
    val transition = rememberInfiniteTransition(label = "breathe")
    val breathe by transition.animateFloat(
        initialValue = 0.94f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "breatheScale",
    )

    Column(
        modifier = Modifier.fillMaxSize().alpha(enter).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height((16 + 24 * (1 - enter)).dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RingGauge(progress = elapsed, diameter = 190.dp, modifier = Modifier.scale(breathe)) {
                Text("Breathe", style = MaterialTheme.typography.titleMedium, color = Accent)
            }
            Spacer(Modifier.height(28.dp))
            if (planText != null) {
                Text("Your mission", style = MaterialTheme.typography.bodySmall, color = Accent, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                planText ?: "One breath before the feed decides for you.",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            if (intentions.isNotEmpty()) {
                Text("What are you here for?", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    intentions.forEach { opt ->
                        GlassPill(
                            opt,
                            onClick = { pickedIntention = opt; onPickIntention(opt) },
                            primary = opt == pickedIntention,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            if (redirects.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    redirects.forEach { entry ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickableNoRipple(onClick = { onRedirect(entry.app) }),
                        ) {
                            AppIcon(entry.icon, size = 44.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(entry.app.label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            GlassPill("Close $label", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().alpha(if (done) 1f else 0.3f)) {
                GlassPill(
                    text = if (done) "Continue · $minutesLeft min left" else "Breathe · ${remainingSecs(elapsed, frictionSeconds)}s",
                    onClick = onContinue,
                    primary = true,
                    enabled = done,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun remainingSecs(elapsed: Float, total: Int): Int =
    ((1f - elapsed) * total).toInt().coerceAtLeast(0)
