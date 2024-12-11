package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Audio
import org.w3c.dom.HTMLAudioElement

internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private val audioElements = mutableMapOf<String, HTMLAudioElement>()
    private var audio: Audio? = null

    private fun preloadSound(uri: String) {
        coroutineScope.launch(Dispatchers.Default) {
            if (audioElements[uri] == null) {
                audioElements[uri] = (document.createElement("audio") as HTMLAudioElement).apply {
                    src = uri
                }
            }
        }
    }

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

    override fun preloadSounds(uris: Collection<String>) = uris.forEach(::preloadSound)

    override fun playSound(uri: String) {
        audioElements[uri].let { audioElement ->
            if (audioElement == null) {
                coroutineScope.launch(Dispatchers.Default) {
                    preloadSound(uri)
                    do {
                        delay(50)
                    } while (audioElements[uri] == null)
                    playSound(uri)
                }
            } else {
                coroutineScope.launch(Dispatchers.Default) {
                    audioElement.play()
                }
            }
        }
    }

    override fun unloadSounds(uris: Collection<String>) = uris.forEach { uri ->
        audioElements[uri]?.unload()
        audioElements.remove(uri)
    }

    override fun dispose() {
        stopMusic()
        audioElements.values.forEach { it.unload() }
        audioElements.clear()
    }

    private fun HTMLAudioElement.unload() {
        if (!paused) {
            pause()
        }
        src = ""
        remove()
    }
}