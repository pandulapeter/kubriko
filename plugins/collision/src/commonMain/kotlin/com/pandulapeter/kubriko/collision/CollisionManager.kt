package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class CollisionManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(isLoggingEnabled, instanceNameForLogging) {

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): CollisionManager = CollisionManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}