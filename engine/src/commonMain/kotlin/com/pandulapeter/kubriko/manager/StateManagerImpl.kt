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

import com.pandulapeter.kubriko.implementation.SyncStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update

@OptIn(FlowPreview::class)
internal class StateManagerImpl(
    val shouldAutoStart: Boolean,
    focusDebounce: Long,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : StateManager(isLoggingEnabled, instanceNameForLogging) {

    private val _isFocused = MutableStateFlow(true)
    override val isFocused by autoInitializingLazy {
        _isFocused.debounce(focusDebounce).asStateFlowOnMainThread(true)
    }
    private val _isRunning = MutableStateFlow(false)
    override val isRunning by autoInitializingLazy {
        val combinedFlow = combine(isFocused, _isRunning) { focused, running ->
            focused && running
        }.asStateFlowOnMainThread(false)
        SyncStateFlow(combinedFlow) {
            isFocused.value && _isRunning.value
        }
    }

    fun updateFocus(isFocused: Boolean) = _isFocused.update { isFocused }

    override fun updateIsRunning(isRunning: Boolean) = _isRunning.update { isRunning }
}