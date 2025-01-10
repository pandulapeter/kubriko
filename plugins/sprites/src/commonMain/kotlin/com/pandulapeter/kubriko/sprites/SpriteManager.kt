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
) : Manager(isLoggingEnabled, instanceNameForLogging) {

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