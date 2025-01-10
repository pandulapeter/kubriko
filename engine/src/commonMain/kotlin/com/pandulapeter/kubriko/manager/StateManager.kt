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