package com.pandulapeter.gameTemplate.editor.implementation.extensions

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.engine.Engine

internal expect fun Modifier.handleMouseZoom(): Modifier

internal fun Modifier.handleDragAndPan() = pointerInput("dragAndPan") {
    detectTransformGestures { _, pan, zoom, _ ->
        Engine.get().viewportManager.addToOffset(-pan)
        Engine.get().viewportManager.multiplyScaleFactor(zoom)
    }
}

internal fun Modifier.handleClick() = pointerInput("click") {
    detectTapGestures(onTap = EditorController::handleClick)
}