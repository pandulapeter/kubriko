/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.extensions.clampWithin
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.toSceneOffset
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInput.extensions.hasLeft
import com.pandulapeter.kubriko.keyboardInput.extensions.hasRight
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.collections.immutable.ImmutableSet

internal class Paddle(
    initialPosition: SceneOffset = SceneOffset(0.sceneUnit, 550.sceneUnit),
) : Visible, Collidable, PointerInputAware, KeyboardInputAware, Dynamic {

    override val body = RectangleBody(
        initialPosition = initialPosition,
        initialSize = SceneSize(Width, Height),
    )
    private lateinit var pointerInputManager: PointerInputManager
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager
    private var previousPointerOffset: SceneOffset? = null
    private var movementPointerId: PointerId? = null

    // On desktop after each detected movement we programmatically move the cursor to the center of the screen.
    // This next flag is there to make sure that that movement is filtered out.
    private var shouldMovePaddle = false

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
            shouldMovePaddle = !shouldMovePaddle
            if (shouldMovePaddle) {
                previousPointerOffset?.let { previousPointerPosition ->
                    val offset = currentPointerPosition - previousPointerPosition
                    body.position = SceneOffset(
                        x = body.position.x + offset.x * POINTER_SPEED_MULTIPLIER,
                        y = body.position.y,
                    ).clampWithin(
                        topLeft = viewportManager.topLeft.value,
                        bottomRight = viewportManager.bottomRight.value,
                    )
                }
                if (!pointerInputManager.tryToMoveHoveringPointer(viewportCenter)) {
                    // If we didn't manage to move the pointer, let's switch the flag back on, to make sure that the next event is consumed.
                    // If we managed to move the cursor, the next event should be skipped!
                    shouldMovePaddle = !shouldMovePaddle
                }
            }
            previousPointerOffset = currentPointerPosition
        }
    }

    override fun DrawScope.draw() {
        drawRect(
            color = Color.White,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(width = 3f),
        )
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

    private var moveInNextStep = SceneUnit.Zero

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (stateManager.isRunning.value) {
            val hasLeft = activeKeys.hasLeft
            val hasRight = activeKeys.hasRight
            if (hasLeft xor hasRight) {
                if (hasLeft) {
                    moveInNextStep = -Speed
                }
                if (hasRight) {
                    moveInNextStep = Speed
                }
            }
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (moveInNextStep != SceneUnit.Zero) {
            body.position = SceneOffset(
                x = body.position.x + moveInNextStep * deltaTimeInMilliseconds,
                y = body.position.y,
            ).clampWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value)
            moveInNextStep = SceneUnit.Zero
        }
    }

    companion object {
        const val POINTER_SPEED_MULTIPLIER = 1.5f
        val Width = 200.sceneUnit
        val Height = 40.sceneUnit
        val Speed = 2.sceneUnit
    }
}
