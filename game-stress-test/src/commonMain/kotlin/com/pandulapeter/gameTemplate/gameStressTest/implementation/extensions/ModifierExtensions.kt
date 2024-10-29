package com.pandulapeter.gameTemplate.gameStressTest.implementation.extensions

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.gameTemplate.engine.Engine

internal expect fun Modifier.handleMouseZoom(): Modifier

internal fun Modifier.handleDragAndPan() = pointerInput("dragAndPan") {
    detectTransformGestures { _, pan, zoom, _ ->
        if (Engine.get().stateManager.isRunning.value) {
            Engine.get().viewportManager.addToCenter(pan)
            Engine.get().viewportManager.multiplyScaleFactor(zoom)
        }
    }
}