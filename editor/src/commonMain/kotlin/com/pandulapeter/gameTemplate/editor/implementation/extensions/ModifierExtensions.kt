package com.pandulapeter.gameTemplate.editor.implementation.extensions

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.gameTemplate.editor.implementation.EditorController

internal expect fun Modifier.handleMouseMove(): Modifier

internal expect fun Modifier.handleMouseZoom(): Modifier

internal expect fun Modifier.handleMouseDrag(): Modifier

internal fun Modifier.handleClick() = pointerInput("click") {
    detectTapGestures(
        onTap = EditorController::handleClick,
    )
}