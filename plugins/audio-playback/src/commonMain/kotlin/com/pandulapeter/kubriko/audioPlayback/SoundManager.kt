package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.Flow

/**
 * TODO: Documentation
 */
// TODO: Add API to control the volume
sealed class SoundManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(isLoggingEnabled, instanceNameForLogging) {

    abstract fun getLoadingProgress(uris: Collection<String>): Flow<Float>

    abstract fun preload(vararg uris: String)

    abstract fun preload(uris: Collection<String>)

    abstract fun play(uri: String)

    abstract fun unload(uri: String)

    companion object {
        fun newInstance(
            maximumSimultaneousStreamsOfTheSameSound: Int = 5,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): SoundManager = SoundManagerImpl(
            maximumSimultaneousStreamsOfTheSameSound = maximumSimultaneousStreamsOfTheSameSound,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}