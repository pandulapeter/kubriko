/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testInput.implementation

import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import kotlinx.collections.immutable.ImmutableList

/**
 * A copy of the values of one gamepad, taken during the tick that reported them.
 *
 * The `GamepadState` the plugin hands out is reused across ticks, so it can't be collected into a list and
 * handed to the UI - this is what a consumer that wants to keep the values around has to build instead.
 */
internal data class GamepadSnapshot(
    val index: Int,
    val name: String?,
    val leftStickX: Float,
    val leftStickY: Float,
    val rightStickX: Float,
    val rightStickY: Float,
    val leftTrigger: Float,
    val rightTrigger: Float,
    val pressedButtons: ImmutableList<GamepadButton>,
)
