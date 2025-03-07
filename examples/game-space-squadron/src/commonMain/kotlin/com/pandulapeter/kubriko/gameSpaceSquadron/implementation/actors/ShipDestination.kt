/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.extensions.clampWithin
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.normalize
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.toSceneOffset
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInput.extensions.KeyboardDirectionState
import com.pandulapeter.kubriko.keyboardInput.extensions.directionState
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.collections.immutable.ImmutableSet

internal class ShipDestination : Positionable, PointerInputAware, KeyboardInputAware, Dynamic {

    override val body = PointBody()
    private lateinit var pointerInputManager: PointerInputManager
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager
    private var previousPointerOffset: SceneOffset? = null
    private var movementPointerId: PointerId? = null

    // On desktop after each detected movement we programmatically move the cursor to the center of the screen.
    // This next flag is there to make sure that that movement is filtered out.
    private var shouldMoveShip = true

    override fun onAdded(kubriko: Kubriko) {
        pointerInputManager = kubriko.get()
        stateManager = kubriko.get()
        viewportManager = kubriko.get()
    }


    fun resetPointerTracking() {
        viewportManager.size.value.center.let { center ->
            pointerInputManager.tryToMoveHoveringPointer(center)
            previousPointerOffset = null
        }
    }


    override fun onPointerOffsetChanged(pointerId: PointerId?, screenOffset: Offset) {
        if (movementPointerId == null) {
            movementPointerId = pointerId
        }
        val viewportCenter = viewportManager.size.value.center
        if (stateManager.isRunning.value && pointerId == movementPointerId) {
            val currentPointerPosition = screenOffset.toSceneOffset(viewportManager)
            shouldMoveShip = !shouldMoveShip
            if (shouldMoveShip) {
                previousPointerOffset?.let { previousPointerPosition ->
                    val offset = currentPointerPosition - previousPointerPosition
                    body.position = (body.position + offset * POINTER_SENSITIVITY).clampWithin(
                        topLeft = viewportManager.topLeft.value,
                        bottomRight = viewportManager.bottomRight.value,
                    )
                }
                if (!pointerInputManager.tryToMoveHoveringPointer(viewportCenter)) {
                    // If we didn't manage to move the pointer, let's switch the flag back on, to make sure that the next event is consumed.
                    // If we managed to move the cursor, the next event should be skipped!
                    shouldMoveShip = !shouldMoveShip
                }
            }
            previousPointerOffset = currentPointerPosition
        }
    }

    override fun onPointerPressed(pointerId: PointerId, screenOffset: Offset) {
        if (movementPointerId == null) {
            movementPointerId = pointerId
        }
    }

    override fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) {
        if (movementPointerId == pointerId) {
            movementPointerId = null
            previousPointerOffset = null
        }
    }

    override fun onPointerLeavingTheViewport() {
        previousPointerOffset = null
    }

    private var moveInNextStep = SceneOffset.Zero

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (stateManager.isRunning.value) {
            moveInNextStep = when (activeKeys.directionState) {
                KeyboardDirectionState.NONE -> SceneOffset.Zero
                KeyboardDirectionState.LEFT -> SceneOffset(-KeyboardMovementSpeed, SceneUnit.Zero)
                KeyboardDirectionState.UP_LEFT -> SceneOffset(-KeyboardMovementSpeed, -KeyboardMovementSpeed)
                KeyboardDirectionState.UP -> SceneOffset(SceneUnit.Zero, -KeyboardMovementSpeed)
                KeyboardDirectionState.UP_RIGHT -> SceneOffset(KeyboardMovementSpeed, -KeyboardMovementSpeed)
                KeyboardDirectionState.RIGHT -> SceneOffset(KeyboardMovementSpeed, SceneUnit.Zero)
                KeyboardDirectionState.DOWN_RIGHT -> SceneOffset(KeyboardMovementSpeed, KeyboardMovementSpeed)
                KeyboardDirectionState.DOWN -> SceneOffset(SceneUnit.Zero, KeyboardMovementSpeed)
                KeyboardDirectionState.DOWN_LEFT -> SceneOffset(-KeyboardMovementSpeed, KeyboardMovementSpeed)
            }.normalize() * KeyboardMovementSpeed
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (moveInNextStep != SceneOffset.Zero) {
            body.position = SceneOffset(
                x = body.position.x + moveInNextStep.x * deltaTimeInMilliseconds,
                y = body.position.y + moveInNextStep.y * deltaTimeInMilliseconds,
            ).clampWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value)
            moveInNextStep = SceneOffset.Zero
        }
    }

    companion object {
        const val POINTER_SENSITIVITY = 2f
        val KeyboardMovementSpeed = 2.sceneUnit
    }
}