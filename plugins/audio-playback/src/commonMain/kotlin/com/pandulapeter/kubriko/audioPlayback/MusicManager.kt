package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.Flow

/**
 * TODO: Documentation
 */
// TODO: Add API to control the volume
sealed class MusicManager(isLoggingEnabled: Boolean) : Manager(isLoggingEnabled) {

    abstract fun getLoadingProgress(uris: Collection<String>): Flow<Float>

    abstract fun preload(vararg uris: String)

    abstract fun preload(uris: Collection<String>)

    abstract fun isPlaying(uri: String) : Boolean

    abstract fun play(uri: String, shouldLoop: Boolean = true)

    abstract fun pause(uri: String)

    abstract fun stop(uri: String)

    abstract fun unload(uri: String)

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
        ): MusicManager = MusicManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}