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
 * TODO: Documentation
 *
 * The first time get is called for a resource it is registered, but it won't be loaded until the SpriteManager Composable
 * has been invoked. This means that the first get() will return null unless the resource was previously registered with
 * a preload() call. So if you require the size of the sprite to be known when, eg, adding an actor, ensure that the
 * asset has been preloaded. If you're only accessing it in draw() then preloading is less necessary.
 *
 * To load a resource with a rotation, eg if your asset points upwards but your game's coordinates system points to the right,
 * you can use the SpriteResource data class and associated SpriteManager functions.
 *
 * Each Rotation of a resource is handled separately when getting, preloading and unloading sprites.
 *
 * DrawableResource parameters are functionally the same as SpriteResource parameters created with Rotation.NONE for all
 * function calls, and can be used interchangeably.
 */
sealed class SpriteManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "SpriteManager",
) {

    abstract fun getLoadingProgress(drawableResources: Collection<DrawableResource>): Flow<Float>

    abstract fun preload(vararg drawableResources: DrawableResource)

    abstract fun preload(drawableResources: Collection<DrawableResource>)

    abstract fun get(drawableResource: DrawableResource): ImageBitmap?

    abstract fun unload(drawableResource: DrawableResource)

    abstract fun getSpriteLoadingProgress(resources: Collection<SpriteResource>): Flow<Float>

    abstract fun preloadSprites(vararg resources: SpriteResource)

    abstract fun preloadSprites(resources: Collection<SpriteResource>)

    abstract fun get(resource: SpriteResource): ImageBitmap?

    abstract fun unload(resource: SpriteResource)

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): SpriteManager = SpriteManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}
