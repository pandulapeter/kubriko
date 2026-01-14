/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.keyboardInput.extensions.KeyboardDirectionState
import com.pandulapeter.kubriko.keyboardInput.extensions.KeyboardZoomState
import com.pandulapeter.kubriko.keyboardInput.extensions.directionState
import com.pandulapeter.kubriko.keyboardInput.extensions.zoomState
import com.pandulapeter.kubriko.manager.ViewportManager

private const val CAMERA_SPEED = 15f
private const val CAMERA_SPEED_DIAGONAL = 0.7071f * CAMERA_SPEED

internal fun ViewportManager.handleKeys(keys: Set<Key>) {
    addToCameraPosition(
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
    multiplyScaleFactor(
        when (keys.zoomState) {
            KeyboardZoomState.NONE -> 1f
            KeyboardZoomState.ZOOM_IN -> 1.02f
            KeyboardZoomState.ZOOM_OUT -> 0.98f
        }
    )
}

internal fun handleKeyPressed(
    key: Key,
    onNavigateBackRequested: () -> Unit,
) {
    when (key) {
        Key.Escape, Key.Back -> onNavigateBackRequested()
    }
}