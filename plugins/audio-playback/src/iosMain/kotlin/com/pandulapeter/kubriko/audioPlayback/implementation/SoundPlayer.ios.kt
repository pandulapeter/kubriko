package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun createSoundPlayer() = object : SoundPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
            prepareToPlay()
        }
    }

    override suspend fun play(sound: Any) = withContext(Dispatchers.Default) {
        sound as AVAudioPlayer
        var wasSoundPlayed: Boolean
        do {
            wasSoundPlayed = sound.play()
        } while (!wasSoundPlayed)
    }

    override suspend fun dispose(sound: Any) {
        sound as AVAudioPlayer
        if (sound.isPlaying()) {
            sound.stop()
        }
    }

    override suspend fun dispose() = Unit
}