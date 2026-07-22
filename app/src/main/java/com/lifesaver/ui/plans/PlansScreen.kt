package com.lifesaver.ui.plans

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.LifesaverApp
import com.lifesaver.data.Settings
import kotlinx.coroutines.launch

/** Standalone plan + redirect editor (dashboard FAB). Persists on exit to avoid per-keystroke writes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(onBack: () -> Unit) {
    val container = LifesaverApp.instance.container
    val loaded by produceState<Settings?>(initialValue = null) {
        value = container.settings.current()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plans & redirects") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val initial = loaded
        if (initial == null) return@Scaffold

        var plans by remember { mutableStateOf(initial.ifThenPlans) }
        var redirects by remember { mutableStateOf(initial.redirectApps) }

        DisposableEffect(Unit) {
            onDispose {
                container.appScope.launch {
                    container.settings.setIfThenPlans(plans)
                    container.settings.setRedirectApps(redirects)
                }
            }
        }

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            PlanEditor(
                plans = initial.ifThenPlans,
                redirects = redirects,
                onPlansChange = { plans = it },
                onRedirectsChange = { redirects = it },
            )
        }
    }
}
