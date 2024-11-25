package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.collision.implementation.CollisionManagerImpl
import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
abstract class CollisionManager : Manager() {

    companion object {
        fun newInstance(): CollisionManager = CollisionManagerImpl()
    }
}