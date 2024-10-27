package com.pandulapeter.gameTemplate.editor.implementation.extensions

import androidx.compose.ui.Modifier

internal expect fun Modifier.handleMouseClick(): Modifier

internal expect fun Modifier.handleMouseMove(): Modifier

internal expect fun Modifier.handleMouseZoom(): Modifier

internal expect fun Modifier.handleMouseDrag(): Modifier