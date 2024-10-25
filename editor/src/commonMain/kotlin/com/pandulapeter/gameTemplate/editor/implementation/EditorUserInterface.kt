package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
) = Text(
    modifier = modifier,
    text = "Editor"
)