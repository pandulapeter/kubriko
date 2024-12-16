package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private var musicPlayer: AVAudioPlayer? = null

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        stopMusic()
        coroutineScope.launch(Dispatchers.Default) {
            musicPlayer = AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                prepareToPlay()
                play()
            }
        }
    }

    override fun resumeMusic() {
        musicPlayer?.run {
            if (!isPlaying()) {
                play()
            }
        }
    }

    override fun pauseMusic() {
        musicPlayer?.run {
            if (isPlaying()) {
                pause()
            }
        }
    }

    override fun stopMusic() {
        musicPlayer?.stop()
        musicPlayer = null
    }


    override fun playSound(uri: String) {
        coroutineScope.launch(Dispatchers.Default) {
            AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                prepareToPlay()
                play()
            }
        }

    }

    override fun dispose() = stopMusic()
}