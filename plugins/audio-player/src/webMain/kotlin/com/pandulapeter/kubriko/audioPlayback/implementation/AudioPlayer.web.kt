package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Audio
import org.w3c.dom.HTMLAudioElement

internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private var audio: Audio? = null

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        stopMusic()
        coroutineScope.launch(Dispatchers.Default) {
            audio = Audio(uri).apply {
                loop = shouldLoop
            }
            audio?.play()
        }
    }

    override fun resumeMusic() {
        audio?.run {
            if (paused) {
                play()
            }
        }
    }

    override fun pauseMusic() {
        audio?.run {
            if (!paused) {
                pause()
            }
        }
    }

    override fun stopMusic() {
        audio?.run {
            currentTime = 0.0
            unload()
            audio = null
        }
    }

    override fun playSound(uri: String) {
        coroutineScope.launch(Dispatchers.Default) {
            (document.createElement("audio") as HTMLAudioElement).apply {
                src = uri
            }.play()
        }
    }

    override fun dispose() {
        stopMusic()
    }

    private fun HTMLAudioElement.unload() {
        if (!paused) {
            pause()
        }
        src = ""
        remove()
    }
}