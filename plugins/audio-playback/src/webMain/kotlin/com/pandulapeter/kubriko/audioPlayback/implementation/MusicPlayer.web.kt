package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        suspendCoroutine { continuation ->
            WebMusicPlayer(
                scope = coroutineScope,
                uri = uri,
                onPreloadReady = { continuation.resume(it) },
            )
        }
    }

    override suspend fun play(music: Any, shouldLoop: Boolean) = withContext(Dispatchers.Default) {
        (music as WebMusicPlayer).run {
            if (!isPlaying) {
                play(shouldLoop)
            }
        }
    }

    override fun isPlaying(music: Any) = (music as WebMusicPlayer).isPlaying

    override fun pause(music: Any) = (music as WebMusicPlayer).pause()

    override suspend fun stop(music: Any) = (music as WebMusicPlayer).stop()

    override suspend fun dispose(music: Any) = stop(music)

    override suspend fun dispose() = Unit
}