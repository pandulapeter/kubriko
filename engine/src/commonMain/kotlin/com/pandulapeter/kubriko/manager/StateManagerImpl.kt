/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

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
    override val isFocused by autoInitializingLazy { _isFocused.debounce(focusDebounce).asStateFlow(true) }
    private val _isRunning = MutableStateFlow(false)
    override val isRunning by autoInitializingLazy {
        combine(isFocused, _isRunning) { isFocused, isRunning -> isFocused && isRunning }.asStateFlow(false)
    }

    fun updateFocus(isFocused: Boolean) = _isFocused.update { isFocused }

    override fun updateIsRunning(isRunning: Boolean) = _isRunning.update { isRunning }
}