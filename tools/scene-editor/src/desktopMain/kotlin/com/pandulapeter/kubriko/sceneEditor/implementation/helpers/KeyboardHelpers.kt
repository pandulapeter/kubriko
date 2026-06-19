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
import com.pandulapeter.kubriko.keyboardInput.extensions.KeyboardZoomState
import com.pandulapeter.kubriko.keyboardInput.extensions.zoomState
import com.pandulapeter.kubriko.manager.ViewportManager

private const val CAMERA_SPEED = 15f
private const val CAMERA_SPEED_DIAGONAL = 0.7071f * CAMERA_SPEED

/**
 * Pans and zooms the camera from the held keys. Panning is intentionally bound to the arrow keys only
 * (not the plugin's `directionState`, which also accepts WASD) so the W/A/S/D letters stay free for the
 * editor's own shortcuts.
 */
internal fun ViewportManager.handleKeys(keys: Set<Key>) {
    val horizontal = when {
        keys.contains(Key.DirectionLeft) && !keys.contains(Key.DirectionRight) -> -1
        keys.contains(Key.DirectionRight) && !keys.contains(Key.DirectionLeft) -> 1
        else -> 0
    }
    val vertical = when {
        keys.contains(Key.DirectionUp) && !keys.contains(Key.DirectionDown) -> -1
        keys.contains(Key.DirectionDown) && !keys.contains(Key.DirectionUp) -> 1
        else -> 0
    }
    if (horizontal != 0 || vertical != 0) {
        val speed = if (horizontal != 0 && vertical != 0) CAMERA_SPEED_DIAGONAL else CAMERA_SPEED
        addToCameraPosition(Offset(horizontal * speed, vertical * speed))
    }
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