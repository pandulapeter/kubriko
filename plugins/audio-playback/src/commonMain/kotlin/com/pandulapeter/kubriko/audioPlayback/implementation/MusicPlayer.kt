package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

internal interface MusicPlayer {

    suspend fun preload(uri: String): Any?

    suspend fun play(music: Any, shouldLoop: Boolean)

    fun isPlaying(music: Any): Boolean

    fun pause(music: Any)

    suspend fun stop(music: Any)

    suspend fun dispose(music: Any)

    suspend fun dispose()
}

@Composable
internal expect fun createMusicPlayer(coroutineScope: CoroutineScope): MusicPlayer