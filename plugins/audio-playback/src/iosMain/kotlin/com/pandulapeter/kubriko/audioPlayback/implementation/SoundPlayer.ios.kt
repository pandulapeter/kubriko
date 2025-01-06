package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        buildList {
            repeat(maximumSimultaneousStreamsOfTheSameSound) {
                add(
                    AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                        prepareToPlay()
                    }
                )
            }
        }
    }

    override suspend fun play(sound: Any) = withContext(Dispatchers.Default) {
        sound as List<AVAudioPlayer>
        var wasSoundPlayed: Boolean
        do {
            wasSoundPlayed = sound.firstOrNull { !it.playing }?.play() == true
        } while (!wasSoundPlayed)
    }

    override suspend fun dispose(sound: Any) {
        sound as List<AVAudioPlayer>
        sound.forEach {
            if (it.isPlaying()) {
                it.stop()
            }
        }
    }

    override suspend fun dispose() = Unit
}