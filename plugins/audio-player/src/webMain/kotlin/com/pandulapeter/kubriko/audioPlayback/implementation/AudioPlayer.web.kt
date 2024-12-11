package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Audio
import org.w3c.dom.HTMLAudioElement

@Composable
internal actual fun rememberAudioPlayer(): AudioPlayer {
    val coroutineScope = rememberCoroutineScope()
    return remember {
        object : AudioPlayer {
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
                audio = Audio(uri).apply {
                    loop = shouldLoop
                    play()
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
                audio?.pause()
            }

            override fun stopMusic() {
                audio?.currentTime = 0.0
                audio?.unload()
                audio = null
            }

            override fun preloadSounds(uris: Collection<String>) = uris.forEach(::preloadSound)

            override fun playSound(uri: String) {
                audioElements[uri].let { audioElement ->
                    if (audioElement == null) {
                        coroutineScope.launch {
                            preloadSound(uri)
                            do {
                                delay(50)
                            } while (audioElements[uri] == null)
                        }
                        playSound(uri)
                    } else {
                        audioElement.play()
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
                pause()
                src = ""
                remove()
            }
        }
    }
}