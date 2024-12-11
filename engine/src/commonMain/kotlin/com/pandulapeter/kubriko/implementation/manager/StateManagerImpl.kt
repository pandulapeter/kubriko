package com.pandulapeter.kubriko.implementation.manager

import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update

@OptIn(FlowPreview::class)
internal class StateManagerImpl(
    val shouldAutoStart: Boolean,
) : StateManager() {

    private val _isFocused = MutableStateFlow(true)
    override val isFocused by autoInitializingLazy { _isFocused.debounce(300).asStateFlow(true) }
    private val _isRunning = MutableStateFlow(false)
    override val isRunning by autoInitializingLazy {
        combine(isFocused, _isRunning) { isFocused, isRunning -> isFocused && isRunning }.asStateFlow(false)
    }

    fun updateFocus(isFocused: Boolean) = _isFocused.update { isFocused }

    override fun updateIsRunning(isRunning: Boolean) = _isRunning.update { isRunning }
}