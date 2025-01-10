package com.pandulapeter.kubriko.shaders

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class ShaderManager(isLoggingEnabled: Boolean) : Manager(isLoggingEnabled) {

    abstract val areShadersSupported: Boolean

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
        ): ShaderManager = ShaderManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}