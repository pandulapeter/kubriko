package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import javazoom.jl.player.FactoryRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.net.URI

@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {

    private val audioDevice by lazy { FactoryRegistry.systemRegistry().createAudioDevice() }

    override suspend fun preload(uri: String) = withContext(Dispatchers.IO) {
        DesktopMusicPlayer(
            inputStream = URI(uri).let { resolvedUri ->
                if (resolvedUri.isAbsolute) {
                    resolvedUri.toURL().openStream()
                } else {
                    FileInputStream(resolvedUri.toString())
                }
            },
            audioDevice = audioDevice,
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
        (cachedMusic as DesktopMusicPlayer).close()
    }

    override fun dispose() = audioDevice.flush()
}