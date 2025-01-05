package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.input.key.Key
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
    private var previousPointerPosition: SceneOffset? = null

    override fun onAdded(kubriko: Kubriko) {
        pointerInputManager = kubriko.get()
        stateManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    private var previousPointerOffset = SceneOffset.Zero

    override fun onPointerOffsetChanged(screenOffset: Offset) {
        val currentPointerPosition = screenOffset.toSceneOffset(viewportManager)
        if (stateManager.isRunning.value) {
            previousPointerPosition?.let { previousPointerPosition ->
                val offset = currentPointerPosition - previousPointerPosition
                if (offset.x != -previousPointerOffset.x) {
                    previousPointerOffset = offset
                    body.position = (body.position + offset).clampWithin(
                        topLeft = viewportManager.topLeft.value,
                        bottomRight = viewportManager.bottomRight.value,
                    )
                    pointerInputManager.movePointer(viewportManager.size.value.center)
                }
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

    private var moveInNextStep = SceneOffset.Zero

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (stateManager.isRunning.value) {
            moveInNextStep = when (activeKeys.directionState) {
                KeyboardDirectionState.NONE -> SceneOffset.Zero
                KeyboardDirectionState.LEFT -> SceneOffset(-Speed, SceneUnit.Zero)
                KeyboardDirectionState.UP_LEFT -> SceneOffset(-Speed, -Speed)
                KeyboardDirectionState.UP -> SceneOffset(SceneUnit.Zero, -Speed)
                KeyboardDirectionState.UP_RIGHT -> SceneOffset(Speed, -Speed)
                KeyboardDirectionState.RIGHT -> SceneOffset(Speed, SceneUnit.Zero)
                KeyboardDirectionState.DOWN_RIGHT -> SceneOffset(Speed, Speed)
                KeyboardDirectionState.DOWN -> SceneOffset(SceneUnit.Zero, Speed)
                KeyboardDirectionState.DOWN_LEFT -> SceneOffset(-Speed, Speed)
            }.normalize() * Speed
        }
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        if (moveInNextStep != SceneOffset.Zero) {
            body.position = SceneOffset(
                x = body.position.x + moveInNextStep.x * deltaTimeInMilliseconds,
                y = body.position.y + moveInNextStep.y * deltaTimeInMilliseconds,
            ).clampWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value)
            moveInNextStep = SceneOffset.Zero
        }
    }

    companion object {
        val Speed = 2.sceneUnit
    }
}