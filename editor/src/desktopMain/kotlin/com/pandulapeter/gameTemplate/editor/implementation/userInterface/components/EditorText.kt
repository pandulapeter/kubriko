package com.pandulapeter.gameTemplate.editor.implementation.userInterface.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorText(
    modifier: Modifier = Modifier,
    text: String,
) = Text(
    modifier = modifier,
    style = MaterialTheme.typography.caption,
    text = text,
)

@Composable
internal fun EditorTextTitle(
    modifier: Modifier = Modifier,
    text: String,
) = Text(
    modifier = modifier.padding(bottom = 4.dp),
    style = MaterialTheme.typography.subtitle2,
    text = text,
)

@Composable
internal fun EditorTextLabel(
    modifier: Modifier = Modifier,
    text: String,
) = Text(
    modifier = modifier,
    style = MaterialTheme.typography.caption,
    text = text,
)
