/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.manager.Manager

/**
 * Manager responsible for handling keyboard input.
 *
 * It tracks the state of all keys on the keyboard.
 *
 * Key events come from a platform listener, not from Compose's focus system: on Desktop, Web and iOS every key is
 * heard, including keys typed into text fields; on Android only keys no view consumed (Compose focus navigation and
 * focused clickables may consume arrows, Tab, Enter and Space).
 */
sealed class KeyboardInputManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "KeyboardInputManager",
) {

    /**
     * Returns true if the specified [key] is currently pressed.
     */
    abstract fun isKeyPressed(key: Key): Boolean

    companion object {
        /**
         * Creates a new [KeyboardInputManager] instance.
         *
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): KeyboardInputManager = KeyboardInputManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}
