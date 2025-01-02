package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class SpriteManager : Manager() {

    // TODO: Add ability to preload sprites

    abstract fun loadSprite(uri: String): ImageBitmap?

    abstract fun unloadSprite(uri: String)

    companion object {
        fun newInstance(): SpriteManager = SpriteManagerImpl()
    }
}