package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class CollisionManager(isLoggingEnabled: Boolean) : Manager(isLoggingEnabled) {

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
        ): CollisionManager = CollisionManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}