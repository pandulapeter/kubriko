/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager responsible for handling collision detection between [Collidable] actors.
 *
 * [CollisionDetector] instances receive callbacks about overlaps.
 */
sealed class CollisionManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "CollisionManager",
) {

    /**
     * The [Collidable] actors currently present in the scene, kept in sync with the active actors.
     *
     * Exposed so game code can run its own queries against the world (collecting obstacle masks for
     * [com.pandulapeter.kubriko.collision.extensions.slidingMovement], testing whether a spawn point is
     * clear, etc.) without re-deriving the list from the actor list itself.
     */
    abstract val collidables: StateFlow<ImmutableList<Collidable>>

    /**
     * The [CollisionDetector] actors currently present in the scene, kept in sync with the active actors.
     */
    abstract val collisionDetectors: StateFlow<ImmutableList<CollisionDetector>>

    companion object {
        /**
         * Creates a new [CollisionManager] instance.
         *
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name to use for this instance in log messages.
         */
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): CollisionManager = CollisionManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}