/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.pandulapeter.kubriko.gamepadInput.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import kotlin.js.ExperimentalWasmJsInterop

@Composable
internal actual fun createGamepadEventHandler(): GamepadEventHandler = object : GamepadEventHandler {

    private var gamepads: Array<RawGamepadState>? = null

    @Composable
    override fun isValid() = true

    override fun startListening(gamepads: Array<RawGamepadState>) {
        this.gamepads = gamepads
    }

    override fun stopListening() {
        gamepads = null
    }

    override fun poll() {
        val gamepads = gamepads ?: return
        val gamepadCount = refreshGamepads()
        for (slot in gamepads.indices) {
            val gamepad = gamepads[slot]
            if (slot >= gamepadCount || !isGamepadConnected(slot)) {
                if (gamepad.isConnected) {
                    gamepad.reset()
                }
                continue
            }
            if (!gamepad.isConnected) {
                gamepad.reset()
                gamepad.isConnected = true
                gamepad.name = getGamepadId(slot)
            }
            gamepad.read(slot)
        }
    }

    private fun RawGamepadState.read(slot: Int) {
        leftStickX = getAxis(slot, LEFT_STICK_X_AXIS).toFloat()
        leftStickY = getAxis(slot, LEFT_STICK_Y_AXIS).toFloat()
        rightStickX = getAxis(slot, RIGHT_STICK_X_AXIS).toFloat()
        rightStickY = getAxis(slot, RIGHT_STICK_Y_AXIS).toFloat()
        leftTrigger = getButtonValue(slot, LEFT_TRIGGER_BUTTON).toFloat()
        rightTrigger = getButtonValue(slot, RIGHT_TRIGGER_BUTTON).toFloat()
        val browserButtons = getButtonMask(slot)
        val buttons = GamepadButton.entries
        for (index in buttons.indices) {
            val browserIndex = STANDARD_BUTTON_INDICES[index]
            if (browserIndex != NO_BUTTON) {
                setButton(buttons[index], browserButtons and (1 shl browserIndex) != 0)
            }
        }
    }
}

/**
 * The browser button index of every [GamepadButton], indexed by ordinal, in the "standard" layout of the Gamepad
 * API. The triggers are [NO_BUTTON] because their analog value is read instead. Gamepads the browser could not
 * fit into the standard layout report a different order, which no amount of guessing here could recover.
 */
private val STANDARD_BUTTON_INDICES = intArrayOf(0, 1, 2, 3, 4, 5, NO_BUTTON, NO_BUTTON, 10, 11, 12, 13, 14, 15, 9, 8, 16)

private const val NO_BUTTON = -1
private const val LEFT_STICK_X_AXIS = 0
private const val LEFT_STICK_Y_AXIS = 1
private const val RIGHT_STICK_X_AXIS = 2
private const val RIGHT_STICK_Y_AXIS = 3
private const val LEFT_TRIGGER_BUTTON = 6
private const val RIGHT_TRIGGER_BUTTON = 7

// The browser hands out a fresh snapshot array on every getGamepads() call, so it is taken once per tick and
// parked on the JavaScript side: reading it from there keeps the number of calls crossing the boundary down.
private fun refreshGamepads(): Int =
    js("(() => { const pads = navigator.getGamepads ? navigator.getGamepads() : []; globalThis.__kubrikoGamepads = pads; return pads.length; })()")

private fun isGamepadConnected(index: Int): Boolean =
    js("(() => { const pad = globalThis.__kubrikoGamepads[index]; return !!pad && pad.connected; })()")

private fun getGamepadId(index: Int): String =
    js("globalThis.__kubrikoGamepads[index].id")

private fun getAxis(index: Int, axis: Int): Double =
    js("(() => { const axes = globalThis.__kubrikoGamepads[index].axes; return axes.length > axis ? axes[axis] : 0; })()")

private fun getButtonValue(index: Int, button: Int): Double =
    js("(() => { const buttons = globalThis.__kubrikoGamepads[index].buttons; return buttons.length > button ? buttons[button].value : 0; })()")

private fun getButtonMask(index: Int): Int =
    js("(() => { const buttons = globalThis.__kubrikoGamepads[index].buttons; let mask = 0; for (let i = 0; i < buttons.length && i < 32; i++) { if (buttons[i].pressed) { mask |= (1 << i); } } return mask; })()")
