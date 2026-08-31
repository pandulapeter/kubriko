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

import kotlin.math.hypot

/**
 * The state of a single gamepad slot, in the platform-independent layout described by [GamepadButton].
 *
 * Instances are owned by the [GamepadInputManager] and reused for the lifetime of the manager: the same object
 * represents the same slot as controllers get plugged in and unplugged, and its properties are overwritten in
 * place on every tick. Read the values you need during the callback that handed you the state, and copy anything
 * you want to keep - storing the [GamepadState] itself and reading it later gives you the values of a later tick.
 *
 * @param index The zero-based slot this state belongs to.
 */
class GamepadState internal constructor(
    val index: Int,
) {
    /**
     * Whether a gamepad is currently connected to this slot. All other properties read as zero while this is false.
     */
    var isConnected: Boolean = false
        internal set

    /**
     * The name the platform reports for the connected gamepad, or null if the platform doesn't provide one.
     */
    var name: String? = null
        internal set

    /**
     * The horizontal position of the left stick, from -1 (fully left) to 1 (fully right), with the dead zone
     * of the [GamepadInputManager] already applied.
     */
    var leftStickX: Float = 0f
        internal set

    /**
     * The vertical position of the left stick, from -1 (fully up) to 1 (fully down), with the dead zone of the
     * [GamepadInputManager] already applied. Positive values point downwards, matching the coordinate system
     * of the engine.
     */
    var leftStickY: Float = 0f
        internal set

    /**
     * The horizontal position of the right stick. See [leftStickX].
     */
    var rightStickX: Float = 0f
        internal set

    /**
     * The vertical position of the right stick. See [leftStickY].
     */
    var rightStickY: Float = 0f
        internal set

    /**
     * How far the left trigger is pulled, from 0 (released) to 1 (fully pulled). Gamepads with digital triggers
     * only ever report 0 or 1.
     */
    var leftTrigger: Float = 0f
        internal set

    /**
     * How far the right trigger is pulled. See [leftTrigger].
     */
    var rightTrigger: Float = 0f
        internal set

    internal var pressedButtons: Int = 0

    /**
     * How far the left stick is pushed from its center, from 0 to 1. Useful as a speed multiplier.
     */
    val leftStickMagnitude get() = hypot(leftStickX, leftStickY)

    /**
     * How far the right stick is pushed from its center, from 0 to 1.
     */
    val rightStickMagnitude get() = hypot(rightStickX, rightStickY)

    /**
     * Returns true if the specified [button] is currently pressed.
     */
    fun isPressed(button: GamepadButton) = pressedButtons and button.bitMask != 0

    internal fun reset() {
        isConnected = false
        name = null
        leftStickX = 0f
        leftStickY = 0f
        rightStickX = 0f
        rightStickY = 0f
        leftTrigger = 0f
        rightTrigger = 0f
        pressedButtons = 0
    }
}
