package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = Surface(
    modifier = modifier,
    tonalElevation = when (isSystemInDarkTheme()) {
        true -> 4.dp
        false -> 0.dp
    },
    shadowElevation = when (isSystemInDarkTheme()) {
        true -> 4.dp
        false -> 2.dp
    },
    content = content,
)