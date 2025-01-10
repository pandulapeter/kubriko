package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class ParticleManager(isLoggingEnabled: Boolean) : Manager(isLoggingEnabled) {

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
        ): ParticleManager = ParticleManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}