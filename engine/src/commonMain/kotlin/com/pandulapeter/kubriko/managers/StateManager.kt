package com.pandulapeter.kubriko.managers

import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
interface StateManager {
    val isFocused: StateFlow<Boolean>
    val isRunning: StateFlow<Boolean>

    fun updateIsRunning(isRunning: Boolean)
}