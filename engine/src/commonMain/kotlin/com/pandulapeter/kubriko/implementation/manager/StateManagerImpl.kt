package com.pandulapeter.kubriko.implementation.manager

import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class StateManagerImpl : StateManager() {

    private val _isFocused = MutableStateFlow(false)
    override val isFocused = _isFocused.asStateFlow()
    private val _isRunning = MutableStateFlow(false)
    override val isRunning by lazy {
        combine(
            isFocused,
            _isRunning
        ) { isFocused, isRunning ->
            isFocused && isRunning
        }.stateIn(scope, SharingStarted.Eagerly, false)
    }

    fun updateFocus(
        isFocused: Boolean,
    ) = _isFocused.update { isFocused }

    override fun updateIsRunning(
        isRunning: Boolean,
    ) = _isRunning.update { isRunning }
}