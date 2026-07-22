package com.lifesaver.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifesaver.ui.theme.Accent
import com.lifesaver.ui.theme.OnAccent

// Raised button: accent bg, dark text, 2dp radius, real shadow (DESIGN.md §3).
// ALL CAPS is enforced here so callers pass normal-cased text.
@Composable
fun RaisedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = OnAccent,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

// Flat/text button: accent text, no background. Used for dialogs and secondary actions.
@Composable
fun FlatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: androidx.compose.ui.graphics.Color = Accent,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
    ) {
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

// Row of right-aligned flat dialog buttons ("CANCEL  CONFIRM").
@Composable
fun DialogButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}
