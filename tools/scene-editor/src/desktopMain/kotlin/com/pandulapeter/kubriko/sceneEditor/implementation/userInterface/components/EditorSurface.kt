package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorSurface(
    modifier: Modifier = Modifier,
    isElevated: Boolean,
    content: @Composable () -> Unit,
) = Surface(
    modifier = modifier,
    tonalElevation = when (isSystemInDarkTheme()) {
        true -> if (isElevated) 8.dp else 4.dp
        false -> 0.dp
    },
    shadowElevation = when (isSystemInDarkTheme()) {
        true -> if (isElevated) 16.dp else 8.dp
        false -> if (isElevated) 8.dp else 4.dp
    },
    content = content,
)