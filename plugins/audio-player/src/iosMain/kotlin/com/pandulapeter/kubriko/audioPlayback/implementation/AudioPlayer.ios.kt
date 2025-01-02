package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private var musicPlayer: AVAudioPlayer? = null
    private var previousAudioReference: AVAudioPlayer? = null
    private var shouldPlayMusic = false

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        stopMusic()
        shouldPlayMusic = true
        coroutineScope.launch(Dispatchers.Default) {
            musicPlayer = AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                prepareToPlay()
            }
            if (shouldPlayMusic) {
                resumeMusic()
            }
        }
    }

    override fun resumeMusic() {
        shouldPlayMusic = true
        musicPlayer?.run {
            if (!isPlaying()) {
                play()
            }
        }
    }

    override fun pauseMusic() {
        shouldPlayMusic = false
        musicPlayer?.run {
            if (isPlaying()) {
                pause()
            }
        }
    }

    override fun stopMusic() {
        shouldPlayMusic = false
        musicPlayer?.stop()
        musicPlayer = null
    }

    override fun playSound(uri: String) {
        coroutineScope.launch(Dispatchers.Default) {
            previousAudioReference = AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                prepareToPlay()
                play()
            }
        }

    }

    override fun dispose() {
        stopMusic()
        previousAudioReference = null
    }
}