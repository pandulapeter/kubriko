package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.browser.window
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

    override suspend fun play(cachedMusic: Any, shouldLoop: Boolean) = withContext(Dispatchers.Default) {
        (cachedMusic as WebMusicPlayer).play(shouldLoop)
    }

    override fun isPlaying(cachedMusic: Any) = (cachedMusic as WebMusicPlayer).isPlaying

    override fun pause(cachedMusic: Any) = (cachedMusic as WebMusicPlayer).pause()

    override fun stop(cachedMusic: Any) = (cachedMusic as WebMusicPlayer).stop()

    override fun dispose(cachedMusic: Any) {
        cachedMusic as WebMusicPlayer
        cachedMusic.dispose()
        // Sometimes on the Chrome Android the first attempt doesn't stop the music
        val handler: () -> JsAny? = {
            cachedMusic.dispose()
            null
        }
        window.setTimeout(
            handler = handler,
            timeout = 250,
        )
    }

    override fun dispose() = Unit
}