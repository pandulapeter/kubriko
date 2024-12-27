package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class CollisionManager : Manager() {

    companion object {
        fun newInstance(): CollisionManager = CollisionManagerImpl()
    }
}