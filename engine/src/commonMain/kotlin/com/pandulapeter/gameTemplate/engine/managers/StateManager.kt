package com.pandulapeter.gameTemplate.engine.managers

import kotlinx.coroutines.flow.StateFlow

interface StateManager {
    val isFocused: StateFlow<Boolean>
    val isRunning: StateFlow<Boolean>

    fun updateIsRunning(isRunning: Boolean)
}