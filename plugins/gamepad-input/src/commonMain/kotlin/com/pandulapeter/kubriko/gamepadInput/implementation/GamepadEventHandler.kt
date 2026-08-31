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

import androidx.compose.runtime.Composable

/**
 * The platform side of the plugin. Implementations keep the array they were handed in [startListening] and fill
 * it with the current values of every gamepad slot, either from platform callbacks as they arrive or during
 * [poll]. Both are called from the thread that drives the game loop.
 */
internal interface GamepadEventHandler {

    @Composable
    fun isValid(): Boolean

    fun startListening(gamepads: Array<RawGamepadState>)

    fun stopListening()

    fun poll()
}

@Composable
internal expect fun createGamepadEventHandler(): GamepadEventHandler
