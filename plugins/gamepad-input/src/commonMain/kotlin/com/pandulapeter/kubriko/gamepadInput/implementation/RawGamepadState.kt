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

import com.pandulapeter.kubriko.gamepadInput.GamepadButton

/**
 * What a [GamepadEventHandler] writes for a single gamepad slot: values straight from the platform, before the
 * manager applies its dead zone and derives the digital trigger buttons.
 *
 * Sticks use the coordinate system of the engine (positive Y points downwards), triggers run from 0 to 1, and
 * [pressedButtons] is a bit set built from [GamepadButton.bitMask].
 */
internal class RawGamepadState {
    var isConnected = false
    var name: String? = null
    var leftStickX = 0f
    var leftStickY = 0f
    var rightStickX = 0f
    var rightStickY = 0f
    var leftTrigger = 0f
    var rightTrigger = 0f
    var pressedButtons = 0

    fun setButton(button: GamepadButton, isPressed: Boolean) {
        pressedButtons = if (isPressed) pressedButtons or button.bitMask else pressedButtons and button.bitMask.inv()
    }

    fun reset() {
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
