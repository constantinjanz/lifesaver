package com.lifesaver.intervention

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifesaver.LifesaverApp
import com.lifesaver.data.InterventionEvent
import com.lifesaver.domain.DayKeys
import com.lifesaver.domain.PlanMatcher
import com.lifesaver.ui.components.CountdownRing
import com.lifesaver.ui.components.FlatButton
import com.lifesaver.ui.components.RaisedButton
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.LifesaverTheme
import com.lifesaver.ui.theme.Surface as SurfaceColor
import com.lifesaver.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * The pre-feed intervention (PRD §3.2). Escalating breathing countdown, the user's own if-then
 * plan quoted back, and a Continue button gated until the countdown ends. Redirect targets and
 * micro-actions (§3.4/M4) attach to the bottom sheet. Enters via the Intervention theme so it
 * covers the target app before its feed renders (§5 race).
 */
class InterventionActivity : ComponentActivity() {

    private var appId: String = ""
    private var openIndex: Int = 1
    private var frictionSeconds: Int = 5
    private var triggeredAt: Long = 0
    private var recorded = false

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
                Surface(modifier = Modifier.fillMaxSize(), color = com.lifesaver.ui.theme.Background) {
                    InterventionContent(
                        label = label,
                        frictionSeconds = frictionSeconds,
                        minutesLeft = minutesLeft,
                        onContinue = { record("continued"); finish() },
                        onDismiss = { record("dismissed"); goHome() },
                    )
                }
            }
        }
    }

    private fun record(action: String) {
        if (recorded) return
        recorded = true
        val event = InterventionEvent(
            ts = triggeredAt,
            dayKey = DayKeys.dayKey(triggeredAt),
            appId = appId,
            openIndex = openIndex,
            frictionSeconds = frictionSeconds,
            action = action,
            latencyMs = System.currentTimeMillis() - triggeredAt,
        )
        val container = LifesaverApp.instance.container
        container.appScope.launch {
            container.database.interventionDao().insert(event)
        }
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

@Composable
private fun InterventionContent(
    label: String,
    frictionSeconds: Int,
    minutesLeft: Int,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    var elapsed by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val total = frictionSeconds.coerceAtLeast(1) * 1000L
        val step = 50L
        var t = 0L
        while (t < total) {
            delay(step)
            t += step
            elapsed = (t.toFloat() / total).coerceIn(0f, 1f)
        }
        elapsed = 1f
    }
    val done = elapsed >= 1f

    val plan by produceState<String?>(initialValue = null) {
        val plans = LifesaverApp.instance.container.settings.current().ifThenPlans
        value = PlanMatcher.match(plans, LocalTime.now().hour)?.text
    }

    // Breathing pulse for the center hint.
    val transition = rememberInfiniteTransition(label = "breathe")
    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CountdownRing(progress = elapsed) {
                Text(
                    "BREATHE",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Accent,
                    modifier = Modifier.alpha(pulse),
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                plan ?: "One breath before the feed decides for you.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }

        Surface(
            color = SurfaceColor,
            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FlatButton("Not now — close $label", onClick = onDismiss)
                Spacer(Modifier.height(8.dp))
                RaisedButton(
                    text = if (done) "Continue — $minutesLeft min left" else "Wait…",
                    onClick = onContinue,
                    enabled = done,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
