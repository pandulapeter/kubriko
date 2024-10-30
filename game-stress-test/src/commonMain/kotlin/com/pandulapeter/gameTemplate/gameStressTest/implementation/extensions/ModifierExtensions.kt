package com.pandulapeter.gameTemplate.gameStressTest.implementation.extensions

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.gameTemplate.engine.managers.StateManager
import com.pandulapeter.gameTemplate.engine.managers.ViewportManager

internal expect fun Modifier.handleMouseZoom(
    stateManager: StateManager,
    viewportManager: ViewportManager,
): Modifier

internal fun Modifier.handleDragAndPan(
    stateManager: StateManager,
    viewportManager: ViewportManager,
) = pointerInput("dragAndPan") {
    detectTransformGestures { _, pan, zoom, _ ->
        if (stateManager.isRunning.value) {
            viewportManager.addToCenter(pan)
            viewportManager.multiplyScaleFactor(zoom)
        }
    }
}