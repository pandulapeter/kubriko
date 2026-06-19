/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.extensions.isCollidingWith
import com.pandulapeter.kubriko.helpers.extensions.angleTowards
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.implementation.SceneEditorInteractionMode
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.snapped
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

private const val MINIMUM_INTERACTIVE_SCALE = 0.05f

private var startOffset: SceneOffset? = null
private var isDragging = false
private var dragStartMouseSceneOffset = SceneOffset.Zero
private var dragStartScale = Scale.Unit
private var dragStartRotation = AngleRadians.Zero
private var dragStartPointerAngle = AngleRadians.Zero

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
                if (mouseSceneOffset.isCollidingWith(selectedActor.body.boundingBoxCollisionMask)) {
                    val body = selectedActor.body
                    startOffset = mouseSceneOffset - body.position
                    dragStartMouseSceneOffset = mouseSceneOffset
                    if (body is BoxBody) {
                        dragStartScale = body.scale
                        dragStartRotation = body.rotation
                        dragStartPointerAngle = body.position.angleTowards(mouseSceneOffset)
                    }
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
    getInteractionMode: () -> SceneEditorInteractionMode,
    isPlacingNewInstance: () -> Boolean,
    getSnapMode: () -> Pair<Int, Int>,
    getMouseSceneOffset: () -> SceneOffset,
    onActorDragStarted: () -> Unit,
    notifySelectedInstanceUpdate: () -> Unit,
): Modifier = onDrag(
    matcher = PointerMatcher.mouse(PointerButton.Tertiary),
) { screenCoordinates ->
    viewportManager.addToCameraPosition(-screenCoordinates)
}.onDrag(
    matcher = PointerMatcher.mouse(PointerButton.Primary),
) { screenCoordinates ->
    val isFirstDragEvent = !isDragging
    isDragging = true
    val isShiftPressed = keyboardInputManager.run { isKeyPressed(Key.ShiftLeft) || isKeyPressed(Key.ShiftRight) }
    val dragStartOffset = startOffset
    if (!isShiftPressed && dragStartOffset != null) {
        getSelectedActor()?.let { selectedActor ->
            if (isFirstDragEvent) {
                onActorDragStarted()
            }
            val mouseSceneOffset = getMouseSceneOffset()
            val body = selectedActor.body
            when (getInteractionMode()) {
                SceneEditorInteractionMode.Translate ->
                    body.position = (mouseSceneOffset - dragStartOffset).snapped(getSnapMode())

                SceneEditorInteractionMode.Scale -> if (body is BoxBody) {
                    val width = body.size.width.raw
                    val height = body.size.height.raw
                    body.scale = Scale(
                        horizontal = if (width > 0f) {
                            (dragStartScale.horizontal + (mouseSceneOffset.x - dragStartMouseSceneOffset.x).raw / width).coerceAtLeast(MINIMUM_INTERACTIVE_SCALE)
                        } else {
                            dragStartScale.horizontal
                        },
                        vertical = if (height > 0f) {
                            (dragStartScale.vertical + (mouseSceneOffset.y - dragStartMouseSceneOffset.y).raw / height).coerceAtLeast(MINIMUM_INTERACTIVE_SCALE)
                        } else {
                            dragStartScale.vertical
                        },
                    )
                }

                SceneEditorInteractionMode.Rotate -> if (body is BoxBody) {
                    body.rotation = dragStartRotation + (body.position.angleTowards(mouseSceneOffset) - dragStartPointerAngle)
                }
            }
            notifySelectedInstanceUpdate()
        }
    } else if (isShiftPressed || !isPlacingNewInstance()) {
        viewportManager.addToCameraPosition(-screenCoordinates)
    }
}