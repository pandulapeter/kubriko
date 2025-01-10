package com.pandulapeter.kubriko.shaders

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class ShaderManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(isLoggingEnabled, instanceNameForLogging) {

    abstract val areShadersSupported: Boolean

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): ShaderManager = ShaderManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}