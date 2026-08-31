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

import android.app.Activity
import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager.Companion.MAX_GAMEPAD_COUNT
import kotlin.math.max

@Composable
internal actual fun createGamepadEventHandler(): GamepadEventHandler = object : GamepadEventHandler, InputManager.InputDeviceListener {

    private var currentActivity = LocalContext.current as? Activity
    private var gamepads: Array<RawGamepadState>? = null
    private val deviceIds = IntArray(MAX_GAMEPAD_COUNT) { NO_DEVICE }

    // Gamepads report their triggers as axes, as buttons, or as both, so the two sources are kept apart and the
    // larger one wins - otherwise a device that sends both would have its analog value flattened to 0 or 1.
    private val axisTriggers = FloatArray(MAX_GAMEPAD_COUNT * 2)
    private val keyTriggers = FloatArray(MAX_GAMEPAD_COUNT * 2)

    // The directional pad has the same problem: a device without a hat reports it as key events only, and the
    // hat axes of every motion event would otherwise immediately clear what those key events set.
    private val hatDpadButtons = IntArray(MAX_GAMEPAD_COUNT)
    private val keyDpadButtons = IntArray(MAX_GAMEPAD_COUNT)

    private val genericMotionListener = View.OnGenericMotionListener { _, event ->
        if (event.source and InputDevice.SOURCE_CLASS_JOYSTICK != InputDevice.SOURCE_CLASS_JOYSTICK ||
            event.action != MotionEvent.ACTION_MOVE
        ) {
            return@OnGenericMotionListener false
        }
        val slot = slotOf(event.deviceId)
        if (slot == NO_SLOT) {
            return@OnGenericMotionListener false
        }
        gamepads?.get(slot)?.applyAxes(event, slot)
        true
    }

    private val keyEventListener = View.OnUnhandledKeyEventListener { _, event ->
        val button = event.keyCode.toGamepadButton() ?: return@OnUnhandledKeyEventListener false
        val slot = slotOf(event.deviceId)
        if (slot == NO_SLOT) {
            return@OnUnhandledKeyEventListener false
        }
        val gamepad = gamepads?.get(slot) ?: return@OnUnhandledKeyEventListener false
        val isPressed = event.action == KeyEvent.ACTION_DOWN
        when (button) {
            GamepadButton.LEFT_TRIGGER -> {
                keyTriggers[slot.leftTriggerIndex] = if (isPressed) 1f else 0f
                gamepad.applyTriggers(slot)
            }

            GamepadButton.RIGHT_TRIGGER -> {
                keyTriggers[slot.rightTriggerIndex] = if (isPressed) 1f else 0f
                gamepad.applyTriggers(slot)
            }

            in DPAD_BUTTONS -> {
                keyDpadButtons[slot] = if (isPressed) {
                    keyDpadButtons[slot] or button.bitMask
                } else {
                    keyDpadButtons[slot] and button.bitMask.inv()
                }
                gamepad.applyDpad(slot)
            }

            else -> gamepad.setButton(button, isPressed)
        }
        // The directional pad also arrives through the hat axes, and the keyboard-input plugin turns the same key
        // codes into arrow keys. Leaving them unhandled keeps both working.
        button !in DPAD_BUTTONS
    }

    @Composable
    override fun isValid() = currentActivity === LocalContext.current

    override fun startListening(gamepads: Array<RawGamepadState>) {
        this.gamepads = gamepads
        currentActivity?.let { activity ->
            activity.window?.decorView?.let { decorView ->
                decorView.setOnGenericMotionListener(genericMotionListener)
                decorView.rootView?.addOnUnhandledKeyEventListener(keyEventListener)
            }
            activity.inputManager?.registerInputDeviceListener(this, null)
        }
        refreshDevices()
    }

    override fun stopListening() {
        currentActivity?.let { activity ->
            activity.window?.decorView?.let { decorView ->
                decorView.setOnGenericMotionListener(null)
                decorView.rootView?.removeOnUnhandledKeyEventListener(keyEventListener)
            }
            activity.inputManager?.unregisterInputDeviceListener(this)
        }
        currentActivity = null
        gamepads = null
    }

    override fun poll() = Unit

    override fun onInputDeviceAdded(deviceId: Int) = refreshDevices()

    override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()

    override fun onInputDeviceRemoved(deviceId: Int) = refreshDevices()

    /**
     * Reconciles the slots with the gamepads the system currently knows about. Slots are sticky: a gamepad keeps
     * the slot it was given until it is unplugged, so player one doesn't change controller when player two leaves.
     */
    private fun refreshDevices() {
        val connectedIds = InputDevice.getDeviceIds()
        for (slot in deviceIds.indices) {
            val deviceId = deviceIds[slot]
            if (deviceId != NO_DEVICE && !connectedIds.contains(deviceId)) {
                releaseSlot(slot)
            }
        }
        connectedIds.forEach { deviceId ->
            val device = InputDevice.getDevice(deviceId)
            if (device != null && device.isGamepad && slotOf(deviceId) == NO_SLOT) {
                claimSlot(deviceId, device.name)
            }
        }
    }

    private fun claimSlot(deviceId: Int, name: String?) {
        val slot = deviceIds.indexOfFirst { it == NO_DEVICE }
        if (slot == NO_SLOT) {
            return
        }
        deviceIds[slot] = deviceId
        releaseAxisState(slot)
        gamepads?.get(slot)?.let { gamepad ->
            gamepad.reset()
            gamepad.isConnected = true
            gamepad.name = name
        }
    }

    private fun releaseSlot(slot: Int) {
        deviceIds[slot] = NO_DEVICE
        releaseAxisState(slot)
        gamepads?.get(slot)?.reset()
    }

    private fun releaseAxisState(slot: Int) {
        axisTriggers[slot.leftTriggerIndex] = 0f
        axisTriggers[slot.rightTriggerIndex] = 0f
        keyTriggers[slot.leftTriggerIndex] = 0f
        keyTriggers[slot.rightTriggerIndex] = 0f
        hatDpadButtons[slot] = 0
        keyDpadButtons[slot] = 0
    }

    private fun slotOf(deviceId: Int) = deviceIds.indexOfFirst { it == deviceId }

    private fun RawGamepadState.applyAxes(event: MotionEvent, slot: Int) {
        leftStickX = event.getAxisValue(MotionEvent.AXIS_X)
        leftStickY = event.getAxisValue(MotionEvent.AXIS_Y)
        rightStickX = event.getAxisValue(MotionEvent.AXIS_Z)
        rightStickY = event.getAxisValue(MotionEvent.AXIS_RZ)
        // Triggers arrive either on the dedicated axes or on the pedal axes of a driving controller. Axes the
        // device doesn't have read as zero, so taking the larger of the two always yields the real value.
        axisTriggers[slot.leftTriggerIndex] = max(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
        axisTriggers[slot.rightTriggerIndex] = max(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
        applyTriggers(slot)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        var hatButtons = 0
        if (hatX < -HAT_THRESHOLD) hatButtons = hatButtons or GamepadButton.DPAD_LEFT.bitMask
        if (hatX > HAT_THRESHOLD) hatButtons = hatButtons or GamepadButton.DPAD_RIGHT.bitMask
        if (hatY < -HAT_THRESHOLD) hatButtons = hatButtons or GamepadButton.DPAD_UP.bitMask
        if (hatY > HAT_THRESHOLD) hatButtons = hatButtons or GamepadButton.DPAD_DOWN.bitMask
        hatDpadButtons[slot] = hatButtons
        applyDpad(slot)
    }

    private fun RawGamepadState.applyTriggers(slot: Int) {
        leftTrigger = max(axisTriggers[slot.leftTriggerIndex], keyTriggers[slot.leftTriggerIndex])
        rightTrigger = max(axisTriggers[slot.rightTriggerIndex], keyTriggers[slot.rightTriggerIndex])
    }

    private fun RawGamepadState.applyDpad(slot: Int) {
        val dpadButtons = hatDpadButtons[slot] or keyDpadButtons[slot]
        for (button in DPAD_BUTTONS) {
            setButton(button, dpadButtons and button.bitMask != 0)
        }
    }
}

private const val NO_DEVICE = -1
private const val NO_SLOT = -1
private const val HAT_THRESHOLD = 0.5f

private val Int.leftTriggerIndex get() = this * 2

private val Int.rightTriggerIndex get() = this * 2 + 1

private val Activity.inputManager get() = getSystemService(Context.INPUT_SERVICE) as? InputManager

private val InputDevice.isGamepad
    get() = sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

private val DPAD_BUTTONS = arrayOf(
    GamepadButton.DPAD_UP,
    GamepadButton.DPAD_DOWN,
    GamepadButton.DPAD_LEFT,
    GamepadButton.DPAD_RIGHT,
)

private fun Int.toGamepadButton() = when (this) {
    KeyEvent.KEYCODE_BUTTON_A -> GamepadButton.SOUTH
    KeyEvent.KEYCODE_BUTTON_B -> GamepadButton.EAST
    KeyEvent.KEYCODE_BUTTON_X -> GamepadButton.WEST
    KeyEvent.KEYCODE_BUTTON_Y -> GamepadButton.NORTH
    KeyEvent.KEYCODE_BUTTON_L1 -> GamepadButton.LEFT_SHOULDER
    KeyEvent.KEYCODE_BUTTON_R1 -> GamepadButton.RIGHT_SHOULDER
    KeyEvent.KEYCODE_BUTTON_L2 -> GamepadButton.LEFT_TRIGGER
    KeyEvent.KEYCODE_BUTTON_R2 -> GamepadButton.RIGHT_TRIGGER
    KeyEvent.KEYCODE_BUTTON_THUMBL -> GamepadButton.LEFT_STICK
    KeyEvent.KEYCODE_BUTTON_THUMBR -> GamepadButton.RIGHT_STICK
    KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> GamepadButton.START
    KeyEvent.KEYCODE_BUTTON_SELECT -> GamepadButton.SELECT
    KeyEvent.KEYCODE_BUTTON_MODE -> GamepadButton.GUIDE
    KeyEvent.KEYCODE_DPAD_UP -> GamepadButton.DPAD_UP
    KeyEvent.KEYCODE_DPAD_DOWN -> GamepadButton.DPAD_DOWN
    KeyEvent.KEYCODE_DPAD_LEFT -> GamepadButton.DPAD_LEFT
    KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadButton.DPAD_RIGHT
    else -> null
}
