package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.sprites.implementation.SpriteManagerImpl
import com.pandulapeter.kubriko.manager.Manager
import org.jetbrains.compose.resources.DrawableResource

/**
 * TODO: Documentation
 */
abstract class SpriteManager : Manager() {

    // TODO: Add ability to preload sprites

    abstract fun loadSprite(drawableResource: DrawableResource): ImageBitmap?

    abstract fun unloadSprite(drawableResource: DrawableResource)

    companion object {
        fun newInstance(): SpriteManager = SpriteManagerImpl()
    }
}