/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class PointerInputManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "PointerInputManager",
) {
    abstract val isPointerPressed: StateFlow<Boolean>
    abstract val pointerScreenOffset: StateFlow<Offset?>

    // TODO: Only works on Desktop. On macOS it requires accessibility permission
    abstract fun movePointer(offset: Offset): Boolean

    companion object {
        fun newInstance(
            isActiveAboveViewport: Boolean = false,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): PointerInputManager = PointerInputManagerImpl(
            isActiveAboveViewport = isActiveAboveViewport,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}