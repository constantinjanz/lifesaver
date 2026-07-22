package com.lifesaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifesaver.service.Permissions
import com.lifesaver.ui.LifesaverNavHost
import com.lifesaver.ui.LifesaverViewModel
import com.lifesaver.ui.theme.Background
import com.lifesaver.ui.theme.LifesaverTheme

class MainActivity : ComponentActivity() {

    private var vm: LifesaverViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: LifesaverViewModel = viewModel()
            vm = viewModel
            val state by viewModel.state.collectAsStateWithLifecycle()

            LifesaverTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    LifesaverNavHost(
                        state = state,
                        onGrantPermission = { kind ->
                            startActivity(Permissions.settingsIntent(this@MainActivity, kind))
                        },
                        onCompleteOnboarding = { budgets ->
                            viewModel.completeOnboarding(
                                plans = emptyList(),
                                redirects = emptyList(),
                                budgets = budgets,
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Permission grants and usage happen outside the app; re-read them each time we return.
        vm?.refreshPermissions()
        vm?.refreshUsageStats()
    }
}
