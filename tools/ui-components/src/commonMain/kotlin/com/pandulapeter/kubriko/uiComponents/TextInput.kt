package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor

@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChanged: (String) -> Unit,
    enabled: Boolean = true,
) {
    // TODO: Focusing this fields should take focus away from KubrikoCanvas to avoid reacting to key presses
    BasicTextField(
        modifier = modifier,
        value = value,
        enabled = enabled,
        onValueChange = onValueChanged,
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        decorationBox = { innerTextField -> innerTextField() }
    )
}