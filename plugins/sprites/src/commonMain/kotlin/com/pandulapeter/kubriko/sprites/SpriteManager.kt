package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.DrawableResource

/**
 * TODO: Documentation
 */
sealed class SpriteManager : Manager() {

    abstract fun getLoadingProgress(drawableResources: Collection<DrawableResource>): Flow<Float>

    abstract fun loadSprite(drawableResource: DrawableResource): ImageBitmap?

    abstract fun unloadSprite(drawableResource: DrawableResource)

    companion object {
        fun newInstance(): SpriteManager = SpriteManagerImpl()
    }
}