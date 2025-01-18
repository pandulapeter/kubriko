/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.extensions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.pandulapeter.kubriko.collision.extensions.isWithin
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.types.SceneOffset

private var startOffset: SceneOffset? = null
private var isDragging = false

// TODO: Fix some clicks registering as drag
@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.handleMouseClick(
    getSelectedActor: () -> Editable<*>?,
    getMouseSceneOffset: () -> SceneOffset,
    onLeftClick: (Offset) -> Unit,
    onRightClick: (Offset) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Press) { event ->
    when (event.button) {
        PointerButton.Primary -> getSelectedActor()?.let { selectedActor ->
            getMouseSceneOffset().let { mouseSceneOffset ->
                if (mouseSceneOffset.isWithin(selectedActor.body)) {
                    startOffset = mouseSceneOffset - selectedActor.body.position
                }
            }
        }
    }
}.onPointerEvent(PointerEventType.Release) { event ->
    when (event.button) {
        PointerButton.Primary -> {
            startOffset = null
            if (!isDragging) {
                event.changes.first().position.let(onLeftClick)
            }
            isDragging = false
        }

        PointerButton.Secondary -> {
            if (!isDragging) {
                event.changes.first().position.let(onRightClick)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.handleMouseMove(
    onMouseMove: (Offset) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Move) {
    onMouseMove(it.changes.first().position)
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.handleMouseZoom(
    viewportManager: ViewportManager,
): Modifier = onPointerEvent(PointerEventType.Scroll) {
    viewportManager.multiplyScaleFactor(
        scaleFactor = 1f - it.changes.first().scrollDelta.y * 0.05f
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.handleMouseDrag(
    keyboardInputManager: KeyboardInputManager,
    viewportManager: ViewportManager,
    getSelectedActor: () -> Editable<*>?,
    getMouseSceneOffset: () -> SceneOffset,
    notifySelectedInstanceUpdate: () -> Unit,
): Modifier = onDrag(
    matcher = PointerMatcher.mouse(PointerButton.Tertiary),
) { screenCoordinates ->
    viewportManager.addToCameraPosition(screenCoordinates)
}.onDrag(
    matcher = PointerMatcher.mouse(PointerButton.Primary),
) { screenCoordinates ->
    isDragging = true
    if (keyboardInputManager.run { isKeyPressed(Key.ShiftLeft) || isKeyPressed(Key.ShiftRight) }) {
        viewportManager.addToCameraPosition(screenCoordinates)
    } else {
        startOffset?.let { startOffset ->
            getSelectedActor()?.let { selectedActor ->
                selectedActor.body.position = getMouseSceneOffset() - startOffset
                notifySelectedInstanceUpdate()
            }
        }
    }
}