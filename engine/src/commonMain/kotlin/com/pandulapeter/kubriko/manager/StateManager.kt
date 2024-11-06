package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.implementation.manager.StateManagerImpl
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
abstract class StateManager : Manager() {

    abstract val isFocused: StateFlow<Boolean>
    abstract val isRunning: StateFlow<Boolean>

    abstract fun updateIsRunning(isRunning: Boolean)

    companion object {
        fun newInstance(
            shouldAutoStart: Boolean = true,
        ): StateManager = StateManagerImpl(
            shouldAutoStart = shouldAutoStart,
        )
    }
}