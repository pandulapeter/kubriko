/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testInput.implementation.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import com.pandulapeter.kubriko.gamepadInput.GamepadInputAware
import com.pandulapeter.kubriko.gamepadInput.GamepadState
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.testInput.implementation.GamepadSnapshot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

internal class InputTestManager : Manager(), KeyboardInputAware, PointerInputAware, GamepadInputAware, Overlay, Unique {
    private val _activeKeys = MutableStateFlow(emptySet<Key>())
    val activeKeys = _activeKeys.asStateFlow()
    private val _gamepads = MutableStateFlow<ImmutableList<GamepadSnapshot>>(persistentListOf())
    val gamepads = _gamepads.asStateFlow()
    private val collectedGamepads = mutableListOf<GamepadSnapshot>()
    private val pointerInputManager by manager<PointerInputManager>()

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(this)
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = _activeKeys.update { activeKeys }

    override fun handleGamepadState(gamepad: GamepadState) {
        collectedGamepads.add(gamepad.toSnapshot())
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (collectedGamepads != _gamepads.value) {
            _gamepads.value = collectedGamepads.toImmutableList()
        }
        collectedGamepads.clear()
    }

    private fun GamepadState.toSnapshot() = GamepadSnapshot(
        index = index,
        name = name,
        leftStickX = leftStickX,
        leftStickY = leftStickY,
        rightStickX = rightStickX,
        rightStickY = rightStickY,
        leftTrigger = leftTrigger,
        rightTrigger = rightTrigger,
        pressedButtons = GamepadButton.entries.filter { isPressed(it) }.toImmutableList(),
    )

    private fun PointerId?.toColor() = this?.value?.let { value ->
        Color.hsv(abs((value * 47) % 360).toFloat(), 0.8f, 0.8f)
    } ?: Color.White

    override fun DrawScope.drawToViewport() {
        pointerInputManager.hoveringPointerPosition.value?.let { pointerOffset ->
            drawCrosshair(pointerOffset, Color.White, 20f)
        }
        pointerInputManager.pressedPointerPositions.value.forEach { (pointerId, pointerOffset) ->
            drawCrosshair(pointerOffset, pointerId.toColor(), 40f)
        }
    }

    private fun DrawScope.drawCrosshair(position: Offset, color: Color, radius: Float) {
        drawLine(
            color = Color.Black,
            start = Offset(position.x, 0f),
            end = Offset(position.x, size.height),
            strokeWidth = 4f,
        )
        drawLine(
            color = color,
            start = Offset(position.x, 0f),
            end = Offset(position.x, size.height),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(0f, position.y),
            end = Offset(size.width, position.y),
            strokeWidth = 4f,
        )
        drawLine(
            color = color,
            start = Offset(0f, position.y),
            end = Offset(size.width, position.y),
            strokeWidth = 2f,
        )
        drawCircle(
            color = color,
            radius = radius,
            center = position,
        )
        drawCircle(
            color = Color.Black,
            radius = radius,
            center = position,
            style = Stroke(),
        )
    }
}