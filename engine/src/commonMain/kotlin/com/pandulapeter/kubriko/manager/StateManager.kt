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

import com.pandulapeter.kubriko.implementation.getDefaultFocusDebounce
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class StateManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "StateManager",
) {
    abstract val isFocused: StateFlow<Boolean>
    abstract val isRunning: StateFlow<Boolean>

    abstract fun updateIsRunning(isRunning: Boolean)

    companion object {
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