package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.net.URI

@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.IO) {
        DesktopMusicPlayer(
            inputStream = URI(uri).let { resolvedUri ->
                if (resolvedUri.isAbsolute) {
                    resolvedUri.toURL().openStream()
                } else {
                    FileInputStream(resolvedUri.toString())
                }
            },
        )
    }

    override suspend fun play(cachedMusic: Any, shouldLoop: Boolean) {
        (cachedMusic as DesktopMusicPlayer).play(coroutineScope, shouldLoop)
    }

    override fun isPlaying(cachedMusic: Any) = (cachedMusic as DesktopMusicPlayer).isPlaying

    override fun pause(cachedMusic: Any) {
        (cachedMusic as DesktopMusicPlayer).pause()
    }

    override fun stop(cachedMusic: Any) {
        (cachedMusic as DesktopMusicPlayer).stop()
    }

    override fun dispose(cachedMusic: Any) {
        (cachedMusic as DesktopMusicPlayer).dispose()
    }

    override fun dispose() = Unit
}