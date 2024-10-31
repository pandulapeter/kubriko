package com.pandulapeter.kubriko.editor.implementation.userInterface.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun EditorNumberInput(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChanged: (Float) -> Unit,
    enabled: Boolean = true,
) = Column(
    modifier = modifier,
) {
    EditorTextLabel(
        text = "$title: ${"%.2f".format(value)}",
    )
    // TODO: Focusing this fields should take focus away from the EngineCanvas to avoid navigation using the arrow keys.
    OutlinedTextField(
        value = "%.2f".format(value),
        enabled = enabled,
        onValueChange = { it.toFloatOrNull()?.let(onValueChanged) }
    )
}