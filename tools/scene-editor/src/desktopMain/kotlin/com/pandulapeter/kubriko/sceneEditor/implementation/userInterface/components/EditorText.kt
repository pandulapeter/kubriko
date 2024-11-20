package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorText(
    modifier: Modifier = Modifier,
    text: String,
    isBold: Boolean = false,
) = Text(
    modifier = modifier,
    style = MaterialTheme.typography.bodySmall,
    text = text,
    fontWeight = if (isBold) FontWeight.Bold else null,
)

@Composable
internal fun EditorTextTitle(
    modifier: Modifier = Modifier,
    text: String,
) = Text(
    modifier = modifier.padding(bottom = 4.dp),
    style = MaterialTheme.typography.titleSmall,
    text = text,
)

@Composable
internal fun EditorTextLabel(
    modifier: Modifier = Modifier,
    text: String,
) = Text(
    modifier = modifier,
    style = MaterialTheme.typography.labelSmall,
    text = text,
)
