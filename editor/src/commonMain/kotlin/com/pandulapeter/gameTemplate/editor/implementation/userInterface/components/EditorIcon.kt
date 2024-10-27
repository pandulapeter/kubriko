package com.pandulapeter.gameTemplate.editor.implementation.userInterface.components

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun EditorIcon(
    modifier: Modifier = Modifier,
    drawableResource: DrawableResource,
    contentDescription: String,
) = Icon(
    modifier = modifier,
    painter = painterResource(drawableResource),
    contentDescription = contentDescription,
)