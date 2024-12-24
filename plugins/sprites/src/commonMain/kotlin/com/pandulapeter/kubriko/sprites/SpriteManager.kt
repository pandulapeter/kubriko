package com.pandulapeter.kubriko.sprites

import com.pandulapeter.kubriko.sprites.implementation.SpriteManagerImpl
import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
abstract class SpriteManager : Manager() {

    companion object {
        fun newInstance(): SpriteManager = SpriteManagerImpl()
    }
}