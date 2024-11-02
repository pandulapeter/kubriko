package com.pandulapeter.kubriko.editor.implementation.userInterface.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun EditorNumberInput(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChanged: (Float) -> Unit,
    enabled: Boolean = true,
) = EditorTextInput(
    modifier = modifier,
    title = title,
    value = "%.2f".format(value),
    onValueChanged = { it.toFloatOrNull()?.let(onValueChanged) },
    enabled = enabled,
)