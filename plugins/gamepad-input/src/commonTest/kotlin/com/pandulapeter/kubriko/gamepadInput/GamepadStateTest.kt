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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GamepadStateTest {

    @Test
    fun releaseInputsKeepsTheConnection() {
        val state = createActiveState()
        state.releaseInputs()
        assertTrue(state.isConnected)
        assertEquals(NAME, state.name)
        assertInputsAreZero(state)
    }

    @Test
    fun resetDisconnects() {
        val state = createActiveState()
        state.reset()
        assertFalse(state.isConnected)
        assertNull(state.name)
        assertInputsAreZero(state)
    }

    private fun createActiveState() = GamepadState(index = 0).apply {
        isConnected = true
        name = NAME
        leftStickX = 0.5f
        leftStickY = -0.5f
        rightStickX = 1f
        rightStickY = -1f
        leftTrigger = 0.3f
        rightTrigger = 0.7f
        pressedButtons = GamepadButton.entries.fold(0) { buttons, button -> buttons or button.bitMask }
    }

    private fun assertInputsAreZero(state: GamepadState) {
        assertEquals(0f, state.leftStickX)
        assertEquals(0f, state.leftStickY)
        assertEquals(0f, state.rightStickX)
        assertEquals(0f, state.rightStickY)
        assertEquals(0f, state.leftTrigger)
        assertEquals(0f, state.rightTrigger)
        GamepadButton.entries.forEach { button -> assertFalse(state.isPressed(button)) }
    }

    private companion object {
        const val NAME = "Test pad"
    }
}
