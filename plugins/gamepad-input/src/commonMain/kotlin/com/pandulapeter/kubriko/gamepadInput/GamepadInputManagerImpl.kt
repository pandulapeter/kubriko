/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gamepadInput

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager.Companion.MAX_GAMEPAD_COUNT
import com.pandulapeter.kubriko.gamepadInput.implementation.GamepadEventHandler
import com.pandulapeter.kubriko.gamepadInput.implementation.GamepadFocusNavigator
import com.pandulapeter.kubriko.gamepadInput.implementation.RawGamepadState
import com.pandulapeter.kubriko.gamepadInput.implementation.createGamepadEventHandler
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlin.math.hypot

internal class GamepadInputManagerImpl(
    private val deadZone: Float,
    private val triggerThreshold: Float,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
    private val initialGamepadEventHandler: GamepadEventHandler? = null,
) : GamepadInputManager(isLoggingEnabled, instanceNameForLogging) {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    override val gamepads = List(MAX_GAMEPAD_COUNT) { GamepadState(index = it) }
    private val rawGamepads = Array(MAX_GAMEPAD_COUNT) { RawGamepadState() }
    private val _connectedGamepadCount = MutableStateFlow(0)
    override val connectedGamepadCount = _connectedGamepadCount.asStateFlow()
    private var gamepadEventHandler: GamepadEventHandler? = null
    private val gamepadInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<GamepadInputAware>() }.asStateFlowOnMainThread(emptyList())
    }
    private var wasFocused = true

    /**
     * Read from the composition, so that turning it on and off starts and stops the frame loop below rather
     * than leaving one running to do nothing on every frame of a game that never navigates anything.
     */
    override var isFocusNavigationEnabled by mutableStateOf(false)

    /**
     * Whether the pads asked anything of the focus when they were last polled - what wakes the frame loop below, which
     * otherwise sleeps: the state it reads only changes when onUpdate() polls, so a frame with nothing held would
     * only ask for a display frame, and a redraw of the whole window on most platforms, to learn nothing.
     */
    private val hasFocusNavigationInput = MutableStateFlow(false)
    private val focusNavigator = GamepadFocusNavigator(gamepads, this)
    /** Scratch storage for applyDeadZone(), which has to return two values without allocating. */
    private var deadZonedX = 0f
    private var deadZonedY = 0f

    override fun onInitialize(kubriko: Kubriko) {
        if (initialGamepadEventHandler != null) {
            gamepadEventHandler = initialGamepadEventHandler.also { it.startListening(rawGamepads) }
        }
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (gamepadEventHandler?.isValid() == false) {
            stopListening(shouldNotifyActors = true)
        }
        if (gamepadEventHandler == null && isInitialized.value) {
            gamepadEventHandler = createGamepadEventHandler().also { it.startListening(rawGamepads) }
        }
        GamepadFocusNavigationHost(this)
        if (isFocusNavigationEnabled) {
            FocusNavigationEffect()
        }
    }

    /**
     * Walks Compose's own focus with the gamepad, for as long as [isFocusNavigationEnabled] is on.
     *
     * Driven by the frames of the composition rather than by [onUpdate], so that the focus is moved from the thread
     * the composition runs on, whichever one the game's ticks run on. Frames are only asked for while the pads are
     * asking something of the focus, though, and for the one frame after that sees them let go: the state read here
     * only changes when [onUpdate] polls it, so the loop sleeps until a poll finds something held - and while the
     * window is unfocused, whose pads nobody polls. Which focus system it walks is read per frame rather than
     * captured here, so that a popup opening and closing moves the sticks between them (see
     * [GamepadFocusNavigationHost]).
     */
    @Composable
    private fun FocusNavigationEffect() {
        LaunchedEffect(Unit) {
            focusNavigator.restFocusNavigation()
            while (isActive) {
                if (_connectedGamepadCount.value == 0) {
                    // Nothing to navigate with, so suspend rather than wake at every frame to read four
                    // disconnected pads. onUpdate() keeps polling for controllers and publishes the count
                    // that resumes this loop; the reset makes the first frame after it a zero delta.
                    _connectedGamepadCount.first { it > 0 }
                    focusNavigator.previousFocusFrameTimeNanos = 0L
                }
                if (!stateManager.isFocused.value) {
                    // What the pads last reported is stale until the window is polled again, so it is taken as
                    // their resting state once the focus is back, the way it is when navigation is turned on.
                    stateManager.isFocused.first { it }
                    focusNavigator.restFocusNavigation()
                    continue
                }
                if (!focusNavigator.hadFocusNavigationInput && !hasFocusNavigationInput.value) {
                    hasFocusNavigationInput.first { it }
                    focusNavigator.previousFocusFrameTimeNanos = 0L
                }
                withFrameNanos(focusNavigator.onFocusNavigationFrame)
            }
        }
    }

    override fun isButtonPressed(gamepadIndex: Int, button: GamepadButton) =
        gamepadIndex >= 0 && gamepadIndex < MAX_GAMEPAD_COUNT && gamepads[gamepadIndex].isPressed(button)

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        val isFocused = stateManager.isFocused.value
        if (!isFocused) {
            if (wasFocused) {
                wasFocused = false
                releaseAllInputs()
            }
            hasFocusNavigationInput.value = false
            return
        }
        wasFocused = true
        val gamepadEventHandler = gamepadEventHandler ?: return
        gamepadEventHandler.poll()
        var connectedCount = 0
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            if (rawGamepads[index].isConnected) {
                connectedCount++
            }
            updateGamepad(gamepads[index], rawGamepads[index])
        }
        _connectedGamepadCount.value = connectedCount
        hasFocusNavigationInput.value = isFocusNavigationEnabled && connectedCount > 0 && (focusNavigator.isAnyFocusDirectionHeld() ||
                focusNavigator.isAnyGamepadPressing(GamepadButton.SOUTH) || focusNavigator.isAnyGamepadPressing(GamepadButton.EAST))
    }

    private fun updateGamepad(gamepad: GamepadState, rawGamepad: RawGamepadState) {
        val wasConnected = gamepad.isConnected
        if (!rawGamepad.isConnected) {
            if (wasConnected) {
                releaseGamepad(gamepad)
                notifyDisconnected(gamepad)
            }
            return
        }
        gamepad.isConnected = true
        gamepad.name = rawGamepad.name
        applyDeadZone(rawGamepad.leftStickX, rawGamepad.leftStickY)
        gamepad.leftStickX = deadZonedX
        gamepad.leftStickY = deadZonedY
        applyDeadZone(rawGamepad.rightStickX, rawGamepad.rightStickY)
        gamepad.rightStickX = deadZonedX
        gamepad.rightStickY = deadZonedY
        gamepad.leftTrigger = rawGamepad.leftTrigger.coerceIn(0f, 1f)
        gamepad.rightTrigger = rawGamepad.rightTrigger.coerceIn(0f, 1f)
        // The triggers are the only inputs that every platform reports differently: some as an axis, some as a
        // button, some as both. Deriving the digital value here instead of in the platform code keeps them
        // consistent, and costs nothing for platforms that only ever report 0 or 1.
        var pressedButtons = rawGamepad.pressedButtons
        pressedButtons = pressedButtons.withButton(GamepadButton.LEFT_TRIGGER, gamepad.leftTrigger > triggerThreshold)
        pressedButtons = pressedButtons.withButton(GamepadButton.RIGHT_TRIGGER, gamepad.rightTrigger > triggerThreshold)
        val previousButtons = gamepad.pressedButtons
        gamepad.pressedButtons = pressedButtons
        if (!wasConnected) {
            notifyConnected(gamepad)
        }
        if (pressedButtons != previousButtons) {
            notifyButtonChanges(gamepad, previousButtons, pressedButtons)
        }
        val actors = gamepadInputAwareActors.value
        for (index in actors.indices) {
            actors[index].handleGamepadState(gamepad)
        }
    }

    /**
     * Rescales the stick position so that the edge of the dead zone maps to zero and the edge of the physical
     * range still maps to one. Radial rather than per-axis, so that a stick held diagonally isn't cut into a
     * square shape. Writes its result into [deadZonedX] and [deadZonedY].
     */
    private fun applyDeadZone(x: Float, y: Float) {
        val magnitude = hypot(x, y)
        if (magnitude <= deadZone) {
            deadZonedX = 0f
            deadZonedY = 0f
            return
        }
        val scale = ((magnitude - deadZone) / (1f - deadZone)).coerceAtMost(1f) / magnitude
        deadZonedX = x * scale
        deadZonedY = y * scale
    }

    private fun Int.withButton(button: GamepadButton, isPressed: Boolean) =
        if (isPressed) this or button.bitMask else this and button.bitMask.inv()

    private fun notifyButtonChanges(gamepad: GamepadState, previousButtons: Int, pressedButtons: Int) {
        val buttons = GamepadButton.entries
        val actors = gamepadInputAwareActors.value
        for (buttonIndex in buttons.indices) {
            val button = buttons[buttonIndex]
            val wasPressed = previousButtons and button.bitMask != 0
            val isPressed = pressedButtons and button.bitMask != 0
            if (wasPressed == isPressed) {
                continue
            }
            for (actorIndex in actors.indices) {
                if (isPressed) {
                    actors[actorIndex].onGamepadButtonPressed(gamepad, button)
                } else {
                    actors[actorIndex].onGamepadButtonReleased(gamepad, button)
                }
            }
        }
    }

    private fun notifyConnected(gamepad: GamepadState) {
        log(
            message = "Gamepad connected in slot ${gamepad.index}.",
            details = gamepad.name,
        )
        val actors = gamepadInputAwareActors.value
        for (index in actors.indices) {
            actors[index].onGamepadConnected(gamepad)
        }
    }

    private fun notifyDisconnected(gamepad: GamepadState) {
        log(message = "Gamepad disconnected from slot ${gamepad.index}.")
        val actors = gamepadInputAwareActors.value
        for (index in actors.indices) {
            actors[index].onGamepadDisconnected(gamepad)
        }
    }

    /**
     * Zeroes the inputs of every gamepad and reports every held button as released, while keeping the gamepads
     * connected: losing the focus doesn't unplug anything, so regaining it mustn't announce the same pads again.
     */
    private fun releaseAllInputs() {
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            val gamepad = gamepads[index]
            val previousButtons = gamepad.pressedButtons
            gamepad.releaseInputs()
            if (previousButtons != 0) {
                notifyButtonChanges(gamepad, previousButtons, 0)
            }
        }
    }

    /**
     * Zeroes the state of a gamepad and reports every button that was held down as released, so that losing
     * a controller can't leave an Actor acting on an input that is no longer there.
     */
    private fun releaseGamepad(gamepad: GamepadState) {
        val previousButtons = gamepad.pressedButtons
        gamepad.reset()
        if (previousButtons != 0) {
            notifyButtonChanges(gamepad, previousButtons, 0)
        }
    }

    /**
     * Drops the platform handler and resets every slot. With [shouldNotifyActors], every connected gamepad is also
     * reported disconnected, so that the connection the next handler reports is paired with it.
     */
    private fun stopListening(shouldNotifyActors: Boolean) {
        gamepadEventHandler?.stopListening()
        gamepadEventHandler = null
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            val gamepad = gamepads[index]
            val wasConnected = gamepad.isConnected
            releaseGamepad(gamepad)
            if (shouldNotifyActors && wasConnected) {
                notifyDisconnected(gamepad)
            }
            rawGamepads[index].reset()
        }
        _connectedGamepadCount.value = 0
        hasFocusNavigationInput.value = false
    }

    override fun onDispose() = stopListening(shouldNotifyActors = false)
}
