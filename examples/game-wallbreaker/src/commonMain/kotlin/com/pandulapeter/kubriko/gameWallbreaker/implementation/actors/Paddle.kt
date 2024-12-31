package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
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
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager
    private var previousPointerPosition: SceneOffset? = null

    override fun onAdded(kubriko: Kubriko) {
        stateManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun DrawScope.draw() {
        drawRect(
            color = Color.White,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
    }

    override fun onPointerOffsetChanged(screenOffset: Offset) {
        val currentPointerPosition = screenOffset.toSceneOffset(viewportManager)
        if (stateManager.isRunning.value) {
            previousPointerPosition?.let { previousPointerPosition ->
                body.position = SceneOffset(
                    x = body.position.x + currentPointerPosition.x - previousPointerPosition.x,
                    y = body.position.y,
                ).clampWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value)
            }
        }
        previousPointerPosition = currentPointerPosition
    }

    override fun onPointerReleased(screenOffset: Offset) {
        previousPointerPosition = null
    }

    override fun onPointerExit() {
        previousPointerPosition = null
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

    override fun update(deltaTimeInMilliseconds: Float) {
        if (moveInNextStep != SceneUnit.Zero) {
            body.position = SceneOffset(
                x = body.position.x + moveInNextStep * deltaTimeInMilliseconds,
                y = body.position.y,
            ).clampWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value)
            moveInNextStep = SceneUnit.Zero
        }
    }

    companion object {
        val Width = 200.sceneUnit
        val Height = 40.sceneUnit
        val Speed = 2.sceneUnit
    }
}
