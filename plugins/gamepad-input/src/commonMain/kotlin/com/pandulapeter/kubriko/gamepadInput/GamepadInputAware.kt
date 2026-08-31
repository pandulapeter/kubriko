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

import com.pandulapeter.kubriko.actor.Actor

/**
 * A trait that can be added to an [Actor] to make it aware of gamepad input.
 */
interface GamepadInputAware : Actor {

    /**
     * Called on every frame, once for each connected gamepad, with its current state.
     *
     * @param gamepad The state of one connected gamepad. Only valid for the duration of this call.
     */
    fun handleGamepadState(gamepad: GamepadState) = Unit

    /**
     * Called when a button is first pressed.
     *
     * @param gamepad The gamepad the button belongs to.
     * @param button The button that was pressed.
     */
    fun onGamepadButtonPressed(gamepad: GamepadState, button: GamepadButton) = Unit

    /**
     * Called when a button is released.
     *
     * @param gamepad The gamepad the button belongs to.
     * @param button The button that was released.
     */
    fun onGamepadButtonReleased(gamepad: GamepadState, button: GamepadButton) = Unit

    /**
     * Called when a gamepad is connected to a previously empty slot.
     *
     * @param gamepad The state of the gamepad that was connected.
     */
    fun onGamepadConnected(gamepad: GamepadState) = Unit

    /**
     * Called when a gamepad is disconnected. Buttons that were held down are reported as released first.
     *
     * @param gamepad The state of the gamepad that was disconnected, already reset to its neutral values.
     */
    fun onGamepadDisconnected(gamepad: GamepadState) = Unit
}
