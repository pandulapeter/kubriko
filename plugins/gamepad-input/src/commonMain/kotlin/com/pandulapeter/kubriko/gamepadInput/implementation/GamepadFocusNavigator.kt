/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gamepadInput.implementation

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager.Companion.MAX_GAMEPAD_COUNT
import com.pandulapeter.kubriko.gamepadInput.GamepadState
import kotlin.math.abs

/**
 * Walks Compose's focus with [gamepads] for the focus-navigation loop of [gamepadInputManager], which hands it every
 * frame; the hosts it walks and the focused activation target are read from [gamepadInputManager].
 */
internal class GamepadFocusNavigator(
    private val gamepads: List<GamepadState>,
    private val gamepadInputManager: GamepadInputManager,
) {
    /**
     * The direction the focus is currently being walked in, and how long it has been held that way. Together
     * they are what turns a stick that is simply pushed and left there into the repeat a held arrow key has.
     */
    private var focusDirection: FocusDirection? = null
    private var timeUntilNextFocusStepInMilliseconds = 0f
    private var wasActivationButtonPressed = false
    private var wasBackButtonPressed = false

    var previousFocusFrameTimeNanos = 0L
    var hadFocusNavigationInput = false

    /** Held rather than written inline, so that awaiting a frame doesn't allocate a lambda every time. */
    val onFocusNavigationFrame: (Long) -> Unit = { frameTimeNanos ->
        val deltaTimeInMilliseconds = if (previousFocusFrameTimeNanos == 0L) {
            0f
        } else {
            ((frameTimeNanos - previousFocusFrameTimeNanos) / NANOSECONDS_PER_MILLISECOND).coerceAtMost(MAXIMUM_FRAME_TIME)
        }
        previousFocusFrameTimeNanos = frameTimeNanos
        hadFocusNavigationInput = updateFocusNavigation(deltaTimeInMilliseconds)
    }

    /**
     * Takes whatever the sticks and the buttons are doing right now as their resting state, so a menu that opens
     * under a held control doesn't immediately act on it: only what the player does next counts.
     */
    fun restFocusNavigation() {
        wasActivationButtonPressed = isAnyGamepadPressing(GamepadButton.SOUTH)
        wasBackButtonPressed = isAnyGamepadPressing(GamepadButton.EAST)
        focusDirection = readFocusDirection()
        timeUntilNextFocusStepInMilliseconds = FOCUS_REPEAT_DELAY
        previousFocusFrameTimeNanos = 0L
        hadFocusNavigationInput = false
    }

    /** Returns whether the pads asked anything of the focus on this frame. */
    fun updateFocusNavigation(deltaTimeInMilliseconds: Float): Boolean {
        val isActivationButtonPressed = isAnyGamepadPressing(GamepadButton.SOUTH)
        val isBackButtonPressed = isAnyGamepadPressing(GamepadButton.EAST)
        val direction = readFocusDirection()
        val hasInput = isActivationButtonPressed || isBackButtonPressed || direction != null
        val host = gamepadInputManager.focusNavigationHosts.lastOrNull() ?: return hasInput
        val focusManager = host.focusManager
        if (hasInput) {
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
            gamepadInputManager.focusedActivationTarget?.activate()
        }
        val wasBackPressed = wasBackButtonPressed
        wasBackButtonPressed = isBackButtonPressed
        if (isBackButtonPressed && !wasBackPressed && host.hasOnBack()) {
            host.onBack()
        }
        if (direction == null) {
            focusDirection = null
            return hasInput
        }
        if (direction != focusDirection) {
            focusDirection = direction
            timeUntilNextFocusStepInMilliseconds = FOCUS_REPEAT_DELAY
            moveFocus(focusManager, direction)
            return hasInput
        }
        timeUntilNextFocusStepInMilliseconds -= deltaTimeInMilliseconds
        if (timeUntilNextFocusStepInMilliseconds <= 0f) {
            timeUntilNextFocusStepInMilliseconds += FOCUS_REPEAT_INTERVAL
            moveFocus(focusManager, direction)
        }
        return hasInput
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
        if (!focusManager.moveFocus(direction) && gamepadInputManager.focusedActivationTarget == null) {
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

    /**
     * Whether [readFocusDirection] would name a direction, without naming it: this runs on every tick, and a nullable
     * [FocusDirection] is boxed. A stick leaning at least the threshold on the axis it leans on most is what the other
     * reads as a direction.
     */
    fun isAnyFocusDirectionHeld(): Boolean {
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            val gamepad = gamepads[index]
            if (!gamepad.isConnected) {
                continue
            }
            if (gamepad.isPressed(GamepadButton.DPAD_LEFT) || gamepad.isPressed(GamepadButton.DPAD_RIGHT) ||
                gamepad.isPressed(GamepadButton.DPAD_UP) || gamepad.isPressed(GamepadButton.DPAD_DOWN)
            ) return true
            if (maxOf(abs(gamepad.leftStickX), abs(gamepad.leftStickY)) >= FOCUS_STICK_THRESHOLD) return true
        }
        return false
    }

    fun isAnyGamepadPressing(button: GamepadButton): Boolean {
        for (index in 0 until MAX_GAMEPAD_COUNT) {
            if (gamepads[index].isConnected && gamepads[index].isPressed(button)) {
                return true
            }
        }
        return false
    }
}

/**
 * How far a stick has to lean before it counts as asking for a direction. Well above the dead zone, so that a
 * stick resting slightly off center never walks the focus on its own.
 */
private const val FOCUS_STICK_THRESHOLD = 0.5f

/**
 * What a held direction does, in milliseconds: the pause before it starts repeating, and the pace it repeats at
 * afterwards. Matches the feel of a held arrow key rather than any one platform's exact numbers.
 */
private const val FOCUS_REPEAT_DELAY = 400f
private const val FOCUS_REPEAT_INTERVAL = 120f

private const val NANOSECONDS_PER_MILLISECOND = 1_000_000f

/** A frame the platform sat on must not turn into one huge jump through the focus. */
private const val MAXIMUM_FRAME_TIME = 100f
