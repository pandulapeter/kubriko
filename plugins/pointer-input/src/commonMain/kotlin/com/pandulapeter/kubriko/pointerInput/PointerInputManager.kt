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
 * Manager responsible for handling pointer input (touch, mouse, etc.).
 *
 * It tracks the positions of all pressed and hovering pointers.
 */
sealed class PointerInputManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "PointerInputManager",
) {
    /**
     * Map of currently pressed pointer IDs to their positions in screen pixels.
     */
    abstract val pressedPointerPositions: StateFlow<PersistentMap<PointerId, Offset>>

    /**
     * The position of the hovering pointer in screen pixels, if any.
     */
    abstract val hoveringPointerPosition: StateFlow<Offset?>

    /**
     * Attempts to move the hovering pointer to the specified [offset].
     *
     * Note: This only works on Desktop and might require special permissions.
     *
     * @return True if the pointer was moved, false otherwise.
     */
    abstract fun tryToMoveHoveringPointer(offset: Offset) : Boolean

    companion object {
        /**
         * Creates a new [PointerInputManager] instance.
         *
         * @param isActiveAboveViewport Whether the manager should receive input even when the pointer is outside the viewport.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
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
