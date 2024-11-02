package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    OutlinedTextField(
        value = value,
        enabled = enabled,
        onValueChange = onValueChanged,
    )
}