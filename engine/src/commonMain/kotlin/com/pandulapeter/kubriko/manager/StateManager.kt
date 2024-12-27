package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.implementation.getDefaultFocusDebounce
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class StateManager : Manager() {

    abstract val isFocused: StateFlow<Boolean>
    abstract val isRunning: StateFlow<Boolean>

    abstract fun updateIsRunning(isRunning: Boolean)

    companion object {
        fun newInstance(
            shouldAutoStart: Boolean = true,
            focusDebounce: Long = getDefaultFocusDebounce(),
        ): StateManager = StateManagerImpl(
            shouldAutoStart = shouldAutoStart,
            focusDebounce = focusDebounce,
        )
    }
}