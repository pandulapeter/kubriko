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

    // Every Objective-C object crossing into Kotlin gets a fresh wrapper, so reading a controller's elements off its
    // profile on every poll would allocate about thirty of them a tick. They are the same objects for as long as the
    // controller stays connected, so they are resolved once per slot and a poll only reads primitives through them.
    private val elements = arrayOfNulls<GamepadElements>(MAX_GAMEPAD_COUNT)
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
        elements.fill(null)
        gamepads = null
    }

    override fun poll() {
        val gamepads = gamepads ?: return
        for (slot in controllers.indices) {
            elements[slot]?.let { gamepads[slot].read(it) }
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
                elements[slot] = null
                gamepads?.get(slot)?.reset()
            }
        }
        connectedControllers.forEach { connectedController ->
            val controller = connectedController as? GCController ?: return@forEach
            val extendedGamepad = controller.extendedGamepad
            if (extendedGamepad == null || controllers.contains(controller)) {
                return@forEach
            }
            val slot = controllers.indexOfFirst { it == null }
            if (slot != NO_SLOT) {
                controllers[slot] = controller
                elements[slot] = GamepadElements(extendedGamepad)
                gamepads?.get(slot)?.let { gamepad ->
                    gamepad.reset()
                    gamepad.isConnected = true
                    gamepad.name = controller.vendorName
                }
            }
        }
    }

    private fun RawGamepadState.read(elements: GamepadElements) {
        leftStickX = elements.leftStickX.value
        rightStickX = elements.rightStickX.value
        // The GameController framework points its vertical axes upwards, the engine points them downwards.
        leftStickY = -elements.leftStickY.value
        rightStickY = -elements.rightStickY.value
        leftTrigger = elements.leftTrigger.value
        rightTrigger = elements.rightTrigger.value
        for (index in READ_BUTTONS.indices) {
            setButton(READ_BUTTONS[index], elements.buttons[index]?.isPressed() == true)
        }
    }
}

/** One extended gamepad's elements, resolved once while its controller stays connected - see `elements` above. */
private class GamepadElements(gamepad: GCExtendedGamepad) {
    val leftStickX = gamepad.leftThumbstick.xAxis
    val leftStickY = gamepad.leftThumbstick.yAxis
    val rightStickX = gamepad.rightThumbstick.xAxis
    val rightStickY = gamepad.rightThumbstick.yAxis
    val leftTrigger = gamepad.leftTrigger
    val rightTrigger = gamepad.rightTrigger

    /** Indexed like [READ_BUTTONS]; null where the controller doesn't have that button. */
    val buttons = arrayOf(
        gamepad.buttonA,
        gamepad.buttonB,
        gamepad.buttonX,
        gamepad.buttonY,
        gamepad.leftShoulder,
        gamepad.rightShoulder,
        gamepad.leftThumbstickButton,
        gamepad.rightThumbstickButton,
        gamepad.dpad.up,
        gamepad.dpad.down,
        gamepad.dpad.left,
        gamepad.dpad.right,
        gamepad.buttonMenu,
        gamepad.buttonOptions,
        gamepad.buttonHome,
    )
}

// The buttons read off the controller, in the order GamepadElements.buttons holds them. The triggers' own buttons are
// left out: the manager derives those from the trigger values the same way on every platform.
private val READ_BUTTONS = arrayOf(
    GamepadButton.SOUTH,
    GamepadButton.EAST,
    GamepadButton.WEST,
    GamepadButton.NORTH,
    GamepadButton.LEFT_SHOULDER,
    GamepadButton.RIGHT_SHOULDER,
    GamepadButton.LEFT_STICK,
    GamepadButton.RIGHT_STICK,
    GamepadButton.DPAD_UP,
    GamepadButton.DPAD_DOWN,
    GamepadButton.DPAD_LEFT,
    GamepadButton.DPAD_RIGHT,
    GamepadButton.START,
    GamepadButton.SELECT,
    GamepadButton.GUIDE,
)

private const val NO_SLOT = -1
