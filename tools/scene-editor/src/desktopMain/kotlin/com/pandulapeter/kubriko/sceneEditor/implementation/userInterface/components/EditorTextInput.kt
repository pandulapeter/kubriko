package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor

@Composable
internal fun EditorTextInput(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    onValueChanged: (String) -> Unit,
    enabled: Boolean = true,
) = Column(
    modifier = modifier,
) {
    EditorTextLabel(
        text = title,
    )
    // TODO: Focusing this fields should take focus away from the EngineCanvas to avoid navigation using the arrow keys.
    BasicTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        enabled = enabled,
        onValueChange = onValueChanged,
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        decorationBox = { innerTextField ->
            innerTextField()
        }
    )
}