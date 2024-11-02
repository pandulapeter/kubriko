package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.implementation.extensions.directionState
import com.pandulapeter.kubriko.implementation.extensions.zoomState
import com.pandulapeter.kubriko.managers.ViewportManager

private const val CAMERA_SPEED = 15f
private const val CAMERA_SPEED_DIAGONAL = 0.7071f * CAMERA_SPEED

internal fun ViewportManager.handleKeys(keys: Set<Key>) {
    addToCenter(
        -when (keys.directionState) {
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.NONE -> Offset.Zero
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.LEFT -> Offset(-CAMERA_SPEED, 0f)
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.UP_LEFT -> Offset(-CAMERA_SPEED_DIAGONAL, -CAMERA_SPEED_DIAGONAL)
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.UP -> Offset(0f, -CAMERA_SPEED)
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.UP_RIGHT -> Offset(CAMERA_SPEED_DIAGONAL, -CAMERA_SPEED_DIAGONAL)
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.RIGHT -> Offset(CAMERA_SPEED, 0f)
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.DOWN_RIGHT -> Offset(CAMERA_SPEED_DIAGONAL, CAMERA_SPEED_DIAGONAL)
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.DOWN -> Offset(0f, CAMERA_SPEED)
            com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState.DOWN_LEFT -> Offset(-CAMERA_SPEED_DIAGONAL, CAMERA_SPEED_DIAGONAL)
        }
    )
    multiplyScaleFactor(
        when (keys.zoomState) {
            com.pandulapeter.kubriko.implementation.extensions.KeyboardZoomState.NONE -> 1f
            com.pandulapeter.kubriko.implementation.extensions.KeyboardZoomState.ZOOM_IN -> 1.02f
            com.pandulapeter.kubriko.implementation.extensions.KeyboardZoomState.ZOOM_OUT -> 0.98f
        }
    )
}

internal fun handleKeyReleased(
    key: Key,
    onNavigateBackRequested: () -> Unit,
) {
    when (key) {
        Key.Escape, Key.Back -> onNavigateBackRequested()
    }
}