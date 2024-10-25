package com.pandulapeter.gameTemplate.gameplayController.implementation.extensions

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.gameplayController.GameplayController

internal expect fun Modifier.handleMouseZoom(): Modifier

internal fun Modifier.handleDragAndPan() = pointerInput(Unit) {
    detectTransformGestures { _, pan, zoom, _ ->
        if (GameplayController.get().isRunning.value) {
            Engine.get().viewportManager.addToOffset(-pan)
            Engine.get().viewportManager.multiplyScaleFactor(zoom)
        }
    }
}