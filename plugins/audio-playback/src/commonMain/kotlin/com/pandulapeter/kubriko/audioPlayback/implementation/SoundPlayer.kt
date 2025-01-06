package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable

internal interface SoundPlayer {

    suspend fun preload(uri: String) : Any?

    suspend fun play(sound: Any)

    suspend fun dispose(sound: Any)

    suspend fun dispose()
}

@Composable
internal expect fun createSoundPlayer(maximumSimultaneousStreamsOfTheSameSound: Int): SoundPlayer