package com.pandulapeter.kubrikoStressTest.implementation.extensions

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager

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
            viewportManager.addToCameraPosition(pan)
            viewportManager.multiplyScaleFactor(zoom)
        }
    }
}