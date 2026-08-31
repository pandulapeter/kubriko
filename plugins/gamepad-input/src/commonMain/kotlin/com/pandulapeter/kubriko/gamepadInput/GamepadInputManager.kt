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

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager responsible for handling gamepad input.
 *
 * It polls every connected gamepad once per tick and exposes their state through a layout that is the same on
 * all platforms, so a game only has to be written against [GamepadButton] and [GamepadState].
 */
sealed class GamepadInputManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "GamepadInputManager",
) {

    /**
     * The state of every gamepad slot, always [MAX_GAMEPAD_COUNT] entries long and ordered by slot index.
     *
     * The entries are reused across ticks and mutated in place, so this is a plain list rather than a
     * [StateFlow] - observe [connectedGamepadCount] to react to gamepads being plugged in or unplugged.
     */
    abstract val gamepads: List<GamepadState>

    /**
     * How many gamepads are currently connected.
     */
    abstract val connectedGamepadCount: StateFlow<Int>

    /**
     * Returns true if the specified [button] of the gamepad in the specified [gamepadIndex] slot is currently
     * pressed. Returns false for empty slots and for indices outside [MAX_GAMEPAD_COUNT].
     */
    abstract fun isButtonPressed(gamepadIndex: Int, button: GamepadButton): Boolean

    companion object {
        /**
         * The number of gamepad slots the manager tracks.
         */
        const val MAX_GAMEPAD_COUNT = 4

        /**
         * The default fraction of the range of a stick that is treated as its center.
         */
        const val DEFAULT_DEAD_ZONE = 0.15f

        /**
         * The default fraction a trigger has to be pulled to count as a pressed [GamepadButton.LEFT_TRIGGER] or
         * [GamepadButton.RIGHT_TRIGGER].
         */
        const val DEFAULT_TRIGGER_THRESHOLD = 0.5f

        /**
         * Creates a new [GamepadInputManager] instance.
         *
         * @param deadZone The fraction of the range of a stick that is treated as its center. Values within the
         * dead zone are reported as zero, and values outside it are rescaled so that the full range stays
         * reachable. Must be between 0 (inclusive) and 1 (exclusive).
         * @param triggerThreshold How far a trigger has to be pulled to count as a pressed button. Only affects
         * the digital [GamepadButton.LEFT_TRIGGER] and [GamepadButton.RIGHT_TRIGGER] values, never the analog
         * [GamepadState.leftTrigger] and [GamepadState.rightTrigger] ones.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun newInstance(
            deadZone: Float = DEFAULT_DEAD_ZONE,
            triggerThreshold: Float = DEFAULT_TRIGGER_THRESHOLD,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): GamepadInputManager = GamepadInputManagerImpl(
            deadZone = deadZone.coerceIn(0f, 0.99f),
            triggerThreshold = triggerThreshold.coerceIn(0f, 1f),
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}
