package com.lifesaver.ui.components.glass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lifesaver.ui.theme.TextPrimary

/**
 * A screen over the ambient glass background with a transparent top bar (no Material surface). Used
 * by every pushed screen so headers float over the blobs instead of a flat bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    seed: Int = 3,
    content: @Composable (PaddingValues) -> Unit,
) {
    GlassBackground(seed = seed) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary,
                    ),
                )
            },
            content = content,
        )
    }
}
