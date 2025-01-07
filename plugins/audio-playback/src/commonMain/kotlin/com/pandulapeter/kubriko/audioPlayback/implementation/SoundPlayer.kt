package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable

internal interface SoundPlayer {

    suspend fun preload(uri: String) : Any?

    suspend fun play(cachedSound: Any)

    fun dispose(cachedSound: Any)

    fun dispose()
}

@Composable
internal expect fun createSoundPlayer(maximumSimultaneousStreamsOfTheSameSound: Int): SoundPlayer