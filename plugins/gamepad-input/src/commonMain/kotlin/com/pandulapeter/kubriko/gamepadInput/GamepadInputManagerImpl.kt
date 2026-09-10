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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager.Companion.MAX_GAMEPAD_COUNT
import com.pandulapeter.kubriko.gamepadInput.implementation.GamepadEventHandler
import com.pandulapeter.kubriko.gamepadInput.implementation.RawGamepadState
import com.pandulapeter.kubriko.gamepadInput.implementation.createGamepadEventHandler
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.hypot

internal class GamepadInputManagerImpl(
    private val deadZone: Float,
    private val triggerThreshold: Float,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
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

    // Read from the composition, so that turning it on and off starts and stops the frame loop below rather
    // than leaving one running to do nothing on every frame of a game that never navigates anything.
    override var isFocusNavigationEnabled by mutableStateOf(false)

    // The direction the focus is currently being walked in, and how long it has been held that way. Together
    // they are what turns a stick that is simply pushed and left there into the repeat a held arrow key has.
    private var focusDirection: FocusDirection? = null
    private var timeUntilNextFocusStepInMilliseconds = 0f
    private var wasActivationButtonPressed = false
    private var wasBackButtonPressed = false

    // Scratch storage for applyDeadZone(), which has to return two values without allocating.
    private var deadZonedX = 0f
    private var deadZonedY = 0f

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (gamepadEventHandler?.isValid() == false) {
            stopListening()
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
     * Driven by the frames of the composition rather than by [onUpdate], so that the menus a game shows while it
     * is paused - the very surfaces this exists for - are navigable while its loop is stopped. Which focus
     * system it walks is read per frame rather than captured here, so that a popup opening and closing moves the
     * sticks between them (see [GamepadFocusNavigationHost]).
     */
    @Composable
    private fun FocusNavigationEffect() {
        LaunchedEffect(Unit) {
            // Whatever the sticks and the buttons are already doing at the moment navigation is turned on is
            // taken as their resting state, so a menu that opens under a held control doesn't immediately act on
            // it: only what the player does next counts.
            wasActivationButtonPressed = isAnyGamepadPressing(GamepadButton.SOUTH)
            wasBackButtonPressed = isAnyGamepadPressing(GamepadButton.EAST)
            focusDirection = readFocusDirection()
            timeUntilNextFocusStepInMilliseconds = FOCUS_REPEAT_DELAY
            var previousFrameTimeNanos = 0L
            while (isActive) {
                if (_connectedGamepadCount.value == 0) {
                    // Nothing to navigate with, so suspend rather than wake at every frame to read four
                    // disconnected pads. onUpdate() keeps polling for controllers and publishes the count
                    // that resumes this loop; the reset makes the first frame after it a zero delta.
                    _connectedGamepadCount.first { it > 0 }
                    previousFrameTimeNanos = 0L
                }
                withFrameNanos { frameTimeNanos ->
                    val deltaTimeInMilliseconds = if (previousFrameTimeNanos == 0L) {
                        0f
                    } else {
                        ((frameTimeNanos - previousFrameTimeNanos) / NANOSECONDS_PER_MILLISECOND).coerceAtMost(MAXIMUM_FRAME_TIME)
                    }
                    previousFrameTimeNanos = frameTimeNanos
                    updateFocusNavigation(deltaTimeInMilliseconds)
                }
            }
        }
    }

    private fun updateFocusNavigation(deltaTimeInMilliseconds: Float) {
        val host = focusNavigationHosts.lastOrNull() ?: return
        val focusManager = host.focusManager
        val isActivationButtonPressed = isAnyGamepadPressing(GamepadButton.SOUTH)
        val isBackButtonPressed = isAnyGamepadPressing(GamepadButton.EAST)
        val direction = readFocusDirection()
        if (isActivationButtonPressed || isBackButtonPressed || direction != null) {
            // Compose decides whether a control can hold the focus at all, and whether holding it is worth
            // drawing, from the last kind of input the window saw: a tap or a mouse click puts it into touch
            // mode, where a control that is only clickable stops being a focus target and stops showing that it
            // is one. A gamepad is a directional input like the arrow keys, so it makes the same claim they do -
            // without this, a player who touched the screen once would be left steering a focus nothing draws.
            host.inputModeManager.requestInputMode(InputMode.Keyboard)
        }
        val wasActivationPressed = wasActivationButtonPressed
        wasActivationButtonPressed = isActivationButtonPressed
        if (isActivationButtonPressed && !wasActivationPressed) {
            focusedActivationTarget?.activate()
        }
        val wasBackPressed = wasBackButtonPressed
        wasBackButtonPressed = isBackButtonPressed
        if (isBackButtonPressed && !wasBackPressed && host.hasOnBack()) {
            host.onBack()
        }
        if (direction == null) {
            focusDirection = null
            return
        }
        if (direction != focusDirection) {
            focusDirection = direction
            timeUntilNextFocusStepInMilliseconds = FOCUS_REPEAT_DELAY
            moveFocus(focusManager, direction)
            return
        }
        timeUntilNextFocusStepInMilliseconds -= deltaTimeInMilliseconds
        if (timeUntilNextFocusStepInMilliseconds <= 0f) {
            timeUntilNextFocusStepInMilliseconds += FOCUS_REPEAT_INTERVAL
            moveFocus(focusManager, direction)
        }
    }

    /**
     * One step of the focus in [direction], falling back to entering whatever holds the focus when there is
     * nowhere to step to.
     *
     * A directional search only ever walks the *siblings* of the focused Composable, so a focus parked on a
     * container - a root that holds it so that the game can read keys, which is the shape of most games that
     * have a menu at all - has nowhere to go, and the first push of the stick would otherwise do nothing at all.
     * Entering is what Compose does for a D-pad center, and it is what gets the focus into a menu that has just
     * come on screen. Only ever tried while the focus is on nothing this manager knows (see [onGamepadActivation]),
     * so that reaching the end of a list steps out of it rather than dropping into the focused control's own
     * insides.
     */
    private fun moveFocus(focusManager: FocusManager, direction: FocusDirection) {
        if (!focusManager.moveFocus(direction) && focusedActivationTarget == null) {
            focusManager.moveFocus(FocusDirection.Enter)
        }
    }

    /**
     * The direction the first gamepad that is asking for one wants the focus moved in, or null while none is.
     *
     * A stick is reduced to the one axis it leans on the most, so a diagonal push picks a single direction
     * instead of walking the focus twice - Compose has no diagonal to move the focus in.
     */
    private fun readFocusDirection(): FocusDirection? {
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            val gamepad = gamepads[index]
            if (!gamepad.isConnected) {
                continue
            }
            if (gamepad.isPressed(GamepadButton.DPAD_LEFT)) return FocusDirection.Left
            if (gamepad.isPressed(GamepadButton.DPAD_RIGHT)) return FocusDirection.Right
            if (gamepad.isPressed(GamepadButton.DPAD_UP)) return FocusDirection.Up
            if (gamepad.isPressed(GamepadButton.DPAD_DOWN)) return FocusDirection.Down
            val stickX = gamepad.leftStickX
            val stickY = gamepad.leftStickY
            if (abs(stickX) > abs(stickY)) {
                if (stickX <= -FOCUS_STICK_THRESHOLD) return FocusDirection.Left
                if (stickX >= FOCUS_STICK_THRESHOLD) return FocusDirection.Right
            } else {
                if (stickY <= -FOCUS_STICK_THRESHOLD) return FocusDirection.Up
                if (stickY >= FOCUS_STICK_THRESHOLD) return FocusDirection.Down
            }
        }
        return null
    }

    private fun isAnyGamepadPressing(button: GamepadButton): Boolean {
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            if (gamepads[index].isConnected && gamepads[index].isPressed(button)) {
                return true
            }
        }
        return false
    }

    override fun isButtonPressed(gamepadIndex: Int, button: GamepadButton) =
        gamepadIndex >= 0 && gamepadIndex < MAX_GAMEPAD_COUNT && gamepads[gamepadIndex].isPressed(button)

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        val isFocused = stateManager.isFocused.value
        if (!isFocused) {
            if (wasFocused) {
                wasFocused = false
                releaseAllGamepads()
            }
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

    private fun releaseAllGamepads() {
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            releaseGamepad(gamepads[index])
        }
    }

    /**
     * Zeroes the state of a gamepad and reports every button that was held down as released, so that losing
     * focus or a controller can't leave an Actor acting on an input that is no longer there.
     */
    private fun releaseGamepad(gamepad: GamepadState) {
        val previousButtons = gamepad.pressedButtons
        gamepad.reset()
        if (previousButtons != 0) {
            notifyButtonChanges(gamepad, previousButtons, 0)
        }
    }

    private fun stopListening() {
        gamepadEventHandler?.stopListening()
        gamepadEventHandler = null
        releaseAllGamepads()
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            rawGamepads[index].reset()
        }
        _connectedGamepadCount.value = 0
    }

    override fun onDispose() = stopListening()
}

// How far a stick has to lean before it counts as asking for a direction. Well above the dead zone, so that a
// stick resting slightly off center never walks the focus on its own.
private const val FOCUS_STICK_THRESHOLD = 0.5f

// What a held direction does, in milliseconds: the pause before it starts repeating, and the pace it repeats at
// afterwards. Matches the feel of a held arrow key rather than any one platform's exact numbers.
private const val FOCUS_REPEAT_DELAY = 400f
private const val FOCUS_REPEAT_INTERVAL = 120f

private const val NANOSECONDS_PER_MILLISECOND = 1_000_000f

// A frame the platform sat on must not turn into one huge jump through the focus.
private const val MAXIMUM_FRAME_TIME = 100f
