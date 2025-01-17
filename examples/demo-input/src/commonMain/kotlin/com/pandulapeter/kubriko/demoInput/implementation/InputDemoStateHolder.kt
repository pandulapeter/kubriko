/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoInput.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoInput.implementation.managers.InputDemoManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shared.StateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface InputDemoStateHolder : StateHolder

internal class InputDemoStateHolderImpl : InputDemoStateHolder {
    private val pointerInputManager = PointerInputManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val keyboardInputManager = KeyboardInputManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val inputDemoManager = InputDemoManager()
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            pointerInputManager,
            keyboardInputManager,
            inputDemoManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko = _kubriko.asStateFlow()

    override fun dispose() = kubriko.value.dispose()
}

private const val LOG_TAG = "Input"