/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.collections.immutable.PersistentMap
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
    abstract val pressedPointerPositions: StateFlow<PersistentMap<PointerId, Offset>>
    abstract val hoveringPointerPosition: StateFlow<Offset?>

    // TODO: Only works on Desktop. On macOS it requires accessibility permission
    abstract fun tryToMoveHoveringPointer(offset: Offset) : Boolean

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