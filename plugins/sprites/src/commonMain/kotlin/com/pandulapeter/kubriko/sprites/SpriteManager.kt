/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.DrawableResource

/**
 * Manager responsible for loading and caching sprite images.
 *
 * It handles the loading of [DrawableResource]s and [SpriteResource]s into [ImageBitmap]s.
 *
 * The first time a resource is requested, it is registered for loading.
 * To ensure a sprite is available immediately, use the [preload] or [preloadSprites] functions.
 *
 * @param isLoggingEnabled Whether to enable logging for this manager.
 * @param instanceNameForLogging Optional name for logging purposes.
 */
sealed class SpriteManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "SpriteManager",
) {

    /**
     * Returns a [Flow] representing the cumulative loading progress of the specified [drawableResources].
     */
    abstract fun getLoadingProgress(drawableResources: Collection<DrawableResource>): Flow<Float>

    /**
     * Preloads the specified [drawableResources] into memory.
     */
    abstract fun preload(vararg drawableResources: DrawableResource)

    /**
     * Preloads the specified [drawableResources] into memory.
     */
    abstract fun preload(drawableResources: Collection<DrawableResource>)

    /**
     * Retrieves the [ImageBitmap] for the given [drawableResource].
     * Returns null if the resource is not yet loaded.
     */
    abstract fun get(drawableResource: DrawableResource): ImageBitmap?

    /**
     * Unloads the specified [drawableResource] from memory.
     */
    abstract fun unload(drawableResource: DrawableResource)

    /**
     * Returns a [Flow] representing the cumulative loading progress of the specified sprite [resources].
     */
    abstract fun getSpriteLoadingProgress(resources: Collection<SpriteResource>): Flow<Float>

    /**
     * Preloads the specified sprite [resources] into memory.
     */
    abstract fun preloadSprites(vararg resources: SpriteResource)

    /**
     * Preloads the specified sprite [resources] into memory.
     */
    abstract fun preloadSprites(resources: Collection<SpriteResource>)

    /**
     * Retrieves the [ImageBitmap] for the given sprite [resource].
     * Returns null if the resource is not yet loaded.
     */
    abstract fun get(resource: SpriteResource): ImageBitmap?

    /**
     * Unloads the specified sprite [resource] from memory.
     */
    abstract fun unload(resource: SpriteResource)

    companion object {
        /**
         * Creates a new [SpriteManager] instance.
         *
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): SpriteManager = SpriteManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}
