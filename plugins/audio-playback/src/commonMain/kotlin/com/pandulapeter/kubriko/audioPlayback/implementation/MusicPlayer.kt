package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

internal interface MusicPlayer {

    suspend fun preload(uri: String): Any?

    suspend fun play(cachedMusic: Any, shouldLoop: Boolean)

    fun isPlaying(cachedMusic: Any): Boolean

    fun pause(cachedMusic: Any)

    fun stop(cachedMusic: Any)

    fun dispose(cachedMusic: Any)

    fun dispose()
}

@Composable
internal expect fun createMusicPlayer(coroutineScope: CoroutineScope): MusicPlayer