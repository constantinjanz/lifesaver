package com.lifesaver.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.components.glass.GlassPill
import com.lifesaver.ui.theme.Accent

/** Back-compat shims: raised → primary glass pill, flat → secondary glass pill (DESIGN v2 §5).
 *  Sentence case is preserved (no more forced ALL CAPS). */
@Composable
fun RaisedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = GlassPill(text = text, onClick = onClick, modifier = modifier, primary = true, enabled = enabled)

@Composable
fun FlatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") contentColor: Color = Accent,
) = GlassPill(text = text, onClick = onClick, modifier = modifier, primary = false, enabled = enabled)

@Composable
fun DialogButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}
