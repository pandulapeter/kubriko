package com.pandulapeter.gameTemplate.gameplay.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.gameplay.implementation.extensions.KeyboardDirectionState
import com.pandulapeter.gameTemplate.gameplay.implementation.extensions.KeyboardZoomState
import com.pandulapeter.gameTemplate.gameplay.implementation.extensions.directionState
import com.pandulapeter.gameTemplate.gameplay.implementation.extensions.zoomState
import kotlin.math.PI
import kotlin.math.sin

private const val CAMERA_SPEED = 15f
private val CAMERA_SPEED_DIAGONAL = (sin(PI / 4) * CAMERA_SPEED).toFloat()

internal fun handleKeys(keys: Set<Key>) {
    Engine.get().viewportManager.addToOffset(
        when (keys.directionState) {
            KeyboardDirectionState.NONE -> Offset.Zero
            KeyboardDirectionState.LEFT -> Offset(-CAMERA_SPEED, 0f)
            KeyboardDirectionState.UP_LEFT -> Offset(-CAMERA_SPEED_DIAGONAL, -CAMERA_SPEED_DIAGONAL)
            KeyboardDirectionState.UP -> Offset(0f, -CAMERA_SPEED)
            KeyboardDirectionState.UP_RIGHT -> Offset(CAMERA_SPEED_DIAGONAL, -CAMERA_SPEED_DIAGONAL)
            KeyboardDirectionState.RIGHT -> Offset(CAMERA_SPEED, 0f)
            KeyboardDirectionState.DOWN_RIGHT -> Offset(CAMERA_SPEED_DIAGONAL, CAMERA_SPEED_DIAGONAL)
            KeyboardDirectionState.DOWN -> Offset(0f, CAMERA_SPEED)
            KeyboardDirectionState.DOWN_LEFT -> Offset(-CAMERA_SPEED_DIAGONAL, CAMERA_SPEED_DIAGONAL)
        }
    )
    Engine.get().viewportManager.multiplyScaleFactor(
        when (keys.zoomState) {
            KeyboardZoomState.NONE -> 1f
            KeyboardZoomState.ZOOM_IN -> 1.02f
            KeyboardZoomState.ZOOM_OUT -> 0.98f
        }
    )
}

internal fun handleKeyReleased(
    key: Key,
    onExitRequested: () -> Unit,
) {
    when (key) {
        Key.Escape, Key.Back, Key.Backspace -> onExitRequested()
    }
}