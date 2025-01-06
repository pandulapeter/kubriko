package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL
import platform.darwin.NSIntegerMax

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
            prepareToPlay()
        }
    }

    override suspend fun play(music: Any, shouldLoop: Boolean) {
        music as AVAudioPlayer
        music.setNumberOfLoops(if (shouldLoop) NSIntegerMax else 0)
        if (!music.isPlaying()) {
            music.play()
        }
    }

    override fun isPlaying(music: Any) = (music as AVAudioPlayer).isPlaying()

    override fun pause(music: Any) {
        music as AVAudioPlayer
        if (music.isPlaying()) {
            music.pause()
        }
    }

    override suspend fun stop(music: Any) {
        music as AVAudioPlayer
        if (music.isPlaying()) {
            music.stop()
        }
    }

    override suspend fun dispose(music: Any) = stop(music)

    override suspend fun dispose() = Unit
}