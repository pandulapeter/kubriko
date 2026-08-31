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
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager.Companion.MAX_GAMEPAD_COUNT
import platform.Foundation.NSNotificationCenter
import platform.GameController.GCController
import platform.GameController.GCControllerDidConnectNotification
import platform.GameController.GCControllerDidDisconnectNotification
import platform.GameController.GCExtendedGamepad
import platform.darwin.NSObjectProtocol

@Composable
internal actual fun createGamepadEventHandler(): GamepadEventHandler = object : GamepadEventHandler {

    private var gamepads: Array<RawGamepadState>? = null
    private val controllers = arrayOfNulls<GCController>(MAX_GAMEPAD_COUNT)
    private var connectObserver: NSObjectProtocol? = null
    private var disconnectObserver: NSObjectProtocol? = null

    @Composable
    override fun isValid() = true

    override fun startListening(gamepads: Array<RawGamepadState>) {
        this.gamepads = gamepads
        NSNotificationCenter.defaultCenter.let { notificationCenter ->
            connectObserver = notificationCenter.addObserverForName(
                name = GCControllerDidConnectNotification,
                `object` = null,
                queue = null,
            ) { refreshControllers() }
            disconnectObserver = notificationCenter.addObserverForName(
                name = GCControllerDidDisconnectNotification,
                `object` = null,
                queue = null,
            ) { refreshControllers() }
        }
        refreshControllers()
    }

    override fun stopListening() {
        NSNotificationCenter.defaultCenter.let { notificationCenter ->
            connectObserver?.let(notificationCenter::removeObserver)
            disconnectObserver?.let(notificationCenter::removeObserver)
        }
        connectObserver = null
        disconnectObserver = null
        controllers.fill(null)
        gamepads = null
    }

    override fun poll() {
        val gamepads = gamepads ?: return
        for (slot in controllers.indices) {
            controllers[slot]?.extendedGamepad?.let { gamepads[slot].read(it) }
        }
    }

    /**
     * Reconciles the slots with the controllers the system currently knows about. Slots are sticky: a controller
     * keeps the slot it was given until it disconnects, so player one doesn't change controller when player two
     * leaves. Controllers without an extended profile are ignored - a remote or a single Joy-Con has no sticks.
     */
    private fun refreshControllers() {
        val connectedControllers = GCController.controllers()
        for (slot in controllers.indices) {
            val controller = controllers[slot]
            if (controller != null && !connectedControllers.contains(controller)) {
                controllers[slot] = null
                gamepads?.get(slot)?.reset()
            }
        }
        connectedControllers.forEach { connectedController ->
            val controller = connectedController as? GCController ?: return@forEach
            if (controller.extendedGamepad == null || controllers.contains(controller)) {
                return@forEach
            }
            val slot = controllers.indexOfFirst { it == null }
            if (slot != NO_SLOT) {
                controllers[slot] = controller
                gamepads?.get(slot)?.let { gamepad ->
                    gamepad.reset()
                    gamepad.isConnected = true
                    gamepad.name = controller.vendorName
                }
            }
        }
    }

    private fun RawGamepadState.read(gamepad: GCExtendedGamepad) {
        leftStickX = gamepad.leftThumbstick.xAxis.value
        rightStickX = gamepad.rightThumbstick.xAxis.value
        // The GameController framework points its vertical axes upwards, the engine points them downwards.
        leftStickY = -gamepad.leftThumbstick.yAxis.value
        rightStickY = -gamepad.rightThumbstick.yAxis.value
        leftTrigger = gamepad.leftTrigger.value
        rightTrigger = gamepad.rightTrigger.value
        setButton(GamepadButton.SOUTH, gamepad.buttonA.isPressed())
        setButton(GamepadButton.EAST, gamepad.buttonB.isPressed())
        setButton(GamepadButton.WEST, gamepad.buttonX.isPressed())
        setButton(GamepadButton.NORTH, gamepad.buttonY.isPressed())
        setButton(GamepadButton.LEFT_SHOULDER, gamepad.leftShoulder.isPressed())
        setButton(GamepadButton.RIGHT_SHOULDER, gamepad.rightShoulder.isPressed())
        setButton(GamepadButton.LEFT_STICK, gamepad.leftThumbstickButton?.isPressed() == true)
        setButton(GamepadButton.RIGHT_STICK, gamepad.rightThumbstickButton?.isPressed() == true)
        setButton(GamepadButton.DPAD_UP, gamepad.dpad.up.isPressed())
        setButton(GamepadButton.DPAD_DOWN, gamepad.dpad.down.isPressed())
        setButton(GamepadButton.DPAD_LEFT, gamepad.dpad.left.isPressed())
        setButton(GamepadButton.DPAD_RIGHT, gamepad.dpad.right.isPressed())
        setButton(GamepadButton.START, gamepad.buttonMenu.isPressed())
        setButton(GamepadButton.SELECT, gamepad.buttonOptions?.isPressed() == true)
        setButton(GamepadButton.GUIDE, gamepad.buttonHome?.isPressed() == true)
    }
}

private const val NO_SLOT = -1
