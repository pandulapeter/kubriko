/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.helpers.ViewportFrameTickSource
import com.pandulapeter.kubriko.implementation.getDefaultFocusDebounce
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the global state of the game engine.
 * This includes tracking whether the game window is focused and whether the game loop is running.
 */
sealed class StateManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "StateManager",
) {
    /**
     * Whether the game window or viewport currently has focus.
     * The value is automatically updated by the engine.
     */
    abstract val isFocused: StateFlow<Boolean>

    /**
     * Whether the game loop is currently running.
     * When false, actors and managers do not receive updates.
     * The value can be updated by the `updateIsRunning()` function,
     * but only for the focused state. If isFocused is false, isRunning
     * will always be false as well.
     *
     * See the `shouldUpdateActorsWhileNotRunning` property of [ActorManager].
     * See the `shouldPauseOnFocusLoss` property of [ViewportFrameTickSource].
     */
    abstract val isRunning: StateFlow<Boolean>

    /**
     * Starts or pauses the game loop.
     *
     * @param isRunning True to start the game, false to pause it.
     */
    abstract fun updateIsRunning(isRunning: Boolean)

    companion object {
        /**
         * Creates a new [StateManager] instance.
         *
         * @param shouldAutoStart Whether the game should start running immediately after initialization.
         * @param focusDebounce The delay in milliseconds before focus changes are applied.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun newInstance(
            shouldAutoStart: Boolean = true,
            focusDebounce: Long = getDefaultFocusDebounce(),
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): StateManager = StateManagerImpl(
            shouldAutoStart = shouldAutoStart,
            focusDebounce = focusDebounce,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}