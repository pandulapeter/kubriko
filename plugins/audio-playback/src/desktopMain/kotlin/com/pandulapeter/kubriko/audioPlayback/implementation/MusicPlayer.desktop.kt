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

    override suspend fun play(music: Any, shouldLoop: Boolean) {
        (music as DesktopMusicPlayer).play(coroutineScope, shouldLoop)
    }

    override fun isPlaying(music: Any) = (music as DesktopMusicPlayer).isPlaying

    override fun pause(music: Any) {
        (music as DesktopMusicPlayer).pause()
    }

    override suspend fun stop(music: Any) {
        (music as DesktopMusicPlayer).stop()
    }

    override suspend fun dispose(music: Any) {
        (music as DesktopMusicPlayer).close()
    }

    override suspend fun dispose() = audioDevice.flush()
}