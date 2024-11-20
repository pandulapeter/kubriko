package com.pandulapeter.kubriko.implementation.manager

import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

internal class StateManagerImpl(
    val shouldAutoStart: Boolean,
) : StateManager() {

    private val _isFocused = MutableStateFlow(false)
    override val isFocused = _isFocused.asStateFlow()
    private val _isRunning = MutableStateFlow(false)
    override val isRunning by autoInitializingLazy {
        combine(isFocused, _isRunning) { isFocused, isRunning -> isFocused && isRunning }.asStateFlow(false)
    }

    fun updateFocus(isFocused: Boolean) = _isFocused.update { isFocused }

    override fun updateIsRunning(isRunning: Boolean) = _isRunning.update { isRunning }
}