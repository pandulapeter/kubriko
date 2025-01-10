package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.implementation.getDefaultFocusDebounce
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class StateManager(isLoggingEnabled: Boolean) : Manager(isLoggingEnabled) {

    abstract val isFocused: StateFlow<Boolean>
    abstract val isRunning: StateFlow<Boolean>

    abstract fun updateIsRunning(isRunning: Boolean)

    companion object {
        fun newInstance(
            shouldAutoStart: Boolean = true,
            focusDebounce: Long = getDefaultFocusDebounce(),
            isLoggingEnabled: Boolean = false,
        ): StateManager = StateManagerImpl(
            shouldAutoStart = shouldAutoStart,
            focusDebounce = focusDebounce,
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}