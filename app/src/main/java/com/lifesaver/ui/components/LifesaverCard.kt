package com.lifesaver.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lifesaver.ui.components.glass.GlassPanel

/** Back-compat shim: the old card is now a glass panel (DESIGN v2). Existing screens keep their
 *  call sites; the material comes from GlassPanel. */
@Composable
fun LifesaverCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassPanel(modifier = modifier.fillMaxWidth(), content = content)
}
