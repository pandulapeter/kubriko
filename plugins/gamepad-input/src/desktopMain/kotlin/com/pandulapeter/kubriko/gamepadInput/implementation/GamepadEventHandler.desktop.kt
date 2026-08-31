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
import com.studiohartman.jamepad.Configuration
import com.studiohartman.jamepad.ControllerAxis
import com.studiohartman.jamepad.ControllerButton
import com.studiohartman.jamepad.ControllerIndex
import com.studiohartman.jamepad.ControllerManager
import com.studiohartman.jamepad.ControllerUnpluggedException

@Composable
internal actual fun createGamepadEventHandler(): GamepadEventHandler = object : GamepadEventHandler {

    private var controllerManager: ControllerManager? = null
    private var gamepads: Array<RawGamepadState>? = null

    @Composable
    override fun isValid() = true

    override fun startListening(gamepads: Array<RawGamepadState>) {
        this.gamepads = gamepads
        controllerManager = JamepadRuntime.acquire()
    }

    override fun stopListening() {
        if (controllerManager != null) {
            controllerManager = null
            JamepadRuntime.release()
        }
        gamepads = null
    }

    override fun poll() {
        val controllerManager = controllerManager ?: return
        val gamepads = gamepads ?: return
        controllerManager.update()
        for (slot in gamepads.indices) {
            gamepads[slot].read(controllerManager.getControllerIndex(slot))
        }
    }

    private fun RawGamepadState.read(controller: ControllerIndex) {
        if (!controller.isConnected) {
            if (isConnected) {
                reset()
            }
            return
        }
        try {
            if (!isConnected) {
                reset()
                isConnected = true
                name = controller.name
            }
            leftStickX = controller.getAxisState(ControllerAxis.LEFTX)
            leftStickY = controller.getAxisState(ControllerAxis.LEFTY)
            rightStickX = controller.getAxisState(ControllerAxis.RIGHTX)
            rightStickY = controller.getAxisState(ControllerAxis.RIGHTY)
            leftTrigger = controller.getAxisState(ControllerAxis.TRIGGERLEFT)
            rightTrigger = controller.getAxisState(ControllerAxis.TRIGGERRIGHT)
            val buttons = GamepadButton.entries
            for (index in buttons.indices) {
                CONTROLLER_BUTTONS[index]?.let { setButton(buttons[index], controller.isButtonPressed(it)) }
            }
        } catch (_: ControllerUnpluggedException) {
            reset()
        }
    }
}

/**
 * Jamepad wraps a single SDL instance for the whole process, while a Kubriko application may well run several
 * [com.pandulapeter.kubriko.Kubriko] instances at once. Reference counting keeps the last one standing from
 * shutting SDL down under the others.
 *
 * A failure to load the native library leaves the plugin inert rather than taking the game down with it: a
 * desktop build that can't reach a gamepad is still perfectly playable with a keyboard.
 */
/**
 * The mapping database handed to Jamepad, which insists on loading one and prints a stack trace when it can't -
 * its own default path naming a file it does not ship. The bundled one is deliberately empty, leaving SDL's
 * compiled-in database and its native drivers in charge; the name is Kubriko's own rather than the
 * `gamecontrollerdb.txt` Jamepad looks for by default, so a game that ships that file keeps its own copy.
 */
private const val MAPPINGS_PATH = "/kubriko-gamepad-mappings.txt"

private object JamepadRuntime {

    private var controllerManager: ControllerManager? = null
    private var referenceCount = 0
    private var isUnavailable = false

    @Synchronized
    fun acquire(): ControllerManager? {
        if (controllerManager == null && !isUnavailable) {
            controllerManager = try {
                ControllerManager(
                    Configuration().apply { maxNumControllers = MAX_GAMEPAD_COUNT },
                    MAPPINGS_PATH,
                ).apply { initSDLGamepad() }
            } catch (_: Throwable) {
                isUnavailable = true
                null
            }
        }
        return controllerManager?.also { referenceCount++ }
    }

    @Synchronized
    fun release() {
        referenceCount--
        if (referenceCount <= 0) {
            referenceCount = 0
            controllerManager?.quitSDLGamepad()
            controllerManager = null
        }
    }
}

/**
 * The Jamepad counterpart of every [GamepadButton], indexed by ordinal. The triggers are null because they are
 * derived from their axes instead.
 */
private val CONTROLLER_BUTTONS = arrayOf(
    ControllerButton.A,
    ControllerButton.B,
    ControllerButton.X,
    ControllerButton.Y,
    ControllerButton.LEFTBUMPER,
    ControllerButton.RIGHTBUMPER,
    null,
    null,
    ControllerButton.LEFTSTICK,
    ControllerButton.RIGHTSTICK,
    ControllerButton.DPAD_UP,
    ControllerButton.DPAD_DOWN,
    ControllerButton.DPAD_LEFT,
    ControllerButton.DPAD_RIGHT,
    ControllerButton.START,
    ControllerButton.BACK,
    ControllerButton.GUIDE,
)
