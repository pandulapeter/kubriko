/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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